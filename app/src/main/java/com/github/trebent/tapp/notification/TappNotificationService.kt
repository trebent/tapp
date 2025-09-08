package com.github.trebent.tapp.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.github.trebent.tapp.R
import com.github.trebent.tapp.api.accountService
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.emailkey
import com.github.trebent.tapp.tokenkey
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TappNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("TappNotificationService", "Message received")
        if (!remoteMessage.data.isEmpty()) {
            val sender: String
            remoteMessage.data.let { data ->
                sender = data["sender"]!!
            }

            val email = runBlocking {
                applicationContext.dataStore.data.first()[emailkey]
            }

            // Determine no-handling
            if (email == sender) {
                Log.d("TappNotificationService", "Sender is me, discarding")
                return
            }
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

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        Log.i(
            "TappNotificationService",
            "FCM token has changed, checking email preferences before sending..."
        )

        val cloudToken = runBlocking {
            applicationContext.dataStore.data.first()[tokenkey]
        }

        if (cloudToken != null) {
            Log.i(
                "TappNotificationService",
                "Found cloud token preference, updating backend..."
            )

            runBlocking {
                val response = accountService.pushFCM(newToken, cloudToken)
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
