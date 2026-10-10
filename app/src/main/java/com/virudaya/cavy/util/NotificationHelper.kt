package com.virudaya.cavy.util

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import com.virudaya.cavy.MainActivity
import com.virudaya.cavy.R
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.json.JSONObject

object NotificationHelper {

    const val CHANNEL_ID = "cavy_messages_channel"
    const val CHANNEL_NAME = "Cavy Messages"
    const val PERMISSION_REQUEST_CODE = 1001

    var BACKEND_URL: String? =  "https://cavy-chat-app.vercel.app/api/sendNotification"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = "Notifications for incoming Cavy messages"
                enableVibration(true)
            }
            val notificationManager: NotificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun requestNotificationPermissionIfNeeded(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    activity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }

    fun showIncomingMessageNotification(
        context: Context,
        conversationId: String,
        otherUid: String,
        otherDisplayName: String,
        messageText: String
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
        }

        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("conversationId", conversationId)
            putExtra("otherUid", otherUid)
            putExtra("otherDisplayName", otherDisplayName)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New message from $otherDisplayName")
            .setContentText(messageText)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        val notificationManager = NotificationManagerCompat.from(context)
        try {
            notificationManager.notify(conversationId.hashCode(), builder.build())
        } catch (e: SecurityException) {
            Log.e("CavyNotification", "Notification permission missing: ${e.message}")
        }
    }

    suspend fun saveFcmToken(uid: String) {
        if (uid.isBlank()) return
        try {
            val token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                val db = FirebaseDatabase.getInstance("https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app")
                db.getReference("privateProfiles").child(uid).child("fcmToken").setValue(token).await()
                Log.d("CavyNotification", "FCM token saved successfully for UID: ${uid.take(8)}")
            } else {
                Log.w("CavyNotification", "FCM token empty or null for UID: ${uid.take(8)}")
            }
        } catch (e: Exception) {
            Log.e("CavyNotification", "Failed to save FCM token: ${e.message}")
        }
    }

    suspend fun clearFcmToken(uid: String) {
        if (uid.isBlank()) return
        try {
            val db = FirebaseDatabase.getInstance("https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app")
            db.getReference("privateProfiles").child(uid).child("fcmToken").removeValue().await()
            Log.d("CavyNotification", "FCM token cleared for UID: ${uid.take(8)}")
        } catch (e: Exception) {
            Log.e("CavyNotification", "Failed to clear FCM token: ${e.message}")
        }
    }

    suspend fun triggerPushNotificationServer(
        conversationId: String,
        messageText: String,
        backendUrl: String? = BACKEND_URL
    ) {
        if (backendUrl.isNullOrBlank()) {
            Log.d("CavyNotification", "Server push notification skipped: BACKEND_URL not set.")
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val user = FirebaseAuth.getInstance().currentUser ?: return@withContext
                val idTokenResult = user.getIdToken(true).await()
                val idToken = idTokenResult.token ?: return@withContext

                val url = URL(backendUrl)
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Authorization", "Bearer $idToken")
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.doOutput = true
                conn.connectTimeout = 5000
                conn.readTimeout = 5000

                val jsonBody = JSONObject().apply {
                    put("conversationId", conversationId)
                    put("messageText", messageText)
                }.toString()

                OutputStreamWriter(conn.outputStream, "UTF-8").use { writer ->
                    writer.write(jsonBody)
                    writer.flush()
                }

                val responseCode = conn.responseCode
                Log.d("CavyNotification", "Server push notification response code: $responseCode")
            } catch (e: Exception) {
                Log.e("CavyNotification", "Failed to trigger server push notification: ${e.message}")
            }
        }
    }

    suspend fun runNotificationDiagnostic(uid: String): String {
        val resultLog = StringBuilder()

        if (uid.isBlank()) {
            return "Diagnostic Failed: User is not authenticated."
        }
        resultLog.append("1. Auth UID: OK (${uid.take(8)})\n")

        // Step 1: Check FCM Token Retrieval
        var token: String? = null
        try {
            token = FirebaseMessaging.getInstance().token.await()
            if (!token.isNullOrBlank()) {
                resultLog.append("2. Device FCM Token: OK (Retrieved successfully)\n")
            } else {
                resultLog.append("2. Device FCM Token: FAILED (Token is null/empty)\n")
            }
        } catch (e: Exception) {
            resultLog.append("2. Device FCM Token: FAILED (${e.localizedMessage})\n")
        }

        // Step 2: Check Token DB Persistence
        try {
            val db = FirebaseDatabase.getInstance("https://cavy-chat-app-default-rtdb.asia-southeast1.firebasedatabase.app")
            val tokenSnap = db.getReference("privateProfiles").child(uid).child("fcmToken").get().await()
            if (tokenSnap.exists()) {
                resultLog.append("3. Token Persistence in /privateProfiles: OK (Saved in RTDB)\n")
            } else {
                resultLog.append("3. Token Persistence in /privateProfiles: FAILED (Path missing or denied)\n")
            }
        } catch (e: Exception) {
            resultLog.append("3. Token Persistence: FAILED (${e.localizedMessage})\n")
        }

        // Step 3: Check Backend URL Configuration
        val url = BACKEND_URL
        if (url.isNullOrBlank()) {
            resultLog.append("4. Notification Backend URL: FAILED (BACKEND_URL is not set)\n")
        } else {
            resultLog.append("4. Notification Backend URL: OK ($url)\n")
        }

        return resultLog.toString()
    }
}
