package com.virudaya.cavy.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.virudaya.cavy.data.auth.AuthRepository
import com.virudaya.cavy.data.auth.AuthRepositoryImpl
import com.virudaya.cavy.data.auth.AuthResult
import com.virudaya.cavy.util.AuthValidation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository = AuthRepositoryImpl(),
) : ViewModel() {

    private val _sessionState = MutableStateFlow<AuthSessionState>(AuthSessionState.Loading)
    val sessionState: StateFlow<AuthSessionState> = _sessionState.asStateFlow()

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            authRepository.authStateFlow.collect { user ->
                if (user != null) {
                    _sessionState.value = AuthSessionState.Authenticated(user)
                } else {
                    _sessionState.value = AuthSessionState.Unauthenticated
                }
            }
        }
    }

    fun onEmailChanged(email: String) {
        _uiState.update {
            it.copy(
                email = email,
                emailError = null,
                generalError = null,
                infoMessage = null
            )
        }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = null,
                generalError = null,
                infoMessage = null
            )
        }
    }

    fun onConfirmPasswordChanged(password: String) {
        _uiState.update {
            it.copy(
                confirmPassword = password,
                confirmPasswordError = null,
                generalError = null,
                infoMessage = null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(generalError = null, infoMessage = null) }
    }

    fun signIn(onSuccess: (() -> Unit)? = null) {
        val currentState = _uiState.value
        val emailValid = AuthValidation.isValidEmail(currentState.email)
        val passwordValid = currentState.password.isNotBlank()

        if (!emailValid || !passwordValid) {
            _uiState.update {
                it.copy(
                    emailError = if (!emailValid) "Please enter a valid email address." else null,
                    passwordError = if (!passwordValid) "Password cannot be empty." else null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = authRepository.signInWithEmail(currentState.email, currentState.password)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is AuthResult.Success -> {
                    resetFormState()
                    onSuccess?.invoke()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(generalError = result.message) }
                }
            }
        }
    }

    fun register(onSuccess: (() -> Unit)? = null) {
        val currentState = _uiState.value
        val emailValid = AuthValidation.isValidEmail(currentState.email)
        val passwordValid = AuthValidation.isValidPassword(currentState.password)
        val passwordsMatch = AuthValidation.doPasswordsMatch(currentState.password, currentState.confirmPassword)

        if (!emailValid || !passwordValid || !passwordsMatch) {
            _uiState.update {
                it.copy(
                    emailError = if (!emailValid) "Please enter a valid email address." else null,
                    passwordError = if (!passwordValid) "Password must be at least 6 characters." else null,
                    confirmPasswordError = if (!passwordsMatch) "Passwords do not match." else null
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            val result = authRepository.signUpWithEmail(currentState.email, currentState.password)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is AuthResult.Success -> {
                    resetFormState()
                    onSuccess?.invoke()
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(generalError = result.message) }
                }
            }
        }
    }

    fun sendPasswordReset() {
        val currentState = _uiState.value
        val emailValid = AuthValidation.isValidEmail(currentState.email)

        if (!emailValid) {
            _uiState.update { it.copy(emailError = "Please enter a valid email address.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null, infoMessage = null) }
            val result = authRepository.sendPasswordResetEmail(currentState.email)
            _uiState.update { it.copy(isLoading = false) }

            when (result) {
                is AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            infoMessage = "Password reset link sent to ${currentState.email}. Please check your inbox."
                        )
                    }
                }
                is AuthResult.Error -> {
                    _uiState.update { it.copy(generalError = result.message) }
                }
            }
        }
    }

    fun signOut() {
        authRepository.signOut()
        resetFormState()
    }

    private fun resetFormState() {
        _uiState.value = AuthUiState()
    }
}
