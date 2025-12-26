package com.technfest.technfestcrm.utils
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.technfest.technfestcrm.R
import kotlin.random.Random

object MissedCallNotifier {

    private const val CHANNEL_ID = "missed_call_channel"
    private const val CHANNEL_NAME = "Missed Calls"

    fun show(context: Context, leadName: String?, number: String?) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            nm.createNotificationChannel(ch)
        }

        val title = "Missed Call"
        val body = buildString {
            append(leadName?.takeIf { it.isNotBlank() } ?: "Unknown Lead")
            if (!number.isNullOrBlank()) append(" • $number")
        }

        val n = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.calls)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        nm.notify(Random.nextInt(100000, 999999), n)
    }
}
