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
import com.technfest.technfestcrm.model.CallLogRequest
import com.technfest.technfestcrm.repository.CallLogRepository
import com.technfest.technfestcrm.worker.MoveRecordingsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.core.content.edit

class CallStateForegroundService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    // runtime state
    private var currentNumber: String? = null
    private var currentDirection: String = "unknown"

    // incoming call actually answered (RINGING -> OFFHOOK)
    private var callAnswered: Boolean = false

    // when TALK actually started (OFFHOOK) -> used for startTime & duration
    private var callStartTimeMs: Long = 0L

    // small threshold to treat extremely short calls as unanswered (for outgoing)
    private val ANSWERED_MIN_SECONDS = 1

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, buildNotification())

        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        registerPhoneStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun registerPhoneStateListener() {
        if (phoneStateListener != null) return

        var wasRinging = false      // observed RINGING
        var wasOffhook = false      // observed OFFHOOK
        var activeNumber: String? = null

        phoneStateListener = object : PhoneStateListener() {
            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                super.onCallStateChanged(state, incomingNumber)

                if (!incomingNumber.isNullOrEmpty()) {
                    activeNumber = incomingNumber
                    currentNumber = incomingNumber
                }

                when (state) {
                    TelephonyManager.CALL_STATE_RINGING -> {
                        // Incoming ringing
                        wasRinging = true
                        wasOffhook = false
                        currentDirection = "incoming"
                        callAnswered = false
                        // 🚫 DO NOT set start time here. We only want from pickup.
                        Log.d(TAG, "RINGING -> number=$activeNumber")
                    }

                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        wasOffhook = true

                        // If we never saw RINGING, this is likely outgoing dial
                        if (!wasRinging) {
                            currentDirection = "outgoing"
                        }

                        // 🔹 Conversation start = when we go OFFHOOK
                        if (callStartTimeMs == 0L) {
                            callStartTimeMs = System.currentTimeMillis()
                        }

                        // 🔹 For INCOMING calls: OFFHOOK after RINGING = user picked the call
                        if (wasRinging && !callAnswered) {
                            callAnswered = true
                            Log.d(
                                TAG,
                                "INCOMING CALL PICKED at ${formatIso(callStartTimeMs)}"
                            )
                        } else if (!wasRinging) {
                            // Outgoing call considered "answered" from our side
                            callAnswered = true
                            Log.d(
                                TAG,
                                "OUTGOING CALL STARTED at ${formatIso(callStartTimeMs)}"
                            )
                        }

                        if (!incomingNumber.isNullOrEmpty()) {
                            currentNumber = incomingNumber
                        }
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        val endTimeMs = System.currentTimeMillis()

                        // 🔹 Avoid fake log when service starts and no call happened
                        val noRealCall =
                            !wasRinging && !wasOffhook &&
                                    callStartTimeMs == 0L &&
                                    !callAnswered &&
                                    currentNumber.isNullOrEmpty()

                        if (noRealCall) {
                            Log.d(TAG, "IDLE -> initial state, no call. Ignoring.")
                            return
                        }

                        val callStatus: String
                        val durationSec: Int
                        val startStr: String
                        val endStr: String

                        // For answered logic we still use small threshold for outgoing
                        val calculatedDurationSec =
                            if (callStartTimeMs > 0L) {
                                ((endTimeMs - callStartTimeMs) / 1000)
                                    .toInt()
                                    .coerceAtLeast(0)
                            } else {
                                0
                            }

                        val isAnswered = when (currentDirection) {
                            "incoming" -> callAnswered
                            "outgoing" -> calculatedDurationSec >= ANSWERED_MIN_SECONDS
                            else -> calculatedDurationSec >= ANSWERED_MIN_SECONDS
                        }

                        if (isAnswered && callStartTimeMs > 0L) {
                            // ✅ Answered call:
                            // start = OFFHOOK (pickup), end = hangup, duration = talk time
                            callStatus = "answered"
                            durationSec = calculatedDurationSec
//                            startStr = formatLocal(callStartTimeMs)
//                            endStr = formatLocal(endTimeMs)
                            // Answered
                            startStr = formatIso(callStartTimeMs)
                            endStr = formatIso(endTimeMs)
                        } else {
                            // ❌ Unanswered call:
                            // start & end = today's 00:00:00, duration = 0
                            callStatus = "unanswered"
                            durationSec = 0
//                            val midnight = todayMidnightMs()
//                            startStr = formatLocal(midnight)
//                            endStr = startStr
                            val midnight = todayMidnightMs()
                            startStr = formatIso(midnight)
                            endStr = startStr
                        }

                        Log.d(
                            TAG,
                            "IDLE -> call ended. number=$currentNumber dir=$currentDirection " +
                                    "status=$callStatus duration=$durationSec s start=$startStr end=$endStr"
                        )

                        // send to server
                        sendCallLogToServer(
                            phoneNumber = currentNumber,
                            direction = currentDirection,
                            durationSeconds = durationSec,
                            callStatus = callStatus,
                            startIso = startStr,   // now in "yyyy-MM-dd HH:mm:ss"
                            endIso = endStr
                        )

                        // Move recordings (as you had)
                        triggerMoveWorker()

                        // reset for next call
                        wasRinging = false
                        wasOffhook = false
                        callAnswered = false
                        callStartTimeMs = 0L
                        currentDirection = "unknown"
                        currentNumber = null
                    }
                }
            }
        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }


    private fun isCallReallyAnswered(): Boolean {
        val am = getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val audioActive =
            am.mode == android.media.AudioManager.MODE_IN_CALL ||
                    am.mode == android.media.AudioManager.MODE_IN_COMMUNICATION
        val micActive = am.isMicrophoneMute == false
        return audioActive && micActive
    }

