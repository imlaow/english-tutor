package com.example.data.remote

import kotlinx.coroutines.Dispatchers
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

    private val endpoint = "${baseUrl.trimEnd('/')}/models/$model:generateContent"

    override suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("x-goog-api-key", apiKey)
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
                throw AiApiException("Gemini request failed with HTTP $status: $body")
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

    companion object {
        const val DEFAULT_MODEL = "gemini-2.5-flash"
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        private const val TIMEOUT_MS = 30_000
    }
}
