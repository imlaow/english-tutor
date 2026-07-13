package com.example.data.remote

import com.example.BuildConfig
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
    private val apiKey: String = BuildConfig.GEMINI_API_KEY
) {

    /**
     * Sends [userPrompt] to Gemini under the given [systemPrompt] and returns the
     * model's text output. Pass responseMimeType = "application/json" to force
     * the model to emit strict JSON.
     */
    suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String? = null
    ): String = withContext(Dispatchers.IO) {
        val connection = (URL(ENDPOINT).openConnection() as HttpURLConnection).apply {
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
                throw GeminiApiException("Gemini request failed with HTTP $status: $body")
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
            throw GeminiApiException("Gemini response contained no candidates: $responseBody")
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

    private companion object {
        const val MODEL = "gemini-2.5-flash"
        const val ENDPOINT =
            "https://generativelanguage.googleapis.com/v1beta/models/$MODEL:generateContent"
        const val TIMEOUT_MS = 30_000
    }
}

class GeminiApiException(message: String) : Exception(message)
