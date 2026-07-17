package com.example.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import com.example.data.local.UserProfileEntity
import com.example.data.remote.GeminiApiService
import com.example.data.repository.ChatRepository
import com.example.data.repository.ProfileRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ChatMessage(
    val id: String = java.util.UUID.randomUUID().toString(),
    val userText: String,
    val aiResponse: String,
    val grammarCorrection: String?,
    val timestamp: Long = System.currentTimeMillis()
)

private fun ChatMessage.toEntity() = ChatMessageEntity(
    id = id,
    userText = userText,
    aiResponse = aiResponse,
    grammarCorrection = grammarCorrection,
    timestamp = timestamp
)

private fun ChatMessageEntity.toChatMessage() = ChatMessage(
    id = id,
    userText = userText,
    aiResponse = aiResponse,
    grammarCorrection = grammarCorrection,
    timestamp = timestamp
)

class ChatViewModel(
    private val profileRepository: ProfileRepository,
    private val chatRepository: ChatRepository,
    private val geminiApiService: GeminiApiService
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

        viewModelScope.launch {
            try {
                val profile = profileRepository.getUserProfile()
                val rawJson = geminiApiService.generateContent(
                    systemPrompt = buildSystemPrompt(profile),
                    userPrompt = buildUserPrompt(history.value, userText),
                    responseMimeType = "application/json"
                )
                val message = parseTutorReply(userText, rawJson)

                chatRepository.saveMessage(message.toEntity())
                _speakEvent.value = message.aiResponse
                _isProcessing.value = false
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "Couldn't reach your AI tutor. Please check your connection and try again."
                _isProcessing.value = false
            }
        }
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
                appendLine("Learner: ${turn.userText}")
                appendLine("Tutor: ${turn.aiResponse}")
            }
            appendLine()
            appendLine("The learner now says:")
            append(userText)
        }
    }

    private fun parseTutorReply(userText: String, rawJson: String): ChatMessage {
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
    private val geminiApiService: GeminiApiService
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(
        profileRepository = ProfileRepository(
            userDao = AppDatabase.getInstance(context).userDao(),
            geminiApiService = GeminiApiService()
        ),
        chatRepository = ChatRepository(
            chatMessageDao = AppDatabase.getInstance(context).chatMessageDao()
        ),
        geminiApiService = GeminiApiService()
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            "ChatViewModelFactory cannot create ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return ChatViewModel(profileRepository, chatRepository, geminiApiService) as T
    }
}
