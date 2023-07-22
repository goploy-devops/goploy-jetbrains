package com.goploy.toolwindow

import javax.swing.tree.DefaultMutableTreeNode

class NamespaceNode(userObject: String, var id: Int) : DefaultMutableTreeNode(userObject)
class ProjectNode(userObject: String, var id: Int, var environment: Int) : DefaultMutableTreeNode(userObject)