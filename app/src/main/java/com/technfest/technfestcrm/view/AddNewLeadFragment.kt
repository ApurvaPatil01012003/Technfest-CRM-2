package com.technfest.technfestcrm.view

import android.R
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import com.technfest.technfestcrm.databinding.FragmentAddNewLeadBinding
import com.technfest.technfestcrm.localdatamanager.LocalLeadManager
import com.technfest.technfestcrm.localdatamanager.LocalTaskManager
import com.technfest.technfestcrm.model.LeadMetaItem
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.LocalTask
import com.technfest.technfestcrm.worker.TaskNotificationWorker
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddNewLeadFragment : Fragment() {

    private var _binding: FragmentAddNewLeadBinding? = null
    private val binding get() = _binding!!
    private var teamList: List<LeadMetaItem> = emptyList()
    private var selectedFollowupDate: Calendar? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNewLeadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeMetaData()
        binding.edtFollowUpdate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)

                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedDate.set(Calendar.MINUTE, minute)
                            selectedDate.set(Calendar.SECOND, 0)

                            selectedFollowupDate = selectedDate // save it here

                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                            binding.edtFollowUpdate.setText(sdf.format(selectedDate.time))

                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }




        binding.btnSaveLead.setOnClickListener {
            saveLead()
        }
    }

    private fun observeMetaData() {
        setSimpleSpinner(
            binding.spnSource,
            listOf(
                "Call",
                "Instagram",
                "WhatsApp",
                "Web Form",
                "Facebook"
            )
        )
        setSimpleSpinner(
            binding.spnCampaignName,
            listOf(
                "FB - Diwali Offer",
                "Cold Calling - Pune",
                "Walk- in Store Enquiry"
            )
        )
        setSimpleSpinner(
            binding.spnStatus,
            listOf(
                "New",
                "Contacted"
            )
        )

        setSimpleSpinner(
            binding.spnStages,
            listOf(
                "Demo Scheduled"
            )
        )

        setSimpleSpinner(
            binding.spnPriority,
            listOf(
                "High",
                "Normal",
                "Low"
            )
        )

        val teamNames = listOf(
            "Sales Team Call",
            "Sales Team WhatsApp",
            "Support Team"
        )

        val teamItems = teamNames.mapIndexed { index, name ->
            LeadMetaItem(
                id = index + 1,
                name = name
            )


        }

        teamList = teamItems

        binding.spnAssignToTeam.adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_dropdown_item,
            teamNames
        )
        setSimpleSpinner(
            binding.spnAssignType,
            listOf(
                "FIFO",
                "Round Robin"
            )
        )

        setSimpleSpinner(
            binding.edtUserName,
            listOf(
                "Sagar",
                "Pratik"
            )
        )


    }

    private fun saveLead() {

        val name = binding.edtLeadName.text.toString().trim()
        val phone = binding.edtleadNumber.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Name & Phone required", Toast.LENGTH_SHORT).show()
            return
        }

        val tagsList = binding.edtTags.text.toString()
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val selectedTeamPosition = binding.spnAssignToTeam.selectedItemPosition
        val selectedTeam =
            if (selectedTeamPosition in teamList.indices) teamList[selectedTeamPosition] else null

        val source = binding.spnSource.selectedItem.toString()
        val status = binding.spnStatus.selectedItem.toString()
        val stage = binding.spnStages.selectedItem.toString()
        val priority = binding.spnPriority.selectedItem.toString()
        val campaignName = binding.spnCampaignName.selectedItem.toString()
        val assigned_to_user = binding.edtUserName.selectedItem.toString()

        val followupDatesList = selectedFollowupDate?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            listOf(sdf.format(it.time))
        } ?: emptyList()

        val nextFollowup = selectedFollowupDate?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(it.time)
        }
        selectedFollowupDate?.let { followupDate ->

            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

            val task = LocalTask(
                id = System.currentTimeMillis().toInt(),
                title = "Follow-up with $name",
                description = binding.edtLeadRequest.text.toString(),
                dueAt = sdf.format(followupDate.time),
                priority = priority,
                status = "Pending",
                taskType = "Lead_FOLLOW_UP",
                leadName = name,
                assignedToUser = assigned_to_user,
                source = source,
                estimatedHours = "0"
            )

            LocalTaskManager.saveTask(requireContext(), task)
            scheduleTaskNotification(task)


        }
        val newId = (System.currentTimeMillis() % Int.MAX_VALUE).toInt()


        val leadRequest = LeadRequest(
            assigned_to = assigned_to_user,
            campaignId = 0,
            campaignName = campaignName,
            company = binding.edtCompanyname.text.toString(),
            email = binding.edtEmail.text.toString(),
            followupDates = followupDatesList,
            fullName = name,
            leadRequirement = binding.edtLeadRequest.text.toString(),
            location = binding.edtLocation.text.toString(),
            mobile = phone,
            nextFollowupAt = nextFollowup,
            priority = priority,
            source = source,
            sourceDetails = "",
            stage = stage,
            status = status,
            tags = tagsList,
            teamId = selectedTeam?.id ?: 0,
            teamName = selectedTeam?.name ?: "",
            ownerName = "",
            note = "",
            id = newId
        )


        LocalLeadManager.saveLead(requireContext(), leadRequest)

        Toast.makeText(requireContext(), "Lead saved locally", Toast.LENGTH_SHORT).show()
        parentFragmentManager.setFragmentResult(
            "lead_added",
            Bundle()
        )
       requireActivity().onBackPressedDispatcher.onBackPressed()


    }


    private fun setSpinner(spinner: Spinner, data: List<LeadMetaItem>) {
        val names = data.map { it.name }
        val adapter = ArrayAdapter(requireContext(), R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setSimpleSpinner(spinner: Spinner, values: List<String>) {
        val adapter = ArrayAdapter(
            requireContext(),
            R.layout.simple_spinner_dropdown_item,
            values
        )
        spinner.adapter = adapter
    }
    private fun scheduleTaskNotification(task: LocalTask) {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        val dueTime = sdf.parse(task.dueAt)?.time ?: return

        val delay = dueTime - System.currentTimeMillis()
        if (delay <= 0) return

        val work = OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delay, java.util.concurrent.TimeUnit.MILLISECONDS)
            .setInputData(
                androidx.work.workDataOf(
                    "taskId" to task.id,
                    "taskTitle" to task.title
                )
            )
            .build()

        androidx.work.WorkManager.getInstance(requireContext())
            .enqueueUniqueWork(
                "task_notify_${task.id}",
                ExistingWorkPolicy.REPLACE,
                work
            )
    }

}
