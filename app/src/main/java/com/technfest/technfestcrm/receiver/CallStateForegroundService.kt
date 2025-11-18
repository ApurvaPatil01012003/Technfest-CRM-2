package com.technfest.technfestcrm.receiver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.worker.MoveRecordingsWorker

class CallStateForegroundService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    override fun onCreate() {
        super.onCreate()
        Log.d("CallStateService", "onCreate called")

        createNotificationChannel()
        startForeground(1001, buildNotification())

        telephonyManager =
            getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        registerPhoneStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("CallStateService", "onStartCommand called")
        return START_STICKY
    }

    private fun registerPhoneStateListener() {
        if (phoneStateListener != null) return

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, phoneNumber: String?) {
                super.onCallStateChanged(state, phoneNumber)
                Log.d("CallStateService", "Call state: $state, num: $phoneNumber")

                if (state == TelephonyManager.CALL_STATE_IDLE) {
                    Log.d("CallStateService", "Call ended → trigger auto move")
                    triggerMoveWorker()
                }
            }
        }

        telephonyManager.listen(
            phoneStateListener,
            PhoneStateListener.LISTEN_CALL_STATE
        )
    }

    private fun triggerMoveWorker() {
        val workRequest = OneTimeWorkRequestBuilder<MoveRecordingsWorker>().build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork(
                "auto_move_recordings_foreground",
                ExistingWorkPolicy.KEEP,
                workRequest
            )
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "call_state_service_channel"
            val channel = NotificationChannel(
                channelId,
                "Technfest CRM Call Sync",
                NotificationManager.IMPORTANCE_MIN
            )
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val channelId =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) "call_state_service_channel" else ""

        return NotificationCompat.Builder(this, channelId)
            .setContentTitle("Technfest CRM – Call recording sync active")
            .setContentText("After Call end recordings are auto sync.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("CallStateService", "onDestroy called, unregister listener")
        phoneStateListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        phoneStateListener = null
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
