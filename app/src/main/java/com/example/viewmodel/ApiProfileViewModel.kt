package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.ApiProfileRepository
import com.example.data.settings.ApiProfile
import com.example.data.settings.ApiSpec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which required field the user left blank, so the form can point at it. */
enum class ApiProfileFormError { NAME, API_KEY }

/**
 * Backs both the profile list and the new/edit form. Compose only reads state
 * and sends intents from here; it never touches [ApiProfileRepository] itself.
 */
class ApiProfileViewModel(
    private val repository: ApiProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<ApiProfile>> = repository.profiles
    val activeProfileId: StateFlow<String?> = repository.activeProfileId

    // Null until loadForEdit() resolves, which keeps the form from flashing
    // blank fields over an existing profile's values.
    private val _draft = MutableStateFlow<ApiProfile?>(null)
    val draft: StateFlow<ApiProfile?> = _draft.asStateFlow()

    private val _formError = MutableStateFlow<ApiProfileFormError?>(null)
    val formError: StateFlow<ApiProfileFormError?> = _formError.asStateFlow()

    fun setActive(id: String) = repository.setActive(id)

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** [profileId] null starts a new profile with the spec defaults. */
    fun loadForEdit(profileId: String?) {
        if (profileId == null) {
            _draft.value = ApiProfile()
            return
        }
        viewModelScope.launch {
            _draft.value = repository.getProfile(profileId) ?: ApiProfile()
        }
    }

    fun updateDraft(transform: (ApiProfile) -> ApiProfile) {
        _draft.value = _draft.value?.let(transform)
        _formError.value = null
    }

    /**
     * Trims stray whitespace and saves. Empty base URL and model stay empty and
     * fall back to the spec's defaults at request time; name and API key have no
     * fallback, so a blank one aborts the save and reports [formError].
     */
    fun save(onSaved: () -> Unit) {
        val draft = _draft.value ?: return
        val normalized = draft.copy(
            name = draft.name.trim(),
            baseUrl = draft.baseUrl.trim(),
            apiKey = draft.apiKey.trim(),
            model = draft.model.trim()
        )
        val error = when {
            normalized.name.isBlank() -> ApiProfileFormError.NAME
            normalized.apiKey.isBlank() -> ApiProfileFormError.API_KEY
            else -> null
        }
        if (error != null) {
            _formError.value = error
            return
        }
        viewModelScope.launch {
            repository.save(normalized)
            onSaved()
        }
    }

    fun updateSpec(spec: ApiSpec) = updateDraft { it.copy(apiSpec = spec) }
}

/**
 * Manual DI factory for [ApiProfileViewModel] (no Hilt/Koin per project rules).
 */
class ApiProfileViewModelFactory(
    private val repository: ApiProfileRepository
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(ApiProfileRepository.getInstance(context))

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ApiProfileViewModel::class.java)) {
            "ApiProfileViewModelFactory cannot create ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ApiProfileViewModel(repository) as T
    }
}
