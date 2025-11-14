package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Task

class TaskAdapter(private val taskList: List<Task>, private val onItemClick: (Task) -> Unit) :
    RecyclerView.Adapter<TaskAdapter.TaskViewHolder>() {

    inner class TaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val heading: TextView = itemView.findViewById(R.id.txtHeading)
        val leadName: TextView = itemView.findViewById(R.id.txtLeadName)
        val city: TextView = itemView.findViewById(R.id.txtCity)
        val time: TextView = itemView.findViewById(R.id.txtTime)
        val status: TextView = itemView.findViewById(R.id.txtStatus)
        val priority: TextView = itemView.findViewById(R.id.txtPriority)
        val channelName: TextView = itemView.findViewById(R.id.txtChnnelName)
        val assignName: TextView = itemView.findViewById(R.id.txtAssign)
        val summary: TextView = itemView.findViewById(R.id.txtSummary)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.task_item, parent, false)
        return TaskViewHolder(view)
    }

    override fun onBindViewHolder(holder: TaskViewHolder, position: Int) {
        val task = taskList[position]
        holder.heading.text = task.heading
        holder.leadName.text = task.leadName
        holder.city.text = task.city
        holder.time.text = task.time
        holder.status.text = task.status
        holder.priority.text = task.priority
        holder.channelName.text = task.channelName
        holder.assignName.text = task.Assign_name
        holder.summary.text = task.summary

        holder.itemView.setOnClickListener { onItemClick(task) }
    }

    override fun getItemCount(): Int = taskList.size
}
