package com.example.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
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
        val connection = openConnection()
        try {
            connection.writeBody(buildRequestBody(systemPrompt, userPrompt, responseMimeType))
            extractText(connection.readBodyOrThrow(PROVIDER))
        } finally {
            connection.disconnect()
        }
    }

    override fun generateContentStream(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): Flow<String> = flow {
        val connection = openConnection()
        try {
            connection.writeBody(
                buildRequestBody(systemPrompt, userPrompt, responseMimeType, stream = true)
            )
            connection.streamSseData(PROVIDER) { data ->
                extractStreamText(data)?.let { emit(it) }
            }
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun openConnection(): HttpURLConnection =
        (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Authorization", "Bearer $apiKey")
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
        }

    private fun buildRequestBody(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?,
        stream: Boolean = false
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
        if (stream) {
            body.put("stream", true)
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

    /**
     * Null for the frames that carry no text — the first chunk announces only
     * the role, and the last carries only a finish reason.
     */
    private fun extractStreamText(data: String): String? =
        JSONObject(data).optJSONArray("choices")
            ?.optJSONObject(0)
            ?.optJSONObject("delta")
            ?.optString("content")
            ?.ifEmpty { null }

    companion object {
        private const val PROVIDER = "OpenAI"
        const val DEFAULT_MODEL = "gpt-4o-mini"
        const val DEFAULT_BASE_URL = "https://api.openai.com/v1"
        private const val TIMEOUT_MS = 30_000
    }
}
