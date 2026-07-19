package com.example.data.settings

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ApiProvider { GEMINI, OPENAI }

/**
 * User-configurable model API settings. Each provider keeps its own base URL,
 * API key and model so switching providers doesn't lose the other's values.
 * Blank base URL and model mean "use the provider's default"; the API key has
 * no fallback and must be entered before the provider can be used.
 */
data class ModelApiSettings(
    val provider: ApiProvider = ApiProvider.GEMINI,
    val geminiBaseUrl: String = "",
    val geminiApiKey: String = "",
    val geminiModel: String = "",
    val openAiBaseUrl: String = "",
    val openAiApiKey: String = "",
    val openAiModel: String = ""
)

/**
 * Persists [ModelApiSettings] in SharedPreferences and exposes them as a
 * StateFlow so API calls always see the latest configuration.
 */
class SettingsRepository private constructor(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(load())
    val settings: StateFlow<ModelApiSettings> = _settings.asStateFlow()

    fun save(settings: ModelApiSettings) {
        prefs.edit()
            .putString(KEY_PROVIDER, settings.provider.name)
            .putString(KEY_GEMINI_BASE_URL, settings.geminiBaseUrl)
            .putString(KEY_GEMINI_API_KEY, settings.geminiApiKey)
            .putString(KEY_GEMINI_MODEL, settings.geminiModel)
            .putString(KEY_OPENAI_BASE_URL, settings.openAiBaseUrl)
            .putString(KEY_OPENAI_API_KEY, settings.openAiApiKey)
            .putString(KEY_OPENAI_MODEL, settings.openAiModel)
            .apply()
        _settings.value = settings
    }

    private fun load(): ModelApiSettings {
        fun read(key: String): String = prefs.getString(key, null).orEmpty()
        return ModelApiSettings(
            provider = prefs.getString(KEY_PROVIDER, null)
                ?.let { name -> ApiProvider.entries.firstOrNull { it.name == name } }
                ?: ApiProvider.GEMINI,
            geminiBaseUrl = read(KEY_GEMINI_BASE_URL),
            geminiApiKey = read(KEY_GEMINI_API_KEY),
            geminiModel = read(KEY_GEMINI_MODEL),
            openAiBaseUrl = read(KEY_OPENAI_BASE_URL),
            openAiApiKey = read(KEY_OPENAI_API_KEY),
            openAiModel = read(KEY_OPENAI_MODEL)
        )
    }

    companion object {
        private const val PREFS_NAME = "model_api_settings"
        private const val KEY_PROVIDER = "provider"
        private const val KEY_GEMINI_BASE_URL = "gemini_base_url"
        private const val KEY_GEMINI_API_KEY = "gemini_api_key"
        private const val KEY_GEMINI_MODEL = "gemini_model"
        private const val KEY_OPENAI_BASE_URL = "openai_base_url"
        private const val KEY_OPENAI_API_KEY = "openai_api_key"
        private const val KEY_OPENAI_MODEL = "openai_model"

        @Volatile
        private var instance: SettingsRepository? = null

        fun getInstance(context: Context): SettingsRepository =
            instance ?: synchronized(this) {
                instance ?: SettingsRepository(context.applicationContext).also { instance = it }
            }
    }
}
