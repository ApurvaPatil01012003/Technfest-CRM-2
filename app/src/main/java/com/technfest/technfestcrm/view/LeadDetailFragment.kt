package com.technfest.technfestcrm.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LeadResponseItem
import com.technfest.technfestcrm.model.TaskRequest
import com.technfest.technfestcrm.utils.MyCallListener
import com.technfest.technfestcrm.repository.LeadRepository
import com.technfest.technfestcrm.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Calendar
import com.technfest.technfestcrm.databinding.FragmentLeadDetailBinding
import com.technfest.technfestcrm.repository.CampaignRepository
import com.technfest.technfestcrm.repository.UsersRepository
import com.technfest.technfestcrm.viewmodel.CallLogViewModel
import com.technfest.technfestcrm.viewmodel.CampaignViewModelFactory
import com.technfest.technfestcrm.viewmodel.CampaignsViewModel
import com.technfest.technfestcrm.viewmodel.TaskViewModel
import com.technfest.technfestcrm.viewmodel.TaskViewModelFactory
import com.technfest.technfestcrm.viewmodel.UserViewModelFactory
import com.technfest.technfestcrm.viewmodel.UsersViewModel

class LeadDetailFragment : Fragment() {
    private lateinit var binding: FragmentLeadDetailBinding
    private lateinit var telephonyManager: TelephonyManager
    private var callListener: MyCallListener? = null


