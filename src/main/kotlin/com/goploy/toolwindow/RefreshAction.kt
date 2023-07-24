package com.goploy.toolwindow

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.ui.treeStructure.Tree

class RefreshAction(private val tree: Tree) : AnAction("Refresh", "Refresh tree", AllIcons.Actions.Refresh) {
    override fun actionPerformed(e: AnActionEvent) {
        ToolWindowFactory.genNamespaceNode(tree)
    }
}