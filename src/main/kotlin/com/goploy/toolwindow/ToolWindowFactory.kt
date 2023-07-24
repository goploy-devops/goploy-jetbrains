package com.goploy.toolwindow

import com.goploy.AppSettingsState
import com.goploy.services.DeployService
import com.goploy.services.NamespaceService
import com.goploy.services.RepositoryService
import com.intellij.icons.AllIcons
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.showYesNoDialog
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.TreeSpeedSearch
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeCellRenderer
import javax.swing.tree.DefaultTreeModel


class ToolWindowFactory : ToolWindowFactory {
    private val deployingProjects = mutableMapOf<Int, String>()

    init {
        thisLogger().warn("Don't forget to remove all non-needed sample code files with their corresponding registration entries in `plugin.xml`.")
    }

    companion object {
        fun genNamespaceNode(tree: Tree) {
            val settings = AppSettingsState.instance
            if ( settings.domain == "" || settings.apiKey == "") {
                ShowSettingsUtil.getInstance().showSettingsDialog(null, "Goploy")
            } else {
                val namespaceOption = NamespaceService.getOption()
                val rootNode = tree.model.root as DefaultMutableTreeNode
                rootNode.removeAllChildren()
                for (item in namespaceOption.list) {
                    val namespaceNode = NamespaceNode(item.namespaceName, item.namespaceId)
                    rootNode.add(namespaceNode)
                    namespaceNode.add(DefaultMutableTreeNode("fake"))
                }
                (tree.model as? DefaultTreeModel)?.reload()
            }
        }
    }

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {

        val pRoot = DefaultMutableTreeNode("fake root")
        val treeModel = DefaultTreeModel(pRoot)
        val jTree = Tree(treeModel)
        TreeSpeedSearch(jTree)
        genNamespaceNode(jTree)
        jTree.isRootVisible = false
        jTree.addTreeWillExpandListener(object : javax.swing.event.TreeWillExpandListener {
            override fun treeWillExpand(event: javax.swing.event.TreeExpansionEvent) {
                val node = event.path.lastPathComponent as NamespaceNode
                val firstChild = node.firstChild as DefaultMutableTreeNode
                if (firstChild.userObject == "fake") {
                    val projects = DeployService.getList(node.id)
                    for (item in projects.list) {
                        val child = ProjectNode(item.name, item.id, item.environment)
                        treeModel.insertNodeInto(child, node, node.childCount)
                    }
                    treeModel.removeNodeFromParent(firstChild)
                }
            }

            override fun treeWillCollapse(event: javax.swing.event.TreeExpansionEvent) {}
        })

        jTree.cellRenderer = object : DefaultTreeCellRenderer() {
            override fun getBackgroundNonSelectionColor(): Color? {
                return UIManager.getColor("Tree.background")
            }

            override fun getTreeCellRendererComponent(
                tree: JTree,
                value: Any,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ): Component {
                super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus)
                borderSelectionColor = null
                icon = if (leaf) {
                    AllIcons.Actions.Run_anything
                } else {
                    AllIcons.Actions.ModuleDirectory
                }
                return this
            }
        }

