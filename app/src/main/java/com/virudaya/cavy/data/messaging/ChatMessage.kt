package com.virudaya.cavy.data.messaging

data class ChatMessage(
    val messageId: String = "",
    val conversationId: String = "",
    val senderUid: String = "",
    val text: String = "",
    val sentAt: Long = 0L
)
