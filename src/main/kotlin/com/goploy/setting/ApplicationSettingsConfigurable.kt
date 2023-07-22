package com.goploy.setting

import com.intellij.openapi.options.Configurable
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.*

class ApplicationSettingsConfigurable : Configurable {

    private val domainField = JBTextField()
    private val apiKeyField = JBTextField()

    override fun getDisplayName(): String {
        return "Goploy" // 在设置对话框中显示的配置页面的名称
    }

    override fun createComponent(): JComponent {
        return FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Domain: "), domainField, 1, false)
            .addLabeledComponent(JBLabel("Api key: "), apiKeyField, 1, false)
            .addComponentFillVertically(JPanel(), 0)
            .panel


    }

    override fun isModified(): Boolean {
        val settings = AppSettingsState.instance
        return !domainField.text.equals(settings.domain) || !apiKeyField.text.equals(settings.apiKey)
    }

    override fun apply() {
        val settings = AppSettingsState.instance
        settings.domain = domainField.text
        settings.apiKey = apiKeyField.text
    }

    override fun reset() {
        val settings = AppSettingsState.instance
        domainField.text = settings.domain
        apiKeyField.text = settings.apiKey
    }
}