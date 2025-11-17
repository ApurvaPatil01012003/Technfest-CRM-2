package com.technfest.technfestcrm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager

class CallReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (stateStr) {

            TelephonyManager.EXTRA_STATE_IDLE -> {
                val serviceIntent = Intent(context, AutoMoveService::class.java)
                context.startService(serviceIntent)
            }
        }
    }
}
