package com.virudaya.cavy.data.profile

data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L
)

data class PrivateProfile(
    val email: String = ""
)
