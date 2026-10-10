package com.virudaya.cavy.data.messaging

data class ConversationSummary(
    val conversationId: String = "",
    val otherUid: String = "",
    val otherDisplayName: String = "",
    val lastMessageText: String = "",
    val lastMessageSenderUid: String = "",
    val lastMessageTimestamp: Long = 0L,
    val updatedAt: Long = 0L
)
