package com.technfest.technfestcrm.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.technfest.technfestcrm.worker.MoveRecordingsWorker

class AutoMoveForegroundService : Service() {

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotification()

        triggerWorkManager()
        stopForeground(true)
        stopSelf()

        return START_STICKY
    }

    private fun createNotification() {
        val channelId = "recording_move_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Recording Auto Move",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Moving Call Recordings")
            .setContentText("Auto-moving recordings in background")
            .setOngoing(true)
            .build()
        startForeground(1, notification)
    }

    private fun triggerWorkManager() {
        val workRequest = OneTimeWorkRequestBuilder<MoveRecordingsWorker>()
            .build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork("auto_move_recordings", ExistingWorkPolicy.KEEP, workRequest)
        Log.d("AutoMoveService", "WorkManager triggered")
    }

    override fun onBind(intent: Intent?) = null
}
