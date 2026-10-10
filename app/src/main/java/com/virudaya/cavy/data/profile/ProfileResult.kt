package com.virudaya.cavy.data.profile

sealed interface ProfileResult<out T> {
    data class Success<out T>(val data: T) : ProfileResult<T>
    data class Error(val message: String) : ProfileResult<Nothing>
    data object Loading : ProfileResult<Nothing>
}
