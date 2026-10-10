package com.virudaya.cavy.data.profile

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ProfileRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app")
) : ProfileRepository {

    private val profilesRef = database.getReference("profiles")
    private val privateProfilesRef = database.getReference("privateProfiles")

    override fun getProfileFlow(uid: String): Flow<ProfileResult<UserProfile>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(ProfileResult.Error("Unauthenticated: Invalid user ID"))
            close()
            return@callbackFlow
        }

        trySend(ProfileResult.Loading)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(ProfileResult.Error("Profile not found in database"))
                    return
                }

                try {
                    val displayName = snapshot.child("displayName").getValue(String::class.java) ?: "Cavy Member"
                    val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                    val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                    val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                    val profile = UserProfile(
                        uid = uid,
                        displayName = displayName,
                        photoUrl = photoUrl,
                        createdAt = createdAt,
                        updatedAt = updatedAt
                    )
                    trySend(ProfileResult.Success(profile))
                } catch (e: Exception) {
                    trySend(ProfileResult.Error(e.localizedMessage ?: "Failed to parse profile data"))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(ProfileResult.Error("Firebase Database error: ${error.message}"))
            }
        }

        val userRef = profilesRef.child(uid)
        userRef.addValueEventListener(listener)

        awaitClose {
            userRef.removeEventListener(listener)
        }
    }

    override fun getPrivateProfileFlow(uid: String): Flow<ProfileResult<PrivateProfile>> = callbackFlow {
        if (uid.isBlank()) {
            trySend(ProfileResult.Error("Unauthenticated: Invalid user ID"))
            close()
            return@callbackFlow
        }

        trySend(ProfileResult.Loading)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    trySend(ProfileResult.Error("Private profile not found"))
                    return
                }

                val email = snapshot.child("email").getValue(String::class.java) ?: ""
                trySend(ProfileResult.Success(PrivateProfile(email = email)))
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(ProfileResult.Error("Firebase Database error: ${error.message}"))
            }
        }

        val privateRef = privateProfilesRef.child(uid)
        privateRef.addValueEventListener(listener)

        awaitClose {
            privateRef.removeEventListener(listener)
        }
    }

    override suspend fun ensureProfileInitialized(
        uid: String,
        email: String?,
        fallbackDisplayName: String?
    ): ProfileResult<UserProfile> {
        if (uid.isBlank()) {
            return ProfileResult.Error("Unauthenticated: Invalid user ID")
        }

        return try {
            val userRef = profilesRef.child(uid)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val displayName = snapshot.child("displayName").getValue(String::class.java) ?: "Cavy Member"
                val photoUrl = snapshot.child("photoUrl").getValue(String::class.java)
                val createdAt = snapshot.child("createdAt").getValue(Long::class.java) ?: System.currentTimeMillis()
                val updatedAt = snapshot.child("updatedAt").getValue(Long::class.java) ?: System.currentTimeMillis()

                return ProfileResult.Success(
                    UserProfile(uid, displayName, photoUrl, createdAt, updatedAt)
                )
            }

            val initialDisplayName = when {
                !fallbackDisplayName.isNullOrBlank() -> fallbackDisplayName.trim()
                !email.isNullOrBlank() && email.contains("@") -> email.substringBefore("@").replace(".", " ")
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                else -> "Cavy Member"
            }

            val currentTime = System.currentTimeMillis()
            val multiPathUpdates = mutableMapOf<String, Any>()

            val profileData = mapOf(
                "displayName" to initialDisplayName,
                "createdAt" to currentTime,
                "updatedAt" to currentTime
            )
            multiPathUpdates["/profiles/$uid"] = profileData

            if (!email.isNullOrBlank()) {
                multiPathUpdates["/privateProfiles/$uid"] = mapOf("email" to email.trim())
            }

            database.reference.updateChildren(multiPathUpdates).await()

            ProfileResult.Success(
                UserProfile(
                    uid = uid,
                    displayName = initialDisplayName,
                    photoUrl = null,
                    createdAt = currentTime,
                    updatedAt = currentTime
                )
            )
        } catch (e: Exception) {
            ProfileResult.Error("Profile initialization failed: ${e.localizedMessage ?: "Permission denied or network failure"}")
        }
    }

    override suspend fun updateDisplayName(uid: String, newDisplayName: String): ProfileResult<Unit> {
        val trimmed = newDisplayName.trim()
        if (uid.isBlank()) {
            return ProfileResult.Error("Unauthenticated: Invalid user ID")
        }
        if (trimmed.length < 2 || trimmed.length > 50) {
            return ProfileResult.Error("Display name must be between 2 and 50 characters.")
        }

        return try {
            val updates = mapOf<String, Any>(
                "displayName" to trimmed,
                "updatedAt" to System.currentTimeMillis()
            )
            profilesRef.child(uid).updateChildren(updates).await()
            ProfileResult.Success(Unit)
        } catch (e: Exception) {
            ProfileResult.Error("Update failed: ${e.localizedMessage ?: "Permission denied or network failure"}")
        }
    }

    override suspend fun checkEngineConnection(uid: String): ProfileResult<String> {
        if (uid.isBlank()) {
            return ProfileResult.Error("Engine status check failed: User is unauthenticated.")
        }

        return try {
            val snapshot = profilesRef.child(uid).get().await()
            if (snapshot.exists()) {
                ProfileResult.Success("CAVY Data Engine active & verified (RTDB connected for UID: ${uid.take(8)})")
            } else {
                ProfileResult.Error("Engine status check: Connected to RTDB, but profile record not found.")
            }
        } catch (e: Exception) {
            ProfileResult.Error("Engine status check failed: ${e.localizedMessage ?: "Permission denied"}")
        }
    }
}
