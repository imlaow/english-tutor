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
 * Minimal REST client for the Google Gemini generateContent API.
 * Uses HttpURLConnection so no extra networking dependencies are required.
 */
class GeminiApiService(
    private val apiKey: String,
    private val model: String = DEFAULT_MODEL,
    baseUrl: String = DEFAULT_BASE_URL
) : AiModelService {

    private val trimmedBaseUrl = baseUrl.trimEnd('/')
    private val endpoint = "$trimmedBaseUrl/models/$model:generateContent"
    // Gemini streams over its own endpoint; alt=sse switches it from a JSON
    // array to server-sent events, which can be read line by line.
    private val streamEndpoint = "$trimmedBaseUrl/models/$model:streamGenerateContent?alt=sse"

    override suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String = withContext(Dispatchers.IO) {
        val connection = openConnection(endpoint)
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
        val connection = openConnection(streamEndpoint)
        try {
            connection.writeBody(buildRequestBody(systemPrompt, userPrompt, responseMimeType))
            connection.streamSseData(PROVIDER) { data ->
                extractStreamText(data)?.let { emit(it) }
            }
        } finally {
            connection.disconnect()
        }
    }.flowOn(Dispatchers.IO)

    private fun openConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
            connectTimeout = TIMEOUT_MS
            readTimeout = TIMEOUT_MS
            doOutput = true
        }

    private fun buildRequestBody(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String {
        val body = JSONObject()
            .put("system_instruction", JSONObject().put("parts", textParts(systemPrompt)))
            .put(
                "contents",
                JSONArray().put(
                    JSONObject()
                        .put("role", "user")
                        .put("parts", textParts(userPrompt))
                )
            )
        if (responseMimeType != null) {
            body.put("generationConfig", JSONObject().put("responseMimeType", responseMimeType))
        }
        return body.toString()
    }

    private fun textParts(text: String): JSONArray =
        JSONArray().put(JSONObject().put("text", text))

    private fun extractText(responseBody: String): String {
        val candidates = JSONObject(responseBody).optJSONArray("candidates")
        if (candidates == null || candidates.length() == 0) {
            throw AiApiException("Gemini response contained no candidates: $responseBody")
        }
        val parts = candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
        return buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text"))
            }
        }
    }

    /**
     * Null for the frames that carry no text — a chunk holding only a finish
     * reason or safety rating is normal mid-stream and must not abort it.
     */
    private fun extractStreamText(data: String): String? {
        val parts = JSONObject(data).optJSONArray("candidates")
            ?.optJSONObject(0)
            ?.optJSONObject("content")
            ?.optJSONArray("parts")
            ?: return null
        val text = buildString {
            for (i in 0 until parts.length()) {
                append(parts.getJSONObject(i).optString("text"))
            }
        }
        return text.ifEmpty { null }
    }

    companion object {
        private const val PROVIDER = "Gemini"
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val TIMEOUT_MS = 30_000
    }
}
