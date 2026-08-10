package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.AiModelService
import com.example.data.remote.ConfigurableAiService
import com.example.data.remote.MissingApiKeyException
import com.example.data.repository.ApiProfileRepository
import com.example.data.repository.ChatRepository
import com.example.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sessionId: String,
    val userText: String,
    val aiResponse: String,
    val grammarCorrection: String?,
    val timestamp: Long = System.currentTimeMillis()
)

private fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    sessionId = sessionId,
    userText = userText,
    aiResponse = aiResponse,
    grammarCorrection = grammarCorrection,
    timestamp = timestamp
)

private fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id = id,
    sessionId = sessionId,
    userText = userText,
    aiResponse = aiResponse,
    grammarCorrection = grammarCorrection,
    timestamp = timestamp
)

class ChatViewModel(
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val apiService: AiModelService
) : ViewModel() {

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    private val _speakEvent = MutableStateFlow<String?>(null)
    val speakEvent: StateFlow<String?> = _speakEvent

    // Backed by Room so the conversation survives the app process being killed.
    val history: StateFlow<List<ChatMessage>> =
        chatRepository.observeHistory()
            .map { entities -> entities.map { it.toChatMessage() } }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.Eagerly,
                initialValue = emptyList()
            )

    // Null until the user starts or resumes a session; while null the most
    // recent stored conversation is treated as current, so relaunching the app
    // picks up where the learner left off.
    private val _currentSessionId = MutableStateFlow<String?>(null)

    // Messages belonging to the current practice session. Older messages stay
    // in Room (and on the history screen); they just no longer feed the
    // on-screen conversation or the prompt context.
    val sessionHistory: StateFlow<List<ChatMessage>> =
        combine(history, _currentSessionId) { messages, currentId ->
            val sessionId = currentId ?: messages.lastOrNull()?.sessionId
            messages.filter { it.sessionId == sessionId }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun startNewSession() {
        _currentSessionId.value = java.util.UUID.randomUUID().toString()
        _error.value = null
    }

    fun resumeSession(sessionId: String) {
        _currentSessionId.value = sessionId
        _error.value = null
    }

    fun deleteSession(sessionId: String) {
        // If the conversation being deleted is the one on screen (explicitly
        // resumed or the implicit most-recent fallback), start a fresh session
        // so the chat screen doesn't silently switch to another old one.
        val effectiveCurrentId = _currentSessionId.value ?: history.value.lastOrNull()?.sessionId
        if (sessionId == effectiveCurrentId) {
            startNewSession()
        }
        viewModelScope.launch {
            chatRepository.deleteSession(sessionId)
        }
    }

    fun clearAllHistory() {
        startNewSession()
        viewModelScope.launch {
            chatRepository.clearHistory()
        }
    }

    fun setRecordingState(isRecording: Boolean) {
        _isRecording.value = isRecording
    }

    fun clearSpeakEvent() {
        _speakEvent.value = null
    }

    fun onSpeechResult(userText: String) {
        if (userText.isBlank()) return

        _isProcessing.value = true
        _error.value = null

        // Pin down the session the reply belongs to before the network call, so a
        // session switch mid-request can't reassign the message.
        val sessionId = _currentSessionId.value
            ?: history.value.lastOrNull()?.sessionId
            ?: java.util.UUID.randomUUID().toString()
        _currentSessionId.value = sessionId

        viewModelScope.launch {
            try {
                val profile = profileRepository.getUserProfile()
                val rawJson = apiService.generateContent(
                    systemPrompt = buildSystemPrompt(profile),
                    userPrompt = buildUserPrompt(sessionHistory.value, userText),
                    responseMimeType = "application/json"
                )
                val message = parseTutorReply(sessionId, userText, rawJson)

                chatRepository.saveMessage(message.toEntity())
                _speakEvent.value = message.aiResponse
                _isProcessing.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: MissingApiKeyException) {
                // No landmark in the wording: this fires mid-conversation, where the
                // top bar shows back/new/history and no gear at all.
                _error.value = "Add your API key in Settings to start chatting."
                _isProcessing.value = false
            } catch (e: Exception) {
                _error.value = "Couldn't reach your AI tutor. Please check your connection and try again."
                _isProcessing.value = false
            }
        }
    }

    /**
     * Opens a fresh conversation about a recommended topic. Unlike
     * [onSpeechResult], there is no learner utterance yet: the tutor speaks
     * first, so the stored turn has a blank [ChatMessage.userText] and no grammar
     * correction. The learner then replies by voice, continuing this session.
     */
    fun startTopic(topic: String) {
        if (topic.isBlank()) return

        startNewSession()
        val sessionId = _currentSessionId.value ?: java.util.UUID.randomUUID().toString()
        _currentSessionId.value = sessionId
        _isProcessing.value = true
        _error.value = null

        viewModelScope.launch {
            try {
                val profile = profileRepository.getUserProfile()
                val rawJson = apiService.generateContent(
                    systemPrompt = buildSystemPrompt(profile),
                    userPrompt = buildTopicOpenerPrompt(topic),
                    responseMimeType = "application/json"
                )
                val message = parseTutorReply(sessionId, userText = "", rawJson = rawJson)

                chatRepository.saveMessage(message.toEntity())
                _speakEvent.value = message.aiResponse
                _isProcessing.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: MissingApiKeyException) {
                // No landmark in the wording: this fires mid-conversation, where the
                // top bar shows back/new/history and no gear at all.
                _error.value = "Add your API key in Settings to start chatting."
                _isProcessing.value = false
            } catch (e: Exception) {
                _error.value = "Couldn't reach your AI tutor. Please check your connection and try again."
                _isProcessing.value = false
            }
        }
    }

    private fun buildTopicOpenerPrompt(topic: String): String = buildString {
        appendLine("Open the conversation yourself: warmly introduce this topic and ask the learner one question to get them talking.")
        appendLine("Topic: \"$topic\"")
        append("The learner hasn't said anything yet, so set grammar_correction to null.")
    }

    private fun buildSystemPrompt(profile: UserProfileEntity?): String = buildString {
        appendLine("You are a friendly, encouraging English tutor in a spoken-conversation practice app.")
        appendLine("Reply naturally to the learner in English, keep it short (1-3 sentences) and easy to speak aloud, and end with a question that keeps the conversation going.")
        if (profile != null) {
            appendLine("Personalize your replies to this learner:")
            appendLine("- English level: ${profile.englishLevel}")
            appendLine("- Learning goal: ${profile.learningGoal}")
            appendLine("- Topics of interest: ${profile.topicsOfInterest.joinToString(", ")}")
            appendLine("- Daily practice time: ${profile.dailyPracticeTime} minutes")
        }
        appendLine("Respond with ONLY a valid JSON object, no markdown fences and no extra text, using exactly this schema:")
        appendLine("""{"ai_response": "...", "grammar_correction": "... or null"}""")
        appendLine("\"ai_response\" is your conversational reply to the learner.")
        append("\"grammar_correction\" is a brief, friendly correction of a mistake in the learner's last utterance, or null when there is nothing worth correcting.")
    }

    private fun buildUserPrompt(history: List<ChatMessage>, userText: String): String {
        val recentTurns = history.takeLast(MAX_HISTORY_TURNS)
        if (recentTurns.isEmpty()) return userText
        return buildString {
            appendLine("Recent conversation:")
            recentTurns.forEach { turn ->
                // A topic-opener turn has no learner utterance; skip the empty line.
                if (turn.userText.isNotBlank()) appendLine("Learner: ${turn.userText}")
                appendLine("Tutor: ${turn.aiResponse}")
            }
            appendLine()
            appendLine("The learner now says:")
            append(userText)
        }
    }

    private fun parseTutorReply(sessionId: String, userText: String, rawJson: String): ChatMessage {
        // Defensive cleanup in case the model wraps the JSON in a markdown fence.
        val cleaned = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)
        val correction = if (json.isNull("grammar_correction")) {
            null
        } else {
            json.getString("grammar_correction")
                .takeUnless { it.isBlank() || it.equals("null", ignoreCase = true) }
        }
        return ChatMessage(
            sessionId = sessionId,
            userText = userText,
            aiResponse = json.getString("ai_response"),
            grammarCorrection = correction
        )
    }

    private companion object {
        const val MAX_HISTORY_TURNS = 6
    }
}

/**
 * Manual DI factory for [ChatViewModel] (no Hilt/Koin per project rules).
 */
class ChatViewModelFactory(
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val apiService: AiModelService
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(
        profileRepository = ProfileRepository(
            userDao = AppDatabase.getInstance(context).userDao(),
            apiService = ConfigurableAiService(ApiProfileRepository.getInstance(context))
        ),
        chatRepository = ChatRepository(
            chatMessageDao = AppDatabase.getInstance(context).chatMessageDao()
        ),
        apiService = ConfigurableAiService(ApiProfileRepository.getInstance(context))
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            "ChatViewModelFactory cannot create ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(profileRepository, chatRepository, apiService) as T
    }
}
