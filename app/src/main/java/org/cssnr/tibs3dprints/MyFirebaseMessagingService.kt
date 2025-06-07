package org.cssnr.tibs3dprints

import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d("FCM", "From: ${remoteMessage.from}")
        remoteMessage.notification?.let {
            Log.d("FCM", "Notification: ${it.title} - ${it.body}")
        }
        remoteMessage.data.takeIf { it.isNotEmpty() }?.let {
            Log.d("FCM", "Data: $it")
        }
    }

    override fun onNewToken(token: String) {
        Log.i("FCM", "onNewToken: token: $token")
        val preferences = PreferenceManager.getDefaultSharedPreferences(this)
        preferences.edit { putString("fcm_token", token) }
    }
}
