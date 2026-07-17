package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [UserProfileEntity::class, ChatMessageEntity::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun chatMessageDao(): ChatMessageDao

    companion object {

        private const val DATABASE_NAME = "app_database"

        // v2 adds the chat_message table so AI conversations persist across app
        // restarts. Non-destructive: existing user_profile rows are left in place.
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `chat_message` (" +
                        "`id` TEXT NOT NULL, " +
                        "`user_text` TEXT NOT NULL, " +
                        "`ai_response` TEXT NOT NULL, " +
                        "`grammar_correction` TEXT, " +
                        "`timestamp` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
                )
            }
        }

        // v3 tags every message with the conversation (session) it belongs to, so
        // history can be grouped into conversations and a past one resumed. All
        // pre-v3 messages end up in a single "legacy" conversation.
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `chat_message` ADD COLUMN `session_id` TEXT NOT NULL DEFAULT 'legacy'"
                )
            }
        }

        @Volatile
        private var instance: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
    }
}
