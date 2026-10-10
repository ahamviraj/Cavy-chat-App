package com.virudaya.cavy.data.messaging

import com.virudaya.cavy.data.profile.UserProfile
import kotlinx.coroutines.flow.Flow

interface MessagingRepository {
    fun generateConversationId(uidA: String, uidB: String): String
    suspend fun getOrCreateConversation(
        currentUid: String,
        currentDisplayName: String,
        otherUid: String,
        otherDisplayName: String
    ): MessagingResult<String>

    fun getConversationsFlow(currentUid: String): Flow<MessagingResult<List<ConversationSummary>>>
    fun getMessagesFlow(conversationId: String): Flow<MessagingResult<List<ChatMessage>>>

    suspend fun sendMessage(
        conversationId: String,
        senderUid: String,
        otherUid: String,
        otherDisplayName: String,
        senderDisplayName: String,
        text: String
    ): MessagingResult<Unit>

    suspend fun searchUsers(currentUid: String, query: String): MessagingResult<List<UserProfile>>
}
