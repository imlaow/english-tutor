package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUserProfile(profile: UserProfileEntity)

    @Query("SELECT * FROM user_profile WHERE id = :id")
    suspend fun getUserProfile(id: Int = UserProfileEntity.SINGLE_PROFILE_ID): UserProfileEntity?

    @Query("SELECT * FROM user_profile WHERE id = :id")
    fun observeUserProfile(id: Int = UserProfileEntity.SINGLE_PROFILE_ID): Flow<UserProfileEntity?>

    @Query("DELETE FROM user_profile")
    suspend fun clearUserProfile()
}
