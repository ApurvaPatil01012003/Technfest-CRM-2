package com.technfest.technfestcrm.view

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.TaskAdapter
import com.technfest.technfestcrm.model.LocalTask
import com.technfest.technfestcrm.model.TaskResponseItem
import com.technfest.technfestcrm.worker.TaskNotificationWorker
import java.time.LocalDate



class TaskFragment : Fragment() {

    private lateinit var taskAdapter: TaskAdapter
    private var originalList: List<TaskResponseItem> = emptyList()

    private lateinit var filterAll: TextView
    private lateinit var filterToday: TextView
    private lateinit var filterPending: TextView
    private lateinit var filterCompleted: TextView
    private lateinit var filterHighPriority: TextView
    private var highlightTaskId: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            highlightTaskId = it.getInt("highlightTaskId", -1).takeIf { id -> id != -1 }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_task, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.taskRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        taskAdapter = TaskAdapter(emptyList()) { task ->
            val detailFragment = TaskDetailFragment.newInstance(task)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }
        recyclerView.adapter = taskAdapter

        filterAll = view.findViewById(R.id.filterAll)
        filterToday = view.findViewById(R.id.filterToday)
        filterPending = view.findViewById(R.id.filterPending)
        filterCompleted = view.findViewById(R.id.filterCompleted)
        filterHighPriority = view.findViewById(R.id.filterHighPriority)

        setupFilters()
        loadLocalTasks()
        applyFilter("All")


        return view
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadLocalTasks() {
        val taskItems = getLocalTasks(requireContext())

        val sorted = taskItems.sortedByDescending {
            try {
                java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    .parse(it.dueAt ?: "")
            } catch (e: Exception) {
                null
            }
        }
        originalList = sorted
        taskAdapter.updateList(sorted)

        highlightTaskId?.let { scrollToHighlight(it) }

        scheduleDueTasksOnly(requireContext(), originalList)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        loadLocalTasks()
        applyFilter("All")
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupFilters() {
        filterAll.setOnClickListener { applyFilter("All") }
        filterToday.setOnClickListener { applyFilter("Today") }
        filterPending.setOnClickListener { applyFilter("Pending") }
        filterCompleted.setOnClickListener { applyFilter("Completed") }
        filterHighPriority.setOnClickListener { applyFilter("HighPriority") }
    }


    private fun scrollToHighlight(taskId: Int) {
        val position = originalList.indexOfFirst { it.id == taskId }
        if (position != -1) {
            view?.findViewById<RecyclerView>(R.id.taskRecyclerView)?.scrollToPosition(position)
            taskAdapter.highlightItem(taskId)
        }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun applyFilter(type: String) {
        highlightSelected(type)

        val filteredList = when (type) {
            "Today" -> {
                val today = LocalDate.now()

                val sdf = java.text.SimpleDateFormat(
                    "yyyy-MM-dd HH:mm:ss",
                    java.util.Locale.getDefault()
                )

                originalList.filter { task ->
                    try {
                        val date = sdf.parse(task.dueAt ?: return@filter false)
                        val taskDate = date.toInstant()
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()

                        taskDate == today
                    } catch (e: Exception) {
                        false
                    }
                }
            }


            "Pending" -> originalList.filter { it.status.equals("Pending", true) }
            "Completed" -> originalList.filter { it.status.equals("Completed", true) }
            "HighPriority" -> originalList.filter { it.priority.equals("High", true) }
            else -> originalList
        }

        taskAdapter.updateList(filteredList)
    }

    private fun highlightSelected(type: String) {
        val defaultBg = R.drawable.unselected_campaign_bg
        val selectedBg = R.drawable.selected_campaign_bg

        filterAll.setBackgroundResource(if (type == "All") selectedBg else defaultBg)
        filterToday.setBackgroundResource(if (type == "Today") selectedBg else defaultBg)
        filterPending.setBackgroundResource(if (type == "Pending") selectedBg else defaultBg)
        filterCompleted.setBackgroundResource(if (type == "Completed") selectedBg else defaultBg)
        filterHighPriority.setBackgroundResource(if (type == "HighPriority") selectedBg else defaultBg)
    }

    private fun getLocalTasks(context: Context): List<TaskResponseItem> {
        val prefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE)
        val json = prefs.getString("task_list", null) ?: return emptyList()

        val type = object : com.google.gson.reflect.TypeToken<List<LocalTask>>() {}.type
        val localTasks: List<LocalTask> = com.google.gson.Gson().fromJson(json, type)

        // Convert each LocalTask to TaskResponseItem
      //  return localTasks.map { it.toTaskResponseItem() }
        return localTasks.map { it.toTaskResponseItem() }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleDueTasksOnly(context: Context, tasks: List<TaskResponseItem>) {
        val now = System.currentTimeMillis()

        tasks.forEach { task ->
            val dueAt = task.dueAt ?: return@forEach
            val dueTime = try {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                val date = sdf.parse(dueAt)
                date?.time ?: return@forEach
            } catch (e: Exception) {
                e.printStackTrace()
                return@forEach
            }

            if (dueTime >= now) {
                scheduleTaskWithWorkManager(task)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun scheduleTaskWithWorkManager(task: TaskResponseItem) {
        val dueTimeMs = parseDueAt(task.dueAt ?: return) ?: return
        val delayMs = dueTimeMs - System.currentTimeMillis()
        if (delayMs <= 0) return

        val work = androidx.work.OneTimeWorkRequestBuilder<TaskNotificationWorker>()
            .setInitialDelay(delayMs, java.util.concurrent.TimeUnit.MILLISECONDS)
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
                androidx.work.ExistingWorkPolicy.REPLACE,
                work
            )
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun parseDueAt(dueAt: String): Long? {
        return try {
            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
            val date = sdf.parse(dueAt)
            date?.time
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }


    private fun LocalTask.toTaskResponseItem(): TaskResponseItem {
        return TaskResponseItem(
            assignedEmployeeName = this.assignedToUser ?: "Self",
            assignedToEmployeeId = 0,
            assignedToUserId = 0,
            assignedUserName = this.assignedToUser ?: "Self",
            completedAt = "",
            createdAt = this.dueAt ?: "",
            createdByName = "",
            createdByUserId = 0,
            currentVersion = 1,
            departmentId = "",
            departmentName = "",
            description = this.description ?: "",
            dueAt = this.dueAt ?: "",
            dueDate = this.dueAt ?: "",
            estimatedHours = this.estimatedHours ?: "0",
            id = this.id,
            isActive = true,
            lastActivityAt = "",
            leadId = 0,
            leadName = this.leadName ?: "",
            priority = this.priority ?: "",
            projectId = 0,
            projectName = "Local",
            status = this.status ?: "Pending",
            taskType = this.taskType ?: "CALL_FOLLOW_UP",
            title = this.title ?: "",
            totalLoggedMinutes = 0,
            updatedAt = "",
            workspaceId = 0
        )


    }
}

