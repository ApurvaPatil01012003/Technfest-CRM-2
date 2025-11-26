package com.technfest.technfestcrm.adapter

import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.TaskResponseItem
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
      //  val channelName: TextView = itemView.findViewById(R.id.txtChnnelName)
        val assignName: TextView = itemView.findViewById(R.id.txtAssign)
        val summary: TextView = itemView.findViewById(R.id.txtSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.heading.text = task.title ?: "null"
        holder.leadName.text = task.leadName ?: "null"
        holder.taskType.text = task.taskType ?: "null"
      //  holder.time.text = task.dueAt?.split("T")?.get(0) ?: "null"
        val formatted = try {
            task.dueAt?.let {
                val zdt = ZonedDateTime.parse(it)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm a")
                zdt.format(formatter)
            } ?: "null"
        } catch (e: Exception) {
            "null"
        }

        holder.time.text = formatted
        holder.status.text = task.status ?: "null"
        holder.priority.text = task.priority ?: "null"
       // holder.channelName.text = task.taskType ?: "null"
        holder.assignName.text = task.assignedEmployeeName ?: "null"
        holder.summary.text = task.description ?: "null"


        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = taskList.size

    fun updateList(newList: List<TaskResponseItem>) {
        taskList = newList
        notifyDataSetChanged()
    }

}