//    private fun sendCallLogToServer(
//        phoneNumber: String?,
//        direction: String,
//        durationSeconds: Int,
//        callStatus: String,
//        startIso: String?,
//        endIso: String
//    ) {
//        val ctx = this
//
//        val sessionPrefs = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
//        val token = sessionPrefs.getString("token", null)
//        val workspaceId = sessionPrefs.getInt("workspaceId", 0)
//        val userId = sessionPrefs.getInt("userId", 0)
//
//        if (token.isNullOrEmpty() || workspaceId == 0) {
//            Log.e(TAG, "Missing session token/workspaceId — skipping call log")
//            return
//        }
//
//        val meta = ctx.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
//        val leadId = meta.getInt("leadId", 0)
//        val leadName = meta.getString("leadName", "") ?: ""
//        val campaignId = meta.getInt("campaignId", 0)
//        val campaignCode = meta.getString("campaignCode", "") ?: ""
//        val savedCustomerNumber = meta.getString("customerNumber", "") ?: ""
//
//        val raw = (phoneNumber ?: savedCustomerNumber).trim()
//        val finalNumber = formatInternationalNumber(raw, "IN")
//
//        val request = CallLogRequest(
//            workspaceId = workspaceId,
//            userId = userId,
//            leadId = if (leadId > 0) leadId else null,
//            campaignId = campaignId,
//            campaignCode = campaignCode,
//            customerNumber = finalNumber,
//            customerName = leadName,
//            direction = direction,
//            callStatus = callStatus,
//            startTime = startIso ?: "",
//            endTime = endIso,
//            durationSeconds = durationSeconds,
//            recordingUrl = "",
//            source = "mobile",
//            notes = "",
//            externalEventId = ""
//        )
//
//        CoroutineScope(Dispatchers.IO).launch {
//            try {
//                val webhookSecret = "super-secret-webhook-key"
//                val resp = CallLogRepository().sendCallLog(webhookSecret, request)
//                if (resp.isSuccessful) {
//                    val id = resp.body()?.id ?: 0
//                    Log.d(TAG, "Call log uploaded, id=$id")
//                    if (id > 0) {
//                        ctx.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE)
//                            .edit() { putInt("lastCallLogId", id) }
//                    }
//                } else {
//                    Log.e(
//                        TAG,
//                        "Call log failed: code=${resp.code()} body=${resp.errorBody()?.string()}"
//                    )
//                }
//            } catch (e: Exception) {
//                Log.e(TAG, "Exception sending call log", e)
//            }
//        }
//    }

    private fun sendCallLogToServer(
        phoneNumber: String?,
        direction: String,
        durationSeconds: Int,
        callStatus: String,
        startIso: String?,
        endIso: String
    ) {
        val ctx = this

        val sessionPrefs = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val token = sessionPrefs.getString("token", null)
        val workspaceId = sessionPrefs.getInt("workspaceId", 0)
        val userId = sessionPrefs.getInt("userId", 0)

        if (token.isNullOrEmpty() || workspaceId == 0) {
            Log.e(TAG, "Missing session token/workspaceId — skipping call log")
            return
        }

        val meta = ctx.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
        val metaLeadId = meta.getInt("leadId", 0)
        val metaLeadName = meta.getString("leadName", "") ?: ""
        val campaignId = meta.getInt("campaignId", 0)
        val campaignCode = meta.getString("campaignCode", "") ?: ""
        val savedCustomerNumber = meta.getString("customerNumber", "") ?: ""


        val rawNumber = (phoneNumber ?: savedCustomerNumber).trim()
        val finalNumber = formatInternationalNumber(rawNumber, "IN")

        // 🔹 2) Decide effective leadId + customerName based on number match
        val normIncoming = normalizeForCompare(phoneNumber)
        val normSaved = normalizeForCompare(savedCustomerNumber)

        val effectiveLeadId: Int? =
            if (!normIncoming.isNullOrBlank() && normIncoming == normSaved && metaLeadId > 0) {
                // number match → attach lead
                metaLeadId
            } else {
                // number mismatch / no meta → do not attach lead
                null
            }

        val effectiveCustomerName: String =
            if (effectiveLeadId != null) metaLeadName else ""

        val request = CallLogRequest(
            workspaceId = workspaceId,
            userId = userId,
            leadId = effectiveLeadId,
            campaignId = campaignId,
            campaignCode = campaignCode,
            customerNumber = finalNumber,
            customerName = effectiveCustomerName,
            direction = direction,
            callStatus = callStatus,
            startTime = startIso ?: "",
            endTime = endIso,
            durationSeconds = durationSeconds,
            recordingUrl = "",
            source = "mobile",
            notes = "",
            externalEventId = ""
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val webhookSecret = "super-secret-webhook-key"
                val resp = CallLogRepository().sendCallLog(webhookSecret, request)
                if (resp.isSuccessful) {
                    val id = resp.body()?.id ?: 0
                    Log.d(TAG, "Call log uploaded, id=$id")
                    if (id > 0) {
                        ctx.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE)
                            .edit { putInt("lastCallLogId", id) }
                    }
                } else {
                    Log.e(
                        TAG,
                        "Call log failed: code=${resp.code()} body=${resp.errorBody()?.string()}"
                    )
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending call log", e)
            }
        }
    }


    private fun triggerMoveWorker() {
        val work = OneTimeWorkRequestBuilder<MoveRecordingsWorker>().build()
        WorkManager.getInstance(this)
            .enqueueUniqueWork("auto_move_recordings_foreground", ExistingWorkPolicy.KEEP, work)
    }

