package com.example.data.settings

import com.example.data.local.ApiProfileEntity
import com.example.data.remote.GeminiApiService
import com.example.data.remote.OpenAiApiService
import java.util.UUID

/** Request/response shape a profile speaks. */
enum class ApiSpec { GEMINI, OPENAI }

val ApiSpec.displayName: String
    get() = when (this) {
        ApiSpec.GEMINI -> "Gemini"
        ApiSpec.OPENAI -> "OpenAI"
    }

val ApiSpec.defaultBaseUrl: String
    get() = when (this) {
        ApiSpec.GEMINI -> GeminiApiService.DEFAULT_BASE_URL
        ApiSpec.OPENAI -> OpenAiApiService.DEFAULT_BASE_URL
    }

val ApiSpec.defaultModel: String
    get() = when (this) {
        ApiSpec.GEMINI -> GeminiApiService.DEFAULT_MODEL
        ApiSpec.OPENAI -> OpenAiApiService.DEFAULT_MODEL
    }

/**
 * One named model API configuration. Blank [baseUrl] and [model] mean "use the
 * spec's default"; [apiKey] has no fallback and must be entered before the
 * profile can be used.
 */
data class ApiProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val apiSpec: ApiSpec = ApiSpec.GEMINI,
    val baseUrl: String = "",
    val apiKey: String = "",
    val model: String = "",
    val enabled: Boolean = true,
    val sortOrder: Int = 0
) {
    val effectiveBaseUrl: String get() = baseUrl.ifBlank { apiSpec.defaultBaseUrl }

    val effectiveModel: String get() = model.ifBlank { apiSpec.defaultModel }
}

fun ApiProfileEntity.toDomain(): ApiProfile = ApiProfile(
    id = id,
    name = name,
    // An unknown spec (older row, renamed enum) falls back rather than crashing.
    apiSpec = ApiSpec.entries.firstOrNull { it.name == apiSpec } ?: ApiSpec.GEMINI,
    baseUrl = baseUrl,
    apiKey = apiKey,
    model = model,
    enabled = enabled,
    sortOrder = sortOrder
)

fun ApiProfile.toEntity(): ApiProfileEntity = ApiProfileEntity(
    id = id,
    name = name,
    apiSpec = apiSpec.name,
    baseUrl = baseUrl,
    apiKey = apiKey,
    model = model,
    enabled = enabled,
    sortOrder = sortOrder
)
