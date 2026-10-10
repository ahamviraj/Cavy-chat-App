package com.virudaya.cavy.ui.messaging

import com.virudaya.cavy.data.messaging.ChatMessage
import com.virudaya.cavy.data.messaging.ConversationSummary
import com.virudaya.cavy.data.profile.UserProfile

data class ConversationListUiState(
    val isLoading: Boolean = true,
    val conversations: List<ConversationSummary> = emptyList(),
    val isSearchingUsers: Boolean = false,
    val userSearchQuery: String = "",
    val availableUsers: List<UserProfile> = emptyList(),
    val isSearchingLoading: Boolean = false,
    val errorMessage: String? = null
)

data class ChatUiState(
    val conversationId: String = "",
    val otherUid: String = "",
    val otherDisplayName: String = "Cavy User",
    val messages: List<ChatMessage> = emptyList(),
    val messageInput: String = "",
    val isLoading: Boolean = true,
    val isSending: Boolean = false,
    val errorMessage: String? = null
)
