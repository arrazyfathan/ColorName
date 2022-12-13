package com.arrazyfathan.colornames.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.arrazyfathan.colornames.MainActivity
import com.arrazyfathan.colornames.R
import com.arrazyfathan.colornames.RGBColor
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Created by Ar Razy Fathan Rabbani on 13/12/22.
 */
private const val TAG = "SendColorNotificationWorker"

class SendColorNotificationWorker(
    context: Context,
    param: WorkerParameters
) : CoroutineWorker(context, param) {

    companion object {
        const val COLOR_NOTIFICATION_CHANNEL = "color_notification_channel"
        const val COLOR_RGB_EXTRA = "color_rgb_extra"
    }

    override suspend fun doWork(): Result = coroutineScope {
        val job = async {
            val rgb = generateRandomColor()
            val colorRgb = Color.rgb(rgb.red, rgb.green, rgb.blue)
            val hex = String.format("%06X", 0xFFFFFF and colorRgb)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createNotificationChannel()
            }
            displayNotificationColor(rgb, hex)
        }
        job.await()
        Result.success()
    }

    private fun generateRandomColor(): RGBColor {
        val randomRed = (0..255).random()
        val randomGreen = (0..255).random()
        val randomBlue = (0..255).random()
        return RGBColor(randomRed, randomGreen, randomBlue)
    }

    fun generateRandomHex() {
        val numberHex = (0..9).random()
        val letters = ('A'..'F').random()
        var hex = ""
        repeat(6) {
            hex += numberHex
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel() {
        val notificationManager =
            applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (notificationManager.getNotificationChannel(COLOR_NOTIFICATION_CHANNEL) == null) {
            val channel = NotificationChannel(
                COLOR_NOTIFICATION_CHANNEL,
                "ColorChannel",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun displayNotificationColor(rgb: RGBColor, hex: String) {
        val contentIntent = Intent(applicationContext, MainActivity::class.java)
        val bundle = Bundle()
        bundle.putString(COLOR_RGB_EXTRA, hex)
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

        val notification = NotificationCompat.Builder(applicationContext, COLOR_NOTIFICATION_CHANNEL)
            .setSmallIcon(R.drawable.ic_notification_icon)
            .setContentTitle("Color Todays")
            .setContentText("Today color is ($rgb) 🥳")
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .build()

        val notificationManager = applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(rgb.toString(), 0, notification)
    }
}
