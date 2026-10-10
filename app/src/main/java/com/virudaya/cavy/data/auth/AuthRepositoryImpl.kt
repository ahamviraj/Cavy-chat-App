package com.virudaya.cavy.data.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.virudaya.cavy.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: CavyUser?
        get() = firebaseAuth.currentUser?.toCavyUser()

    override val authStateFlow: Flow<CavyUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            val user = auth.currentUser
            trySend(user?.toCavyUser())
            if (user != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    NotificationHelper.saveFcmToken(user.uid)
                }
            }
        }
        firebaseAuth.addAuthStateListener(listener)
        val initialUser = firebaseAuth.currentUser
        trySend(initialUser?.toCavyUser())
        if (initialUser != null) {
            CoroutineScope(Dispatchers.IO).launch {
                NotificationHelper.saveFcmToken(initialUser.uid)
            }
        }
        awaitClose {
            firebaseAuth.removeAuthStateListener(listener)
        }
    }

    override suspend fun signInWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.signInWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                NotificationHelper.saveFcmToken(user.uid)
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    override suspend fun signUpWithEmail(email: String, password: String): AuthResult {
        return try {
            val result = firebaseAuth.createUserWithEmailAndPassword(email.trim(), password).await()
            val user = result.user
            if (user != null) {
                NotificationHelper.saveFcmToken(user.uid)
            }
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    override suspend fun sendPasswordResetEmail(email: String): AuthResult {
        return try {
            firebaseAuth.sendPasswordResetEmail(email.trim()).await()
            AuthResult.Success
        } catch (e: Exception) {
            AuthResult.Error(mapAuthException(e))
        }
    }

    override fun signOut() {
        val uid = firebaseAuth.currentUser?.uid
        if (!uid.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                NotificationHelper.clearFcmToken(uid)
            }
        }
        firebaseAuth.signOut()
    }

    private fun mapAuthException(e: Exception): String {
        return when (e) {
            is FirebaseAuthWeakPasswordException -> "Password is too weak. Please use at least 6 characters."
            is FirebaseAuthInvalidUserException -> "No account found with this email."
            is FirebaseAuthInvalidCredentialsException -> "Invalid email or password."
            is FirebaseAuthUserCollisionException -> "An account with this email already exists."
            is FirebaseAuthException -> e.localizedMessage ?: "Authentication error occurred."
            else -> e.localizedMessage ?: "An unexpected error occurred. Please try again."
        }
    }
}
