package com.goploy.toolwindow

import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBList
import javax.swing.*
import java.awt.Dimension

class SearchEverywhereDialog(header: String, private val items: List<String>) : DialogWrapper(true) {

    private val searchTextField = JTextField(20)
    private val searchButton = JButton("Search")
    val searchResultsList = JBList<String>()

    init {
        title = header
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel()

        // 垂直布局
        val verticalLayout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.layout = verticalLayout

        val searchPanel = JPanel()
        searchPanel.layout = BoxLayout(searchPanel, BoxLayout.X_AXIS)
        searchPanel.add(JLabel("Enter search keyword:"))
        searchTextField.maximumSize = Dimension(300, 30)
        searchPanel.add(searchTextField)

        searchButton.addActionListener {
            val keyword = searchTextField.text
            val filteredItems = items.filter { it.contains(keyword, ignoreCase = true) }.toTypedArray()
            searchResultsList.setListData(filteredItems)
        }

        searchPanel.add(searchButton)
        panel.add(searchPanel)
        searchResultsList.setListData(items.toTypedArray())
        panel.add(JScrollPane(searchResultsList))

        return panel
    }
}

