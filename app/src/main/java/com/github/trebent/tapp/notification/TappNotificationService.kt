package com.github.trebent.tapp.notification

import android.Manifest
import android.content.Intent
import android.util.Log
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.github.trebent.tapp.MainActivity
import com.github.trebent.tapp.R
import com.github.trebent.tapp.Tapplication
import com.github.trebent.tapp.api.Account
import com.github.trebent.tapp.api.Tapp
import com.github.trebent.tapp.api.accountService
import com.github.trebent.tapp.dataStore
import com.github.trebent.tapp.emailkey
import com.github.trebent.tapp.tokenkey
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

object TappNotificationEvents {
    val events = MutableSharedFlow<Tapp>()
}

const val NOTIFICATION_GROUP_KEY = "com.github.trebent.tapp"
const val CHANNEL_ID = "tapp_channel"

class TappNotificationService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        Log.d("TappNotificationService", "Message received")
        if (!remoteMessage.data.isEmpty()) {
            val title: String
            val body: String
            val sender: String
            val senderTag: String
            val time: String
            val groupId: String
            remoteMessage.data.let { data ->
                title = data["title"]!!
                body = data["body"]!!
                sender = data["sender"]!!
                senderTag = data["sender_tag"]!!
                time = data["time"]!!
                groupId = data["group_id"]!!
            }

            val email = runBlocking {
                applicationContext.dataStore.data.first()[emailkey]
            }

            // Determine noop
            if (email == sender) {
                Log.d("TappNotificationService", "Sender is me, discarding")
                return
            }

            if (Tapplication.isInForeground) {
                CoroutineScope(Dispatchers.Default).launch {
                    TappNotificationEvents.events.emit(
                        Tapp(
                            groupId.toInt(),
                            time.toLong(),
                            Account(sender, "", senderTag)
                        )
                    )
                }
            }

            showNotification(title, body)
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

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)

        if (!Tapplication.isInForeground) {
            val intent: Intent = Intent(this, MainActivity::class)
            builder.setContentIntent(intent)
        }

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
