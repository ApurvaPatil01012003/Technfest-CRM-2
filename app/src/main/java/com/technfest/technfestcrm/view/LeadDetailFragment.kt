package com.technfest.technfestcrm.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
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
import androidx.lifecycle.lifecycleScope
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LeadResponseItem
import com.technfest.technfestcrm.model.TaskRequest
import com.technfest.technfestcrm.repository.LeadRepository
import com.technfest.technfestcrm.repository.TaskRepository
import kotlinx.coroutines.launch
import java.util.Calendar

class LeadDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lead_detail, container, false)
        val toolbar =
            view.findViewById<MaterialToolbar>(R.id.toolbar)
        (activity as? AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        val token = arguments?.getString("token") ?: ""
        val workspaceId = arguments?.getInt("workspaceId") ?: 0


        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        val toolbarTitle: TextView = view.findViewById(R.id.toolbarTitle)
        val toolbarSubtitle: TextView = view.findViewById(R.id.toolbarSubtitle)
        val userInitialCircle : TextView =view.findViewById(R.id.userInitialCircle)
        val name: TextView = view.findViewById(R.id.txtName)
        val location: TextView = view.findViewById(R.id.txtLeadLocation)
        val mobile: TextView = view.findViewById(R.id.txtLeadNumber)
        val email: TextView = view.findViewById(R.id.txtLeadEmail)
        val companyName: TextView = view.findViewById(R.id.txtCompanyName)
        val source: TextView = view.findViewById(R.id.txtSource)
        val status: TextView = view.findViewById(R.id.txtLeadStatus)
        val stage: TextView = view.findViewById(R.id.txtStage)
        val priority: TextView = view.findViewById(R.id.txtPriority)
        val campaignName: TextView = view.findViewById(R.id.txtCampaignName)
        val leadRequirement: TextView = view.findViewById(R.id.txtLeadRequirement)
        val ownerName: TextView = view.findViewById(R.id.txtOwnerName)
        val teamName: TextView = view.findViewById(R.id.txtTeam)
        val note: TextView = view.findViewById(R.id.txtNote)
        val scheduledFollowUp: TextView = view.findViewById(R.id.txtScheduledFollowUp)
        val followUpStatus: TextView = view.findViewById(R.id.txtFllowUpStatus)

        val btnReschedule: MaterialButton = view.findViewById(R.id.btnReschedule)
        val btnAddNote: MaterialButton = view.findViewById(R.id.btnAddNote)
        val btnCreateTask: MaterialButton = view.findViewById(R.id.btnCreateTask)
        val btnCall : MaterialButton = view.findViewById(R.id.btnCall)

        btnCall.setOnClickListener {

        }
        btnReschedule.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_reschedule, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        btnAddNote.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_add_note, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        btnCreateTask.setOnClickListener {

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
            val btnSave = dialogView.findViewById<MaterialButton>(R.id.btnCreateTask)
            val btnCancel = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
            val ivClose = dialogView.findViewById<ImageView>(R.id.ivClose)
            leadNameText.text = leadNameStr

            // Load priority from API
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
                        Toast.makeText(requireContext(), "Failed to load priority!", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }


            val assignUserList = listOf("Developer Pratik")
            val assignAdapter =
                ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, assignUserList)
            assignAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinnerAssignToUser.adapter = assignAdapter

            // Date Picker
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

            // SAVE BUTTON HANDLER
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
                        Log.d("Toekns",passedToken)
                        val passedWorkspaceId = workspaceId

                        val repo = TaskRepository()
                        val response = repo.createTask(passedToken, taskRequest)


                        if (response.isSuccessful) {
                            Toast.makeText(requireContext(), "Task Created Successfully", Toast.LENGTH_LONG)
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
                        Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
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
        name.text = args?.getString("name")
        companyName.text = args?.getString("company")
        location.text = args?.getString("location")
        mobile.text = args?.getString("mobile")
        email.text = args?.getString("email")
        status.text = args?.getString("status")
        source.text = args?.getString("source")
        stage.text = args?.getString("stage")
        priority.text = args?.getString("priority")
        campaignName.text = args?.getString("campaignName")
        leadRequirement.text = args?.getString("leadRequirement")
        scheduledFollowUp.text = args?.getString("nextFollowupAt")
        followUpStatus.text = args?.getString("followupStatus")
        ownerName.text = args?.getString("ownerName")
        teamName.text = args?.getString("teamName")
        note.text = args?.getString("note")
        toolbarTitle.text = args?.getString("name") ?: "N/A"
        toolbarSubtitle.text = args?.getString("location") ?: "N/A"
        val leadName = args?.getString("name") ?: "N/A"
        userInitialCircle.text = getInitials(leadName)

        return view
    }

    companion object {
        fun newInstance(
            lead: LeadResponseItem,
            token: String,
            workspaceId: Int
        ): LeadDetailFragment {

            val fragment = LeadDetailFragment()
            val args = Bundle().apply {

                putString("token", token)
                putInt("workspaceId", workspaceId)
                putInt("leadId", lead.id)

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



}
