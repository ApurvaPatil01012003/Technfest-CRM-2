package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LeadResponseItem

class LeadAdapter(
    private var leadList: List<LeadResponseItem>,
    private val onItemClick: (LeadResponseItem) -> Unit
) : RecyclerView.Adapter<LeadAdapter.LeadViewHolder>() {

    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadName: TextView = itemView.findViewById(R.id.leadName)
        val leadNumber : TextView = itemView.findViewById(R.id.leadNumber)
        val location: TextView = itemView.findViewById(R.id.leadLocation)
        val priority: TextView = itemView.findViewById(R.id.leadPriority)
        val source: TextView = itemView.findViewById(R.id.source)
        val status: TextView = itemView.findViewById(R.id.status)
        val leadStage: TextView = itemView.findViewById(R.id.leadStage)
        val companyName: TextView = itemView.findViewById(R.id.companyName)
        val leadOwner: TextView = itemView.findViewById(R.id.leadOwner)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lead_items, parent, false)
        return LeadViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val lead = leadList[position]

        holder.leadName.text = lead.fullName
        holder.leadNumber.text = "+91 ${lead.mobile}"
        holder.location.text = "Location : ${lead.location?.toString() ?: "Not availble"}"
        holder.priority.text = "Priority : ${lead.priority}"
        holder.source.text ="Source : ${lead.source?.toString() ?: "Not availble" }"
        holder.status.text = "Status : ${lead.status}"
        holder.leadStage.text = "Stage : ${lead.stage ?: "Not availble"}"
        holder.companyName.text = "Company Name : ${lead.company?.toString() ?: "Not availble"}"
        holder.leadOwner.text = "Owner Name : ${lead.ownerName?.toString() ?: "Not availble"}"


        when (lead.priority.lowercase()) {

            "high" -> {
                holder.priority.background =
                    holder.itemView.context.getDrawable(R.drawable.bg_priority_tag)
                holder.priority.setTextColor(
                    holder.itemView.context.getColor(R.color.red)
                )
            }

            "normal" -> {
                holder.priority.background =
                    holder.itemView.context.getDrawable(R.drawable.chip_blue)
                holder.priority.setTextColor(
                    holder.itemView.context.getColor(R.color.blue)
                )
            }

            else -> {
                holder.priority.background =
                    holder.itemView.context.getDrawable(R.drawable.bg_stage_tag)
                holder.priority.setTextColor(
                    holder.itemView.context.getColor(R.color.green)
                )
            }
        }
        holder.itemView.setOnClickListener { onItemClick(lead) }
    }

    override fun getItemCount() = leadList.size

    fun updateList(newList: List<LeadResponseItem>) {
        leadList = newList
        notifyDataSetChanged()
    }
}
