package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Minimal REST client for the OpenAI Chat Completions API. Works with any
 * OpenAI-compatible endpoint (OpenAI, DeepSeek, local servers, ...) via the
 * configurable base URL. Uses HttpURLConnection so no extra networking
 * dependencies are required.
 */
class OpenAiApiService(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    baseUrl: String = DEFAULT_BASE_URL
) : AiModelService {

    private val endpoint = "${baseUrl.trimEnd('/')}/chat/completions"

    override suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
        }
        try {
            connection.outputStream.use { output ->
                output.write(buildRequestBody(systemPrompt, userPrompt, responseMimeType).toByteArray())
            }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val body = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                throw AiApiException("OpenAI request failed with HTTP $status: $body")
            }
            extractText(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun buildRequestBody(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String {
        val body = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", systemPrompt))
                    .put(JSONObject().put("role", "user").put("content", userPrompt))
            )
        if (responseMimeType == "application/json") {
            body.put("response_format", JSONObject().put("type", "json_object"))
        }
        return body.toString()
    }

    private fun extractText(responseBody: String): String {
        val choices = JSONObject(responseBody).optJSONArray("choices")
        if (choices == null || choices.length() == 0) {
            throw AiApiException("OpenAI response contained no choices: $responseBody")
        }
        return choices.getJSONObject(0)
            .getJSONObject("message")
            .optString("content")
    }

    companion object {
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val TIMEOUT_MS = 30_000
    }
}
