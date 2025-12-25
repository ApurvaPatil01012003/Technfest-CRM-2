package com.technfest.technfestcrm.view

import android.Manifest
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LeadResponseItem
import com.technfest.technfestcrm.repository.TaskRepository
import java.util.Calendar
import com.technfest.technfestcrm.databinding.FragmentLeadDetailBinding
import com.technfest.technfestcrm.repository.CampaignRepository
import com.technfest.technfestcrm.repository.UsersRepository
import com.technfest.technfestcrm.viewmodel.CampaignViewModelFactory
import com.technfest.technfestcrm.viewmodel.CampaignsViewModel
import com.technfest.technfestcrm.viewmodel.TaskViewModel
import com.technfest.technfestcrm.viewmodel.TaskViewModelFactory
import com.technfest.technfestcrm.viewmodel.UserViewModelFactory
import com.technfest.technfestcrm.viewmodel.UsersViewModel
import androidx.core.net.toUri
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.adapter.RecentFeedbackAdapter
import com.technfest.technfestcrm.localdatamanager.LocalLeadManager
import com.technfest.technfestcrm.model.CallFeedback
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.LocalTask
import java.util.Locale

import com.technfest.technfestcrm.adapter.RecentActivityAdapter
import com.technfest.technfestcrm.model.RecentActivityItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.technfest.technfestcrm.utils.SimSyncStore
import com.technfest.technfestcrm.model.RecentCallItem


class LeadDetailFragment : Fragment() {
    private val callPhonePermissionLauncher =
        registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { granted ->
            if (granted) {
                prepareLeadMetaForService()
                startCallWithSyncedSimChooser()
            } else {
                Toast.makeText(requireContext(), "Call permission denied", Toast.LENGTH_SHORT).show()
            }
        }

    private lateinit var binding: FragmentLeadDetailBinding
    private lateinit var telephonyManager: TelephonyManager
    private var workspaceId = 0
    private var token = ""

    private var leadNumber: String = ""
    private var leadName: String = ""
    private var leadId: Int = 0
    private var userId: Int = 0
    private var campaignId: Int = 0
    //private lateinit var feedbackAdapter: RecentFeedbackAdapter
    private lateinit var activityAdapter: RecentActivityAdapter



    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
       binding = FragmentLeadDetailBinding.inflate(inflater, container, false)
        token = arguments?.getString("token") ?: ""
        workspaceId = arguments?.getInt("workspaceId") ?: 0
        leadNumber = arguments?.getString("mobile") ?: ""
        leadName = arguments?.getString("name") ?: ""
        leadId = arguments?.getInt("leadId") ?: 0
        userId = arguments?.getInt("ownerUserId") ?: 0
        campaignId = arguments?.getInt("campaignId") ?: 0

        telephonyManager =
            requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        fetchCategories()
//        binding.btnCall.setOnClickListener {
//
//            // Overlay permission check (your existing)
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
//                !Settings.canDrawOverlays(requireContext())
//            ) {
//                val intent = Intent(
//                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
//                    Uri.parse("package:${requireContext().packageName}")
//                )
//                startActivity(intent)
//                Toast.makeText(requireContext(), "Allow overlay permission to show call popup", Toast.LENGTH_LONG).show()
//                return@setOnClickListener
//            }
//
//            // ✅ CALL_PHONE runtime permission check
//            val hasCallPermission =
//                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) ==
//                        android.content.pm.PackageManager.PERMISSION_GRANTED
//
//            if (!hasCallPermission) {
//                callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
//                return@setOnClickListener
//            }
//
//            // ✅ Now safe
//            prepareLeadMetaForService()
//            startCallWithSyncedSimChooser()
//        }


