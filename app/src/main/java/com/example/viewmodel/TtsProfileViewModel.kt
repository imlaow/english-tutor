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

/**
 * Which field the form should point at: the three the user left blank, plus the
 * style degree, which is the one optional field whose value can be wrong rather
 * than merely missing.
 */
enum class TtsProfileFormError { NAME, SPEECH_KEY, REGION, STYLE_DEGREE }

/**
 * Backs both the voice list and the new/edit form. Compose only reads state and
 * sends intents from here; it never touches [TtsProfileRepository] itself.
 */
class TtsProfileViewModel(
    private val repository: TtsProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<TtsProfile>> = repository.profiles

    /**
     * The profile playback uses — already accounts for a stored id that points
     * at a disabled or deleted profile. Null until the user saves one.
     */
    val activeProfile: StateFlow<TtsProfile?> = repository.activeProfile

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
     *
     * Of the four expression fields only the style degree is validated, because
     * it is the only one where a wrong value is knowable here. Azure silently
     * ignores a style the voice does not support — the app cannot tell a typo
     * from a style it has not heard of — and pitch and rate each have five valid
     * syntaxes (`%`, `Hz`, `st`, named constants like `x-high`, and absolute
     * values), so a rejection here would be a guess dressed up as a rule.
     */
    fun save(onSaved: () -> Unit) {
        val normalized = _draft.value?.normalized() ?: return
        val error = when {
            normalized.name.isBlank() -> TtsProfileFormError.NAME
            normalized.speechKey.isBlank() -> TtsProfileFormError.SPEECH_KEY
            normalized.region.isBlank() -> TtsProfileFormError.REGION
            // Azure's documented range. toDoubleOrNull() also rejects a
            // comma-decimal "1,6" off a European-locale keyboard, which the
            // service would reject too — hence the field's "0.01 to 2" copy.
            !normalized.styleDegree.isValidStyleDegree() -> TtsProfileFormError.STYLE_DEGREE
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
        voice = voice.trim(),
        style = style.trim(),
        styleDegree = styleDegree.trim(),
        pitch = pitch.trim(),
        rate = rate.trim()
    )

    /** Blank is valid — it means "leave the degree to Azure". */
    private fun String.isValidStyleDegree(): Boolean =
        isBlank() || (toDoubleOrNull()?.let { it in STYLE_DEGREE_RANGE } == true)

    private companion object {
        /** The range Azure documents for `styledegree`. */
        val STYLE_DEGREE_RANGE = 0.01..2.0
    }
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
