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
        val incomingNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER)

        Log.d("CallReceiver", "onReceive → state=$state, incoming=$incomingNumber")

        val callStateServiceIntent = Intent(context, CallStateForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(callStateServiceIntent)
        } else {
            context.startService(callStateServiceIntent)
        }

        if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            Log.d("CallReceiver", "Call ended → triggering auto-move")

            val autoMoveIntent = Intent(context, AutoMoveForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(autoMoveIntent)
            } else {
                context.startService(autoMoveIntent)
            }
        }
    }
}
