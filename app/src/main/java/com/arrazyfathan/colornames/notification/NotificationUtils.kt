package com.arrazyfathan.colornames.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.arrazyfathan.colornames.MainActivity
import com.arrazyfathan.colornames.R
import com.arrazyfathan.colornames.RGBColor
import com.arrazyfathan.colornames.workers.SendColorNotificationWorker

/**
 * Created by Ar Razy Fathan Rabbani on 14/12/22.
 */

private val NOTIFICATION_ID = 0
private val REQUEST_CODE = 0
private val FLAGS = 0

@RequiresApi(Build.VERSION_CODES.O)
fun createNotificationChannel(applicationContext: Context) {
    val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    if (notificationManager.getNotificationChannel(SendColorNotificationWorker.COLOR_NOTIFICATION_CHANNEL) == null) {
        val channel = NotificationChannel(
            SendColorNotificationWorker.COLOR_NOTIFICATION_CHANNEL,
            "ColorChannel",
            NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)
    }
}

fun NotificationManager.sendNotification(rgb: RGBColor, hex: String, applicationContext: Context) {
    val contentIntent = Intent(applicationContext, MainActivity::class.java)
    val bundle = Bundle()
    bundle.putString(SendColorNotificationWorker.COLOR_RGB_EXTRA, hex)
    contentIntent.putExtras(bundle)
    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    } else {
        PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    val notification = NotificationCompat.Builder(
        applicationContext,
        SendColorNotificationWorker.COLOR_NOTIFICATION_CHANNEL
    )
        .setSmallIcon(R.drawable.ic_notification_icon)
        .setContentTitle("Color Todays")
        .setContentText("Today color is ($rgb) 🥳")
        .setAutoCancel(false)
        .setContentIntent(pendingIntent)
        .build()

    notify(NOTIFICATION_ID, notification)
}

fun displayNotificationColor(hex: String, applicationContext: Context) {
    val contentIntent = Intent(applicationContext, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }.putExtra("hex", hex)
    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_IMMUTABLE
        )
    } else {
        PendingIntent.getActivity(
            applicationContext,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT
        )
    }

    val notification = NotificationCompat.Builder(
        applicationContext,
        SendColorNotificationWorker.COLOR_NOTIFICATION_CHANNEL
    )
        .setSmallIcon(R.drawable.ic_notification_icon)
        .setContentTitle("Color Todays")
        .setContentText("Today color is (#$hex) 🥳")
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .build()

    val notificationManager =
        applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    notificationManager.notify(NOTIFICATION_ID, notification)
}
