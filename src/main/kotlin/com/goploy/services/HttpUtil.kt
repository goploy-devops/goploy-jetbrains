package com.goploy.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.goploy.AppSettingsState
import java.net.HttpURLConnection
import java.net.URI
data class Common<T>(val code: Int, val message: String, val data: T)

class HttpUtil {
    companion object {
        fun doGet(url: String): JsonNode {
            val settings = AppSettingsState.instance
            val uri = URI.create(settings.domain.trim('/') + url)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.setRequestProperty("X-API-KEY", settings.apiKey)
            connection.requestMethod = "GET"
            // 发起请求并获取响应代码
            val responseCode = connection.responseCode
            println("Response Code: $responseCode")
            // 读取响应数据
            val mapper = jacksonObjectMapper()
            val jsonNode = mapper.readTree(connection.inputStream)
            connection.disconnect()
            return jsonNode
        }
        fun doPost(url: String, value: Any): JsonNode {
            val settings = AppSettingsState.instance
            val uri = URI.create(settings.domain + url)
            val connection = uri.toURL().openConnection() as HttpURLConnection
            connection.setRequestProperty("X-API-KEY", settings.apiKey)
            connection.setRequestProperty("Content-Type", "application/json")
            connection.requestMethod = "POST"

            connection.doOutput = true
            jacksonObjectMapper().writeValue(connection.outputStream, value)

            // 发起请求并获取响应代码
            val responseCode = connection.responseCode
            println("Response Code: $responseCode")
            // 读取响应数据
            val mapper = jacksonObjectMapper()
            val jsonNode = mapper.readTree(connection.inputStream)
            connection.disconnect()
            return jsonNode
        }
    }
}

