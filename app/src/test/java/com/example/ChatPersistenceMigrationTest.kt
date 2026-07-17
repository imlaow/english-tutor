package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.ChatMessageEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Guards the fix for "AI conversations disappear after exiting the app": chat is now
 * persisted in Room. Also verifies the upgrade chain from older schema versions is
 * non-destructive so existing users keep their profile and messages.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatPersistenceMigrationTest {

    // AppDatabase caches its instance in a static field that survives between
    // Robolectric tests, so each test resets it to get a fresh open (and fresh
    // migrations) against the database file it just prepared.
    private fun resetDatabaseSingleton() {
        val field = AppDatabase::class.java.getDeclaredField("instance")
        field.isAccessible = true
        (field.get(null) as? AppDatabase)?.close()
        field.set(null, null)
    }

    @Test
    fun `v1 to v3 migration keeps the profile and persists chat`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        resetDatabaseSingleton()

        // Simulate a user who installed at schema v1 (profile table only, no chat table).
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
                "INSERT INTO user_profile " +
                    "(id, english_level, learning_goal, topics_of_interest, daily_practice_time) " +
                    "VALUES (1, 'B1', 'Travel abroad', 'movies', 15)"
            )
            version = 1 // PRAGMA user_version; tells Room to run the migration chain on open.
            close()
        }

        // Opening the real database runs MIGRATION_1_2 then MIGRATION_2_3. Room validates
        // the migrated schema against the entities, so a wrong migration would throw here.
        val db = AppDatabase.getInstance(context)

        // The pre-existing profile must survive the upgrade (non-destructive migration).
        val profile = db.userDao().getUserProfile()
        assertNotNull("Profile should survive the v1 -> v3 upgrade", profile)
        assertEquals("B1", profile!!.englishLevel)
        assertEquals("Travel abroad", profile.learningGoal)
        assertEquals(listOf("movies"), profile.topicsOfInterest)
        assertEquals(15, profile.dailyPracticeTime)

        // A chat message now round-trips through SQLite instead of living only in memory,
        // which is exactly what makes conversations survive the app process being killed.
        val message = ChatMessageEntity(
            id = "msg-1",
            sessionId = "session-1",
            userText = "How do I order coffee?",
            aiResponse = "You could say: 'A latte, please.' What size would you like?",
            grammarCorrection = null,
            timestamp = 1_000L
        )
        db.chatMessageDao().insert(message)

        val stored = db.chatMessageDao().observeAll().first()
        assertEquals(1, stored.size)
        assertEquals(message, stored.first())
    }

    @Test
    fun `v2 to v3 migration groups existing messages into the legacy session`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        resetDatabaseSingleton()

        // Simulate a user upgrading from schema v2: chat_message exists but has no
        // session_id column yet, and already holds a message.
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
                    "PRIMARY KEY(`id`))"
            )
            execSQL(
                "INSERT INTO chat_message (id, user_text, ai_response, grammar_correction, timestamp) " +
                    "VALUES ('old-msg', 'Hello!', 'Hi there! What did you do today?', NULL, 500)"
            )
            version = 2
            close()
        }

        val db = AppDatabase.getInstance(context)

        val stored = db.chatMessageDao().observeAll().first()
        assertEquals(1, stored.size)
        assertEquals("old-msg", stored.first().id)
        assertEquals("legacy", stored.first().sessionId)
    }
}
