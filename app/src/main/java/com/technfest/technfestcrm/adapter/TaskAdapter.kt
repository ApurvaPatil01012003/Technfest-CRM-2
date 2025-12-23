package com.technfest.technfestcrm.adapter

import android.graphics.Color
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.TaskResponseItem
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class TaskAdapter(
    private var taskList: List<TaskResponseItem>,
    private val onItemClick: (TaskResponseItem) -> Unit
) : RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val heading: TextView = itemView.findViewById(R.id.txtHeading)
        val leadName: TextView = itemView.findViewById(R.id.txtLeadName)
        val taskType: TextView = itemView.findViewById(R.id.txtTaskType)
        val time: TextView = itemView.findViewById(R.id.txtTime)
        val status: TextView = itemView.findViewById(R.id.txtStatus)
        val priority: TextView = itemView.findViewById(R.id.txtPriority)
        val assignName: TextView = itemView.findViewById(R.id.txtAssign)
    }

    private var highlightedTaskId: Int? = null

    fun highlightItem(taskId: Int) {
        highlightedTaskId = taskId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.heading.text = task.title ?: "--"
        holder.leadName.text = task.leadName ?: "--"
        holder.taskType.text = task.taskType ?: "--"
        holder.status.text = task.status ?: "--"
        holder.priority.text = task.priority ?: "--"
        holder.assignName.text = task.assignedEmployeeName ?: "--"

        // Format dueAt
        val formattedTime = try {
            task.dueAt?.let {
                val inputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val ldt = LocalDateTime.parse(it, inputFormatter)
                val outputFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")
                ldt.format(outputFormatter)
            } ?: "--"
        } catch (e: Exception) {
            "--"
        }
        holder.time.text = formattedTime

        // Highlight background if task is highlighted
        holder.itemView.setBackgroundColor(
            if (task.id == highlightedTaskId)
                holder.itemView.context.getColor(R.color.blue) // highlighted color
            else
                holder.itemView.context.getColor(android.R.color.white)
        )
        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = taskList.size


    fun updateList(newList: List<TaskResponseItem>) {
        taskList = newList
        notifyDataSetChanged()
    }
}

