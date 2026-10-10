package com.virudaya.cavy.ui.auth

import com.virudaya.cavy.data.auth.CavyUser

sealed interface AuthSessionState {
    data object Loading : AuthSessionState
    data class Authenticated(val user: CavyUser) : AuthSessionState
    data object Unauthenticated : AuthSessionState
}

data class AuthUiState(
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val generalError: String? = null,
    val infoMessage: String? = null
)
