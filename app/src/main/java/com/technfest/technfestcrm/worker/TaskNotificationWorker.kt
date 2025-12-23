package com.technfest.technfestcrm.worker

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.view.MainActivity

class TaskNotificationWorker(
    context: Context,
    params: androidx.work.WorkerParameters
) : androidx.work.CoroutineWorker(context, params) {

    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
    override suspend fun doWork(): Result {
        val taskId = inputData.getInt("taskId", -1)
        val taskTitle = inputData.getString("taskTitle") ?: "Task Reminder"

        if (taskId == -1) return Result.failure()
        val clickIntent = Intent(applicationContext, MainActivity::class.java).apply {
            putExtra("openFragment", "TASK")
            putExtra("highlightTaskId", taskId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
        }



        val pendingClick = PendingIntent.getActivity(
            applicationContext,
            taskId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, "task_channel")
            .setSmallIcon(R.drawable.baseline_person_outline_24)
            .setContentTitle("Task Reminder")
            .setContentText(taskTitle)
            .setContentIntent(pendingClick)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(taskId, notification)

        return Result.success()
    }
}
