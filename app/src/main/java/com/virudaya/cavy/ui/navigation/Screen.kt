package com.virudaya.cavy.ui.navigation

import java.net.URLEncoder

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object SignIn : Screen("sign_in")
    data object Register : Screen("register")
    data object ForgotPassword : Screen("forgot_password")
    data object Home : Screen("home")
    data object ConversationList : Screen("conversation_list")
    data object Chat : Screen("chat/{conversationId}/{otherUid}/{otherDisplayName}") {
        fun createRoute(conversationId: String, otherUid: String, otherDisplayName: String): String {
            val encodedName = URLEncoder.encode(otherDisplayName.ifBlank { "Cavy User" }, "UTF-8")
            return "chat/$conversationId/$otherUid/$encodedName"
        }
    }
}
