package com.virudaya.cavy.services

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.virudaya.cavy.util.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CavyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val conversationId = remoteMessage.data["conversationId"] ?: ""
        val otherUid = remoteMessage.data["otherUid"] ?: ""
        val otherDisplayName = remoteMessage.data["otherDisplayName"] ?: "Cavy User"
        val messageText = remoteMessage.notification?.body ?: remoteMessage.data["text"] ?: "New message"

        NotificationHelper.showIncomingMessageNotification(
            context = applicationContext,
            conversationId = conversationId,
            otherUid = otherUid,
            otherDisplayName = otherDisplayName,
            messageText = messageText
        )
    }

    override fun onNewToken(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        if (!uid.isNullOrBlank()) {
            CoroutineScope(Dispatchers.IO).launch {
                NotificationHelper.saveFcmToken(uid)
            }
        }
    }
}
