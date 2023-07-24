package com.goploy.toolwindow

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import com.goploy.services.DeployService
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.table.DefaultTableModel


class ResultDialog(private val projectName: String, private val namespaceId: Int, private val projectId: Int) : DialogWrapper(true) {
    init {
        title = projectName
        init()
    }

    override fun createCenterPanel(): JBPanel<*> {
        val table = createTable()
        val scrollPane = JBScrollPane(table)
        val panel = JBPanel<JBPanel<*>>(BorderLayout()).withPreferredSize(650, 300)
        panel.add(scrollPane)
        return panel
    }

    override fun createActions(): Array<out Action> {
        return emptyArray() // 不显示任何动作按钮
    }

    private fun createTable(): JBTable {
        val columnNames = arrayOf("Token", "Commit", "Publisher", "Time", "State", "")
        val data = DeployService.previews(namespaceId, projectId)
        val tableModel = object : DefaultTableModel(null, columnNames) {
            override fun isCellEditable(row: Int, column: Int): Boolean {
                // 返回 false 禁止所有单元格编辑
                return column == 5
            }
        }
        for (item in data.list) {
            var state = "×"
            if (item.state != 0) {
                state = "✔"
            }
            tableModel.addRow(arrayOf(item.token, item.ext.trim('"'), item.publisherName, item.updateTime, state, "Rollback"))
        }

        val table = JBTable(tableModel)
        val columnModel = table.columnModel
        columnModel.getColumn(0).preferredWidth = JBUI.scale(60)
        columnModel.getColumn(1).preferredWidth = JBUI.scale(200)
        columnModel.getColumn(3).preferredWidth = JBUI.scale(180)
        columnModel.getColumn(4).preferredWidth = JBUI.scale(60)
        columnModel.getColumn(5).preferredWidth = JBUI.scale(90)

        val delete: Action = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                println(e)
//                val jTable = e.source as JTable
                val index = Integer.valueOf(e.actionCommand)
                val res = showYesNoDialog("$projectName rollback", "Rollback to ${data.list[index].ext}", null)
                if (!res) {
                    return
                }
                val rebuildRes = DeployService.rebuild(namespaceId, projectId, data.list[index].token)

                if (rebuildRes.type == "symlink") {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("Goploy")
                        .createNotification("Rebuild $projectName Complete", NotificationType.INFORMATION)
                        .notify(null)
                    return
                }
                ProgressManager.getInstance()
                    .run(object : Task.Backgroundable(null, projectName) {
                        override fun run(indicator: ProgressIndicator) {
                            indicator.isIndeterminate = true
                            while (true) {
                                Thread.sleep(1000)
                                val progressRes = DeployService.progress(namespaceId, rebuildRes.token)
                                when (progressRes.state) {
                                    1 -> {
                                        indicator.text = progressRes.stage
                                    }

                                    0 -> {
                                        throw Exception(progressRes.message)
                                    }

                                    else -> {
                                        indicator.fraction = 1.0
                                        break
                                    }
                                }
                            }

                        }

                        override fun onSuccess() {
                            NotificationGroupManager.getInstance()
                                .getNotificationGroup("Goploy")
                                .createNotification("Rebuild $projectName Complete", NotificationType.INFORMATION)
                                .notify(null)

                        }

                        override fun onThrowable(error: Throwable) {
                            NotificationGroupManager.getInstance()
                                .getNotificationGroup("Goploy")
                                .createNotification(
                                    "Rebuild $projectName Failed: {$error.message}",
                                    NotificationType.ERROR
                                )
                                .notify(null)
                        }

                    })

            }
        }

        ButtonColumn(table, delete, 5)
        return table
    }
}

