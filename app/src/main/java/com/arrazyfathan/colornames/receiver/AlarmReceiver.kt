package com.arrazyfathan.colornames.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import com.arrazyfathan.colornames.RGBColor
import com.arrazyfathan.colornames.notification.createNotificationChannel
import com.arrazyfathan.colornames.notification.displayNotificationColor

/**
 * Created by Ar Razy Fathan Rabbani on 14/12/22.
 */
class AlarmReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {
        val rgb = generateRandomColor()
        val colorRgb = Color.rgb(rgb.red, rgb.green, rgb.blue)
        val hex = String.format("%06X", 0xFFFFFF and colorRgb)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(context)
        }

        displayNotificationColor(hex, context)
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
}
