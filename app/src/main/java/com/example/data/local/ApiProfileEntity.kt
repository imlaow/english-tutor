package com.example.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One saved model API configuration. Several profiles may share an API spec
 * (e.g. two different OpenAI-compatible endpoints); only the profile the user
 * marked active is used for requests.
 */
@Entity(tableName = "api_profile")
data class ApiProfileEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    // ApiSpec.name; stored as text so adding a spec doesn't shift existing rows.
    @ColumnInfo(name = "api_spec")
    val apiSpec: String,

    // Blank base URL and model mean "use the spec's default".
    @ColumnInfo(name = "base_url")
    val baseUrl: String,

    @ColumnInfo(name = "api_key")
    val apiKey: String,

    @ColumnInfo(name = "model")
    val model: String,

    @ColumnInfo(name = "enabled")
    val enabled: Boolean,

    @ColumnInfo(name = "sort_order")
    val sortOrder: Int
)
