package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.remote.ConnectionTestResult
import com.example.data.repository.ApiProfileRepository
import com.example.data.settings.ApiProfile
import com.example.data.settings.ApiSpec
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Which required field the user left blank, so the form can point at it. */
enum class ApiProfileFormError { NAME, API_KEY }

/** Where the edit form's "Test connection" button is in its cycle. */
sealed interface ConnectionTestState {

    data object Idle : ConnectionTestState

    data object Running : ConnectionTestState

    data class Done(val result: ConnectionTestResult) : ConnectionTestState
}

/**
 * Backs both the profile list and the new/edit form. Compose only reads state
 * and sends intents from here; it never touches [ApiProfileRepository] itself.
 */
class ApiProfileViewModel(
    private val repository: ApiProfileRepository
) : ViewModel() {

    val profiles: StateFlow<List<ApiProfile>> = repository.profiles

    /** Only these can be made active, so only these belong in a picker. */
    val enabledProfiles: StateFlow<List<ApiProfile>> = profiles
        .map { list -> list.filter { it.enabled } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /**
     * The profile requests actually use — already accounts for a stored id that
     * points at a disabled or deleted profile, so screens showing "active" agree
     * with what [ApiProfileRepository.currentProfile] would pick.
     */
    val activeProfile: StateFlow<ApiProfile?> = repository.activeProfile

    /** The stored topic-generation choice; null means "reuse the chat provider". */
    val topicProfileId: StateFlow<String?> = repository.topicProfileId

    /** The profile topic generation actually uses, with the chat-provider fallback resolved. */
    val topicProfile: StateFlow<ApiProfile?> = repository.topicProfile

    // Null until loadForEdit() resolves, which keeps the form from flashing
    // blank fields over an existing profile's values.
    private val _draft = MutableStateFlow<ApiProfile?>(null)
    val draft: StateFlow<ApiProfile?> = _draft.asStateFlow()

    private val _formError = MutableStateFlow<ApiProfileFormError?>(null)
    val formError: StateFlow<ApiProfileFormError?> = _formError.asStateFlow()

    private val _connectionTest = MutableStateFlow<ConnectionTestState>(ConnectionTestState.Idle)
    val connectionTest: StateFlow<ConnectionTestState> = _connectionTest.asStateFlow()

    private var connectionTestJob: Job? = null

    fun setActive(id: String) = repository.setActive(id)

    /** Passing null clears the choice, falling back to the active chat provider. */
    fun setTopicProfile(id: String?) = repository.setTopicProfile(id)

    fun setEnabled(id: String, enabled: Boolean) {
        viewModelScope.launch { repository.setEnabled(id, enabled) }
    }

    fun delete(id: String) {
        viewModelScope.launch { repository.delete(id) }
    }

    /** [profileId] null starts a new profile with the spec defaults. */
    fun loadForEdit(profileId: String?) {
        clearConnectionTest()
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
        // A result from before the edit describes settings that no longer exist;
        // leaving a green check next to a changed API key would mislead.
        clearConnectionTest()
    }

    /**
     * Sends a throwaway request with the draft's settings — without saving them —
     * and reports the streaming and non-streaming results separately. A blank
     * key is reported inline like [save] does, with no request attempted.
     */
    fun testConnection() {
        val profile = _draft.value?.normalized() ?: return
        if (profile.apiKey.isBlank()) {
            _formError.value = ApiProfileFormError.API_KEY
            return
        }
        // The button is disabled while running, but a second call must never
        // leave two probes racing to write the result.
        connectionTestJob?.cancel()
        _connectionTest.value = ConnectionTestState.Running
        // No try/catch: ConnectionTester turns every transport failure into a
        // ProbeOutcome.Failure, so anything escaping it is a genuine bug.
        connectionTestJob = viewModelScope.launch {
            _connectionTest.value = ConnectionTestState.Done(repository.testConnection(profile))
        }
    }

    private fun clearConnectionTest() {
        connectionTestJob?.cancel()
        connectionTestJob = null
        _connectionTest.value = ConnectionTestState.Idle
    }

    /**
     * Trims stray whitespace and saves. Empty base URL and model stay empty and
     * fall back to the spec's defaults at request time; name and API key have no
     * fallback, so a blank one aborts the save and reports [formError].
     */
    fun save(onSaved: () -> Unit) {
        val normalized = _draft.value?.normalized() ?: return
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

    /**
     * Strips the whitespace a paste can leave around a key or URL. Shared by
     * [save] and [testConnection] so a test never probes a value that differs
     * from what saving would store.
     */
    private fun ApiProfile.normalized(): ApiProfile = copy(
        name = name.trim(),
        baseUrl = baseUrl.trim(),
        apiKey = apiKey.trim(),
        model = model.trim()
    )
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
