package com.ysproduction.freedium

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import androidx.core.app.NotificationCompat


class MyForegroundService : Service() {

    private val notificationId = 1
    private var notificationManager: NotificationManager? = null

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification(intent?.getStringExtra("url").toString(), notificationManager!!)
        return START_STICKY // Ensures service restarts if killed unexpectedly
    }

    fun updateNotificationTitle(
        url: String,
        context: Context,
        notificationManager: NotificationManager
    ) {
        notificationManager.notify(
            notificationId,
            buildNotification(url, context, notificationManager)
        )
    }

    private fun buildNotification(
        url: String,
        context: Context,
        notificationManager: NotificationManager
    ): Notification {
        val channelId = createNotificationChannel(notificationManager)
        val notificationBuilder = NotificationCompat.Builder(context, channelId)
            .setContentTitle("Current url")
            .setContentText(url)
            .setSmallIcon(R.drawable.freedium) // Replace with your icon resource
            .setOngoing(true) // Make the notification ongoing (can't be swiped away)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                NotificationCompat.Action( // Create action for the button
                    R.drawable.freedium, // Replace with your button icon resource
                    "Open",
                    createPendingIntent(url,context)
                )
            ).build()

        return notificationBuilder
    }

    private fun createNotification(url: String, notificationManager: NotificationManager) {
        val channelId = createNotificationChannel(notificationManager)
        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Current url")
            .setContentText(url)
            .setSmallIcon(R.drawable.freedium)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                NotificationCompat.Action(
                    R.drawable.freedium,
                    "Open",
                    createPendingIntent(url)
                )
            ).build()

        startForeground(notificationId, notificationBuilder)
    }

    private fun createPendingIntent(url: String, context: Context = this): PendingIntent? {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent
    }

    private fun createNotificationChannel(
        notificationManager: NotificationManager,
        importance: Int = NotificationManager.IMPORTANCE_LOW
    ): String {
        val channelName = "Foreground Service Channel"
        val channelDescription = "Channel for foreground service notifications"
        val channelId = "OpenUrl"
        val channel = NotificationChannel(channelId, channelName, importance)
        channel.description = channelDescription
        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationManager?.cancelAll()
        stopSelf()
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
