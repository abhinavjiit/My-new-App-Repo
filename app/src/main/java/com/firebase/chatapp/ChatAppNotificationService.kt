package com.firebase.chatapp

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class ChatAppNotificationService : FirebaseMessagingService() {
    private val SUMMARY_ID = 0
    private val GROUP_KEY_WORK_EMAIL = "com.android.example.WORK_EMAIL"
    private val CHANNEL_ID = "1"


    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
        Log.d("tag", newToken.toString())
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val data = message.data




        createNotificationChannel()
        val newMessageNotification1 = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setContentTitle("emailObject1.getSummary()")
            .setContentText("You will not believe...")
            .setGroup(GROUP_KEY_WORK_EMAIL)
            .build()

        /* val newMessageNotification2 = NotificationCompat.Builder(this, CHANNEL_ID)
             .setSmallIcon(R.drawable.ic_launcher_background)
             .setContentTitle("emailObject2.getSummary()")
             .setContentText("Please join us to celebrate the...")
             .setGroup(GROUP_KEY_WORK_EMAIL)
             .build()*/

        val summaryNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("emailObject.getSummary()")
            //set content text to support devices running API level < 24
            .setContentText("Two new messages")
            .setSmallIcon(R.drawable.ic_launcher_background)
            //build summary info into InboxStyle template
            .setStyle(
                NotificationCompat.InboxStyle()
                    .addLine("Alex Faarborg Check this out")
                    .addLine("Jeff Chang Launch Party")
                    .setBigContentTitle("2 new messages")
                    .setSummaryText("janedoe@example.com")
            )
            //specify which group this notification belongs to
            .setGroup(GROUP_KEY_WORK_EMAIL)
            //set this notification as the summary for the group
            .setGroupSummary(true)
            .build()

        NotificationManagerCompat.from(this).apply {
            notify(1, newMessageNotification1)
            //   notify(0, newMessageNotification2)
            // notify(SUMMARY_ID, summaryNotification)
        }
        // }
    }

    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "ChatAppNotification"
            val descriptionText = "doChat"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}