package com.example.data.remote

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
            ApiProvider.GEMINI -> {
                if (settings.geminiApiKey.isBlank()) {
                    throw MissingApiKeyException("No Gemini API key configured in Settings.")
                }
                GeminiApiService(
                    apiKey = settings.geminiApiKey,
                    model = settings.geminiModel.ifBlank { GeminiApiService.DEFAULT_MODEL },
                    baseUrl = settings.geminiBaseUrl.ifBlank { GeminiApiService.DEFAULT_BASE_URL }
                )
            }
            ApiProvider.OPENAI -> {
                if (settings.openAiApiKey.isBlank()) {
                    throw MissingApiKeyException("No OpenAI API key configured in Settings.")
                }
                OpenAiApiService(
                    apiKey = settings.openAiApiKey,
                    model = settings.openAiModel.ifBlank { OpenAiApiService.DEFAULT_MODEL },
                    baseUrl = settings.openAiBaseUrl.ifBlank { OpenAiApiService.DEFAULT_BASE_URL }
                )
            }
        }
        return service.generateContent(systemPrompt, userPrompt, responseMimeType)
    }
}
