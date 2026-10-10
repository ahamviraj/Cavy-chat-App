package com.virudaya.cavy.ui.profile

import com.virudaya.cavy.data.profile.UserProfile

data class ProfileUiState(
    val isLoading: Boolean = true,
    val authenticatedUid: String? = null,
    val userProfile: UserProfile? = null,
    val email: String = "",
    val editDisplayNameInput: String = "",
    val isEditingDisplayName: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)
