package com.example.data.remote

import com.example.BuildConfig
import com.example.data.settings.ApiProvider
import com.example.data.settings.SettingsRepository

/**
 * [AiModelService] that routes each request to the provider currently selected
 * in the settings screen, so configuration changes take effect immediately
 * without recreating repositories or view models.
 */
class ConfigurableAiService(
    private val settingsRepository: SettingsRepository
) : AiModelService {

    override suspend fun generateContent(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): String {
        val settings = settingsRepository.settings.value
        val service = when (settings.provider) {
            ApiProvider.GEMINI -> GeminiApiService(
                apiKey = settings.geminiApiKey.ifBlank { BuildConfig.GEMINI_API_KEY },
                model = settings.geminiModel,
                baseUrl = settings.geminiBaseUrl
            )
            ApiProvider.OPENAI -> OpenAiApiService(
                apiKey = settings.openAiApiKey,
                model = settings.openAiModel,
                baseUrl = settings.openAiBaseUrl
            )
        }
        return service.generateContent(systemPrompt, userPrompt, responseMimeType)
    }
}