    private var workspaceId = 0
    private var token = ""
    private var leadNumber: String = ""
    private var leadName: String = ""
    private var leadId: Int = 0
    private var userId: Int = 0
    private var campaignId: Int = 0


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentLeadDetailBinding.inflate(inflater, container, false)
        val toolbar =
            view?.findViewById<MaterialToolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }


        token = arguments?.getString("token") ?: ""
        workspaceId = arguments?.getInt("workspaceId") ?: 0
        leadNumber = arguments?.getString("mobile") ?: ""
        leadName = arguments?.getString("name") ?: ""
        leadId = arguments?.getInt("leadId") ?: 0
        userId = arguments?.getInt("ownerUserId") ?: 0
        campaignId = arguments?.getInt("campaignId") ?: 0


        toolbar?.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        telephonyManager =
            requireContext().getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        fetchCategories()
        binding.btnCall.setOnClickListener {
            prepareLeadMetaForService()
            registerCallListener()
            makeCall()
        }

        binding.btnMarkCompleted.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_mark_completed, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()

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

            val leadNameStr = arguments?.getString("name") ?: "N/A"
            val leadId = arguments?.getInt("leadId") ?: 0


            // Views
            val leadNameText = dialogView.findViewById<TextView>(R.id.txtLeadName)
            val edtTitle = dialogView.findViewById<EditText>(R.id.edtTitle)
            val edtDescription = dialogView.findViewById<EditText>(R.id.edtDescription)
            // val edtProjectName = dialogView.findViewById<EditText>(R.id.edtProjectName)

            val edtDueDate = dialogView.findViewById<EditText>(R.id.edtDueDate)
            val edtDueTime = dialogView.findViewById<EditText>(R.id.edtDueTime)
            val edtStatus = dialogView.findViewById<EditText>(R.id.edtStatus)
            val spinnerPriority = dialogView.findViewById<Spinner>(R.id.spinnerPriority)
            val spinnerAssignToUser = dialogView.findViewById<Spinner>(R.id.spnAssignToUser)
            val edtEstimateHours = dialogView.findViewById<EditText>(R.id.edtEstimateHours)
            val spinnerTaskType = dialogView.findViewById<Spinner>(R.id.spinnerTaskType)
            val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnCreateTask)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
            val ivClose = dialogView.findViewById<ImageView>(R.id.ivClose)
            leadNameText.text = leadNameStr

            taskViewModel.fetchTaskType(token, workspaceId)

            taskViewModel.taskTypeResult.observe(requireActivity()) { types ->
                if (!types.isNullOrEmpty()) {
                    val adapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        types
                    )
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerTaskType.adapter = adapter
                }
            }


            lifecycleScope.launch {
                try {
                    val repo = LeadRepository()
                    val response = repo.getLeadMeta(token, workspaceId, "priority")

                    if (response.isSuccessful && response.body() != null) {
                        val priorityItems = response.body()!!

                        val priorityNames = priorityItems.map { it.name }

                        val adapter = ArrayAdapter(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            priorityNames
                        )
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                        spinnerPriority.adapter = adapter
                    } else {
                        Toast.makeText(
                            requireContext(),
                            "Failed to load priority!",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT)
                        .show()
                }
            }




            usersViewModel.usersList.observe(viewLifecycleOwner) { users ->
                if (users != null) {
                    val assignUserList = users.map { it.full_name }
                    val assignAdapter = ArrayAdapter(
                        requireContext(),
                        android.R.layout.simple_spinner_item,
                        assignUserList
                    )
                    assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    spinnerAssignToUser.adapter = assignAdapter

                    val userIdMap = users.associate { it.full_name to it.id }


                    val assignedUserName = spinnerAssignToUser.selectedItem.toString()
                    val assignedUserId = userIdMap[assignedUserName] ?: 0
                }
            }


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

            // Time Picker
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

            dialog.show()


            btnSave.setOnClickListener {
                val leadNameStr = arguments?.getString("name") ?: "N/A"

                val title = edtTitle.text.toString().trim()
                val description = edtDescription.text.toString().trim()
                val dueDate = edtDueDate.text.toString().trim()
                val dueTime = edtDueTime.text.toString().trim()
                //  val projectName = edtProjectName.text.toString().trim()

                val status = edtStatus.text.toString().trim()
                val estimateHoursStr = edtEstimateHours.text.toString().trim()
                val priority = spinnerPriority.selectedItem.toString()
                val assignedUserName = spinnerAssignToUser.selectedItem.toString()
                val estimateHours: Int? = if (estimateHoursStr.isNotEmpty()) {
                    estimateHoursStr.toIntOrNull()
                } else null
                val finalDueDate = if (dueDate.isNotEmpty())
                    "${dueDate}" else null

                val finalDueAt = if (dueDate.isNotEmpty() && dueTime.isNotEmpty())
                    "$dueDate $dueTime" else null

                val taskRequest = TaskRequest(
                    workspaceId = arguments?.getInt("workspaceId") ?: 0,
                    title = title,
                    description = description,
                    taskType = "general",
                    projectId = 2,
                    assignedToEmployeeId = 5,
                    estimatedHours = estimateHours,
                    dueDate = finalDueDate,
                    dueAt = finalDueAt,
                    projectName = "Client Onboarding Automation",
                    status = status,
                    priority = priority,
                    assignedUserName = assignedUserName,
                    leadName = leadNameStr,
                    leadId = leadId
                )
                Log.d("WorkspaceIDDDD", workspaceId.toString())

                lifecycleScope.launch {
                    try {
                        val passedToken = token
                        Log.d("Toekns", passedToken)
                        val passedWorkspaceId = workspaceId

                        val repo = TaskRepository()
                        val response = repo.createTask(passedToken, taskRequest)


                        if (response.isSuccessful) {
                            Toast.makeText(
                                requireContext(),
                                "Task Created Successfully",
                                Toast.LENGTH_LONG
                            )
                                .show()
                            dialog.dismiss()
                        } else {
                            Toast.makeText(
                                requireContext(),
                                "Failed: ${response.errorBody()?.string()}",
                                Toast.LENGTH_LONG
                            ).show()
                        }

                    } catch (e: Exception) {
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG)
                            .show()
                    }
                }


            }
            btnCancel.setOnClickListener {
                dialog.dismiss()
            }
            ivClose.setOnClickListener {
                dialog.dismiss()
            }
        }


        val args = arguments
        binding.txtName.text = args?.getString("name")
        binding.txtCompanyName.text = args?.getString("company")
        binding.txtLeadLocation.text = args?.getString("location")
        binding.txtLeadNumber.text = args?.getString("mobile")
        binding.txtLeadEmail.text = args?.getString("email")
        binding.txtLeadStatus.text = args?.getString("status")
        binding.txtSource.text = args?.getString("source")
        binding.txtStage.text = args?.getString("stage")
        binding.txtPriority.text = args?.getString("priority")
        binding.txtCampaignName.text = args?.getString("campaignName")
        binding.txtLeadRequirement.text = args?.getString("leadRequirement")
        binding.txtScheduledFollowUp.text = args?.getString("nextFollowupAt")
        binding.txtFllowUpStatus.text = args?.getString("followupStatus")
        binding.txtOwnerName.text = args?.getString("ownerName")
        binding.txtTeam.text = args?.getString("teamName")
        binding.txtNote.text = args?.getString("note")
        binding.toolbarTitle.text = args?.getString("name") ?: "N/A"
        binding.toolbarSubtitle.text = args?.getString("location") ?: "N/A"
        val leadName = args?.getString("name") ?: "N/A"
        binding.userInitialCircle.text = getInitials(leadName)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = binding.toolbar
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
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

                putString("token", token)
                putInt("workspaceId", workspaceId)
                putInt("leadId", lead.id)
                putInt("campaignCategoryId", campaignCategoryId)
                putInt("ownerUserId", (lead.ownerUserId ?: 0))
                putInt("campaignId", lead.campaignId ?: 0)



                putString("name", lead.fullName ?: "N/A")
                putString("location", (lead.location ?: "N/A").toString())
                putString("mobile", lead.mobile ?: "N/A")
                putString("email", lead.email ?: "N/A")
                putString("company", (lead.company ?: "N/A").toString())
                putString("source", (lead.source ?: "N/A").toString())
                putString("status", lead.status ?: "N/A")
                putString("stage", (lead.stage ?: "N/A").toString())
                putString("priority", lead.priority ?: "N/A")
                putString("campaignName", lead.campaignName ?: "N/A")
                putString("leadRequirement", (lead.leadRequirement ?: "N/A").toString())
                putString("nextFollowupAt", (lead.nextFollowupAt ?: "N/A").toString())
                putString("followupStatus", (lead.followupStatus ?: "N/A").toString())
                putString("ownerName", (lead.ownerName ?: "N/A").toString())
                putString("teamName", (lead.teamName ?: "N/A").toString())
                putString("note", lead.notes ?: "N/A")
            }

            fragment.arguments = args
            return fragment
        }

    }

    private fun getInitials(fullName: String): String {
        if (fullName.isBlank()) return "U"
        return fullName.trim()
            .split("\\s+".toRegex())
            .filter { it.isNotEmpty() }
            .map { it.first().uppercaseChar() }
            .joinToString("")
    }

    //    private fun handleCallFinished(number: String) {
