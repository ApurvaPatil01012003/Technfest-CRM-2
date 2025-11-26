package com.technfest.technfestcrm.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.android.material.appbar.MaterialToolbar
import com.technfest.technfestcrm.R
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

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        toolbar.title = leadName ?: "Task Details"
        toolbar.subtitle = heading ?: "Heading"

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

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
    }
}