        binding.btnCall.setOnClickListener {

            // 1) Overlay permission (your existing)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                !Settings.canDrawOverlays(requireContext())
            ) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:${requireContext().packageName}")
                )
                startActivity(intent)
                Toast.makeText(requireContext(), "Allow overlay permission to show call popup", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // 2) CALL_PHONE permission
            val hasCallPermission =
                ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) ==
                        android.content.pm.PackageManager.PERMISSION_GRANTED

            if (!hasCallPermission) {
                callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
                return@setOnClickListener
            }

            // 3) ✅ Check synced SIMs BEFORE calling
            val synced = SimSyncStore.getSynced(requireContext())

            if (synced.isEmpty()) {
                Toast.makeText(requireContext(), "Please sync number first", Toast.LENGTH_LONG).show()
                return@setOnClickListener // ✅ STOP here, don't call
            }

            // 4) Prepare lead meta for service (leadId/name/campaign etc)
            prepareLeadMetaForService()

            val meta = requireContext().getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)

            // 5) 1 SIM synced -> call directly
            if (synced.size == 1) {
                val pick = synced[0]
                meta.edit()
                    .putInt("selectedSubId", pick.subId)
                    .putString("selectedSimNumber", pick.number ?: "")
                    .apply()

                placeCallUsingSimSubId(leadNumber, pick.subId)
                return@setOnClickListener
            }

            // 6) Multiple SIMs -> ask user
            val items = synced.map {
                val num = it.number ?: "Unknown"
                "${it.displayName} ($num)"
            }.toTypedArray()

            AlertDialog.Builder(requireContext())
                .setTitle("Call from which SIM?")
                .setItems(items) { _, which ->
                    val pick = synced[which]
                    meta.edit()
                        .putInt("selectedSubId", pick.subId)
                        .putString("selectedSimNumber", pick.number ?: "")
                        .apply()

                    placeCallUsingSimSubId(leadNumber, pick.subId)
                }
                .setCancelable(true)
                .show()
        }

        binding.btnMarkCompleted.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_mark_completed, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()

        }
        binding.btnCreateQuotation.setOnClickListener {

            val dialogView = layoutInflater.inflate(R.layout.alert_create_quotation, null)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()

            dialog.show()

            val radioOnline = dialogView.findViewById<RadioButton>(R.id.radioOnline)
            val radioManually = dialogView.findViewById<RadioButton>(R.id.radioManually)
            val layoutOnlinePay = dialogView.findViewById<LinearLayout>(R.id.layoutOnlinePay)
            val layoutManual = dialogView.findViewById<LinearLayout>(R.id.layoutManual)
            val btnSend = dialogView.findViewById<Button>(R.id.btnSend)
            val btnClose = dialogView.findViewById<ImageView>(R.id.btnClose)
            btnClose.setOnClickListener { dialog.dismiss() }

            layoutOnlinePay.visibility = View.GONE
            btnSend.visibility = View.GONE
            layoutManual.visibility = View.GONE


            radioOnline.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    layoutOnlinePay.visibility = View.VISIBLE
                    btnSend.visibility = View.VISIBLE
                } else {
                    layoutOnlinePay.visibility = View.GONE
                    btnSend.visibility = View.GONE
                }
            }
            radioManually.setOnCheckedChangeListener { _, isChecked ->
                if (isChecked) {
                    layoutManual.visibility = View.VISIBLE

                } else {
                    layoutManual.visibility = View.GONE
                }
            }

        }

        binding.btnReschedule.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_reschedule, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        binding.btnAddNote.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_add_note, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        val usersRepo = UsersRepository()
        val usersViewModel = ViewModelProvider(
            this,
            UserViewModelFactory(usersRepo)
        )[UsersViewModel::class.java]
        usersViewModel.fetchUsers(token)
        val taskViewModel = ViewModelProvider(
            this,
            TaskViewModelFactory(TaskRepository())
        )[TaskViewModel::class.java]



        binding.btnCreateTask.setOnClickListener {

            val dialogView = layoutInflater.inflate(R.layout.alert_lead_create_lead_task, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()

            // Views
            val leadNameText = dialogView.findViewById<TextView>(R.id.txtLeadName)
            val edtTitle = dialogView.findViewById<EditText>(R.id.edtTitle)
            val edtDescription = dialogView.findViewById<EditText>(R.id.edtDescription)
            val edtDueDate = dialogView.findViewById<EditText>(R.id.edtDueDate)
            val edtDueTime = dialogView.findViewById<EditText>(R.id.edtDueTime)
            val edtStatus = dialogView.findViewById<EditText>(R.id.edtStatus)
            val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
            val spinnerAssignToUser = dialogView.findViewById<Spinner>(R.id.spnAssignToUser)
            val spinnerTaskType = dialogView.findViewById<Spinner>(R.id.spinnerTaskType)
            val edtEstimateHours = dialogView.findViewById<EditText>(R.id.edtEstimateHours)
            val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnCreateTask)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
            val ivClose = dialogView.findViewById<ImageView>(R.id.ivClose)

            leadNameText.text = arguments?.getString("name") ?: "N/A"

            // --- Set Static Spinner Values ---
            val taskTypes = listOf("general")
            spinnerTaskType.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                taskTypes
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            val priorities = listOf("High", "Low", "Normal")
            spinnerPriority.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                priorities
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            val assignUsers = listOf("Sagar", "Pratik")
            spinnerAssignToUser.adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                assignUsers
            ).apply { setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            // --- Date & Time Pickers ---
            edtDueDate.setOnClickListener {
                val c = Calendar.getInstance()
                DatePickerDialog(
                    requireContext(),
                    { _, year, month, day ->
                        edtDueDate.setText(String.format("%04d-%02d-%02d", year, month + 1, day))
                    },
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH)
                ).show()
            }

            edtDueTime.setOnClickListener {
                val c = Calendar.getInstance()
                TimePickerDialog(
                    requireContext(),
                    { _, hour, min ->
                        edtDueTime.setText(String.format("%02d:%02d:00", hour, min))
                    },
                    c.get(Calendar.HOUR_OF_DAY),
                    c.get(Calendar.MINUTE),
                    true
                ).show()
            }

            btnSave.setOnClickListener {
                val title = edtTitle.text.toString().trim()
                val description = edtDescription.text.toString().trim()
                val dueDate = edtDueDate.text.toString().trim()
                val dueTime = edtDueTime.text.toString().trim()
                val status = edtStatus.text.toString().trim()
                val priority = spinnerPriority.selectedItem.toString()

                val finalDueAt = if (dueDate.isNotEmpty() && dueTime.isNotEmpty()) "$dueDate $dueTime" else ""

                val localTask = LocalTask(
                    id = System.currentTimeMillis().toInt(),
                    title = edtTitle.text.toString().trim(),
                    description = edtDescription.text.toString().trim(),
                    dueAt = finalDueAt,
                    status = edtStatus.text.toString().ifEmpty { "Pending" },
                    priority = spinnerPriority.selectedItem.toString(),
                    taskType = spinnerTaskType.selectedItem.toString(),
                    source = "Manual",
                    leadName = leadNameText.text.toString(),
                    assignedToUser = spinnerAssignToUser.selectedItem.toString(),
                    estimatedHours = edtEstimateHours.text.toString().ifEmpty { "0" }
                )
                saveTaskLocally(requireContext(), localTask)

                scheduleTaskNotification(requireContext(), localTask)

                Toast.makeText(requireContext(), "Task saved locally", Toast.LENGTH_LONG).show()
                dialog.dismiss()
            }

            btnCancel.setOnClickListener { dialog.dismiss() }
            ivClose.setOnClickListener { dialog.dismiss() }

            dialog.show()
        }



        val args = arguments
        binding.txtName.text = args.getSafeString("name")
        binding.txtCompanyName.text = args.getSafeString("company")
        binding.txtLeadLocation.text = args.getSafeString("location")
        binding.txtLeadNumber.text = args.getSafeString("mobile")
        binding.txtLeadEmail.text = args.getSafeString("email")
        binding.txtLeadStatus.text = args.getSafeString("status")
        binding.txtSource.text = args.getSafeString("source")
        binding.txtStage.text = args.getSafeString("stage")
        binding.txtPriority.text = args.getSafeString("priority")
        binding.txtCampaignName.text = args.getSafeString("campaignName")
        binding.txtLeadRequirement.text = args.getSafeString("leadRequirement")

        val leadFromLocal: LeadRequest? = if (leadNumber.isNotBlank()) {
            LocalLeadManager.getLeads(requireContext()).find { it.mobile == leadNumber }
        } else null


        val followUpDate = leadFromLocal?.nextFollowupAt ?: args.getSafeString("nextFollowupAt")
        binding.txtScheduledFollowUp.text = followUpDate
        binding.txtFllowUpStatus.text =
            if (!followUpDate.isNullOrBlank() && followUpDate != "N/A") "Scheduled" else "Not Scheduled"


        binding.txtOwnerName.text = args.getSafeString("ownerName")
        binding.txtTeam.text = args.getSafeString("teamName")
        binding.txtNote.text = args.getSafeString("note")

        val leadName = args?.getString("name") ?: "N/A"
        binding.btnZoom.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_zoom_task, null)
            AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
                .show()
        }

        binding.rvRecentActivity.layoutManager = LinearLayoutManager(requireContext())
        activityAdapter = RecentActivityAdapter(emptyList())
        binding.rvRecentActivity.adapter = activityAdapter

        refreshRecentActivity()


        return binding.root
    }

    private fun saveTaskLocally(context: Context, task: LocalTask) {
        val prefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE)
        val json = prefs.getString("task_list", null)

        // Correct type for LocalTask list
        val type = object : com.google.gson.reflect.TypeToken<MutableList<LocalTask>>() {}.type

        val tasks: MutableList<LocalTask> = if (json != null) {
            com.google.gson.Gson().fromJson(json, type)
        } else mutableListOf()

        tasks.add(task)
        prefs.edit().putString("task_list", com.google.gson.Gson().toJson(tasks)).apply()
    }


    companion object {
        fun newInstance(
            lead: LeadResponseItem,
            token: String,
            workspaceId: Int,
            campaignCategoryId: Int,
            ownerUserId: Int,
            campaignId: Int
        ): LeadDetailFragment {

            val fragment = LeadDetailFragment()
            val args = Bundle().apply {
                putInt("leadId", lead.id)
                putString("name", lead.fullName ?: "N/A")
                putString("mobile", lead.mobile ?: "N/A")
                putString("status", lead.status ?: "N/A")
                putString("stage", (lead.stage ?: "N/A").toString())
                putString("priority", lead.priority ?: "N/A")
                putString("leadRequirement", (lead.leadRequirement ?: "N/A").toString())
                putString("campaignName", lead.campaignName ?: "N/A")
                putString("location", (lead.location ?: "N/A").toString())
                putString("email", lead.email ?: "N/A")
                putString("company", (lead.company ?: "N/A").toString())
                putString("source", (lead.source ?: "N/A").toString())
                putString("nextFollowupAt", lead.nextFollowupAt ?: "N/A")
                putString("ownerName", (lead.ownerName ?: "N/A").toString())
                putString("teamName", (lead.teamName ?: "N/A").toString())

            }

            fragment.arguments = args
            return fragment
        }

        fun newInstanceById(
            leadId: Int,
            token: String,
            workspaceId: Int
        ): LeadDetailFragment {
            return LeadDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("leadId", leadId)
                    putString("token", token)
                    putInt("workspaceId", workspaceId)
                }
            }
        }

    }

    private fun makeCall() {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = "tel:$leadNumber".toUri()
        startActivity(intent)
    }

    private var categoryMap: Map<Int, String> = emptyMap()

    private fun fetchCategories() {
        val campaignRepo = CampaignRepository()
        val campaignVM = ViewModelProvider(
            this,
            CampaignViewModelFactory(campaignRepo)
        )[CampaignsViewModel::class.java]

        campaignVM.fetchCategories(token)
        campaignVM.categoriesLiveData.observe(viewLifecycleOwner) { categories ->
            categoryMap = categories.associateBy({ it.id }, { it.code })
        }
    }
    private fun prepareLeadMetaForService() {
        val prefs = requireContext().getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)

        val e164 = normalizeToE164(requireContext(), leadNumber)

        prefs.edit()
            .putInt("leadId", leadId)
            .putString("leadName", leadName)
            .putInt("campaignId", campaignId)
            .putInt("campaignCategoryId", arguments?.getInt("campaignCategoryId") ?: 0)
            .putString("customerNumber", leadNumber)
            .putString("customerNumberE164", e164)
            .apply()
    }
    private fun normalizeToE164(context: Context, raw: String): String {
        if (raw.isBlank()) return ""
        return try {
            val cleaned = raw.replace("[^0-9+]".toRegex(), "")

            val phoneUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance()

            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val region = (tm.networkCountryIso ?: tm.simCountryIso ?: "")
                .uppercase()
                .takeIf { it.isNotBlank() }

            val proto = if (cleaned.startsWith("+")) {
                phoneUtil.parse(cleaned, null)
            } else {
                phoneUtil.parse(cleaned, region ?: "US") // fallback if region not available
            }

            phoneUtil.format(proto, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            ""
        }
    }

    private fun getFeedbackForLead(leadId: Int): List<CallFeedback> {

        val prefs = requireContext()
            .getSharedPreferences("CallFeedbackStore", Context.MODE_PRIVATE)

        val json = prefs.getString("feedback_list", null) ?: return emptyList()

        val type = object : TypeToken<List<CallFeedback>>() {}.type

        val list = Gson().fromJson<List<CallFeedback>>(json, type) ?: emptyList()

        val currentLeadE164 = normalizeToE164(requireContext(), leadNumber)

        return list
            .filter { fb ->
                fb.leadId == leadId ||
                        (currentLeadE164.isNotBlank() &&
                                normalizeToE164(requireContext(), fb.number ?: "") == currentLeadE164)
            }
            .sortedByDescending { it.timestamp }
    }


    private fun Bundle?.getSafeString(key: String): String {
        val value = this?.getString(key)
        return if (value.isNullOrBlank()) "N/A" else value
    }

    private fun startCallWithSyncedSimChooser() {

        val hasCallPermission =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }

        val synced = SimSyncStore.getSynced(requireContext())

        if (synced.isEmpty()) {
            Toast.makeText(requireContext(), "Please sync number first", Toast.LENGTH_LONG).show()
            return
        }

        val meta = requireContext().getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)

        if (synced.size == 1) {
            meta.edit()
                .putInt("selectedSubId", synced[0].subId)
                .putString("selectedSimNumber", synced[0].number ?: "")
                .apply()

            placeCallUsingSimSubId(leadNumber, synced[0].subId)
            return
        }

        val items = synced.map {
            val num = it.number ?: "Unknown"
            "${it.displayName} ($num)"
        }.toTypedArray()

        AlertDialog.Builder(requireContext())
            .setTitle("Call from which SIM?")
            .setItems(items) { _, which ->
                val pick = synced[which]
                meta.edit()
                    .putInt("selectedSubId", pick.subId)
                    .putString("selectedSimNumber", pick.number ?: "")
                    .apply()

                placeCallUsingSimSubId(leadNumber, pick.subId)
            }
            .setCancelable(true)
            .show()
    }


    private fun scheduleTaskNotification(context: Context, task: LocalTask) {
        val dueAt = task.dueAt ?: return

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        sdf.isLenient = false

        val dueMs = try { sdf.parse(dueAt)?.time } catch (e: Exception) { null } ?: return

        val delay = dueMs - System.currentTimeMillis()
        if (delay <= 0) return

        val safeId = (task.id and Int.MAX_VALUE)

        val work = androidx.work.OneTimeWorkRequestBuilder<com.technfest.technfestcrm.worker.TaskNotificationWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(
                androidx.work.workDataOf(
                    "taskId" to safeId,
                    "taskTitle" to (task.title ?: "Task Reminder")
                )
            )
            .build()

        androidx.work.WorkManager.getInstance(context)
            .enqueueUniqueWork("task_notify_$safeId", androidx.work.ExistingWorkPolicy.REPLACE, work)
    }
    private fun placeCallUsingSimSubId(phone: String, subId: Int) {

        val hasCallPermission =
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CALL_PHONE) ==
                    android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!hasCallPermission) {
            callPhonePermissionLauncher.launch(Manifest.permission.CALL_PHONE)
            return
        }

        val telecom = requireContext().getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val uri = Uri.fromParts("tel", phone, null)

        val handle = resolvePhoneAccountHandleForSubId(requireContext(), subId)

        if (handle == null) {
            Toast.makeText(requireContext(), "SIM mapping failed, calling default SIM", Toast.LENGTH_SHORT).show()
            startActivity(Intent(Intent.ACTION_CALL, uri))
            return
        }

        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, handle)
        }

        telecom.placeCall(uri, extras)
    }


    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    private fun resolvePhoneAccountHandleForSubId(ctx: Context, subId: Int): PhoneAccountHandle? {
        val telecom = ctx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val subMgr = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val targetInfo = subMgr.activeSubscriptionInfoList?.firstOrNull { it.subscriptionId == subId }

        val handles = telecom.callCapablePhoneAccounts ?: return null

        // best-effort matching by label/slot
        for (h in handles) {
            val acc = telecom.getPhoneAccount(h) ?: continue
            val label = acc.label?.toString()?.lowercase() ?: ""

            // If we have target sim info, match by displayName / slot wordings
            if (targetInfo != null) {
                val disp = (targetInfo.displayName?.toString() ?: "").lowercase()
                val slot = targetInfo.simSlotIndex + 1

                if (disp.isNotBlank() && label.contains(disp)) return h
                if (label.contains("sim$slot") || label.contains("sim $slot")) return h
            }
        }

        // fallback: return first SIM account
        return handles.firstOrNull()
    }

    private fun refreshRecentActivity() {

        // 1) Feedback -> RecentActivityItem.FeedbackItem
        val feedbackItems: List<RecentActivityItem> = getFeedbackForLead(leadId).map { fb ->
            RecentActivityItem.FeedbackItem(feedback = fb, timestamp = fb.timestamp)
        }

        // 2) Calls -> RecentActivityItem.CallItem
        // NOTE: Calls already saved only for synced SIM in service (because you return early if not synced)
        val callItems: List<RecentActivityItem> = getCallsForLead(leadId).map { c ->
            RecentActivityItem.CallItem(
                leadId = c.leadId,
                leadName = c.leadName,
                leadNumber = c.number,
                callStatusLabel = c.statusLabel,
                startIso = c.startIso,
                endIso = c.endIso,
                durationSec = c.durationSec,
                timestamp = c.timestampMs
            )
        }

        // 3) Merge + sort by timestamp
        val merged: List<RecentActivityItem> = (callItems + feedbackItems).sortedByDescending { item ->
            when (item) {
                is RecentActivityItem.CallItem -> item.timestamp
                is RecentActivityItem.FeedbackItem -> item.timestamp   // OR item.feedback.timestamp (both same)
            }
        }

        // 4) Update recycler
        activityAdapter.update(merged)

        // 5) Show/hide recycler
        binding.rvRecentActivity.visibility = if (merged.isNotEmpty()) View.VISIBLE else View.GONE
        Log.d("LeadDetail", "Loading feedback for leadId=$leadId total=${getFeedbackForLead(leadId).size}")
        Log.d("LeadDetail", "Calls for leadId=$leadId totalCalls=${getCallsForLead(leadId).size}")

    }

    private fun getCallsForLead(leadId: Int): List<RecentCallItem> {
        val prefs = requireContext().getSharedPreferences("RecentCallsStore", Context.MODE_PRIVATE)
        val json = prefs.getString("recent_calls", null) ?: return emptyList()

        val type = object : TypeToken<List<RecentCallItem>>() {}.type

        val currentLeadE164 = normalizeToE164(requireContext(), leadNumber)

        return try {
            Gson().fromJson<List<RecentCallItem>>(json, type)
                .filter { c ->
                    c.leadId == leadId ||
                            (currentLeadE164.isNotBlank() &&
                                    normalizeToE164(requireContext(), c.number) == currentLeadE164)
                }
                .sortedByDescending { it.timestampMs }
        } catch (e: Exception) {
            emptyList()
        }
    }


    private val activityUpdateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refreshRecentActivity()
        }
    }
    override fun onStart() {
        super.onStart()

        val filter = IntentFilter("com.technfest.technfestcrm.CALL_ACTIVITY_UPDATED")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(
                activityUpdateReceiver,
                filter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            ContextCompat.registerReceiver(
                requireContext(),
                activityUpdateReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }


    override fun onStop() {
        super.onStop()
        try { requireContext().unregisterReceiver(activityUpdateReceiver) } catch (_: Exception) {}
    }



}