//    // 🔹 Format as "yyyy-MM-dd HH:mm:ss" (local time)
//    private fun formatLocal(epochMs: Long): String {
//        return try {
//            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
//            sdf.timeZone = TimeZone.getDefault()
//            sdf.format(Date(epochMs))
//        } catch (e: Exception) {
//            ""
//        }
//    }
    fun formatIso(epochMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(epochMs))
        } catch (e: Exception) {
            ""
        }
    }

    // 🔹 Today at 00:00:00 (local)
    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun buildNotification(): Notification {
        val channel = "call_state_service_channel"
        return NotificationCompat.Builder(this, channel)
            .setContentTitle("Technfest CRM – Call sync active")
            .setContentText("Syncing call logs & recordings…")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(
                "call_state_service_channel",
                "Technfest CRM Call Sync",
                NotificationManager.IMPORTANCE_MIN
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
        phoneStateListener = null
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "CallStateService"
    }
}

// --- helper outside class, kept same ---
private fun formatInternationalNumber(rawNumber: String?, defaultRegion: String = "IN"): String {
    if (rawNumber.isNullOrEmpty()) return ""

    return try {
        val cleaned = rawNumber.replace("[^0-9+]".toRegex(), "")
        val phoneUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance()
        val region = if (cleaned.startsWith("+")) null else defaultRegion
        val numberProto = phoneUtil.parse(cleaned, region)
        phoneUtil.format(
            numberProto,
            com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
        )
    } catch (e: Exception) {
        rawNumber
    }


}
private fun normalizeForCompare(number: String?): String {
    if (number.isNullOrBlank()) return ""
    val digits = number.filter { it.isDigit() }
    return if (digits.length > 10) digits.takeLast(10) else digits
}

fun mapCallStatusLabel(direction: String?, callStatus: String?): String {
    return when (direction?.lowercase()) {
        "incoming" -> {
            when (callStatus?.lowercase()) {
                "answered" -> "Answered"
                "unanswered" -> "Missed"
                else -> "Missed" // fallback
            }
        }

        "outgoing" -> {
            when (callStatus?.lowercase()) {
                "answered" -> "Dialed"
                "unanswered" -> "No Dial"
                else -> "Dialed" // fallback
            }
        }

        else -> {
            // unknown direction
            "Unknown"
        }
    }
}