        jTree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                val path = jTree.getClosestPathForLocation(e.x, e.y)
                jTree.selectionPath = path
            }

            override fun mouseReleased(e: MouseEvent) {
                val treePath = jTree.getClosestPathForLocation(e.x, e.y)
                if (treePath != null) {
                    if (treePath.lastPathComponent is ProjectNode) {
                        if (e.isPopupTrigger || SwingUtilities.isRightMouseButton(e)) {
                            createPopupMenu(jTree).show(jTree, e.x, e.y)
                        }
                    }
                }
            }
        })

        val jBPanel = JPanel(BorderLayout())
        jBPanel.add(jTree, BorderLayout.CENTER)
        jBPanel.background = UIManager.getColor("Tree.background")
        val jBScrollPane = JBScrollPane(jBPanel)
        val content = ContentFactory.getInstance().createContent(jBScrollPane, null, false)
        toolWindow.contentManager.addContent(content)
        toolWindow.setTitleActions(listOf(RefreshAction(jTree)))
    }

    fun createPopupMenu(jTree: Tree): JPopupMenu {
        val popup = JPopupMenu()
        popup.add(runItem(jTree))
        popup.add(branchItem(jTree))
        popup.add(tagItem(jTree))
        popup.add(resultItem(jTree))
        return popup
    }

    private fun runItem(jTree: Tree): JMenuItem {
        val runItem = JMenuItem("Run", AllIcons.Actions.Execute)
        runItem.addActionListener {
            val selectedPath = jTree.selectionPath
            if (selectedPath?.lastPathComponent is ProjectNode) {
                val projectNode = selectedPath.lastPathComponent as ProjectNode
                val namespaceNode = projectNode.parent as NamespaceNode
                if (projectNode.environment == 1) {
                    val res = showYesNoDialog("Publish ${projectNode.userObject} ?", "", null)
                    if (!res) {
                        return@addActionListener
                    }
                }
                publish(projectNode, namespaceNode.id, "", "")
            }
        }
        return runItem
    }

    private fun branchItem(jTree: Tree): JMenuItem {
        val menuItem = JMenuItem("Branch", AllIcons.Vcs.Branch)

        menuItem.addActionListener {
            val selectedPath = jTree.selectionPath
            if (selectedPath?.lastPathComponent is ProjectNode) {
                val projectNode = selectedPath.lastPathComponent as ProjectNode
                val namespaceNode = projectNode.parent as NamespaceNode
                val branchRes = RepositoryService.branches(namespaceNode.id, projectNode.id)
                var dialog = SearchEverywhereDialog(
                    "Select Branch",
                    branchRes.list.filter { !it.contains("HEAD", ignoreCase = true) })
                var result = dialog.showAndGet()
                if (!result) {
                    return@addActionListener
                }
                val selectedBranch = dialog.searchResultsList.selectedValue ?: return@addActionListener
                val commitRes = RepositoryService.commits(namespaceNode.id, projectNode.id, selectedBranch)
                dialog = SearchEverywhereDialog(
                    "Select Commit",
                    commitRes.list.map { it.commit + ", " + it.author + ", " + it.message })
                result = dialog.showAndGet()
                if (!result) {
                    return@addActionListener
                }
                val selectedCommit = dialog.searchResultsList.selectedValue ?: return@addActionListener
                publish(projectNode, namespaceNode.id, selectedBranch, selectedCommit.substringBefore(","))
            }
        }
        return menuItem
    }

    private fun tagItem(jTree: Tree): JMenuItem {
        val menuItem = JMenuItem("Tag", AllIcons.Actions.AddToDictionary)
        menuItem.addActionListener {
            val selectedPath = jTree.selectionPath
            if (selectedPath?.lastPathComponent is ProjectNode) {
                val projectNode = selectedPath.lastPathComponent as ProjectNode
                val namespaceNode = projectNode.parent as NamespaceNode
                val tagRes = RepositoryService.tags(namespaceNode.id, projectNode.id)
                val dialog = SearchEverywhereDialog("Select Tag", tagRes.list.map { it.tag + ", " + it.message })
                val result = dialog.showAndGet()
                if (!result) {
                    return@addActionListener
                }
                val selectedTag = dialog.searchResultsList.selectedValue ?: return@addActionListener
                val tag = tagRes.list.find { selectedTag.substringBefore(",") == it.tag }
                val branch = tag?.branch ?: ""
                val commit = tag?.commit ?: ""
                publish(projectNode, namespaceNode.id, branch, commit)
            }
        }
        return menuItem
    }

    private fun resultItem(jTree: Tree): JMenuItem {
        val menuItem = JMenuItem("Result", AllIcons.Actions.Preview)

        menuItem.addActionListener {
            val selectedPath = jTree.selectionPath
            if (selectedPath?.lastPathComponent is ProjectNode) {
                val projectNode = selectedPath.lastPathComponent as ProjectNode
                val projectName = projectNode.userObject as String
                val namespaceNode = projectNode.parent as NamespaceNode
                val dialog = ResultDialog(projectName, namespaceNode.id, projectNode.id)
                dialog.show()
            }
        }
        return menuItem
    }

    private fun publish(projectNode: ProjectNode, namespaceId: Int, branch: String, commit: String) {
        val projectName = projectNode.userObject as String
        if (projectNode.id in deployingProjects) {
            Messages.showMessageDialog(
                null,
                "Wait for the previous task to finish.",
                projectNode.userObject as String,
                Messages.getErrorIcon()
            )
            return
        }

        val publishRes = DeployService.publish(namespaceId, projectNode.id, branch, commit)
        deployingProjects[projectNode.id] = publishRes.token

        ProgressManager.getInstance()
            .run(object : Task.Backgroundable(null, projectName) {
                override fun run(indicator: ProgressIndicator) {
                    indicator.isIndeterminate = true
                    while (true) {
                        Thread.sleep(1000)
                        val data = DeployService.progress(namespaceId, publishRes.token)
                        when (data.state) {
                            1 -> {
                                indicator.text = data.stage
                            }

                            0 -> {
                                throw Exception(data.message)
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
                        .createNotification("Publish $projectName Complete", NotificationType.INFORMATION)
                        .notify(project)

                }

                override fun onThrowable(error: Throwable) {
                    NotificationGroupManager.getInstance()
                        .getNotificationGroup("Goploy")
                        .createNotification(
                            "Publish $projectName Failed: {$error.message}",
                            NotificationType.ERROR
                        )
                        .notify(project)
                }

                override fun onFinished() {
                    deployingProjects.remove(projectNode.id)
                    println("Loading finished.")
                }
            })
    }

    override fun shouldBeAvailable(project: Project) = true

}

