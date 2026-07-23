package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.remote.ConfigurableAiService
import com.example.data.remote.MissingApiKeyException
import com.example.data.repository.ApiProfileRepository
import com.example.data.repository.ProfileRepository
import com.example.data.repository.TopicGenerator
import com.example.data.repository.TopicRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Backs the recommended-topic cards on the chat screen's empty state. Generates
 * a set on creation (once per app launch, since the chat back-stack entry keeps
 * this view model alive across navigation) and on manual refresh.
 */
class TopicsViewModel(
    private val topicGenerator: TopicGenerator
) : ViewModel() {

    private val _topics = MutableStateFlow<List<String>>(emptyList())
    val topics: StateFlow<List<String>> = _topics.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private var refreshJob: Job? = null

    init {
        refresh()
    }

    fun refresh() {
        // A rapid double-tap must never leave two requests racing to write topics.
        refreshJob?.cancel()
        _isLoading.value = true
        _error.value = null
        refreshJob = viewModelScope.launch {
            try {
                _topics.value = topicGenerator.generateTopics()
                _isLoading.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: MissingApiKeyException) {
                _error.value = "Add an API provider in Settings to see topic suggestions."
                _isLoading.value = false
            } catch (e: Exception) {
                _error.value = "Couldn't load topics. Tap refresh to try again."
                _isLoading.value = false
            }
        }
    }
}

/**
 * Manual DI factory for [TopicsViewModel] (no Hilt/Koin per project rules).
 */
class TopicsViewModelFactory(
    private val topicGenerator: TopicGenerator
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(
        TopicRepository(
            apiProfileRepository = ApiProfileRepository.getInstance(context),
            profileRepository = ProfileRepository(
                userDao = AppDatabase.getInstance(context).userDao(),
                apiService = ConfigurableAiService(ApiProfileRepository.getInstance(context))
            )
        )
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(TopicsViewModel::class.java)) {
            "TopicsViewModelFactory cannot create ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return TopicsViewModel(topicGenerator) as T
    }
}
