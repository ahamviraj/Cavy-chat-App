package com.virudaya.cavy.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virudaya.cavy.data.auth.AuthRepository
import com.virudaya.cavy.data.auth.AuthRepositoryImpl
import com.virudaya.cavy.data.profile.ProfileRepository
import com.virudaya.cavy.data.profile.ProfileRepositoryImpl
import com.virudaya.cavy.data.profile.ProfileResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
    private val profileRepository: ProfileRepository = ProfileRepositoryImpl()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private var currentUid: String? = null

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                if (user != null) {
                    if (currentUid != user.uid) {
                        currentUid = user.uid
                        _uiState.update { it.copy(authenticatedUid = user.uid) }
                        initializeAndObserveProfile(user.uid, user.email)
                    }
                } else {
                    currentUid = null
                    _uiState.value = ProfileUiState(isLoading = false, authenticatedUid = null)
                }
            }
        }
    }

    private fun initializeAndObserveProfile(uid: String, email: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, authenticatedUid = uid, email = email ?: "") }

            // Step 1: Ensure profile initialized in RTDB safely
            val initResult = profileRepository.ensureProfileInitialized(
                uid = uid,
                email = email
            )

            if (initResult is ProfileResult.Error) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = initResult.message
                    )
                }
            }

            // Step 2: Observe profile flow
            launch {
                profileRepository.getProfileFlow(uid).collect { result ->
                    when (result) {
                        is ProfileResult.Loading -> {
                            _uiState.update { state -> state.copy(isLoading = true) }
                        }
                        is ProfileResult.Success -> {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    userProfile = result.data,
                                    editDisplayNameInput = if (!state.isEditingDisplayName) result.data.displayName else state.editDisplayNameInput,
                                    errorMessage = null
                                )
                            }
                        }
                        is ProfileResult.Error -> {
                            _uiState.update { state ->
                                state.copy(
                                    isLoading = false,
                                    errorMessage = result.message
                                )
                            }
                        }
                    }
                }
            }

            // Step 3: Observe private profile flow for email
            launch {
                profileRepository.getPrivateProfileFlow(uid).collect { result ->
                    if (result is ProfileResult.Success) {
                        _uiState.update { state ->
                            state.copy(email = result.data.email.ifBlank { state.email })
                        }
                    }
                }
            }
        }
    }

    fun startEditingDisplayName() {
        _uiState.update {
            it.copy(
                isEditingDisplayName = true,
                editDisplayNameInput = it.userProfile?.displayName ?: "",
                errorMessage = null,
                successMessage = null
            )
        }
    }

    fun onDisplayNameInputChanged(input: String) {
        _uiState.update {
            it.copy(
                editDisplayNameInput = input,
                errorMessage = null
            )
        }
    }

    fun cancelEditingDisplayName() {
        _uiState.update {
            it.copy(
                isEditingDisplayName = false,
                editDisplayNameInput = it.userProfile?.displayName ?: "",
                errorMessage = null
            )
        }
    }

    fun saveDisplayName() {
        val uid = currentUid ?: return
        val newName = _uiState.value.editDisplayNameInput.trim()

        if (newName.length < 2 || newName.length > 50) {
            _uiState.update {
                it.copy(errorMessage = "Display name must be between 2 and 50 characters.")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, successMessage = null) }
            val result = profileRepository.updateDisplayName(uid, newName)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is ProfileResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isEditingDisplayName = false,
                            successMessage = "Profile updated successfully!"
                        )
                    }
                }
                is ProfileResult.Error -> {
                    _uiState.update {
                        it.copy(errorMessage = result.message)
                    }
                }
                else -> {}
            }
        }
    }

    fun checkEngineStatus(onResult: (String) -> Unit) {
        val uid = currentUid
        if (uid.isNullOrBlank()) {
            onResult("Engine check failed: Unauthenticated user session.")
            return
        }

        viewModelScope.launch {
            val result = profileRepository.checkEngineConnection(uid)
            when (result) {
                is ProfileResult.Success -> onResult(result.data)
                is ProfileResult.Error -> onResult(result.message)
                else -> onResult("Engine check failed")
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
