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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val profileRepository: ProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun onLevelChanged(level: String) =
        _uiState.update { it.copy(answers = it.answers.copy(selfAssessedLevel = level)) }

    fun onGoalChanged(goal: String) =
        _uiState.update { it.copy(answers = it.answers.copy(learningGoal = goal)) }

    fun onInterestsChanged(interests: String) =
        _uiState.update { it.copy(answers = it.answers.copy(interests = interests)) }

    fun onPracticeMinutesChanged(minutes: String) =
        _uiState.update { it.copy(answers = it.answers.copy(dailyPracticeMinutes = minutes)) }

    fun dismissError() =
        _uiState.update { it.copy(errorMessage = null) }

    /**
     * Sends the onboarding answers to Gemini via the repository, which parses
     * the returned JSON profile and saves it into the Room database.
     */
    fun completeOnboarding() {
        val current = _uiState.value
        if (!current.canSubmit) return

        _uiState.update { it.copy(isGeneratingProfile = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                profileRepository.generateAndSaveUserProfile(
                    systemPrompt = SYSTEM_PROMPT,
                    onboardingAnswers = formatAnswersPrompt(current.answers)
                )
                _uiState.update { it.copy(isGeneratingProfile = false, isProfileSaved = true) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: MissingApiKeyException) {
                _uiState.update {
                    it.copy(
                        isGeneratingProfile = false,
                        errorMessage = "Add your API key first: tap the gear icon at the top right to open Settings."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isGeneratingProfile = false,
                        errorMessage = e.message ?: "Failed to generate your profile. Please try again."
                    )
                }
            }
        }
    }

    companion object {

        val SYSTEM_PROMPT = """
            You are the onboarding assistant of an English learning app.
            Analyze the learner's answers and generate their user profile.
            Respond with ONLY a valid JSON object, no markdown fences and no extra text,
            using exactly this schema:
            {
              "english_level": "one of: Beginner, Elementary, Intermediate, Upper-Intermediate, Advanced",
              "learning_goal": "a concise one-sentence summary of the learner's primary goal",
              "topics_of_interest": ["topic1", "topic2", "..."],
              "daily_practice_time": 30
            }
            "daily_practice_time" must be an integer number of minutes per day.
        """.trimIndent()

        fun formatAnswersPrompt(answers: OnboardingAnswers): String = """
            Here are my onboarding answers:
            - My English level: ${answers.selfAssessedLevel}
            - My learning goal: ${answers.learningGoal}
            - Topics I am interested in: ${answers.interests}
            - Time I can practice daily: ${answers.dailyPracticeMinutes} minutes
        """.trimIndent()
    }
}

/**
 * Manual DI factory for [OnboardingViewModel] (no Hilt/Koin per project rules).
 */
class OnboardingViewModelFactory(
    private val profileRepository: ProfileRepository
) : ViewModelProvider.Factory {

    constructor(context: Context) : this(
        ProfileRepository(
            userDao = AppDatabase.getInstance(context).userDao(),
            apiService = ConfigurableAiService(ApiProfileRepository.getInstance(context))
        )
    )

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(OnboardingViewModel::class.java)) {
            "OnboardingViewModelFactory cannot create ${modelClass.name}"
        }
        @Suppress("UNCHECKED_CAST")
        return OnboardingViewModel(profileRepository) as T
    }
}
