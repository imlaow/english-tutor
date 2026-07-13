package com.example.data.repository

import com.example.data.local.UserDao
import com.example.data.local.UserProfileEntity
import kotlinx.coroutines.flow.Flow

class ProfileRepository(private val userDao: UserDao) {

    suspend fun saveUserProfile(profile: UserProfileEntity) =
        userDao.upsertUserProfile(profile)

    suspend fun getUserProfile(): UserProfileEntity? =
        userDao.getUserProfile()

    fun observeUserProfile(): Flow<UserProfileEntity?> =
        userDao.observeUserProfile()

    suspend fun clearUserProfile() =
        userDao.clearUserProfile()
}
