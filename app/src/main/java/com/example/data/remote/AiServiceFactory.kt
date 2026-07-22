package com.example.data.remote

import com.example.data.settings.ApiProfile
import com.example.data.settings.ApiSpec

/**
 * Builds the client a profile's [ApiSpec] speaks. Works on any [ApiProfile],
 * saved or not, so the connection test can probe a draft the user hasn't
 * committed yet.
 */
fun ApiProfile.toAiService(): AiModelService = when (apiSpec) {
    ApiSpec.GEMINI -> GeminiApiService(
        apiKey = apiKey,
        model = effectiveModel,
        baseUrl = effectiveBaseUrl
    )
    ApiSpec.OPENAI -> OpenAiApiService(
        apiKey = apiKey,
        model = effectiveModel,
        baseUrl = effectiveBaseUrl
    )
}
