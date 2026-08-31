package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        UserProfileEntity::class,
        ChatMessageEntity::class,
        ApiProfileEntity::class,
        TtsProfileEntity::class
    ],
    version = 5,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao

    abstract fun chatMessageDao(): ChatMessageDao

    abstract fun apiProfileDao(): ApiProfileDao

    abstract fun ttsProfileDao(): TtsProfileDao

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

        // v4 replaces the single global model API configuration (previously kept in
        // SharedPreferences) with a list of named profiles. Nothing is carried over:
        // the table starts empty and the user re-enters their API key once.
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
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
            }
        }

        // v5 gives text-to-speech the same treatment v4 gave the model APIs: the
        // Azure key, region and voice move out of BuildConfig (where only whoever
        // built the APK could set them) into a list of named profiles the user
        // edits in Settings. Nothing is carried over — the table starts empty, and
        // a build that still has the keys in its `.env` keeps using them until the
        // first profile is saved.
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `tts_profile` (" +
                        "`id` TEXT NOT NULL, " +
                        "`name` TEXT NOT NULL, " +
                        "`speech_key` TEXT NOT NULL, " +
                        "`region` TEXT NOT NULL, " +
                        "`voice` TEXT NOT NULL, " +
                        "`enabled` INTEGER NOT NULL, " +
                        "`sort_order` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`id`))"
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
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
                    .build()
                    .also { instance = it }
            }
    }
}
