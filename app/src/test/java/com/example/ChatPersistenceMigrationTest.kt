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
 * persisted in Room. Also verifies the v1 -> v2 upgrade is non-destructive so existing
 * users keep their profile.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ChatPersistenceMigrationTest {

    @Test
    fun `v1 to v2 migration keeps the profile and persists chat`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()

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
            version = 1 // PRAGMA user_version; tells Room to run MIGRATION_1_2 on open.
            close()
        }

        // Opening the real database runs MIGRATION_1_2. Room then validates the migrated
        // schema against ChatMessageEntity, so a wrong CREATE TABLE would throw right here.
        val db = AppDatabase.getInstance(context)

        // The pre-existing profile must survive the upgrade (non-destructive migration).
        val profile = db.userDao().getUserProfile()
        assertNotNull("Profile should survive the v1 -> v2 upgrade", profile)
        assertEquals("B1", profile!!.englishLevel)
        assertEquals("Travel abroad", profile.learningGoal)
        assertEquals(listOf("movies"), profile.topicsOfInterest)
        assertEquals(15, profile.dailyPracticeTime)

        // A chat message now round-trips through SQLite instead of living only in memory,
        // which is exactly what makes conversations survive the app process being killed.
        val message = ChatMessageEntity(
            id = "msg-1",
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
}
