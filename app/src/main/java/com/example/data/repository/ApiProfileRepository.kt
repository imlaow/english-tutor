package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ApiProfileDao
import com.example.data.remote.ConnectionTestResult
import com.example.data.remote.ConnectionTester
import com.example.data.remote.toAiService
import com.example.data.settings.ApiProfile
import com.example.data.settings.toDomain
import com.example.data.settings.toEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Owns the list of saved [ApiProfile]s (Room) plus which one is currently
 * active (SharedPreferences — a single scalar doesn't warrant a table).
 *
 * Disabled profiles can't be active: if the stored id points at a profile that
 * was disabled or deleted, [activeProfile] falls back to the first enabled one,
 * so a request never silently uses a configuration the user turned off.
 */
class ApiProfileRepository private constructor(
    private val dao: ApiProfileDao,
    context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val profiles: StateFlow<List<ApiProfile>> = dao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _activeProfileId = MutableStateFlow(prefs.getString(KEY_ACTIVE_PROFILE_ID, null))
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    val activeProfile: StateFlow<ApiProfile?> =
        combine(profiles, _activeProfileId) { list, id -> list.pickActive(id) }
            .stateIn(scope, SharingStarted.Eagerly, null)

    // Which profile generates the home-screen topic suggestions. Kept separate
    // from the active chat profile so the user can point topic generation at a
    // cheaper model; null means "reuse whatever the chat is using".
    private val _topicProfileId = MutableStateFlow(prefs.getString(KEY_TOPIC_PROFILE_ID, null))
    val topicProfileId: StateFlow<String?> = _topicProfileId.asStateFlow()

    val topicProfile: StateFlow<ApiProfile?> =
        combine(profiles, _topicProfileId, _activeProfileId) { list, topicId, activeId ->
            list.pickTopic(topicId, activeId)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    /**
     * One-shot read of the active profile, going straight to the DAO. Request
     * time can't use [activeProfile] because its first emission may not have
     * arrived yet right after process start.
     */
    suspend fun currentProfile(): ApiProfile? =
        dao.getAll().map { it.toDomain() }.pickActive(_activeProfileId.value)

    /**
     * One-shot read of the topic-generation profile (same reasoning as
     * [currentProfile]). Falls back to the active chat profile when none is set.
     */
    suspend fun currentTopicProfile(): ApiProfile? =
        dao.getAll().map { it.toDomain() }.pickTopic(_topicProfileId.value, _activeProfileId.value)

    suspend fun getProfile(id: String): ApiProfile? = dao.getById(id)?.toDomain()

    /**
     * Probes [profile] without saving it, so the edit form can verify a key
     * before it is committed. Nothing here touches the stored profiles.
     */
    suspend fun testConnection(profile: ApiProfile): ConnectionTestResult =
        ConnectionTester.test(profile.toAiService())

    /**
     * Inserts or updates [profile]. New profiles go to the end of the list, and
     * the very first one saved becomes active so a fresh install works without
     * an extra tap.
     */
    suspend fun save(profile: ApiProfile) {
        val existing = dao.getById(profile.id)
        val sortOrder = existing?.sortOrder ?: (dao.maxSortOrder() + 1)
        dao.upsert(profile.copy(sortOrder = sortOrder).toEntity())
        if (_activeProfileId.value == null && profile.enabled) {
            setActive(profile.id)
        }
    }

    suspend fun delete(id: String) {
        dao.deleteById(id)
        if (_activeProfileId.value == id) {
            setActive(null)
        }
        if (_topicProfileId.value == id) {
            setTopicProfile(null)
        }
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        if (!enabled && _activeProfileId.value == id) {
            setActive(null)
        }
        if (!enabled && _topicProfileId.value == id) {
            setTopicProfile(null)
        }
    }

    /** Passing null clears the choice, letting [activeProfile] pick the fallback. */
    fun setActive(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
        _activeProfileId.value = id
    }

    /** Passing null clears the choice, letting [topicProfile] fall back to the active chat profile. */
    fun setTopicProfile(id: String?) {
        prefs.edit().putString(KEY_TOPIC_PROFILE_ID, id).apply()
        _topicProfileId.value = id
    }

    private fun List<ApiProfile>.pickActive(id: String?): ApiProfile? =
        firstOrNull { it.id == id && it.enabled } ?: firstOrNull { it.enabled }

    private fun List<ApiProfile>.pickTopic(topicId: String?, activeId: String?): ApiProfile? =
        firstOrNull { it.id == topicId && it.enabled } ?: pickActive(activeId)

    companion object {
        private const val PREFS_NAME = "api_profile_settings"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
        private const val KEY_TOPIC_PROFILE_ID = "topic_profile_id"

        @Volatile
        private var instance: ApiProfileRepository? = null

        fun getInstance(context: Context): ApiProfileRepository =
            instance ?: synchronized(this) {
                instance ?: ApiProfileRepository(
                    dao = AppDatabase.getInstance(context).apiProfileDao(),
                    context = context.applicationContext
                ).also { instance = it }
            }
    }
}
