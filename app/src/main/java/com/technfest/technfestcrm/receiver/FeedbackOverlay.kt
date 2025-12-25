package com.technfest.technfestcrm.receiver

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.graphics.Rect
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.InputType
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.RatingBar
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.CallFeedback
import com.technfest.technfestcrm.model.LocalTask
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object FeedbackOverlay {

    private var view: View? = null
    private var windowManager: WindowManager? = null
    fun show(context: Context) {
        if (view != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(context)) {
                Log.e("FeedbackOverlay", "Overlay permission missing")
                return
            }
        }

        windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val themedContext = ContextThemeWrapper(
            context,
            R.style.Theme_TechnfestCRM
        )


        view = LayoutInflater.from(themedContext)
            .inflate(R.layout.alert_feedback_form, null)

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.TOP
        params.softInputMode =
            WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE



        try {
            windowManager?.addView(view, params)
            setupUI(context)
            Log.d("FeedbackOverlay", "Feedback overlay shown")
        } catch (e: Exception) {
            Log.e("FeedbackOverlay", "addView failed", e)
        }
    }



    private fun setupUI(context: Context) {
        setupReceivedBySpinner(context)
        setupFollowUpPicker(context)
        setupKeyboardScroll()

        val etLeadName = view!!.findViewById<EditText>(R.id.etLeadName)
        val etNumber = view!!.findViewById<EditText>(R.id.etNumber)
        val ivClose = view!!.findViewById<ImageView>(R.id.ivClose)
        val btnSave = view!!.findViewById<MaterialButton>(R.id.btnSaveFeedback)

        val prefs = context.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
        val savedNumber = prefs.getString("customerNumber", "") ?: ""
        val leadName = prefs.getString("leadName", "")

        val currentNumber = CallStateForegroundService.currentNumberGlobal ?: ""

        etLeadName.setText(if (currentNumber == savedNumber) leadName else "")
        etNumber.setText(currentNumber.ifEmpty { savedNumber })

        ivClose.setOnClickListener { hide() }

        btnSave.setOnClickListener {
            saveFeedback(context) }
    }

    private fun saveFeedback(context: Context) {
        val etLeadName = view!!.findViewById<EditText>(R.id.etLeadName)
        val etNumber = view!!.findViewById<EditText>(R.id.etNumber)
        val rgStatus = view!!.findViewById<RadioGroup>(R.id.rgStatus)

        val selectedId = rgStatus.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(context, "Please select call status", Toast.LENGTH_SHORT).show()
            return // exit, overlay stays open
        }

        // --- proceed to save ---
        val editedName = etLeadName.text.toString().trim()
        val number = etNumber.text.toString().trim()
        if (editedName.isNotEmpty() && number.isNotEmpty()) {
            saveEditedLeadName(context, number, editedName)
        }

        val etNote = view!!.findViewById<EditText>(R.id.etNote)
        val ratingBar = view!!.findViewById<RatingBar>(R.id.ratingBar)
        val edtFollowUp = view!!.findViewById<EditText>(R.id.edtFollowUpdate)
        val spinner = view!!.findViewById<Spinner>(R.id.spnLastCallReceivedUser)

        val status = view!!.findViewById<RadioButton>(selectedId).text.toString()
        val receivedBy = spinner.selectedItem?.toString()

        val followUpText = edtFollowUp.text.toString().trim()

        if (followUpText.isNotEmpty()) {
            createLocalFollowUpTask(
                context = context,
                number = number,
                leadName = editedName.ifBlank { etLeadName.text.toString() },
                followUpDate = followUpText,
                assignedUser = receivedBy.toString()
            )
        }
        val metaPrefs =
            context.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)

        val resolvedLeadId = resolveLeadIdForNumber(context, number)
        val leadId = if (resolvedLeadId > 0) resolvedLeadId else metaPrefs.getInt("leadId", 0)

        Log.d("FeedbackOverlay", "Resolved leadId=$leadId number=$number")



        val feedback = CallFeedback(
            leadId = leadId,
            number = number,
            leadName = etLeadName.text.toString().ifBlank { null },
            callStatus = status,
            rating = ratingBar.rating.toInt(),
            note = etNote.text.toString(),
            followUp = edtFollowUp.text.toString(),
            receivedBy = receivedBy,
           timestamp = System.currentTimeMillis()
        )

        saveFeedbackToPrefs(context, feedback)

        context.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
            .edit()
            .remove("campaignId")
            .remove("campaignCode")
            // keep leadId, leadName, customerNumber, selectedSubId
            .apply()

        Log.d("FeedbackOverlay", "Saving feedback leadId=$leadId number=$number")

        Toast.makeText(context, "Feedback Saved", Toast.LENGTH_SHORT).show()
        hide() // hide overlay only after successful save
    }

    private fun saveFeedbackToPrefs(context: Context, feedback: CallFeedback) {

        val prefs = context.getSharedPreferences("CallFeedbackStore", Context.MODE_PRIVATE)
        val gson = Gson()

        val oldJson = prefs.getString("feedback_list", null)
        val type = object :
            TypeToken<MutableList<CallFeedback>>() {}.type

        val list: MutableList<CallFeedback> =
            if (oldJson != null)
                gson.fromJson(oldJson, type)
            else
                mutableListOf()

        list.add(feedback)

        prefs.edit()
            .putString("feedback_list", gson.toJson(list))
            .apply()


        context.sendBroadcast(
            Intent("com.technfest.technfestcrm.CALL_ACTIVITY_UPDATED")
                .setPackage(context.packageName)
        )

    }




    fun hide() {
        try {
            if (view != null && view?.isAttachedToWindow == true) {
                windowManager?.removeView(view)
            }
        } catch (e: IllegalArgumentException) {
            Log.w("FeedbackOverlay", "View already removed", e)
        } finally {
            view = null
        }
    }

    private fun setupReceivedBySpinner(context: Context) {
        val spinner = view!!.findViewById<Spinner>(R.id.spnLastCallReceivedUser)

        val names = listOf("Sagar", "Pratik")

        spinner.adapter = ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            names
        )
        spinner.setSelection(0)
    }


    private fun setupFollowUpPicker(context: Context) {

        val edtFollowUp = view!!.findViewById<EditText>(R.id.edtFollowUpdate)

        edtFollowUp.apply {
            isFocusable = false
            isClickable = true
            isLongClickable = false
            inputType = InputType.TYPE_NULL
        }

        edtFollowUp.setOnClickListener {
            pickDateThenTime(context) { result ->
                edtFollowUp.setText(result)
            }
        }
    }
    private fun pickDateThenTime(
        context: Context,
        onResult: (String) -> Unit
    ) {
        val cal = Calendar.getInstance()

        val datePicker = DatePickerDialog(
            context,
            { _, year, month, day ->

                cal.set(Calendar.YEAR, year)
                cal.set(Calendar.MONTH, month)
                cal.set(Calendar.DAY_OF_MONTH, day)

                showOverlayTimePicker24(context, cal, onResult)

            },
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH),
            cal.get(Calendar.DAY_OF_MONTH)
        )

        datePicker.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        )

        datePicker.show()
    }

    private fun showOverlayTimePicker24(
        context: Context,
        cal: Calendar,
        onResult: (String) -> Unit
    ) {
        val timePicker = TimePickerDialog(
            context,
            { _, hour, min ->

                cal.set(Calendar.HOUR_OF_DAY, hour)
                cal.set(Calendar.MINUTE, min)
                cal.set(Calendar.SECOND, 0)

                val finalDateTime = String.format(
                    "%04d-%02d-%02d %02d:%02d:00",
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH) + 1,
                    cal.get(Calendar.DAY_OF_MONTH),
                    hour,
                    min
                )
                Log.d("FollowUpPicker", "Picker result = $finalDateTime")


                onResult(finalDateTime)
            },
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            true
        )

        timePicker.window?.setType(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE
        )

        timePicker.show()
    }

    private fun saveEditedLeadName(context: Context, number: String, name: String) {

        val e164 = normalizeToE164(context, number)

        val prefs = context.getSharedPreferences("EditedLeadNames", Context.MODE_PRIVATE)
        prefs.edit()
            .putString(e164, name)
            .apply()
    }

    private fun normalizeToE164(context: Context, raw: String): String {
        if (raw.isBlank()) return ""

        return try {
            val cleaned = raw.replace("[^0-9+]".toRegex(), "")

            val phoneUtil = PhoneNumberUtil.getInstance()
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

            val region = (tm.networkCountryIso ?: tm.simCountryIso)
                ?.uppercase()
                ?.takeIf { it.isNotBlank() }

            val proto = if (cleaned.startsWith("+")) {
                phoneUtil.parse(cleaned, null)
            } else {
                phoneUtil.parse(cleaned, region ?: "IN")
            }

            phoneUtil.format(
                proto,
                PhoneNumberUtil.PhoneNumberFormat.E164
            )
        } catch (e: Exception) {
            ""
        }
    }
    private fun setupKeyboardScroll() {

        val scrollView = view!!.findViewById<ScrollView>(R.id.scrollContainer)
        val root = view!!

        root.viewTreeObserver.addOnGlobalLayoutListener {
            val rect = Rect()
            root.getWindowVisibleDisplayFrame(rect)

            val screenHeight = root.rootView.height
            val keyboardHeight = screenHeight - rect.bottom

            val isKeyboardOpen = keyboardHeight > screenHeight * 0.15

            if (isKeyboardOpen) {
                val focused = root.findFocus()
                focused?.let {
                    scrollView.post {
                        scrollView.smoothScrollTo(0, it.bottom)
                    }
                }
            }
        }
    }




    private fun createLocalFollowUpTask(
        context: Context,
        number: String,
        leadName: String?,
        assignedUser: String,
        followUpDate: String
    ) {
        val prefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE)
        val gson = Gson()

        val oldJson = prefs.getString("task_list", null)
        val type = object : TypeToken<MutableList<LocalTask>>() {}.type

        val list: MutableList<LocalTask> =
            if (oldJson != null) gson.fromJson(oldJson, type) else mutableListOf()

        val task = LocalTask(
            id = System.currentTimeMillis().toInt(),
            title = "Call Follow-up",
            description = "Follow-up with ${leadName ?: number}",
            dueAt = followUpDate,
            priority = "High",
            status = "Pending",
            source = "Local",
            taskType = "Auto generated",
            leadName = leadName ?: "Unknown",
            assignedToUser = assignedUser,
            estimatedHours = " "

        )
        list.add(task)

        prefs.edit()
            .putString("task_list", gson.toJson(list))
            .apply()
        scheduleTaskWithWorkManager(context, task)

        Log.d("LocalTask", "Auto follow-up task created")
    }

    private fun scheduleTaskWithWorkManager(context: Context, task: LocalTask) {
        val dueAt = task.dueAt ?: return
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        val dueTimeMs = sdf.parse(dueAt)?.time ?: return

        val delayMs = dueTimeMs - System.currentTimeMillis()
        if (delayMs <= 0) return

        val safeId = (task.id and Int.MAX_VALUE) // avoid negative
        val work = androidx.work.OneTimeWorkRequestBuilder<com.technfest.technfestcrm.worker.TaskNotificationWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(
                androidx.work.workDataOf(
                    "taskId" to safeId,
                    "taskTitle" to task.title
                )
            )
            .build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork("task_notify_$safeId", androidx.work.ExistingWorkPolicy.REPLACE, work)
    }


    private fun resolveLeadIdForNumber(context: Context, number: String): Int {
        val normalized = normalizeToE164(context, number)
        if (normalized.isBlank()) return 0

        // 1) Try LeadCache map (best)
        val cachePrefs = context.getSharedPreferences("LeadCache", Context.MODE_PRIVATE)
        val json = cachePrefs.getString("lead_map", null)
        if (!json.isNullOrBlank()) {
            val type = object : com.google.gson.reflect.TypeToken<Map<String, com.technfest.technfestcrm.view.LeadsFragment.LeadCacheItem>>() {}.type
            val map: Map<String, com.technfest.technfestcrm.view.LeadsFragment.LeadCacheItem> =
                try { com.google.gson.Gson().fromJson(json, type) } catch (_: Exception) { emptyMap() }

            val hit = map[normalized]
            if (hit != null && hit.id > 0) return hit.id
            Log.d("FeedbackOverlay", "LeadCache keys sample=${map.keys.take(5)}")

        }

        // 2) Try LocalLeadManager (fallback)
        val local = com.technfest.technfestcrm.localdatamanager.LocalLeadManager
            .getLeads(context)
            .firstOrNull { normalizeToE164(context, it.mobile) == normalized }
        Log.d("FeedbackOverlay", "resolveLeadIdForNumber normalized=$normalized")
        Log.d("FeedbackOverlay", "LeadCache json size=${json?.length ?: 0}")


        return local?.id ?: 0
    }

}