//        Log.d("CALL_FINISHED", "Call ended with $number")
//        sendCallLogToAPI(number)
//    }
//
//    private fun sendCallLogToAPI(number: String) {
//        lifecycleScope.launch {
//            try {
//                val request = CallLogRequest(
//                    callStatus = "completed",
//                    campaignCode = "",
//                    campaignId = 0,
//                    customerName = arguments?.getString("name") ?: "",
//                    customerNumber = number,
//                    direction = "outgoing",
//                    durationSeconds = 0,
//                    endTime = "",
//                    externalEventId = "",
//                    leadId = arguments?.getInt("leadId") ?: 0,
//                    notes = "",
//                    recordingUrl = "",
//                    source = "mobile",
//                    startTime = "",
//                    userId = 0,
//                    workspaceId = arguments?.getInt("workspaceId") ?: 0
//                )
//
//                val token = arguments?.getString("token") ?: ""
//
//                val response = RetrofitInstance.apiInterface.sendCallLog(
//                    "Bearer $token",
//                    request
//                )
//
//                if (response.isSuccessful) {
//                    val b = response.body()
//                    Log.d("CALL_LOG", "Uploaded Successfully : $b")
//                } else {
//                    Log.e("CALL_LOG", response.errorBody()?.string() ?: "Error")
//                }
//
//            } catch (e: Exception) {
//                Log.e("CALL_LOG", "Exception: ${e.message}")
//            }
//        }
//    }
    private fun registerCallListener() {
        if (callListener == null) {
            callListener = MyCallListener(
                requireContext(),
                leadNumber
            ) { number, duration, direction, callStatus, startTime, endTime ->
                // handleCallFinished(number, duration, direction, callStatus, startTime, endTime)
            }

            telephonyManager.listen(
                callListener,
                PhoneStateListener.LISTEN_CALL_STATE
            )
        }
    }


    private fun makeCall() {
        val intent = Intent(Intent.ACTION_CALL)
        intent.data = Uri.parse("tel:$leadNumber")
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


//    private fun handleCallFinished(
//        number: String,
//        durationSeconds: Long,
//        direction: String,
//        callStatus: String,
//        startTime: String,
//        endTime: String
//    ) {
//        val campaignCategoryId = arguments?.getInt("campaignCategoryId") ?: 0
//        val campaignCode = categoryMap[campaignCategoryId] ?: ""
//
//        val callRequest = CallLogRequest(
//            workspaceId = workspaceId,
//            userId = userId,
//            leadId = leadId.takeIf { it > 0 },
//            campaignId = campaignId,
//            campaignCode = campaignCode,
//            customerNumber = number,
//            customerName = leadName,
//            direction = direction,
//            callStatus = callStatus,
//            startTime = startTime,
//            endTime = endTime,
//            durationSeconds = durationSeconds.toInt(),
//            recordingUrl = "",
//            source = "mobile",
//            notes = "",
//            externalEventId = ""
//        )
//
//        val callLogRepo = CallLogRepository()
//        lifecycleScope.launch {
//            try {
//                val webhookSecret = "super-secret-webhook-key"
//                val response = callLogRepo.sendCallLog(webhookSecret, callRequest)
//
//                if (response.isSuccessful) {
//                    val callLogResponse = response.body()!!
//                    val callLogId = callLogResponse.id
//                    Log.d("CALL_LOG", "Uploaded successfully $callLogId")
//                    saveCallLogId(callLogId)
//                } else {
//                    Log.e("CALL_LOG", response.errorBody()?.string() ?: "Unknown error")
//                }
//            } catch (e: Exception) {
//                Log.e("CALL_LOG", "Exception: ${e.message}")
//            }
//        }
//    }


    override fun onDestroyView() {
        super.onDestroyView()

        callListener?.let {
            telephonyManager.listen(it, PhoneStateListener.LISTEN_NONE)
        }
        callListener = null
    }

    private fun prepareLeadMetaForService() {
        val prefs =
            requireContext().getSharedPreferences("ActiveCallLeadMeta", Context.MODE_PRIVATE)
        prefs.edit()
            .putInt("leadId", leadId)
            .putString("leadName", leadName)
            .putInt("campaignId", campaignId)
            .putInt("campaignCategoryId", arguments?.getInt("campaignCategoryId") ?: 0)
            .putString("customerNumber", leadNumber)
            .apply()
    }


}
