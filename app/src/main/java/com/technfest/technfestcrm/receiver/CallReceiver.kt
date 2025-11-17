//package com.technfest.technfestcrm.receiver
//
//import android.content.BroadcastReceiver
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.telephony.TelephonyManager
//import android.util.Log
//import com.technfest.technfestcrm.service.AutoMoveForegroundService
//
//class CallReceiver : BroadcastReceiver() {
//
//    override fun onReceive(context: Context, intent: Intent) {
//
//        Log.d("CallReceiver", "Broadcast received: ${intent.action}")
//
//        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
//        Log.d("CallReceiver", "Call state = $state")
//
//        if (state == TelephonyManager.EXTRA_STATE_IDLE) {
//            Log.d("CallReceiver", "Call ended → starting AutoMoveForegroundService")
//
//            val serviceIntent = Intent(context, AutoMoveForegroundService::class.java)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                try {
//                    context.startForegroundService(serviceIntent)
//                    Log.d("CallReceiver", "Foreground service started")
//                } catch (e: Exception) {
//                    Log.e("CallReceiver", "ERROR starting service: ${e.message}")
//                }
//            } else {
//                context.startService(serviceIntent)
//            }
//        }
//    }
//
//}




package com.technfest.technfestcrm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.TelephonyManager
import android.util.Log

class CallReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            Log.d("CallReceiver", "Call ended → triggering auto-move")

            val serviceIntent = Intent(context, AutoMoveForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent)
            } else {
                context.startService(serviceIntent)
            }
        }
    }
}
