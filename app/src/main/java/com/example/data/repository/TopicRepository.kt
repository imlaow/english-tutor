package com.example.data.repository

import com.example.data.local.UserProfileEntity
import com.example.data.remote.MissingApiKeyException
import com.example.data.remote.toAiService
import org.json.JSONObject

/**
 * Produces short spoken-conversation starters for the home screen. Kept behind
 * an interface so the view model can be tested with a fake instead of a live
 * network call.
 */
interface TopicGenerator {
    suspend fun generateTopics(count: Int = 3): List<String>
}

/**
 * Generates topics with the user's own API. Resolves its own service from the
 * dedicated topic-generation profile (falling back to the active chat profile),
 * so it does not go through [com.example.data.remote.ConfigurableAiService],
 * which always uses the active profile.
 */
class TopicRepository(
    private val apiProfileRepository: ApiProfileRepository,
    private val profileRepository: ProfileRepository
) : TopicGenerator {

    override suspend fun generateTopics(count: Int): List<String> {
        val profile = apiProfileRepository.currentTopicProfile()
            ?: throw MissingApiKeyException("No API provider configured for topic generation.")
        if (profile.apiKey.isBlank()) {
            throw MissingApiKeyException("No API key configured for provider \"${profile.name}\".")
        }
        val service = profile.toAiService()
        val learner = profileRepository.getUserProfile()
        val rawJson = service.generateContent(
            systemPrompt = buildSystemPrompt(learner, count),
            userPrompt = buildUserPrompt(learner),
            responseMimeType = "application/json"
        )
        return parseTopics(rawJson, count)
    }

    private fun buildSystemPrompt(profile: UserProfileEntity?, count: Int): String = buildString {
        appendLine("You suggest conversation topics for a spoken English practice app.")
        appendLine("Propose $count fresh, varied topics the learner can talk about out loud.")
        appendLine("Each topic must be a short phrase of 2-5 words (e.g. \"Your weekend plans\"), not a full sentence or question.")
        if (profile != null) {
            appendLine("Tailor them to this learner:")
            appendLine("- English level: ${profile.englishLevel}")
            appendLine("- Learning goal: ${profile.learningGoal}")
            appendLine("- Topics of interest: ${profile.topicsOfInterest.joinToString(", ")}")
        }
        appendLine("Respond with ONLY a valid JSON object, no markdown fences and no extra text, using exactly this schema:")
        append("""{"topics": ["...", "...", "..."]}""")
    }

    private fun buildUserPrompt(profile: UserProfileEntity?): String =
        if (profile == null) {
            "Suggest topics for a general English learner."
        } else {
            "Suggest topics suited to a ${profile.englishLevel} learner whose goal is ${profile.learningGoal}."
        }

    companion object {
        /**
         * Reads the topics array out of the model's reply, tolerating a markdown
         * fence the model may wrap the JSON in (same defensive cleanup the chat
         * and onboarding parsers use). Kept as a pure function so it can be
         * unit-tested without Room or the network.
         */
        internal fun parseTopics(rawJson: String, count: Int): List<String> {
            val cleaned = rawJson.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
            val topics = JSONObject(cleaned).getJSONArray("topics")
            return List(topics.length()) { topics.getString(it) }
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .take(count)
        }
    }
}
