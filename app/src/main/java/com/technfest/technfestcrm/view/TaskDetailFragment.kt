package com.technfest.technfestcrm.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LocalTask
import com.technfest.technfestcrm.model.TaskResponseItem
import java.text.SimpleDateFormat
import java.util.*

class TaskDetailFragment : Fragment() {

    private var heading: String? = null
    private var leadName: String? = null
    private var dueDate: String? = null
    private var assignName: String? = null
    private var summary: String? = null
    private var taskType: String? = null
    private var status: String? = null
    private var priority: String? = null
    private var estimatedHours: String? = null
    private var taskId: Int = -1
    private var isUserAction = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            heading = it.getString("heading")
            leadName = it.getString("leadName")
            dueDate = it.getString("dueDate")
            assignName = it.getString("assignName")
            summary = it.getString("summary")
            taskType = it.getString("taskType")
            status = it.getString("status")
            priority = it.getString("priority")
            estimatedHours = it.getString("estimatedHours")
            taskId = it.getInt("taskId", -1)


        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_task_detail, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<TextView>(R.id.txtLeadName).text = leadName ?: "null"
        view.findViewById<TextView>(R.id.txtDue).text = formatDate(dueDate)
        view.findViewById<TextView>(R.id.txtAssignTo).text = assignName ?: "null"
        view.findViewById<TextView>(R.id.txtDescription).text = summary ?: "null"
        view.findViewById<TextView>(R.id.txtTaskType).text = taskType ?: "null"
        view.findViewById<TextView>(R.id.txtStatus).text = "Status : ${status ?: "null"}"
        view.findViewById<TextView>(R.id.txtEstimateHours).text =
            "Estimated Duration : ${estimatedHours ?: "N/A"} Hrs"

        view.findViewById<TextView>(R.id.txtPriority).text = "Priority : ${priority ?: "null"}"


        val priorityView = view.findViewById<TextView>(R.id.txtPriority)

        priorityView.text = "Priority : ${priority ?: "null"}"
        val spnStatus = view.findViewById<Spinner>(R.id.spnStatus)
        val txtStatusView = view.findViewById<TextView>(R.id.txtStatus)
        val rescheduleLayout = view.findViewById<LinearLayout>(R.id.layoutReschedule)

        val statusList = listOf("Pending", "Completed")

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_dropdown_item,
            statusList
        )



        spnStatus.adapter = adapter

        spnStatus.setOnTouchListener { _, _ ->
            isUserAction = true
            false
        }
        val defaultIndex = statusList.indexOfFirst {
            it.equals(status, true)
        }.takeIf { it != -1 } ?: 0

        spnStatus.setSelection(defaultIndex, false)

        rescheduleLayout.visibility =
            if (status.equals("Pending", true)) View.VISIBLE else View.GONE


        spnStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {

            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                if (!isUserAction) return

                val selectedStatus = statusList[position]
                txtStatusView.text = "Status : $selectedStatus"

                updateLocalTask(
                    requireContext(),
                    taskId,
                    selectedStatus,
                    null
                )

                rescheduleLayout.visibility =
                    if (selectedStatus.equals("Pending", true)) View.VISIBLE else View.GONE
            }

            override fun onNothingSelected(parent: AdapterView<*>) {}
        }


        priority?.lowercase()?.let { pr ->
            when (pr) {

                "high" -> {
                    priorityView.background =
                        requireContext().getDrawable(R.drawable.bg_priority_tag)
                    priorityView.setTextColor(
                        requireContext().getColor(R.color.red)
                    )
                }

                "normal" -> {
                    priorityView.background =
                        requireContext().getDrawable(R.drawable.chip_blue)
                    priorityView.setTextColor(
                        requireContext().getColor(R.color.blue)
                    )
                }

                else -> {
                    priorityView.background =
                        requireContext().getDrawable(R.drawable.bg_status_tag)
                    priorityView.setTextColor(
                        requireContext().getColor(R.color.green)
                    )
                }
            }
        }
        val edtFollowUpdate = view.findViewById<EditText>(R.id.edtFollowUpdate)
        val txtDueView = view.findViewById<TextView>(R.id.txtDue)

        edtFollowUpdate.setOnClickListener {

            val calendar = Calendar.getInstance()

            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->

                    calendar.set(Calendar.YEAR, year)
                    calendar.set(Calendar.MONTH, month)
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)

                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->

                            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            calendar.set(Calendar.MINUTE, minute)

                            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
//                            val formattedDate = sdf.format(calendar.time)
//
//                            edtFollowUpdate.setText(formattedDate)
//                            txtDueView.text = formattedDate

                            val displayDate = sdf.format(calendar.time)
                            edtFollowUpdate.setText(displayDate)
                            txtDueView.text = displayDate

                            val backendDueAt = convertToBackendFormat(displayDate)

                            updateLocalTask(
                                requireContext(),
                                taskId,
                                status ?: "Pending",
                                backendDueAt
                            )


                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        false
                    ).show()

                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

    }
    private fun convertToBackendFormat(displayDate: String): String {
        val input = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val output = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return output.format(input.parse(displayDate)!!)
    }


    private fun formatDate(dateStr: String?): String {
        if (dateStr.isNullOrEmpty()) return "null"

        return try {
            val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
            input.timeZone = TimeZone.getTimeZone("UTC")
            val date = input.parse(dateStr)

            val output = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            output.format(date!!)
        } catch (e: Exception) {
            dateStr
        }
    }

    companion object {
        fun newInstance(task: TaskResponseItem): TaskDetailFragment {
            val fragment = TaskDetailFragment()
            val args = Bundle().apply {
                putInt("taskId", task.id)
                putString("heading", task.title)
                putString("leadName", task.leadName)
                putString("dueDate", task.dueDate)
                putString("assignName", task.assignedEmployeeName)
                putString("summary", task.description)
                putString("taskType", task.taskType)
                putString("status", task.status)
                putString("priority", task.priority)
                putString("estimatedHours", task.estimatedHours)
            }
            fragment.arguments = args
            return fragment
        }

        private fun updateLocalTask(
            context: Context,
            taskId: Int,
            newStatus: String,
            newDueAt: String?
        ) {
            val prefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE)
            val json = prefs.getString("task_list", null) ?: return

            val type = object : com.google.gson.reflect.TypeToken<MutableList<LocalTask>>() {}.type
            val taskList: MutableList<LocalTask> =
                com.google.gson.Gson().fromJson(json, type)

            val index = taskList.indexOfFirst { it.id == taskId }
            if (index == -1) return

            val oldTask = taskList[index]

            taskList[index] = oldTask.copy(
                status = newStatus,
                dueAt = newDueAt ?: oldTask.dueAt
            )

            prefs.edit()
                .putString("task_list", com.google.gson.Gson().toJson(taskList))
                .apply()
        }

    }
}
