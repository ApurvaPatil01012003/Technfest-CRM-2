package com.technfest.technfestcrm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi

class BootRestartReceiver : BroadcastReceiver() {

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context, intent: Intent?) {

        if (intent?.action == Intent.ACTION_BOOT_COMPLETED ||
            intent?.action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {

            Log.d("BootReceiver", "Device rebooted → CallReceiver will work again")

            // OPTIONAL: start your service immediately after boot
            // to clean old recordings if any
            val serviceIntent = Intent(context, AutoMoveForegroundService::class.java)
            try {
                context.startForegroundService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
