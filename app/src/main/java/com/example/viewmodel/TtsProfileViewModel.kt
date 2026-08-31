package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.repository.TtsProfileRepository
import com.example.data.settings.TtsProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Which required field the user left blank, so the form can point at it. */
enum class TtsProfileFormError { NAME, SPEECH_KEY, REGION }

/**
 * Backs both the voice list and the new/edit form. Compose only reads state and
 * sends intents from here; it never touches [TtsProfileRepository] itself.
 */
class TtsProfileViewModel(
    private val repository: TtsProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<TtsProfile>> = repository.profiles

    /**
     * The saved profile playback uses — already accounts for a stored id that
     * points at a disabled or deleted profile.
     */
    val activeProfile: StateFlow<TtsProfile?> = repository.activeProfile

    /** The same, with the build's own keys resolved as the last fallback. */
    val effectiveProfile: StateFlow<TtsProfile?> = repository.effectiveProfile

    // Null until loadForEdit() resolves, which keeps the form from flashing
    // blank fields over an existing profile's values.
    private val _draft = MutableStateFlow<TtsProfile?>(null)
    val draft: StateFlow<TtsProfile?> = _draft.asStateFlow()

    private val _formError = MutableStateFlow<TtsProfileFormError?>(null)
    val formError: StateFlow<TtsProfileFormError?> = _formError.asStateFlow()

    fun setActive(id: String) = repository.setActive(id)

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** [profileId] null starts a new profile with the voice default. */
    fun loadForEdit(profileId: String?) {
        if (profileId == null) {
            _draft.value = TtsProfile()
            return
        }
        viewModelScope.launch {
            _draft.value = repository.getProfile(profileId) ?: TtsProfile()
        }
    }

    fun updateDraft(transform: (TtsProfile) -> TtsProfile) {
        _draft.value = _draft.value?.let(transform)
        _formError.value = null
    }

    /**
     * Trims stray whitespace and saves. An empty voice stays empty and falls
     * back to [TtsProfile.DEFAULT_VOICE] at playback time; name, key and region
     * have no fallback, so a blank one aborts the save and reports [formError].
     */
    fun save(onSaved: () -> Unit) {
        val normalized = _draft.value?.normalized() ?: return
        val error = when {
            normalized.name.isBlank() -> TtsProfileFormError.NAME
            normalized.speechKey.isBlank() -> TtsProfileFormError.SPEECH_KEY
            normalized.region.isBlank() -> TtsProfileFormError.REGION
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

    /** Strips the whitespace a paste can leave around a key, region or voice. */
    private fun TtsProfile.normalized(): TtsProfile = copy(
        name = name.trim(),
        speechKey = speechKey.trim(),
        region = region.trim(),
        voice = voice.trim()
    )
}

/**
 * Manual DI factory for [TtsProfileViewModel] (no Hilt/Koin per project rules).
 */
class TtsProfileViewModelFactory(
    private val repository: TtsProfileRepository
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(TtsProfileRepository.getInstance(context))

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TtsProfileViewModel::class.java)) {
            "TtsProfileViewModelFactory cannot create ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TtsProfileViewModel(repository) as T
    }
}
