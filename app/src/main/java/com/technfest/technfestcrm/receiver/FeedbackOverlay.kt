package com.technfest.technfestcrm.receiver

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.CallFeedback

object FeedbackOverlay {

    private var view: View? = null
    private var windowManager: WindowManager? = null
    fun show(context: Context) {
        if (view != null) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(context)) {
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
            WindowManager.LayoutParams.WRAP_CONTENT,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                WindowManager.LayoutParams.TYPE_PHONE,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        )

        params.gravity = Gravity.CENTER

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

        val etLeadName = view!!.findViewById<EditText>(R.id.etLeadName)
        val etNumber = view!!.findViewById<EditText>(R.id.etNumber)
        val ivClose = view!!.findViewById<ImageView>(R.id.ivClose)
        val btnSave = view!!.findViewById<MaterialButton>(R.id.btnSaveFeedback)

        val prefs = context.getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)

        val savedNumber = prefs.getString("customerNumber", "") ?: ""
        val leadName = prefs.getString("leadName", "")

        val currentNumber = CallStateForegroundService.currentNumberGlobal ?: ""

// Only prefill if this is a CRM call
        etLeadName.setText(if (currentNumber == savedNumber) leadName else "")
        etNumber.setText(currentNumber.ifEmpty { savedNumber })


        ivClose.setOnClickListener {
            hide()
        }

        btnSave.setOnClickListener {
            saveFeedback(context)
            hide()
        }
    }

    private fun saveFeedback(context: Context) {

        val etLeadName = view!!.findViewById<EditText>(R.id.etLeadName)
        val etNumber = view!!.findViewById<EditText>(R.id.etNumber)
        val etNote = view!!.findViewById<EditText>(R.id.etNote)
        val ratingBar = view!!.findViewById<android.widget.RatingBar>(R.id.ratingBar)
        val rgStatus = view!!.findViewById<android.widget.RadioGroup>(R.id.rgStatus)
        val edtFollowUp = view!!.findViewById<EditText>(R.id.edtFollowUpdate)
        val spinner =
            view!!.findViewById<android.widget.Spinner>(R.id.spnLastCallReceivedUser)

        val selectedId = rgStatus.checkedRadioButtonId
        if (selectedId == -1) {
            Toast.makeText(context, "Please select call status", Toast.LENGTH_SHORT).show()
            return
        }

        val status =
            view!!.findViewById<android.widget.RadioButton>(selectedId).text.toString()

        val receivedBy = spinner.selectedItem?.toString()

        val feedback = CallFeedback(
            number = etNumber.text.toString(),
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
            .edit().clear().apply()

        Toast.makeText(context, "Feedback Saved", Toast.LENGTH_SHORT).show()
    }

    private fun saveFeedbackToPrefs(context: Context, feedback: CallFeedback) {

        val prefs = context.getSharedPreferences("CallFeedbackStore", Context.MODE_PRIVATE)
        val gson = com.google.gson.Gson()

        val oldJson = prefs.getString("feedback_list", null)
        val type = object :
            com.google.gson.reflect.TypeToken<MutableList<CallFeedback>>() {}.type

        val list: MutableList<CallFeedback> =
            if (oldJson != null)
                gson.fromJson(oldJson, type)
            else
                mutableListOf()

        list.add(feedback)

        prefs.edit()
            .putString("feedback_list", gson.toJson(list))
            .apply()
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

        val spinner =
            view!!.findViewById<android.widget.Spinner>(R.id.spnLastCallReceivedUser)

        val prefs = context.getSharedPreferences("CallFeedbackStore", Context.MODE_PRIVATE)
        val json = prefs.getString("feedback_list", null)

        val names = if (!json.isNullOrEmpty()) {

            val type = object :
                com.google.gson.reflect.TypeToken<List<CallFeedback>>() {}.type

            val list: List<CallFeedback> =
                com.google.gson.Gson().fromJson(json, type)

            list.sortedByDescending { it.timestamp }
                .mapNotNull { it.receivedBy ?: it.leadName }
                .distinct()

        } else emptyList()

        val finalList =
            if (names.isEmpty()) listOf("Self")
            else listOf("Self") + names

        spinner.adapter = android.widget.ArrayAdapter(
            context,
            android.R.layout.simple_spinner_dropdown_item,
            finalList
        )

        spinner.setSelection(0)
    }


    private fun setupFollowUpPicker(context: Context) {

        val edtFollowUp = view!!.findViewById<EditText>(R.id.edtFollowUpdate)
        val calendar = java.util.Calendar.getInstance()

        edtFollowUp.apply {
            isFocusable = false
            isClickable = true
            isLongClickable = false
            inputType = android.text.InputType.TYPE_NULL
        }

        val dialogContext = ContextThemeWrapper(
            context,
            R.style.Theme_TechnfestCRM
        )

        edtFollowUp.setOnClickListener {

            val datePicker = android.app.DatePickerDialog(
                dialogContext,
                { _, year, month, dayOfMonth ->

                    calendar.set(year, month, dayOfMonth)

                    val timePicker = android.app.TimePickerDialog(
                        dialogContext,
                        { _, hour, minute ->

                            calendar.set(java.util.Calendar.HOUR_OF_DAY, hour)
                            calendar.set(java.util.Calendar.MINUTE, minute)

                            val format = java.text.SimpleDateFormat(
                                "dd/MM/yyyy hh:mm a",
                                java.util.Locale.getDefault()
                            )

                            edtFollowUp.setText(format.format(calendar.time))

                        },
                        calendar.get(java.util.Calendar.HOUR_OF_DAY),
                        calendar.get(java.util.Calendar.MINUTE),
                        false
                    )

                    timePicker.show()
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )

            datePicker.datePicker.minDate = System.currentTimeMillis()
            datePicker.show()
        }
    }




}
