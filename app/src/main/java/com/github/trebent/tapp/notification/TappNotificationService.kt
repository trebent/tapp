/**
 * The app's notification service.
 */

package com.github.trebent.tapp.notification

import android.Manifest
import android.app.PendingIntent
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

    /**
     * Determine notification dispatching and tapp event topic update. Both depend on the foreground
     * status of Tapp.
     *  - If Tapp is in the background, a notification is shown that is *clickable*.
     *  - If Tapp is in the foreground, the notification still shows but is NOT clickable.
     *  - A notification originating from an event that the current user triggered will NOT lead
     *    to a notification on the triggering device.
     *  - The Tapp event update depends on if the application is in the foreground, since the only
     *    side-effects of an event-dispatch is that the Tapp is added to the tapp group view model
     *    list of tapps.
     */
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

            // non-null when receiving targetted notifications, meaning they should be displayed
            // no matter the sender.
            val individual: String?
            
            remoteMessage.data.let { data ->
                title = data["title"]!!
                body = data["body"]!!
                sender = data["sender"]!!
                senderTag = data["sender_tag"]!!
                time = data["time"]!!
                groupId = data["group_id"]!!
                individual = data["individual"]
            }

            val email = runBlocking {
                applicationContext.dataStore.data.first()[emailkey]
            }

            // Determine noop, if I am the sender, I don't need a notification.
            if (individual == null && email == sender) {
                Log.d("TappNotificationService", "Sender is me, discarding")
                return
            }

            CoroutineScope(Dispatchers.Default).launch {
                TappNotificationEvents.events.emit(
                    Tapp(
                        groupId.toInt(),
                        time.toLong(),
                        // A Tapp has a companion object that will determine if the email or tag
                        // is to be used for the tapp listing. The tag will be preferred if it
                        // exists.
                        Account(sender, "", senderTag)
                    )
                )
            }

            showNotification(title, body)
        }
    }

    /**
     * Update the Tapp backend with the device FCM. Each FCM is used to map which device should
     * receive a notification. The FCM is mapped to one or more group IDs in the Tapp backend.
     */
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

    /**
     * Show a notification on screen. This is called from the message receiver.
     */
    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    private fun showNotification(title: String, message: String) {
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setGroup(NOTIFICATION_GROUP_KEY)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setAutoCancel(true)

        // Only make the notification clickable when Tapp is not in the foreground.
        if (!Tapplication.isInForeground) {
            val intent = Intent(this, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT,
            )
            builder.setContentIntent(pendingIntent)
        }

        NotificationManagerCompat.from(this)
            .notify(System.currentTimeMillis().toInt(), builder.build())
    }
}
