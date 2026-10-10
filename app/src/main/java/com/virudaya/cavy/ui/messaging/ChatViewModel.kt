package com.virudaya.cavy.ui.messaging

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virudaya.cavy.data.auth.AuthRepository
import com.virudaya.cavy.data.auth.AuthRepositoryImpl
import com.virudaya.cavy.data.messaging.MessagingRepository
import com.virudaya.cavy.data.messaging.MessagingRepositoryImpl
import com.virudaya.cavy.data.messaging.MessagingResult
import com.virudaya.cavy.data.profile.ProfileRepository
import com.virudaya.cavy.data.profile.ProfileRepositoryImpl
import com.virudaya.cavy.data.profile.ProfileResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val messagingRepository: MessagingRepository = MessagingRepositoryImpl(),
    private val profileRepository: ProfileRepository = ProfileRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null
    private var currentDisplayName: String = "Cavy User"
    private var messagesJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                if (user != null) {
                    currentUid = user.uid
                    loadCurrentUserDisplayName(user.uid)
                } else {
                    currentUid = null
                    messagesJob?.cancel()
                    _uiState.value = ChatUiState(isLoading = false)
                }
            }
        }
    }

    private fun loadCurrentUserDisplayName(uid: String) {
        viewModelScope.launch {
            profileRepository.getProfileFlow(uid).collect { result ->
                if (result is ProfileResult.Success) {
                    currentDisplayName = result.data.displayName
                }
            }
        }
    }

    fun initializeChat(conversationId: String, otherUid: String, otherDisplayName: String) {
        _uiState.update {
            it.copy(
                conversationId = conversationId,
                otherUid = otherUid,
                otherDisplayName = otherDisplayName.ifBlank { "Cavy User" },
                isLoading = true,
                errorMessage = null
            )
        }

        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            messagingRepository.getMessagesFlow(conversationId).collect { result ->
                when (result) {
                    is MessagingResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                    is MessagingResult.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                messages = result.data,
                                errorMessage = null
                            )
                        }
                    }
                    is MessagingResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                errorMessage = result.message
                            )
                        }
                    }
                }
            }
        }
    }

    fun onMessageInputChanged(input: String) {
        _uiState.update { it.copy(messageInput = input, errorMessage = null) }
    }

    fun sendMessage() {
        val currentState = _uiState.value
        val text = currentState.messageInput.trim()
        val uid = currentUid ?: return

        if (text.isEmpty()) return
        if (text.length > 4000) {
            _uiState.update { it.copy(errorMessage = "Message exceeds 4000 character limit.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSending = true, errorMessage = null) }

            val result = messagingRepository.sendMessage(
                conversationId = currentState.conversationId,
                senderUid = uid,
                otherUid = currentState.otherUid,
                otherDisplayName = currentState.otherDisplayName,
                senderDisplayName = currentDisplayName,
                text = text
            )

            _uiState.update { it.copy(isSending = false) }

            when (result) {
                is MessagingResult.Success -> {
                    _uiState.update { it.copy(messageInput = "") }
                }
                is MessagingResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
