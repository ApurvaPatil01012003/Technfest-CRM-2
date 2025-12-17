package com.technfest.technfestcrm.receiver

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.provider.CallLog
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.TextView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.CallFeedback
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.technfest.technfestcrm.view.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CallPopupOverlay {

    private var windowManager: WindowManager? = null
    private var popupView: View? = null

    fun show(context: Context) {
        if (popupView != null) return

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = LayoutInflater.from(context)
        popupView = inflater.inflate(R.layout.call_pop_up, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT
        )
        params.y = 100

        val prefs = context.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
        val savedNumber = prefs.getString("customerNumber", "") ?: ""
        val leadName = prefs.getString("leadName", "") ?: ""

        val currentNumberRaw = CallStateForegroundService.currentNumberGlobal ?: ""

        val currentE164 = normalizeForCompare(currentNumberRaw)
        val savedE164 = normalizeForCompare(savedNumber)

        val displayLeadName = when {
            leadName.isNotBlank() && currentE164.isNotBlank() && savedE164.isNotBlank() && currentE164 == savedE164 -> leadName
            leadName.isNotBlank() -> leadName
            else -> "Unknown"
        }

        // ✅ Number to display: prefer current number, else saved number
        val displayNumber = if (currentNumberRaw.isNotBlank()) currentNumberRaw else savedNumber

        popupView?.findViewById<TextView>(R.id.tvLeadName)?.text = displayLeadName
        popupView?.findViewById<TextView>(R.id.tvMobile)?.text = displayNumber
        popupView?.findViewById<TextView>(R.id.tvLastCallTime)?.text =
            getLastCallTime(context, displayNumber)

        try {
            windowManager?.addView(popupView, params)
            Log.d("CallPopupOverlay", "Popup shown for number=$displayNumber lead=$displayLeadName")
        } catch (e: Exception) {
            Log.e("CallPopupOverlay", "Failed to show popup", e)
        }

        val lastFeedback = getLastFeedbackForNumber(context, displayNumber)
        popupView?.findViewById<TextView>(R.id.tvFeedbackNote)?.text =
            lastFeedback?.note?.takeIf { it.isNotBlank() } ?: "—"
        popupView?.findViewById<TextView>(R.id.tvLastCallBy)?.text =
            lastFeedback?.receivedBy ?: "—"
        popupView?.findViewById<ImageView>(R.id.ivClose)?.setOnClickListener {
            hide()
        }


        popupView?.findViewById<TextView>(R.id.tvViewLead)?.setOnClickListener {

            val prefs = context.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
            val leadId = prefs.getInt("leadId", 0)
            val workspaceId = prefs.getInt("workspaceId", 0)
            val token = prefs.getString("token", "") ?: ""

//            if (leadId > 0 && token.isNotBlank()) {

                val intent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("OPEN_LEAD_ID", leadId)
                    putExtra("OPEN_LEAD_WORKSPACE_ID", workspaceId)
                    putExtra("OPEN_LEAD_TOKEN", token)
                }
                context.startActivity(intent)
                hide()
            //}
        }

    }


    fun hide() {
        try {
            if (popupView != null && popupView?.isAttachedToWindow == true) {
                windowManager?.removeView(popupView)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("CallPopupOverlay", "View already removed", e)
        } finally {
            popupView = null
        }
    }

    private fun getLastCallTime(context: Context, phoneNumber: String): String {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            CallLog.Calls.CONTENT_URI,
            arrayOf(CallLog.Calls.DATE),
            CallLog.Calls.NUMBER + "=?",
            arrayOf(phoneNumber),
            CallLog.Calls.DATE + " DESC"
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val dateMillis = it.getLong(it.getColumnIndexOrThrow(CallLog.Calls.DATE))
                val sdf = SimpleDateFormat("dd MMM yyyy, h:mm a", Locale.getDefault())
                return "Last Call: ${sdf.format(Date(dateMillis))}"
            }
        }
        return "Last Call: —"
    }

    private fun getLastFeedbackForNumber(context: Context, phoneNumber: String): CallFeedback? {
        val prefs = context.getSharedPreferences("CallFeedbackStore", Context.MODE_PRIVATE)
        val json = prefs.getString("feedback_list", null) ?: return null

        val type = object : com.google.gson.reflect.TypeToken<List<CallFeedback>>() {}.type
        val list: List<CallFeedback> = com.google.gson.Gson().fromJson(json, type)

        return list.filter { it.number == phoneNumber }.maxByOrNull { it.timestamp }
    }

    private fun normalizeForCompare(number: String?, defaultRegion: String = "IN"): String {
        if (number.isNullOrBlank()) return ""
        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()
            val cleaned = number.replace("[^0-9+]".toRegex(), "")
            val region = if (cleaned.startsWith("+")) null else defaultRegion
            val proto = phoneUtil.parse(cleaned, region)
            phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            val digits = number.filter { it.isDigit() }
            if (digits.length > 10) digits.takeLast(10) else digits
        }
    }
}
