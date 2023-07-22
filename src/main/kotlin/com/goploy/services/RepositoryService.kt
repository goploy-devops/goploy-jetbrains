package com.goploy.services

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.treeToValue

data class Branches(val list: List<String>)

data class Commits(val list: List<Item>) {
    data class Item(
        val branch: String,
        val commit: String,
        val author: String,
        val timestamp: Int,
        val message: String,
        val tag: String,
        val diff: String,
    )
}

data class Tags(val list: List<Commits.Item>)


class RepositoryService {
    companion object {
        fun branches(namespaceId: Int, projectId: Int): Branches {
            val json = HttpUtil.doGet("/repository/getBranchList?G-N-ID=$namespaceId&id=$projectId")
            val common: Common<Branches> = jacksonObjectMapper().treeToValue<Common<Branches>>(json)
            return common.data
        }
        fun commits(namespaceId: Int, projectId: Int, branch: String): Commits {
            val json = HttpUtil.doGet("/repository/getCommitList?G-N-ID=$namespaceId&id=$projectId&branch=$branch")
            val common: Common<Commits> = jacksonObjectMapper().treeToValue<Common<Commits>>(json)
            return common.data
        }
        fun tags(namespaceId: Int, projectId: Int): Tags {
            val json = HttpUtil.doGet("/repository/getTagList?G-N-ID=$namespaceId&id=$projectId")
            val common: Common<Tags> = jacksonObjectMapper().treeToValue<Common<Tags>>(json)
            return common.data
        }
    }
}