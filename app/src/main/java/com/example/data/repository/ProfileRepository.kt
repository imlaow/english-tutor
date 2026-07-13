package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserProfileEntity
import com.example.data.remote.GeminiApiService
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

class ProfileRepository(
    private val userDao: UserDao,
    private val geminiApiService: GeminiApiService
) {

    suspend fun saveUserProfile(profile: UserProfileEntity) =
        userDao.upsertUserProfile(profile)

    suspend fun getUserProfile(): UserProfileEntity? =
        userDao.getUserProfile()

    fun observeUserProfile(): Flow<UserProfileEntity?> =
        userDao.observeUserProfile()

    suspend fun clearUserProfile() =
        userDao.clearUserProfile()

    /**
     * Asks Gemini to generate a user profile from the onboarding answers,
     * parses the returned JSON, persists it into Room, and returns the entity.
     */
    suspend fun generateAndSaveUserProfile(
        systemPrompt: String,
        onboardingAnswers: String
    ): UserProfileEntity {
        val rawJson = geminiApiService.generateContent(
            systemPrompt = systemPrompt,
            userPrompt = onboardingAnswers,
            responseMimeType = "application/json"
        )
        val profile = parseUserProfileJson(rawJson)
        userDao.upsertUserProfile(profile)
        return profile
    }

    private fun parseUserProfileJson(rawJson: String): UserProfileEntity {
        // Defensive cleanup in case the model wraps the JSON in a markdown fence.
        val cleaned = rawJson.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()
        val json = JSONObject(cleaned)
        val topicsJson = json.getJSONArray("topics_of_interest")
        return UserProfileEntity(
            englishLevel = json.getString("english_level"),
            learningGoal = json.getString("learning_goal"),
            topicsOfInterest = List(topicsJson.length()) { topicsJson.getString(it) },
            dailyPracticeTime = json.getInt("daily_practice_time")
        )
    }
}
