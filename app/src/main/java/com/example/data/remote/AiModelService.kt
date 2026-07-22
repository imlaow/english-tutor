package com.example.data.remote

import kotlinx.coroutines.flow.Flow

/**
 * Common contract for chat-completion backends (Gemini, OpenAI-compatible, ...).
 */
interface AiModelService {

    /**
     * Sends [userPrompt] to the model under the given [systemPrompt] and returns
     * the model's text output. Pass responseMimeType = "application/json" to
     * force the model to emit strict JSON.
     */
    suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String? = null
    ): String

    /**
     * Same request as [generateContent], but emits the text incrementally as the
     * model produces it. Only the connection test uses this today; the chat
     * still waits for a complete reply because it parses one JSON object per turn.
     */
    fun generateContentStream(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String? = null
    ): Flow<String>
}

class AiApiException(message: String) : Exception(message)

/** Thrown before any network call when the selected provider has no API key configured. */
class MissingApiKeyException(message: String) : Exception(message)
