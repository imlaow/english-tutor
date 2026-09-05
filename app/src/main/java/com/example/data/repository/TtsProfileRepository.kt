package com.example.data.repository

import android.content.Context
import com.example.data.local.AppDatabase
import com.example.data.local.TtsProfileDao
import com.example.data.settings.TtsProfile
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
 * Owns the list of saved [TtsProfile]s (Room) plus which one is currently
 * active (SharedPreferences — a single scalar doesn't warrant a table).
 *
 * The shape deliberately mirrors [ApiProfileRepository]: disabled profiles
 * can't be active, and a stored id pointing at a disabled or deleted profile
 * falls back to the first enabled one, so playback never silently uses a
 * configuration the user turned off.
 *
 * A saved profile is the only source of credentials. Nothing is compiled into
 * the build, so with an empty table the app simply cannot speak.
 */
class TtsProfileRepository private constructor(
    private val dao: TtsProfileDao,
    context: Context
) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val profiles: StateFlow<List<TtsProfile>> = dao.observeAll()
        .map { entities -> entities.map { it.toDomain() } }
        .stateIn(scope, SharingStarted.Eagerly, emptyList())

    private val _activeProfileId = MutableStateFlow(prefs.getString(KEY_ACTIVE_PROFILE_ID, null))
    val activeProfileId: StateFlow<String?> = _activeProfileId.asStateFlow()

    /**
     * What the synthesizer is built from. Null means nothing has been saved
     * yet, and the app cannot speak until the user adds a profile.
     */
    val activeProfile: StateFlow<TtsProfile?> =
        combine(profiles, _activeProfileId) { list, id -> list.pickActive(id) }
            .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun getProfile(id: String): TtsProfile? = dao.getById(id)?.toDomain()

    /**
     * Inserts or updates [profile]. New profiles go to the end of the list, and
     * the very first one saved becomes active so adding a key is enough to be
     * heard, with no extra tap.
     */
    suspend fun save(profile: TtsProfile) {
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

    private fun List<TtsProfile>.pickActive(id: String?): TtsProfile? =
        firstOrNull { it.id == id && it.enabled } ?: firstOrNull { it.enabled }

    companion object {
        private const val PREFS_NAME = "tts_profile_settings"
        private const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"

        @Volatile
        private var instance: TtsProfileRepository? = null

        fun getInstance(context: Context): TtsProfileRepository =
            instance ?: synchronized(this) {
                instance ?: TtsProfileRepository(
                    dao = AppDatabase.getInstance(context).ttsProfileDao(),
                    context = context.applicationContext
                ).also { instance = it }
            }
    }
}
