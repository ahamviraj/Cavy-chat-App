package com.virudaya.cavy.data.messaging

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.virudaya.cavy.data.profile.UserProfile
import com.virudaya.cavy.util.NotificationHelper
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class MessagingRepositoryImpl(
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance("https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app")
) : MessagingRepository {

    private val conversationsRef = database.getReference("conversations")
    private val userConversationsRef = database.getReference("userConversations")
    private val profilesRef = database.getReference("profiles")

    override fun generateConversationId(uidA: String, uidB: String): String {
        return if (uidA < uidB) "${uidA}_${uidB}" else "${uidB}_${uidA}"
    }

    override suspend fun getOrCreateConversation(
        currentUid: String,
        currentDisplayName: String,
        otherUid: String,
        otherDisplayName: String
    ): MessagingResult<String> {
        if (currentUid.isBlank() || otherUid.isBlank()) {
            return MessagingResult.Error("Invalid user session.")
        }
        if (currentUid == otherUid) {
            return MessagingResult.Error("Cannot start a conversation with yourself.")
        }

        val conversationId = generateConversationId(currentUid, otherUid)

        return try {
            val userConvSnapshot = userConversationsRef.child(currentUid).child(conversationId).get().await()
            if (!userConvSnapshot.exists()) {
                val currentTime = System.currentTimeMillis()
                val updates = mutableMapOf<String, Any>()

                updates["/conversations/$conversationId"] = mapOf(
                    "creatorUid" to currentUid,
                    "createdAt" to currentTime,
                    "participants" to mapOf(currentUid to true, otherUid to true)
                )

                updates["/userConversations/$currentUid/$conversationId"] = mapOf(
                    "otherUid" to otherUid,
                    "otherDisplayName" to otherDisplayName.ifBlank { "Cavy User" },
                    "lastMessageText" to "",
                    "lastMessageSenderUid" to "",
                    "lastMessageTimestamp" to currentTime,
                    "updatedAt" to currentTime
                )

                updates["/userConversations/$otherUid/$conversationId"] = mapOf(
                    "otherUid" to currentUid,
                    "otherDisplayName" to currentDisplayName.ifBlank { "Cavy User" },
                    "lastMessageText" to "",
                    "lastMessageSenderUid" to "",
                    "lastMessageTimestamp" to currentTime,
                    "updatedAt" to currentTime
                )

                database.reference.updateChildren(updates).await()
            }

            MessagingResult.Success(conversationId)
        } catch (e: Exception) {
            Log.e("CavyMessaging", "getOrCreateConversation failed for $conversationId: ${e.localizedMessage}")
            MessagingResult.Error(e.localizedMessage ?: "Failed to create conversation.")
        }
    }

    override fun getConversationsFlow(currentUid: String): Flow<MessagingResult<List<ConversationSummary>>> = callbackFlow {
        if (currentUid.isBlank()) {
            trySend(MessagingResult.Error("Unauthenticated: Invalid user ID"))
            close()
            return@callbackFlow
        }

        trySend(MessagingResult.Loading)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ConversationSummary>()
                for (child in snapshot.children) {
                    val conversationId = child.key ?: continue
                    val otherUid = child.child("otherUid").getValue(String::class.java) ?: continue
                    val otherDisplayName = child.child("otherDisplayName").getValue(String::class.java) ?: "Cavy User"
                    val lastMessageText = child.child("lastMessageText").getValue(String::class.java) ?: ""
                    val lastMessageSenderUid = child.child("lastMessageSenderUid").getValue(String::class.java) ?: ""
                    val lastMessageTimestamp = child.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0L
                    val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L

                    list.add(
                        ConversationSummary(
                            conversationId = conversationId,
                            otherUid = otherUid,
                            otherDisplayName = otherDisplayName,
                            lastMessageText = lastMessageText,
                            lastMessageSenderUid = lastMessageSenderUid,
                            lastMessageTimestamp = lastMessageTimestamp,
                            updatedAt = updatedAt
                        )
                    )
                }

                list.sortByDescending { it.updatedAt }
                trySend(MessagingResult.Success(list))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CavyMessaging", "getConversationsFlow cancelled for UID slice: ${currentUid.take(8)} - Code: ${error.code}, Message: ${error.message}")
                trySend(MessagingResult.Error("Firebase Database error: ${error.message}"))
            }
        }

        val ref = userConversationsRef.child(currentUid)
        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override fun getMessagesFlow(conversationId: String): Flow<MessagingResult<List<ChatMessage>>> = callbackFlow {
        if (conversationId.isBlank()) {
            trySend(MessagingResult.Error("Invalid conversation ID"))
            close()
            return@callbackFlow
        }

        trySend(MessagingResult.Loading)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ChatMessage>()
                for (child in snapshot.children) {
                    val messageId = child.key ?: continue
                    val senderUid = child.child("senderUid").getValue(String::class.java) ?: continue
                    val text = child.child("text").getValue(String::class.java) ?: ""
                    val sentAt = child.child("sentAt").getValue(Long::class.java) ?: 0L

                    list.add(
                        ChatMessage(
                            messageId = messageId,
                            conversationId = conversationId,
                            senderUid = senderUid,
                            text = text,
                            sentAt = sentAt
                        )
                    )
                }

                list.sortBy { it.sentAt }
                trySend(MessagingResult.Success(list))
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("CavyMessaging", "getMessagesFlow cancelled for conversation $conversationId - Code: ${error.code}, Message: ${error.message}")
                trySend(MessagingResult.Error("Firebase Database error: ${error.message}"))
            }
        }

        val ref = conversationsRef.child(conversationId).child("messages")
        ref.addValueEventListener(listener)

        awaitClose {
            ref.removeEventListener(listener)
        }
    }

    override suspend fun sendMessage(
        conversationId: String,
        senderUid: String,
        otherUid: String,
        otherDisplayName: String,
        senderDisplayName: String,
        text: String
    ): MessagingResult<Unit> {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return MessagingResult.Error("Message cannot be empty.")
        }
        if (trimmed.length > 4000) {
            return MessagingResult.Error("Message exceeds 4000 character limit.")
        }
        if (conversationId.isBlank() || senderUid.isBlank() || otherUid.isBlank()) {
            return MessagingResult.Error("Invalid conversation details.")
        }

        return try {
            val messageRef = conversationsRef.child(conversationId).child("messages").push()
            val messageId = messageRef.key ?: return MessagingResult.Error("Failed to generate message ID.")
            val currentTime = System.currentTimeMillis()

            val messageData = mapOf(
                "senderUid" to senderUid,
                "text" to trimmed,
                "sentAt" to currentTime
            )

            val updates = mutableMapOf<String, Any>()
            updates["/conversations/$conversationId/messages/$messageId"] = messageData
            updates["/conversations/$conversationId/lastMessageText"] = trimmed
            updates["/conversations/$conversationId/lastMessageSenderUid"] = senderUid
            updates["/conversations/$conversationId/lastMessageTimestamp"] = currentTime

            updates["/userConversations/$senderUid/$conversationId"] = mapOf(
                "otherUid" to otherUid,
                "otherDisplayName" to otherDisplayName.ifBlank { "Cavy User" },
                "lastMessageText" to trimmed,
                "lastMessageSenderUid" to senderUid,
                "lastMessageTimestamp" to currentTime,
                "updatedAt" to currentTime
            )

            updates["/userConversations/$otherUid/$conversationId"] = mapOf(
                "otherUid" to senderUid,
                "otherDisplayName" to senderDisplayName.ifBlank { "Cavy User" },
                "lastMessageText" to trimmed,
                "lastMessageSenderUid" to senderUid,
                "lastMessageTimestamp" to currentTime,
                "updatedAt" to currentTime
            )

            database.reference.updateChildren(updates).await()

            // Trigger push notification to Vercel server backend AFTER database write succeeds
            NotificationHelper.triggerPushNotificationServer(
                conversationId = conversationId,
                messageText = trimmed
            )

            MessagingResult.Success(Unit)
        } catch (e: Exception) {
            Log.e("CavyMessaging", "sendMessage failed for $conversationId: ${e.localizedMessage}")
            MessagingResult.Error(e.localizedMessage ?: "Failed to send message.")
        }
    }

    override suspend fun searchUsers(currentUid: String, query: String): MessagingResult<List<UserProfile>> {
        return try {
            val snapshot = profilesRef.get().await()
            val users = mutableListOf<UserProfile>()
            val queryTrimmed = query.trim().lowercase()

            for (child in snapshot.children) {
                val uid = child.key ?: continue
                if (uid == currentUid) continue

                val displayName = child.child("displayName").getValue(String::class.java) ?: "Cavy Member"
                val photoUrl = child.child("photoUrl").getValue(String::class.java)
                val createdAt = child.child("createdAt").getValue(Long::class.java) ?: 0L
                val updatedAt = child.child("updatedAt").getValue(Long::class.java) ?: 0L

                if (queryTrimmed.isEmpty() || displayName.lowercase().contains(queryTrimmed)) {
                    users.add(
                        UserProfile(
                            uid = uid,
                            displayName = displayName,
                            photoUrl = photoUrl,
                            createdAt = createdAt,
                            updatedAt = updatedAt
                        )
                    )
                }
            }

            users.sortBy { it.displayName }
            MessagingResult.Success(users)
        } catch (e: Exception) {
            Log.e("CavyMessaging", "searchUsers failed: ${e.localizedMessage}")
            MessagingResult.Error(e.localizedMessage ?: "Failed to fetch Cavy members: ${e.localizedMessage}")
        }
    }
}
