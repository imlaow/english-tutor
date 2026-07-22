package com.example.data.remote

import com.example.data.repository.ApiProfileRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

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
    ): String = activeService().generateContent(systemPrompt, userPrompt, responseMimeType)

    override fun generateContentStream(
        systemPrompt: String,
        userPrompt: String,
        responseMimeType: String?
    ): Flow<String> = flow {
        // Resolved inside the flow so the active profile is read when collection
        // starts, not when the flow is built.
        val service = activeService()
        emitAll(service.generateContentStream(systemPrompt, userPrompt, responseMimeType))
    }.flowOn(Dispatchers.IO)

    private suspend fun activeService(): AiModelService {
        val profile = apiProfileRepository.currentProfile()
            ?: throw MissingApiKeyException("No API profile configured in Settings.")
        if (profile.apiKey.isBlank()) {
            throw MissingApiKeyException("No API key configured for profile \"${profile.name}\".")
        }
        return profile.toAiService()
    }
}
