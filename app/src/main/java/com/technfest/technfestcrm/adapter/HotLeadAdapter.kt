package com.technfest.technfestcrm.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R

import com.technfest.technfestcrm.model.HotLead

class HotLeadAdapter(private val leadList: List<HotLead>) :
    RecyclerView.Adapter<HotLeadAdapter.LeadViewHolder>() {

    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadName: TextView = itemView.findViewById(R.id.leadName)
        val leadDetails: TextView = itemView.findViewById(R.id.leadDetails)
        val leadStatus: TextView = itemView.findViewById(R.id.leadStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_hot_lead_item, parent, false)
        return LeadViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val lead = leadList[position]
        holder.leadName.text = lead.name
        holder.leadDetails.text = lead.details + " •"
        holder.leadStatus.text = lead.status
    }

    override fun getItemCount(): Int = leadList.size
}
