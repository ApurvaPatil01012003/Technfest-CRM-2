package com.technfest.technfestcrm.utils

import android.content.Context
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager

class MyCallListener(
    private val context: Context,
    private val phoneNumber: String,
    private val onCallEnd: (number: String, duration: Long, direction: String, callStatus: String, startTime: String, endTime: String) -> Unit
) : PhoneStateListener() {

    private var isCalling = false
    private var callStartTimeMillis = 0L
    private var callDirection = "outgoing"
    private var callAnswered = false

    private fun formatTimestamp(millis: Long): String {
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(millis))
    }

    override fun onCallStateChanged(state: Int, incomingNumber: String?) {
        when (state) {

            TelephonyManager.CALL_STATE_RINGING -> {
                callDirection = "incoming"
                callAnswered = false
            }

            TelephonyManager.CALL_STATE_OFFHOOK -> {
                if (callDirection == "incoming") callAnswered = true
                else callDirection = "outgoing"
                callAnswered = true
                isCalling = true
                callStartTimeMillis = System.currentTimeMillis()
            }

            TelephonyManager.CALL_STATE_IDLE -> {
                if (isCalling) {
                    val callEndTimeMillis = System.currentTimeMillis()
                    isCalling = false
                    val duration = (callEndTimeMillis - callStartTimeMillis) / 1000
                    val callStatus = if (callAnswered) "answered" else "unanswered"
                    val startTime = formatTimestamp(callStartTimeMillis)
                    val endTime = formatTimestamp(callEndTimeMillis)
                    onCallEnd(phoneNumber, duration, callDirection, callStatus, startTime, endTime)
                }
            }
        }
    }
}
