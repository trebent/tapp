package com.github.trebent.tapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.trebent.tapp.R
import com.github.trebent.tapp.api.accountService
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.tokenkey
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TappNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Data payload
        remoteMessage.data.let { data ->
            val sender = data["sender"]
            val groupId = data["group_id"]
            Log.d("TappNotificationService", "Data received: sender=$sender group=$groupId")
            // Update UI or app logic if needed
        }

        // Notification payload
        remoteMessage.notification?.let {
            Log.d(
                "TappNotificationService",
                "Notification received: title=${it.title} body=${it.body}"
            )

            showNotification(
                it.title ?: "Got a new Tapp!",
                it.body ?: "Open the Tapp app to view it"
            )
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(
            "TappNotificationService",
            "FCM token has changed, checking email preferences before sending..."
        )

        val token = runBlocking {
            applicationContext.dataStore.data.first()[tokenkey]
        }

        if (token != null) {
            Log.i(
                "TappNotificationService",
                "Found token preference, updating backend..."
            )

            runBlocking {
                val response = accountService.pushFCM(token, token)
                if (response.isSuccessful) {
                    Log.i("TappNotificationService", "FCM update successful")
                } else {
                    Log.e("TappNotificationService", "FCM update failed")
                }
            }
        }
    }

    private fun showNotification(title: String, message: String) {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        val channelId = "tapp_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel =
                NotificationChannel(channelId, "Tapp", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
