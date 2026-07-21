package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.ApiProfileDao
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

    /**
     * One-shot read of the active profile, going straight to the DAO. Request
     * time can't use [activeProfile] because its first emission may not have
     * arrived yet right after process start.
     */
    suspend fun currentProfile(): ApiProfile? =
        dao.getAll().map { it.toDomain() }.pickActive(_activeProfileId.value)

    suspend fun getProfile(id: String): ApiProfile? = dao.getById(id)?.toDomain()

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
    }

    suspend fun setEnabled(id: String, enabled: Boolean) {
        dao.setEnabled(id, enabled)
        if (!enabled && _activeProfileId.value == id) {
            setActive(null)
        }
    }

    /** Passing null clears the choice, letting [activeProfile] pick the fallback. */
    fun setActive(id: String?) {
        prefs.edit().putString(KEY_ACTIVE_PROFILE_ID, id).apply()
        _activeProfileId.value = id
    }

    private fun List<ApiProfile>.pickActive(id: String?): ApiProfile? =
        firstOrNull { it.id == id && it.enabled } ?: firstOrNull { it.enabled }

    companion object {
        private const val PREFS_NAME = "api_profile_settings"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

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
