package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Lead

class LeadAdapter(private val leadList: List<Lead>, private val onItemClick: (Lead) -> Unit ) :
    RecyclerView.Adapter<LeadAdapter.LeadViewHolder>() {

    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadName: TextView = itemView.findViewById(R.id.leadName)
        val location: TextView = itemView.findViewById(R.id.leadLocation)
        val priority: TextView = itemView.findViewById(R.id.leadPriority)
        val source: TextView = itemView.findViewById(R.id.source)
        val task: TextView = itemView.findViewById(R.id.leadTask)
        val leadStage: TextView = itemView.findViewById(R.id.leadStage)
        val companyName: TextView = itemView.findViewById(R.id.companyName)
        val leadOwner: TextView = itemView.findViewById(R.id.leadOwner)
        val time: TextView = itemView.findViewById(R.id.time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lead_items, parent, false)
        return LeadViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val lead = leadList[position]
        holder.leadName.text = lead.name
        holder.location.text = lead.location
        holder.priority.text = lead.status
        holder.source.text = lead.Source
        holder.task.text = lead.task
        holder.leadStage.text = lead.stage
        holder.companyName.text = lead.company
        holder.leadOwner.text = lead.owner
        holder.time.text = "Today 11:00am"

        holder.itemView.setOnClickListener {
            onItemClick(lead)
        }
    }

    override fun getItemCount(): Int = leadList.size
}
