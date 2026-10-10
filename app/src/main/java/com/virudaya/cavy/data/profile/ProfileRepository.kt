package com.virudaya.cavy.data.profile

import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun getProfileFlow(uid: String): Flow<ProfileResult<UserProfile>>
    fun getPrivateProfileFlow(uid: String): Flow<ProfileResult<PrivateProfile>>
    suspend fun ensureProfileInitialized(
        uid: String,
        email: String?,
        fallbackDisplayName: String? = null
    ): ProfileResult<UserProfile>
    suspend fun updateDisplayName(uid: String, newDisplayName: String): ProfileResult<Unit>
    suspend fun checkEngineConnection(uid: String): ProfileResult<String>
}
