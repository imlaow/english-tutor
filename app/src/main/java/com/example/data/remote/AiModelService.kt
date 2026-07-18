package com.example.data.remote

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
}

class AiApiException(message: String) : Exception(message)
