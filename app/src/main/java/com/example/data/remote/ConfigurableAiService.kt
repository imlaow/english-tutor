package com.example.data.remote

import com.example.data.repository.ApiProfileRepository
import com.example.data.settings.ApiSpec

/**
 * [AiModelService] that routes each request to the API profile currently active
 * in the settings screen, so configuration changes take effect immediately
 * without recreating repositories or view models.
 */
class ConfigurableAiService(
    private val apiProfileRepository: ApiProfileRepository
) : AiModelService {

    override suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String {
        val profile = apiProfileRepository.currentProfile()
            ?: throw MissingApiKeyException("No API profile configured in Settings.")
        if (profile.apiKey.isBlank()) {
            throw MissingApiKeyException("No API key configured for profile \"${profile.name}\".")
        }
        val service = when (profile.apiSpec) {
            ApiSpec.GEMINI -> GeminiApiService(
                apiKey = profile.apiKey,
                model = profile.effectiveModel,
                baseUrl = profile.effectiveBaseUrl
            )
            ApiSpec.OPENAI -> OpenAiApiService(
                apiKey = profile.apiKey,
                model = profile.effectiveModel,
                baseUrl = profile.effectiveBaseUrl
            )
        }
        return service.generateContent(systemPrompt, userPrompt, responseMimeType)
    }
}
