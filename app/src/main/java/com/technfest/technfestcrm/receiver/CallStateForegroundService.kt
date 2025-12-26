package com.technfest.technfestcrm.receiver

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.os.Build
import android.os.IBinder
import android.provider.CallLog
import android.telecom.PhoneAccountHandle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
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
import com.technfest.technfestcrm.utils.SimPhoneAccountResolver
import com.technfest.technfestcrm.utils.SimSyncStore
import com.technfest.technfestcrm.view.LeadsFragment.LeadCacheItem
import com.technfest.technfestcrm.view.MainActivity
import com.technfest.technfestcrm.worker.MoveRecordingsWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.compareTo

class CallStateForegroundService : Service() {

    private lateinit var telephonyManager: TelephonyManager
    private var phoneStateListener: PhoneStateListener? = null

    private var currentNumber: String? = null
    private var currentDirection: String = "unknown"
    private var callAnswered: Boolean = false
    private var callStartTimeMs: Long = 0L

    private var lastKnownCallLogDateMs: Long = 0L
    private var lastCallSeenAtIdleMs: Long = 0L


    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(1001, buildNotification())

        telephonyManager = getSystemService(TELEPHONY_SERVICE) as TelephonyManager

        // baseline
        lastKnownCallLogDateMs = readLatestCallLog()?.dateMs ?: 0L

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
                            val hit = fillMetaFromLeadCacheIfNeeded(num)
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
                        Log.d(TAG, "OFFHOOK -> dir=$currentDirection number=$num")
                    }
                    TelephonyManager.CALL_STATE_IDLE -> {

                        val noRealCall =
                            !wasRinging && !wasOffhook && callStartTimeMs == 0L && !callAnswered && currentNumber.isNullOrEmpty()

                        if (noRealCall) {
                            Log.d(TAG, "IDLE -> initial state, no call. Ignoring.")
                            return
                        }

                        val expectedNumber = currentNumber

                        CoroutineScope(Dispatchers.IO).launch {

                            val last: LastCallLogRow? = waitForLatestCallLog(
                                expectedNumber = expectedNumber,
                                minDateMs = lastKnownCallLogDateMs,
                                maxWaitMs = 2500L,
                                intervalMs = 250L
                            )

                            if (last == null) {
                                Log.w(TAG, "IDLE -> calllog not updated in time. Skipping save.")
                                wasRinging = false
                                wasOffhook = false
                                resetState()
                                return@launch
                            }

                            // ✅ update baseline
                            lastKnownCallLogDateMs = maxOf(lastKnownCallLogDateMs, last.dateMs)

                            // ✅ Filter: only synced SIM
                            val ok = isCallFromSyncedSim(last.phoneAccountId, last.phoneAccountComponent)
                            if (!ok) {
                                Log.d(TAG, "Ignoring call: not from synced SIM. accId=${last.phoneAccountId}")
                                wasRinging = false
                                wasOffhook = false
                                resetState()
                                return@launch
                            }

                            val direction = when (last.type) {
                                CallLog.Calls.OUTGOING_TYPE -> "outgoing"
                                CallLog.Calls.INCOMING_TYPE,
                                CallLog.Calls.MISSED_TYPE,
                                CallLog.Calls.REJECTED_TYPE,
                                CallLog.Calls.BLOCKED_TYPE -> "incoming"
                                else -> currentDirection
                            }

                            val status = when (last.type) {
                                CallLog.Calls.MISSED_TYPE,
                                CallLog.Calls.REJECTED_TYPE,
                                CallLog.Calls.BLOCKED_TYPE -> "unanswered"
                                else -> if (last.durationSec > 0) "answered" else "unanswered"
                            }

                            val startStr = formatLocalDateTime(last.dateMs)
                            val endStr = formatLocalDateTime(last.dateMs + (last.durationSec * 1000L))

                            // ✅ Lead name for missed call notification
                            val meta = getSharedPreferences("ActiveCallLeadMeta", MODE_PRIVATE)
                            val leadName = meta.getString("leadName", "Unknown Lead") ?: "Unknown Lead"
                            val numberToShow = (last.number ?: meta.getString("customerNumber", "") ?: "").trim()

                            val isMissedLike =
                                last.type == CallLog.Calls.MISSED_TYPE ||
                                        last.type == CallLog.Calls.REJECTED_TYPE ||
                                        last.type == CallLog.Calls.BLOCKED_TYPE

                            // ✅ Save local + server for all calls (optional, you can skip for missed if you want)
                            saveRecentCallLocal(
                                number = last.number,
                                direction = direction,
                                status = status,
                                duration = last.durationSec,
                                startIso = startStr,
                                endIso = endStr,
                                phoneAccountId = last.phoneAccountId,
                                phoneAccountComponent = last.phoneAccountComponent,
                                timestampMsOverride = last.dateMs
                            )

                            sendCallLogToServer(
                                phoneNumber = last.number,
                                direction = direction,
                                durationSeconds = last.durationSec,
                                callStatus = status,
                                startIso = startStr,
                                endIso = endStr
                            )

                            // ✅ MISSED: notification only, NO feedback form
                            if (isMissedLike) {
                                CoroutineScope(Dispatchers.Main).launch {
                                    showMissedCallNotification(leadName, numberToShow)
                                }

                                wasRinging = false
                                wasOffhook = false
                                resetState()
                                return@launch
                            }

                            // ✅ ANSWERED (incoming/outgoing): show feedback form
                            CoroutineScope(Dispatchers.Main).launch {
                                FeedbackOverlay.show(this@CallStateForegroundService)
                            }

                            wasRinging = false
                            wasOffhook = false
                            resetState()
                        }
                    }


                }
            }


        }

        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE)



    }
    private fun showMissedCallNotification(leadName: String, number: String) {

        val channelId = "missed_call_channel"
        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ✅ Intent to open app + CallsFragment
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_SCREEN", "CALLS")
            putExtra("CALL_NUMBER", number)   // optional (if you want to auto-highlight/search)
            putExtra("LEAD_NAME", leadName)   // optional
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            2001,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                    (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Missed Calls",
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val title = "Missed Call"
        val body = "${leadName.ifBlank { "Unknown Lead" }}\n$number"

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(leadName)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent) // ✅ IMPORTANT
            .build()

        notificationManager.notify((System.currentTimeMillis() % 100000).toInt(), notification)
    }


    private fun resetState() {
        callAnswered = false
        callStartTimeMs = 0L
        currentDirection = "unknown"
        currentNumber = null
        CallPopupOverlay.hide()
    }

    // ----------------------------
    // LeadCache -> ActiveCallLeadMeta
    // ----------------------------
    private fun fillMetaFromLeadCacheIfNeeded(numberRaw: String?): Boolean {
        if (numberRaw.isNullOrBlank()) return false

        val e164 = normalizeForCompare(numberRaw)
        if (e164.isBlank()) return false

        val cachePrefs = getSharedPreferences("LeadCache", MODE_PRIVATE)
        val json = cachePrefs.getString("lead_map", null) ?: return false

        val type = object : TypeToken<Map<String, LeadCacheItem>>() {}.type
        val map: Map<String, LeadCacheItem> =
            try { Gson().fromJson(json, type) } catch (_: Exception) { emptyMap() }

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
    // Save Recent Call (LOCAL) using CallLog timestamp
    // ----------------------------
    private fun saveRecentCallLocal(
        number: String?,
        direction: String,
        status: String,
        duration: Int,
        startIso: String,
        endIso: String,
        phoneAccountId: String?,
        phoneAccountComponent: String?,
        timestampMsOverride: Long? = null
    ) {
        val meta = getSharedPreferences("ActiveCallLeadMeta", MODE_PRIVATE)
        val leadId = meta.getInt("leadId", 0)
        val leadName = meta.getString("leadName", "Unknown") ?: "Unknown"
        val num = number ?: meta.getString("customerNumber", "") ?: ""

        val selectedSubId = meta.getInt("selectedSubId", -1).takeIf { it != -1 }

        // dialer-like labels
        val statusLabel = when {
            direction == "incoming" && status == "answered" -> "Incoming"
            direction == "incoming" && status == "unanswered" -> "Missed"
            direction == "outgoing" && status == "answered" -> "Outgoing answered"
            direction == "outgoing" && status == "unanswered" -> "Outgoing Not answered"
            else -> "$direction • $status"
        }

        val prefs = getSharedPreferences("RecentCallsStore", MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString("recent_calls", null)

        val type = object : TypeToken<MutableList<RecentCallItem>>() {}.type
        val list: MutableList<RecentCallItem> =
            if (!json.isNullOrBlank()) {
                try { gson.fromJson(json, type) }
                catch (e: Exception) {
                    Log.e(TAG, "Gson parse failed for recent_calls. Resetting list!", e)
                    mutableListOf()
                }
            } else mutableListOf()

        val ts = timestampMsOverride ?: System.currentTimeMillis()

        list.add(
            0,
            RecentCallItem(
                id = ts,
                leadId = leadId,
                leadName = leadName,
                number = num,
                direction = direction,
                status = status,
                statusLabel = statusLabel,
                durationSec = duration,
                startIso = startIso,
                endIso = endIso,
                timestampMs = ts, // ✅ real dialer time
                phoneAccountId = phoneAccountId,
                phoneAccountComponent = phoneAccountComponent,
                usedSubId = selectedSubId
            )
        )

        while (list.size > 200) list.removeAt(list.size - 1)

        prefs.edit { putString("recent_calls", gson.toJson(list)) }

        // update UI
        sendBroadcast(Intent("com.technfest.technfestcrm.CALL_ACTIVITY_UPDATED"))
    }


    private fun getOrCreateUnknownLead(context: Context, rawNumber: String): LeadRequest {
        val normalized = normalizeForCompare(rawNumber)

        val existing = LocalLeadManager.getLeads(context).firstOrNull {
            normalizeForCompare(it.mobile) == normalized
        }
        if (existing != null) return existing

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
            assigned_to = "",
            campaignId = 0,
            followupDates = emptyList(),
            sourceDetails = "",
            tags = emptyList(),
            teamId = 0
        )

        LocalLeadManager.saveLead(context, newLead)
        return newLead
    }

    // ----------------------------
    // Send Call Log to Server
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

        val effectiveCustomerName =
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
            source = "call",
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
    // Notifications
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

    // ----------------------------
    // CallLog read (latest row)
    // ----------------------------
    private data class LastCallLogRow(
        val number: String?,
        val type: Int,
        val durationSec: Int,
        val dateMs: Long,
        val phoneAccountId: String?,
        val phoneAccountComponent: String?
    )

    @SuppressLint("MissingPermission")
    private fun readLatestCallLog(): LastCallLogRow? {
        return try {
            val projection = arrayOf(
                CallLog.Calls.NUMBER,
                CallLog.Calls.TYPE,
                CallLog.Calls.DURATION,
                CallLog.Calls.DATE,
                CallLog.Calls.PHONE_ACCOUNT_ID,
                CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME
            )

            contentResolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                CallLog.Calls.DATE + " DESC"
            )?.use { c ->
                if (!c.moveToFirst()) return null

                LastCallLogRow(
                    number = c.getStringSafe(CallLog.Calls.NUMBER),
                    type = c.getIntSafe(CallLog.Calls.TYPE),
                    durationSec = c.getIntSafe(CallLog.Calls.DURATION),
                    dateMs = c.getLongSafe(CallLog.Calls.DATE),
                    phoneAccountId = c.getStringSafe(CallLog.Calls.PHONE_ACCOUNT_ID),
                    phoneAccountComponent = c.getStringSafe(CallLog.Calls.PHONE_ACCOUNT_COMPONENT_NAME)
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "readLatestCallLog failed", e)
            null
        }
    }



    private suspend fun waitForLatestCallLog(
        expectedNumber: String?,
        minDateMs: Long,
        maxWaitMs: Long,
        intervalMs: Long
    ): LastCallLogRow? {

        val start = System.currentTimeMillis()
        val expectedNorm = normalizeForCompare(expectedNumber)

        while (System.currentTimeMillis() - start < maxWaitMs) {
            val row = readLatestCallLog()
            if (row != null) {
                val isNew = row.dateMs > minDateMs
                val rowNorm = normalizeForCompare(row.number)
                val matches = expectedNorm.isBlank() || (expectedNorm == rowNorm)

                if (isNew && matches) return row
            }

            kotlinx.coroutines.delay(intervalMs)
        }

        return null


    }



    // ----------------------------
    // Synced-SIM filter
    // ----------------------------
    private fun isCallFromSyncedSim(phoneAccountId: String?, phoneAccountComponent: String?): Boolean {
        val synced = SimSyncStore.getSynced(this)
        if (synced.isEmpty()) return false

        // If CallLog doesn't provide phoneAccount fields -> safe fallback
        if (phoneAccountId.isNullOrBlank() || phoneAccountComponent.isNullOrBlank()) {
            val meta = getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
            val selectedSubId = meta.getInt("selectedSubId", -1)

            if (selectedSubId != -1) {
                return synced.any { it.subId == selectedSubId }
            }
            return synced.size == 1
        }

        val comp = ComponentName.unflattenFromString(phoneAccountComponent) ?: return false

        if (!hasReadPhoneState(this)) {
            Log.w(TAG, "READ_PHONE_STATE missing; cannot match SIM accurately. Using safe fallback.")
            return synced.size == 1
        }

        val allowedHandles = mutableListOf<PhoneAccountHandle>()
        for (ss in synced) {
            try {
                val h = SimPhoneAccountResolver.resolvePhoneAccountHandleForSubId(this, ss.subId)
                if (h != null) allowedHandles.add(h)
            } catch (se: SecurityException) {
                Log.w(TAG, "SecurityException resolving handle for subId=${ss.subId}", se)
            } catch (e: Exception) {
                Log.e(TAG, "Error resolving handle for subId=${ss.subId}", e)
            }
        }

        if (allowedHandles.isEmpty()) {
            Log.w(TAG, "No phone account handles resolved; fallback.")
            return synced.size == 1
        }

        return allowedHandles.any { h ->
            h.componentName == comp && h.id == phoneAccountId
        }
    }

    // ----------------------------
    // Cursor safe helpers
    // ----------------------------
    private fun Cursor.getStringSafe(col: String): String? {
        val i = getColumnIndex(col)
        return if (i >= 0 && !isNull(i)) getString(i) else null
    }

    private fun Cursor.getIntSafe(col: String): Int {
        val i = getColumnIndex(col)
        return if (i >= 0 && !isNull(i)) getInt(i) else 0
    }

    private fun Cursor.getLongSafe(col: String): Long {
        val i = getColumnIndex(col)
        return if (i >= 0 && !isNull(i)) getLong(i) else 0L
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
// Permissions helper
// ----------------------------
private fun hasReadPhoneState(context: Context): Boolean {
    return ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.READ_PHONE_STATE
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
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

private fun formatLocalDateTime(epochMs: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss a", Locale.ENGLISH)
    sdf.timeZone = TimeZone.getDefault()
    return sdf.format(Date(epochMs))
}



