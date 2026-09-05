package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.repository.TtsProfileRepository
import com.example.data.settings.TtsProfile
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * The data path behind the Settings → Text to speech screens, which the
 * screenshot goldens cannot see: the schema upgrade that introduces
 * `tts_profile`, and the rules that decide which profile the synthesizer is
 * built from.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class TtsProfileTest {

    // Both singletons cache themselves in a static field that survives between
    // Robolectric tests, while the database file and preferences underneath them
    // do not. Each test resets them to get a fresh open against its own state.
    @Before
    fun resetSingletons() {
        AppDatabase::class.java.getDeclaredField("instance").let { field ->
            field.isAccessible = true
            (field.get(null) as? AppDatabase)?.close()
            field.set(null, null)
        }
        TtsProfileRepository::class.java.getDeclaredField("instance").let { field ->
            field.isAccessible = true
            field.set(null, null)
        }
    }

    @Test
    fun `v4 to v5 migration keeps the API profiles and adds the voice table`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

        // Simulate a user who is on schema v4: everything up to the API profile
        // table exists, with a profile already saved, and tts_profile does not.
        val dbFile = context.getDatabasePath("app_database")
        dbFile.parentFile?.mkdirs()
        if (dbFile.exists()) dbFile.delete()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).apply {
            execSQL(
                "CREATE TABLE IF NOT EXISTS `user_profile` (" +
                    "`id` INTEGER NOT NULL, " +
                    "`english_level` TEXT NOT NULL, " +
                    "`learning_goal` TEXT NOT NULL, " +
                    "`topics_of_interest` TEXT NOT NULL, " +
                    "`daily_practice_time` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `chat_message` (" +
                    "`id` TEXT NOT NULL, " +
                    "`user_text` TEXT NOT NULL, " +
                    "`ai_response` TEXT NOT NULL, " +
                    "`grammar_correction` TEXT, " +
                    "`timestamp` INTEGER NOT NULL, " +
                    "`session_id` TEXT NOT NULL DEFAULT 'legacy', " +
                    "PRIMARY KEY(`id`))"
            )
            execSQL(
                "CREATE TABLE IF NOT EXISTS `api_profile` (" +
                    "`id` TEXT NOT NULL, " +
                    "`name` TEXT NOT NULL, " +
                    "`api_spec` TEXT NOT NULL, " +
                    "`base_url` TEXT NOT NULL, " +
                    "`api_key` TEXT NOT NULL, " +
                    "`model` TEXT NOT NULL, " +
                    "`enabled` INTEGER NOT NULL, " +
                    "`sort_order` INTEGER NOT NULL, " +
                    "PRIMARY KEY(`id`))"
            )
            execSQL(
                "INSERT INTO api_profile " +
                    "(id, name, api_spec, base_url, api_key, model, enabled, sort_order) " +
                    "VALUES ('p1', 'Gemini', 'GEMINI', '', 'key-1', '', 1, 0)"
            )
            version = 4 // PRAGMA user_version; tells Room to run MIGRATION_4_5 on open.
            close()
        }

        // Room validates the migrated schema against the entities, so a wrong
        // CREATE TABLE in MIGRATION_4_5 would throw right here.
        val db = AppDatabase.getInstance(context)

        // The upgrade is non-destructive: the API profile the user already had is
        // still there, key included.
        val apiProfiles = db.apiProfileDao().getAll()
        assertEquals(1, apiProfiles.size)
        assertEquals("key-1", apiProfiles.first().apiKey)

        // And the new table is usable rather than merely present.
        val repository = TtsProfileRepository.getInstance(context)
        repository.save(TtsProfile(id = "v1", name = "Azure", speechKey = "k", region = "eastus"))
        assertEquals("Azure", repository.getProfile("v1")?.name)
    }

    @Test
    fun `the first saved voice becomes active`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = TtsProfileRepository.getInstance(context)
        assertNull("Nothing is active before anything is saved", repository.activeProfileId.value)

        repository.save(TtsProfile(id = "v1", name = "Azure", speechKey = "k", region = "eastus"))

        // Saving a key is enough to be heard; the user should not have to come
        // back and tick the row they just created.
        assertEquals("v1", repository.activeProfileId.value)
        assertEquals("v1", repository.activeProfile.first { it != null }?.id)
    }

    @Test
    fun `disabling the active voice falls back to an enabled one`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = TtsProfileRepository.getInstance(context)
        repository.save(TtsProfile(id = "v1", name = "Personal", speechKey = "k", region = "eastus"))
        repository.save(TtsProfile(id = "v2", name = "Work", speechKey = "k2", region = "westus"))

        repository.setEnabled("v1", false)

        // A disabled profile can't be spoken with, so the stored choice is dropped
        // and the resolver picks the first profile that can.
        assertNull(repository.activeProfileId.value)
        assertEquals("v2", repository.activeProfile.first { it?.id == "v2" }?.id)
    }

    @Test
    fun `deleting the last voice leaves nothing to speak with`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = TtsProfileRepository.getInstance(context)
        repository.save(TtsProfile(id = "v1", name = "Personal", speechKey = "k", region = "eastus"))

        repository.delete("v1")

        assertNull(repository.activeProfileId.value)
        assertEquals(emptyList<TtsProfile>(), repository.profiles.first { it.isEmpty() })
        // Nothing is compiled in to fall back on, so the app goes mute until the
        // user saves another profile.
        assertNull(repository.activeProfile.first { it == null })
    }

    @Test
    fun `a blank voice field speaks with the default voice`() {
        val profile = TtsProfile(name = "Azure", speechKey = "k", region = "eastus")

        assertEquals(TtsProfile.DEFAULT_VOICE, profile.effectiveVoice)
        assertEquals(TtsProfile.DEFAULT_VOICE, profile.toConfig().voice)
        // A voice that was typed in is used verbatim.
        assertEquals("en-GB-RyanNeural", profile.copy(voice = "en-GB-RyanNeural").toConfig().voice)
    }
}
