package com.virudaya.cavy.data.messaging

sealed interface MessagingResult<out T> {
    data class Success<out T>(val data: T) : MessagingResult<T>
    data class Error(val message: String) : MessagingResult<Nothing>
    data object Loading : MessagingResult<Nothing>
}
