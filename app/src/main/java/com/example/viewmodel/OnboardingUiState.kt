package com.example.viewmodel

/**
 * Raw answers the user gives during onboarding. Gemini turns these
 * free-form answers into the structured user profile.
 */
data class OnboardingAnswers(
    val selfAssessedLevel: String = "",
    val learningGoal: String = "",
    val interests: String = "",
    val dailyPracticeMinutes: String = ""
)

data class OnboardingUiState(
    val answers: OnboardingAnswers = OnboardingAnswers(),
    val isGeneratingProfile: Boolean = false,
    val isProfileSaved: Boolean = false,
    val errorMessage: String? = null
) {
    val canSubmit: Boolean
        get() = !isGeneratingProfile &&
            answers.selfAssessedLevel.isNotBlank() &&
            answers.learningGoal.isNotBlank() &&
            answers.interests.isNotBlank() &&
            answers.dailyPracticeMinutes.isNotBlank()
}
