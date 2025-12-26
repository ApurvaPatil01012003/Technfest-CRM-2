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
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class HomeTaskAdapter(
    private var taskList: List<TaskResponseItem>,
    private val onItemClick: (TaskResponseItem) -> Unit
) : RecyclerView.Adapter<HomeTaskAdapter.HomeTaskViewHolder>() {

    inner class HomeTaskViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtTaskName: TextView = itemView.findViewById(R.id.txtTaskName)
        val leadStatus: TextView = itemView.findViewById(R.id.leadStatus)
        val txtLeadName: TextView = itemView.findViewById(R.id.txtLeadName)
        val txtTaskDue: TextView = itemView.findViewById(R.id.txtTaskDue)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HomeTaskViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_todyas_task, parent, false)
        return HomeTaskViewHolder(view)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onBindViewHolder(holder: HomeTaskViewHolder, position: Int) {
        val task = taskList[position]

        holder.txtTaskName.text = task.title ?: "No Title"
        holder.txtLeadName.text = "Lead Name : ${task.leadName ?: "No Lead"}"
        holder.leadStatus.text = task.status ?: "Pending"

        val formattedTime = try {
            task.dueAt?.let {
                val formatterInput = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                val dateTime = LocalDateTime.parse(it, formatterInput)

                val formatterOutput = DateTimeFormatter.ofPattern("hh:mm a")
                "Due At : ${dateTime.format(formatterOutput)}"
            } ?: "No Due Time"
        } catch (e: Exception) {
            "No Due Time"
        }
        holder.txtTaskDue.text = formattedTime


        holder.itemView.setOnClickListener {
            onItemClick(task)
        }


    }

    override fun getItemCount(): Int = taskList.size

    fun updateList(newList: List<TaskResponseItem>) {
        taskList = newList
        notifyDataSetChanged()
    }


}
