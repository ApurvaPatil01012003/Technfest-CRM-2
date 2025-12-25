//package com.technfest.technfestcrm.receiver
//
//import android.app.Notification
//import android.app.NotificationChannel
//import android.app.NotificationManager
//import android.app.Service
//import android.content.Context
//import android.content.Intent
//import android.os.Build
//import android.os.IBinder
//import android.telephony.PhoneStateListener
//import android.telephony.TelephonyManager
//import android.util.Log
//import androidx.core.app.NotificationCompat
//import androidx.work.ExistingWorkPolicy
//import androidx.work.OneTimeWorkRequestBuilder
//import androidx.work.WorkManager
//import com.technfest.technfestcrm.R
//import com.technfest.technfestcrm.model.CallLogRequest
//import com.technfest.technfestcrm.repository.CallLogRepository
//import com.technfest.technfestcrm.worker.MoveRecordingsWorker
//import kotlinx.coroutines.CoroutineScope
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.launch
//import java.text.SimpleDateFormat
//import java.util.Calendar
//import java.util.Date
//import java.util.Locale
//import java.util.TimeZone
//import androidx.core.content.edit
//import com.technfest.technfestcrm.view.LeadsFragment.LeadCacheItem
//
//
//class CallStateForegroundService : Service() {
//
//    private lateinit var telephonyManager: TelephonyManager
//    private var phoneStateListener: PhoneStateListener? = null
//
//    private var currentNumber: String? = null
//    private var currentDirection: String = "unknown"
//
//    private var callAnswered: Boolean = false
//
//    private var callStartTimeMs: Long = 0L
//
//    private val ANSWERED_MIN_SECONDS = 5
//
//    override fun onCreate() {
//        super.onCreate()
//        createNotificationChannel()
//        startForeground(1001, buildNotification())
//
//        telephonyManager = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
//        registerPhoneStateListener()
//    }
//
//    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return START_STICKY
//    }
//
//    private fun registerPhoneStateListener() {
//        if (phoneStateListener != null) return
//
//        var wasRinging = false
//        var wasOffhook = false
//        var activeNumber: String? = null
//
//        phoneStateListener = object : PhoneStateListener() {
//            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
//                super.onCallStateChanged(state, incomingNumber)
//
//                if (!incomingNumber.isNullOrEmpty()) {
//                    activeNumber = incomingNumber
//                    currentNumber = incomingNumber
//                    currentNumberGlobal = incomingNumber
//                }
//
//                when (state) {
//
//                    TelephonyManager.CALL_STATE_RINGING -> {
//                        fillMetaFromLeadCacheIfNeeded(incomingNumber)
//                        wasRinging = true
//                        wasOffhook = false
//                        currentDirection = "incoming"
//                        callAnswered = false
//                        Log.d(TAG, "RINGING -> number=$activeNumber")
//                    }
//
//                    TelephonyManager.CALL_STATE_OFFHOOK -> {
//                        fillMetaFromLeadCacheIfNeeded(incomingNumber ?: currentNumber)
//                        CallPopupOverlay.show(this@CallStateForegroundService)
//                        wasOffhook = true
//
//                        // If we never saw RINGING, this is likely outgoing dial
//                        if (!wasRinging) {
//                            currentDirection = "outgoing"
//                        }
//
//                        // 🔹 Conversation start = when we go OFFHOOK
//                        if (callStartTimeMs == 0L) {
//                            callStartTimeMs = System.currentTimeMillis()
//                        }
//
//                        // 🔹 For INCOMING calls: OFFHOOK after RINGING = user picked the call
//                        if (wasRinging && !callAnswered) {
//                            callAnswered = true
//                            Log.d(
//                                TAG,
//                                "INCOMING CALL PICKED at ${formatIso(callStartTimeMs)}"
//                            )
//                        } else if (!wasRinging) {
//                            // Outgoing call considered "answered" from our side
//                            callAnswered = true
//                            Log.d(
//                                TAG,
//                                "OUTGOING CALL STARTED at ${formatIso(callStartTimeMs)}"
//                            )
//                        }
//
//                        if (!incomingNumber.isNullOrEmpty()) {
//                            currentNumber = incomingNumber
//                            currentNumberGlobal = incomingNumber
//                        }
//
//                        CallPopupOverlay.show(this@CallStateForegroundService)
//                    }
//
//                    TelephonyManager.CALL_STATE_IDLE -> {
//                        val endTimeMs = System.currentTimeMillis()
//
//                        val noRealCall =
//                            !wasRinging && !wasOffhook &&
//                                    callStartTimeMs == 0L &&
//                                    !callAnswered &&
//                                    currentNumber.isNullOrEmpty()
//
//                        if (noRealCall) {
//                            Log.d(TAG, "IDLE -> initial state, no call. Ignoring.")
//                            return
//                        }
//
//                        val callStatus: String
//                        val durationSec: Int
//                        val startStr: String
//                        val endStr: String
//
//                        // For answered logic we still use threshold for outgoing
//                        val calculatedDurationSec =
//                            if (callStartTimeMs > 0L) {
//                                ((endTimeMs - callStartTimeMs) / 1000)
//                                    .toInt()
//                                    .coerceAtLeast(0)
//                            } else {
//                                0
//                            }
//
//                        val isAnswered = when (currentDirection) {
//                            "incoming" -> callAnswered
//                            "outgoing" -> calculatedDurationSec >= ANSWERED_MIN_SECONDS
//                            else -> calculatedDurationSec >= ANSWERED_MIN_SECONDS
//                        }
//
//                        if (isAnswered && callStartTimeMs > 0L) {
//                            // ✅ Answered call:
//                            // start = OFFHOOK (pickup), end = hangup, duration = talk time
//                            callStatus = "answered"
//                            durationSec = calculatedDurationSec
//                            startStr = formatIso(callStartTimeMs)
//                            endStr = formatIso(endTimeMs)
//                        } else {
//
//                            callStatus = "unanswered"
//                            durationSec = 0
//                            val midnight = todayMidnightMs()
//                            startStr = formatIso(midnight)
//                            endStr = startStr
//                        }
//
//                        Log.d(
//                            TAG,
//                            "IDLE -> call ended. number=$currentNumber dir=$currentDirection " +
//                                    "status=$callStatus duration=$durationSec s start=$startStr end=$endStr"
//                        )
//
//
//
//
//                        // send to server
//                        sendCallLogToServer(
//                            phoneNumber = currentNumber,
//                            direction = currentDirection,
//                            durationSeconds = durationSec,
//                            callStatus = callStatus,
//                            startIso = startStr,   // now in ISO
//                            endIso = endStr
//                        )
//
//                        wasRinging = false
//                        wasOffhook = false
//                        callAnswered = false
//                        callStartTimeMs = 0L
//                        currentDirection = "unknown"
//                        currentNumber = null
//
//                        CallPopupOverlay.hide()
//                        FeedbackOverlay.show(this@CallStateForegroundService)
//                    }
//                }
//            }
//        }
//
//        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
//    }
//
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
//        val metaLeadId = meta.getInt("leadId", 0)
//        val metaLeadName = meta.getString("leadName", "") ?: ""
//        val campaignId = meta.getInt("campaignId", 0)
//        val campaignCode = meta.getString("campaignCode", "") ?: ""
//        val savedCustomerNumber = meta.getString("customerNumber", "") ?: ""
//
//        // 1) Final display number (universal format)
//        val rawNumber = (phoneNumber ?: savedCustomerNumber).trim()
//        val finalNumber = formatInternationalNumber(rawNumber, "IN")
//
//
//        val normIncoming = normalizeForCompare(phoneNumber)
//        val normSaved = normalizeForCompare(savedCustomerNumber)
//
//
//        val effectiveLeadId: Int? = when {
//
//            metaLeadId <= 0 -> null
//
//            normIncoming.isBlank() && normSaved.isNotBlank() -> metaLeadId
//
//            normIncoming.isNotBlank() && normIncoming == normSaved -> metaLeadId
//
//            else -> null
//        }
//
////        val effectiveCustomerName: String =
////            if (effectiveLeadId != null) metaLeadName else ""
//
//        val effectiveCustomerName: String =
//            if (effectiveLeadId != null && metaLeadName.isNotBlank()) metaLeadName else ""
//
//
//        val request = CallLogRequest(
//            workspaceId = workspaceId,
//            userId = userId,
//            leadId = effectiveLeadId,
//            campaignId = campaignId,
//            campaignCode = campaignCode,
//            customerNumber = finalNumber,
//            customerName = effectiveCustomerName,
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
//                    if (id > 0) {
//                        ctx.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE)
//                            .edit { putInt("lastCallLogId", id) }
//
////                        val work = OneTimeWorkRequestBuilder<MoveRecordingsWorker>().build()
////                        WorkManager.getInstance(ctx)
////                            .enqueueUniqueWork("auto_move_recordings_foreground", ExistingWorkPolicy.REPLACE, work)
//
//
//                        val callEndMs = System.currentTimeMillis()
//
//                        val work = OneTimeWorkRequestBuilder<MoveRecordingsWorker>()
//                            .setInputData(
//                                androidx.work.workDataOf(
//                                    "CALL_LOG_ID" to id,
//                                    "CALL_END_MS" to callEndMs
//                                )
//                            )
//                            .build()
//
//                        WorkManager.getInstance(ctx)
//                            // ✅ unique per call log id, so same call won't run twice
//                            .enqueueUniqueWork(
//                                "move_recording_call_$id",
//                                ExistingWorkPolicy.KEEP,
//                                work
//                            )
//
//                    }
//                }
//
//            } catch (e: Exception) {
//                Log.e(TAG, "Exception sending call log", e)
//            }
//        }
//    }
//
//    fun formatIso(epochMs: Long): String {
//        return try {
//            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
//            sdf.timeZone = TimeZone.getTimeZone("UTC")
//            sdf.format(Date(epochMs))
//        } catch (e: Exception) {
//            ""
//        }
//    }
//
//    private fun todayMidnightMs(): Long {
//        val cal = Calendar.getInstance()
//        cal.set(Calendar.HOUR_OF_DAY, 0)
//        cal.set(Calendar.MINUTE, 0)
//        cal.set(Calendar.SECOND, 0)
//        cal.set(Calendar.MILLISECOND, 0)
//        return cal.timeInMillis
//    }
//
//    private fun buildNotification(): Notification {
//        val channel = "call_state_service_channel"
//        return NotificationCompat.Builder(this, channel)
//            .setContentTitle("Technfest CRM – Call sync active")
//            .setContentText("Syncing call logs & recordings…")
//            .setSmallIcon(R.drawable.ic_launcher_foreground)
//            .setPriority(NotificationCompat.PRIORITY_MIN)
//            .setOngoing(true)
//            .build()
//    }
//    private fun fillMetaFromLeadCacheIfNeeded(numberRaw: String?) {
//        if (numberRaw.isNullOrBlank()) return
//
//        val e164 = normalizeForCompare(numberRaw)
//        if (e164.isBlank()) return
//
//        val cachePrefs = getSharedPreferences("LeadCache", Context.MODE_PRIVATE)
//        val json = cachePrefs.getString("lead_map", null) ?: return
//
//        val type = object : com.google.gson.reflect.TypeToken<Map<String, LeadCacheItem>>() {}.type
//        val map: Map<String, LeadCacheItem> = com.google.gson.Gson().fromJson(json, type)
//
//        val hit = map[e164] ?: return
//
//        // ✅ write ActiveCallLeadMeta so overlay can show name even for default dialer first call
//        val meta = getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
//        meta.edit()
//            .putInt("leadId", hit.id)
//            .putString("leadName", hit.name)
//            .putInt("campaignId", hit.campaignId)
//            .putString("campaignCode", hit.campaignCode)
//            .putString("customerNumber", hit.customerNumber)
//            .apply()
//    }
//
//
//    private fun createNotificationChannel() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            val ch = NotificationChannel(
//                "call_state_service_channel",
//                "Technfest CRM Call Sync",
//                NotificationManager.IMPORTANCE_MIN
//            )
//            getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
//        }
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        phoneStateListener?.let { telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE) }
//        phoneStateListener = null
//    }
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    companion object {
//        private const val TAG = "CallStateService"
//        var currentNumberGlobal: String? = null
//    }
//
//}
//private fun lookupLeadNameByNumber(ctx: Context, rawNumber: String?): Pair<Int, String>? {
//    if (rawNumber.isNullOrBlank()) return null
//
//    val e164 = normalizeForCompare(rawNumber) // returns E164 like +9198...
//    if (e164.isBlank()) return null
//
//    val prefs = ctx.getSharedPreferences("LeadCache", Context.MODE_PRIVATE)
//    val name = prefs.getString("lead_name_$e164", null)
//    val id = prefs.getInt("lead_id_$e164", 0)
//
//    return if (!name.isNullOrBlank() && id > 0) Pair(id, name) else null
//}
//
//private fun formatInternationalNumber(rawNumber: String?, defaultRegion: String = "IN"): String {
//    if (rawNumber.isNullOrEmpty()) return ""
//
//    return try {
//        val cleaned = rawNumber.replace("[^0-9+]".toRegex(), "")
//        val phoneUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance()
//        val region = if (cleaned.startsWith("+")) null else defaultRegion
//        val numberProto = phoneUtil.parse(cleaned, region)
//        phoneUtil.format(
//            numberProto,
//            com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL
//        )
//    } catch (e: Exception) {
//        rawNumber
//    }
//}
//private fun normalizeForCompare(number: String?, defaultRegion: String = "IN"): String {
//    if (number.isNullOrBlank()) return ""
//    return try {
//        val phoneUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance()
//        val cleaned = number.replace("[^0-9+]".toRegex(), "")
//        val region = if (cleaned.startsWith("+")) null else defaultRegion
//        val proto = phoneUtil.parse(cleaned, region)
//
//        phoneUtil.format(
//            proto,
//            com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164
//        )
//    } catch (e: Exception) {
//
//        val digits = number.filter { it.isDigit() }
//        if (digits.length > 10) digits.takeLast(10) else digits
//    }
//
//
//
//
//}
//
//
//
//


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
import androidx.core.content.edit
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.localdatamanager.LocalLeadManager
import com.technfest.technfestcrm.model.CallLogRequest
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.RecentCallItem
import com.technfest.technfestcrm.repository.CallLogRepository
import com.technfest.technfestcrm.view.LeadsFragment.LeadCacheItem
import com.technfest.technfestcrm.worker.MoveRecordingsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class CallStateForegroundService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    private var currentNumber: String? = null
    private var currentDirection: String = "unknown"
    private var callAnswered: Boolean = false
    private var callStartTimeMs: Long = 0L

    private val ANSWERED_MIN_SECONDS = 5

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, buildNotification())

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager
        registerPhoneStateListener()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int = START_STICKY

    private fun registerPhoneStateListener() {
        if (phoneStateListener != null) return

        var wasRinging = false
        var wasOffhook = false

        phoneStateListener = object : PhoneStateListener() {

            override fun onCallStateChanged(state: Int, incomingNumber: String?) {
                super.onCallStateChanged(state, incomingNumber)

                if (!incomingNumber.isNullOrBlank()) {
                    currentNumber = incomingNumber
                    currentNumberGlobal = incomingNumber
                }

                when (state) {

                    TelephonyManager.CALL_STATE_RINGING -> {
                        wasRinging = true
                        wasOffhook = false
                        currentDirection = "incoming"
                        callAnswered = false
                        callStartTimeMs = 0L

                        val num = incomingNumber ?: currentNumber
                        if (!num.isNullOrBlank()) {
                            // 1) try from LeadCache -> ActiveCallLeadMeta
                            val hit = fillMetaFromLeadCacheIfNeeded(num)

                            // 2) if not found -> create unknown lead locally & fill meta
                            if (!hit) {
                                val lead = getOrCreateUnknownLead(this@CallStateForegroundService, num)
                                writeActiveCallMeta(leadId = lead.id, leadName = lead.fullName, customerNumber = num)
                            }
                        }

                        Log.d(TAG, "RINGING -> number=$num")
                    }

                    TelephonyManager.CALL_STATE_OFFHOOK -> {
                        wasOffhook = true

                        if (!wasRinging) currentDirection = "outgoing"

                        if (callStartTimeMs == 0L) callStartTimeMs = System.currentTimeMillis()

                        callAnswered = true

                        val num = incomingNumber ?: currentNumber
                        if (!num.isNullOrBlank()) {
                            val hit = fillMetaFromLeadCacheIfNeeded(num)
                            if (!hit) {
                                val lead = getOrCreateUnknownLead(this@CallStateForegroundService, num)
                                writeActiveCallMeta(leadId = lead.id, leadName = lead.fullName, customerNumber = num)
                            }
                        }

                        CallPopupOverlay.show(this@CallStateForegroundService)
                        Log.d(TAG, "OFFHOOK -> dir=$currentDirection number=$num start=${formatIso(callStartTimeMs)}")
                    }

                    TelephonyManager.CALL_STATE_IDLE -> {
                        val endTimeMs = System.currentTimeMillis()

                        val noRealCall =
                            !wasRinging && !wasOffhook &&
                                    callStartTimeMs == 0L &&
                                    !callAnswered &&
                                    currentNumber.isNullOrEmpty()

                        if (noRealCall) {
                            Log.d(TAG, "IDLE -> initial state, no call. Ignoring.")
                            return
                        }

                        val calculatedDurationSec =
                            if (callStartTimeMs > 0L) ((endTimeMs - callStartTimeMs) / 1000).toInt().coerceAtLeast(0)
                            else 0

                        val isAnswered = when (currentDirection) {
                            "incoming" -> callAnswered
                            "outgoing" -> calculatedDurationSec >= ANSWERED_MIN_SECONDS
                            else -> calculatedDurationSec >= ANSWERED_MIN_SECONDS
                        }

                        val callStatus: String
                        val durationSec: Int
                        val startStr: String
                        val endStr: String

                        if (isAnswered && callStartTimeMs > 0L) {
                            callStatus = "answered"
                            durationSec = calculatedDurationSec
                            startStr = formatIso(callStartTimeMs)
                            endStr = formatIso(endTimeMs)
                        } else {
                            callStatus = "unanswered"
                            durationSec = 0
                            val midnight = todayMidnightMs()
                            startStr = formatIso(midnight)
                            endStr = startStr
                        }

                        Log.d(
                            TAG,
                            "IDLE -> end. number=$currentNumber dir=$currentDirection status=$callStatus dur=$durationSec start=$startStr end=$endStr"
                        )

                        saveRecentCallLocal(
                            number = currentNumber,
                            direction = currentDirection,
                            status = callStatus,
                            duration = durationSec,
                            startIso = startStr,
                            endIso = endStr
                        )

                        sendCallLogToServer(
                            phoneNumber = currentNumber,
                            direction = currentDirection,
                            durationSeconds = durationSec,
                            callStatus = callStatus,
                            startIso = startStr,
                            endIso = endStr
                        )

                        // reset
                        wasRinging = false
                        wasOffhook = false
                        callAnswered = false
                        callStartTimeMs = 0L
                        currentDirection = "unknown"
                        currentNumber = null

                        CallPopupOverlay.hide()
                        FeedbackOverlay.show(this@CallStateForegroundService)
                    }
                }
            }
        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    // ----------------------------
    // ✅ LeadCache -> ActiveCallLeadMeta
    // returns true if cache hit
    // ----------------------------
    private fun fillMetaFromLeadCacheIfNeeded(numberRaw: String?): Boolean {
        if (numberRaw.isNullOrBlank()) return false

        val e164 = normalizeForCompare(numberRaw)
        if (e164.isBlank()) return false

        val cachePrefs = getSharedPreferences("LeadCache", MODE_PRIVATE)
        val json = cachePrefs.getString("lead_map", null) ?: return false

        val type = object : TypeToken<Map<String, LeadCacheItem>>() {}.type
        val map: Map<String, LeadCacheItem> = try { Gson().fromJson(json, type) } catch (_: Exception) { emptyMap() }

        val hit = map[e164] ?: return false

        getSharedPreferences("ActiveCallLeadMeta", MODE_PRIVATE).edit()
            .putInt("leadId", hit.id)
            .putString("leadName", hit.name)
            .putInt("campaignId", hit.campaignId)
            .putString("campaignCode", hit.campaignCode)
            .putString("customerNumber", hit.customerNumber)
            .apply()

        return true
    }

    private fun writeActiveCallMeta(leadId: Int, leadName: String, customerNumber: String) {
        getSharedPreferences("ActiveCallLeadMeta", MODE_PRIVATE).edit()
            .putInt("leadId", leadId)
            .putString("leadName", leadName)
            .putString("customerNumber", customerNumber)
            .apply()
    }

    // ----------------------------
    // ✅ Local Recent Calls Store
    // ----------------------------
    data class RecentCallItem(
        val id: Long,
        val leadId: Int,
        val leadName: String,
        val number: String,
        val direction: String,
        val status: String,
        val durationSec: Int,
        val startIso: String,
        val endIso: String,
        val timestampMs: Long
    )

    private fun saveRecentCallLocal(
        number: String?,
        direction: String,
        status: String,
        duration: Int,
        startIso: String,
        endIso: String
    ) {
        val meta = getSharedPreferences("ActiveCallLeadMeta", MODE_PRIVATE)
        val leadId = meta.getInt("leadId", 0)
        val leadName = meta.getString("leadName", "Unknown") ?: "Unknown"
        val num = number ?: meta.getString("customerNumber", "") ?: ""

        val prefs = getSharedPreferences("RecentCallsStore", MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("recent_calls", null)

        val type = object : TypeToken<MutableList<RecentCallItem>>() {}.type
        val list: MutableList<RecentCallItem> = if (!json.isNullOrBlank()) {
            try { gson.fromJson(json, type) } catch (_: Exception) { mutableListOf() }
        } else mutableListOf()

        list.add(
            0,
            RecentCallItem(
                id = System.currentTimeMillis(),
                leadId = leadId,
                leadName = leadName,
                number = num,
                direction = direction,
                status = status,
                durationSec = duration,
                startIso = startIso,
                endIso = endIso,
                timestampMs = System.currentTimeMillis()
            )
        )

        while (list.size > 200) list.removeAt(list.size - 1)

        prefs.edit { putString("recent_calls", gson.toJson(list)) }

        sendBroadcast(Intent("com.technfest.technfestcrm.CALL_ACTIVITY_UPDATED"))
    }

    // ----------------------------
    // ✅ Create/Reuse Unknown lead locally (NO TODO)
    // ----------------------------
    private fun getOrCreateUnknownLead(context: Context, rawNumber: String): LeadRequest {
        val normalized = normalizeForCompare(rawNumber)

        val existing = LocalLeadManager.getLeads(context).firstOrNull {
            normalizeForCompare(it.mobile) == normalized
        }
        if (existing != null) return existing

        // 🔻 Adjust defaults if your LeadRequest types differ
        val newLead = LeadRequest(
            id = (System.currentTimeMillis() % Int.MAX_VALUE).toInt(),
            fullName = "Unknown",
            mobile = rawNumber,
            status = "New",
            stage = "New",
            priority = "Normal",
            source = "Call",
            company = "",
            email = "",
            location = "",
            leadRequirement = "",
            campaignName = "",
            nextFollowupAt = "",
            ownerName = "",
            teamName = "",
            note = "",

            assigned_to = "",              // Int default
            campaignId = 0,               // Int default
            followupDates = emptyList(),  // List default
            sourceDetails = "",           // String default (or emptyMap if your type is Map)
            tags = emptyList(),           // List default
            teamId = 0                    // Int default
        )

        LocalLeadManager.saveLead(context, newLead)
        return newLead
    }

    // ----------------------------
    // ✅ Send Call Log to Server (your “effectiveLeadId” safe logic)
    // ----------------------------
    private fun sendCallLogToServer(
        phoneNumber: String?,
        direction: String,
        durationSeconds: Int,
        callStatus: String,
        startIso: String?,
        endIso: String
    ) {
        val ctx = this

        val sessionPrefs = ctx.getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = sessionPrefs.getString("token", null)
        val workspaceId = sessionPrefs.getInt("workspaceId", 0)
        val userId = sessionPrefs.getInt("userId", 0)

        if (token.isNullOrEmpty() || workspaceId == 0) {
            Log.e(TAG, "Missing session token/workspaceId — skipping call log")
            return
        }

        val meta = ctx.getSharedPreferences("ActiveCallLeadMeta", MODE_PRIVATE)
        val metaLeadId = meta.getInt("leadId", 0)
        val metaLeadName = meta.getString("leadName", "") ?: ""
        val campaignId = meta.getInt("campaignId", 0)
        val campaignCode = meta.getString("campaignCode", "") ?: ""
        val savedCustomerNumber = meta.getString("customerNumber", "") ?: ""

        val rawNumber = (phoneNumber ?: savedCustomerNumber).trim()
        val finalNumber = formatInternationalNumber(rawNumber, "IN")

        val normIncoming = normalizeForCompare(phoneNumber)
        val normSaved = normalizeForCompare(savedCustomerNumber)

        val effectiveLeadId: Int? = when {
            metaLeadId <= 0 -> null
            normIncoming.isBlank() && normSaved.isNotBlank() -> metaLeadId
            normIncoming.isNotBlank() && normIncoming == normSaved -> metaLeadId
            else -> null
        }

        val effectiveCustomerName: String =
            if (effectiveLeadId != null && metaLeadName.isNotBlank()) metaLeadName else ""

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
                    if (id > 0) {
                        ctx.getSharedPreferences("CallLogPrefs", MODE_PRIVATE)
                            .edit { putInt("lastCallLogId", id) }

                        val callEndMs = System.currentTimeMillis()
                        val work = OneTimeWorkRequestBuilder<MoveRecordingsWorker>()
                            .setInputData(workDataOf("CALL_LOG_ID" to id, "CALL_END_MS" to callEndMs))
                            .build()

                        WorkManager.getInstance(ctx).enqueueUniqueWork(
                            "move_recording_call_$id",
                            ExistingWorkPolicy.KEEP,
                            work
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception sending call log", e)
            }
        }
    }

    // ----------------------------
    // Utils
    // ----------------------------
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

    private fun todayMidnightMs(): Long {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun formatIso(epochMs: Long): String {
        return try {
            val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.format(Date(epochMs))
        } catch (_: Exception) {
            ""
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
        var currentNumberGlobal: String? = null
    }
}

// ----------------------------
// Number helpers
// ----------------------------
private fun formatInternationalNumber(rawNumber: String?, defaultRegion: String = "IN"): String {
    if (rawNumber.isNullOrEmpty()) return ""
    return try {
        val cleaned = rawNumber.replace("[^0-9+]".toRegex(), "")
        val phoneUtil = PhoneNumberUtil.getInstance()
        val region = if (cleaned.startsWith("+")) null else defaultRegion
        val numberProto = phoneUtil.parse(cleaned, region)
        phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.INTERNATIONAL)
    } catch (_: Exception) {
        rawNumber
    }
}

private fun normalizeForCompare(number: String?, defaultRegion: String = "IN"): String {
    if (number.isNullOrBlank()) return ""
    return try {
        val phoneUtil = PhoneNumberUtil.getInstance()
        val cleaned = number.replace("[^0-9+]".toRegex(), "")
        val region = if (cleaned.startsWith("+")) null else defaultRegion
        val proto = phoneUtil.parse(cleaned, region)
        phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)
    } catch (_: Exception) {
        val digits = number.filter { it.isDigit() }
        if (digits.length > 10) digits.takeLast(10) else digits
    }
}
