package com.technfest.technfestcrm.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.technfest.technfestcrm.utils.RecordingsMover

class CallReceiver : BroadcastReceiver() {

    private var lastState = TelephonyManager.CALL_STATE_IDLE

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != "android.intent.action.PHONE_STATE") return

        val stateStr = intent.getStringExtra(TelephonyManager.EXTRA_STATE)

        when (stateStr) {

            TelephonyManager.EXTRA_STATE_IDLE -> {
                if (lastState != TelephonyManager.CALL_STATE_IDLE) {

                    // 📌 CALL ENDED → MOVE THE LATEST RECORDING
                    RecordingsMover.moveLatestRecording(context)
                }
                lastState = TelephonyManager.CALL_STATE_IDLE
            }

            TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                lastState = TelephonyManager.CALL_STATE_OFFHOOK
            }

            TelephonyManager.EXTRA_STATE_RINGING -> {
                lastState = TelephonyManager.CALL_STATE_RINGING
            }
        }
    }
}
