package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter

@Entity(tableName = "user_profile")
data class UserProfileEntity(
    // Single-row table: the app has exactly one local user profile.
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int = SINGLE_PROFILE_ID,

    @ColumnInfo(name = "english_level")
    val englishLevel: String,

    @ColumnInfo(name = "learning_goal")
    val learningGoal: String,

    @ColumnInfo(name = "topics_of_interest")
    val topicsOfInterest: List<String>,

    @ColumnInfo(name = "daily_practice_time")
    val dailyPracticeTime: Int
) {
    companion object {
        const val SINGLE_PROFILE_ID = 1
    }
}

class StringListConverter {

    @TypeConverter
    fun fromList(topics: List<String>): String =
        topics.joinToString(SEPARATOR)

    @TypeConverter
    fun toList(raw: String): List<String> =
        if (raw.isEmpty()) emptyList() else raw.split(SEPARATOR)

    private companion object {
        // Unit Separator control character: safe delimiter for free-form topic text.
        const val SEPARATOR = ""
    }
}
