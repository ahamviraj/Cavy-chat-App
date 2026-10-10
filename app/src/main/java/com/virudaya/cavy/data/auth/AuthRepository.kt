package com.virudaya.cavy.data.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val currentUser: CavyUser?
    val authStateFlow: Flow<CavyUser?>

    suspend fun signInWithEmail(email: String, password: String): AuthResult
    suspend fun signUpWithEmail(email: String, password: String): AuthResult
    suspend fun sendPasswordResetEmail(email: String): AuthResult
    fun signOut()
}
