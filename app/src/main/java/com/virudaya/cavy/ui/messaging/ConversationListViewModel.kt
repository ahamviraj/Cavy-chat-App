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
import com.virudaya.cavy.data.profile.UserProfile
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ConversationListViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val messagingRepository: MessagingRepository = MessagingRepositoryImpl(),
    private val profileRepository: ProfileRepository = ProfileRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationListUiState())
    val uiState: StateFlow<ConversationListUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null
    private var currentDisplayName: String = "Cavy User"
    private var conversationsJob: Job? = null

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                if (user != null) {
                    if (currentUid != user.uid) {
                        currentUid = user.uid
                        loadUserProfileAndConversations(user.uid, user.email)
                    }
                } else {
                    currentUid = null
                    conversationsJob?.cancel()
                    _uiState.value = ConversationListUiState(isLoading = false)
                }
            }
        }
    }

    private fun loadUserProfileAndConversations(uid: String, email: String?) {
        viewModelScope.launch {
            launch {
                profileRepository.getProfileFlow(uid).collect { profileRes ->
                    if (profileRes is ProfileResult.Success) {
                        currentDisplayName = profileRes.data.displayName
                    }
                }
            }

            conversationsJob?.cancel()
            conversationsJob = launch {
                messagingRepository.getConversationsFlow(uid).collect { result ->
                    when (result) {
                        is MessagingResult.Loading -> {
                            _uiState.update { it.copy(isLoading = true) }
                        }
                        is MessagingResult.Success -> {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    conversations = result.data,
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
    }

    fun openUserDiscovery() {
        _uiState.update { it.copy(isSearchingUsers = true, userSearchQuery = "", errorMessage = null) }
        searchUsers("")
    }

    fun closeUserDiscovery() {
        _uiState.update { it.copy(isSearchingUsers = false, availableUsers = emptyList()) }
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(userSearchQuery = query) }
        searchUsers(query)
    }

    private fun searchUsers(query: String) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSearchingLoading = true) }
            val result = messagingRepository.searchUsers(uid, query)
            _uiState.update { it.copy(isSearchingLoading = false) }

            when (result) {
                is MessagingResult.Success -> {
                    _uiState.update { it.copy(availableUsers = result.data) }
                }
                is MessagingResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun startConversationWithUser(
        otherUser: UserProfile,
        onConversationReady: (conversationId: String, otherUid: String, otherDisplayName: String) -> Unit
    ) {
        val uid = currentUid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val result = messagingRepository.getOrCreateConversation(
                currentUid = uid,
                currentDisplayName = currentDisplayName,
                otherUid = otherUser.uid,
                otherDisplayName = otherUser.displayName
            )
            _uiState.update { it.copy(isLoading = false, isSearchingUsers = false) }

            when (result) {
                is MessagingResult.Success -> {
                    onConversationReady(result.data, otherUser.uid, otherUser.displayName)
                }
                is MessagingResult.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
                else -> {}
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
