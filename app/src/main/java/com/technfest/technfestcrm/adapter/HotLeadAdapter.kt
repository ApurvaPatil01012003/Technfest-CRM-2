package com.technfest.technfestcrm.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R

import com.technfest.technfestcrm.model.HotLead
import com.technfest.technfestcrm.model.LeadResponseItem

class HotLeadAdapter(private val leadList: List<LeadResponseItem>,
                     private val onItemClick: (LeadResponseItem) -> Unit) :
    RecyclerView.Adapter<HotLeadAdapter.LeadViewHolder>() {

    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadName: TextView = itemView.findViewById(R.id.leadName)
        val leadCampaign: TextView = itemView.findViewById(R.id.leadCampaign)
        val leadStage: TextView = itemView.findViewById(R.id.leadStage)
        val leadSourceType : TextView = itemView.findViewById(R.id.leadSource)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.home_hot_lead_item, parent, false)
        return LeadViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val lead = leadList[position]
        holder.leadName.text = lead.fullName
        holder.leadCampaign.text = lead.campaignName
        holder.leadStage.text = lead.stage as CharSequence?
        holder.leadSourceType.text = lead.source as CharSequence?
        holder.itemView.setOnClickListener {
            onItemClick(lead)
        }
    }

    override fun getItemCount(): Int = leadList.size
}
