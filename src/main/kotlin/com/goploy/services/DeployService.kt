package com.goploy.services

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue

@JsonIgnoreProperties(ignoreUnknown = true)
data class Projects(val list: List<Item>) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Item(val id: Int, val name: String, val environment: Int)
}

data class Token(val token: String)
data class Progress(val state: Int, val stage: String, val message: String)
data class Rebuild(val type: String, val token: String)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Previews(val list: List<Item>) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class Item(val token: String, val ext: String, val publisherName: String, val updateTime: String, val state: Int)
}

class DeployService {
    companion object {
        fun getList(namespaceId: Int): Projects {
            val json = HttpUtil.doGet("/deploy/getList?G-N-ID=$namespaceId")
            val common: Common<Projects> = jacksonObjectMapper().treeToValue<Common<Projects>>(json)
            return common.data
        }

        fun previews(namespaceId: Int, projectId: Int): Previews {
            val json = HttpUtil.doGet("/deploy/getPreview?G-N-ID=$namespaceId&projectId=$projectId&state=-1&page=1&rows=10")
            val common: Common<Previews> = jacksonObjectMapper().treeToValue<Common<Previews>>(json)
            return common.data
        }

        fun progress(namespaceId: Int, lastPublishToken: String): Progress {
            val json = HttpUtil.doGet("/deploy/getPublishProgress?G-N-ID=$namespaceId&lastPublishToken=$lastPublishToken")
            val common: Common<Progress> = jacksonObjectMapper().treeToValue<Common<Progress>>(json)
            return common.data
        }

        fun publish(namespaceId: Int, projectId: Int, branch: String, commit: String): Token {
            data class Data(val projectId: Int, val branch: String, val commit: String)
            val json = HttpUtil.doPost("/deploy/publish?G-N-ID=$namespaceId", Data(projectId, branch, commit))
            val common: Common<Token> = jacksonObjectMapper().treeToValue<Common<Token>>(json)
            return common.data
        }

        fun rebuild(namespaceId: Int, projectId: Int, token: String): Rebuild {
            data class Data(val projectId: Int, val token: String)
            val json = HttpUtil.doPost("/deploy/rebuild?G-N-ID=$namespaceId", Data(projectId, token))
            val common: Common<Rebuild> = jacksonObjectMapper().treeToValue<Common<Rebuild>>(json)
            return common.data
        }
    }
}