package com.virudaya.cavy.data.auth

import com.google.firebase.auth.FirebaseUser

data class CavyUser(
    val uid: String,
    val email: String? = null
)

fun FirebaseUser.toCavyUser(): CavyUser {
    return CavyUser(
        uid = uid,
        email = email
    )
}
