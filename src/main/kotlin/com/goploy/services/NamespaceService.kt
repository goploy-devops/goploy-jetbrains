package com.goploy.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue

data class Option(val list: List<Item>) {
    data class Item(val namespaceId: Int, val namespaceName: String)
}

class NamespaceService {
    companion object {
        fun getOption(): Option {
            val json = HttpUtil.doGet("/namespace/getOption")
            val common: Common<Option> = jacksonObjectMapper().treeToValue<Common<Option>>(json)
            return common.data
        }
    }
}