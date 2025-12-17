package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.CampaignResponseItem


class CampaignAdapter(
    private var campaignList: List<CampaignResponseItem>,
    private val onItemClick: (CampaignResponseItem) -> Unit
) : RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {

    class CampaignViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.campaignName)
        val type: TextView = itemView.findViewById(R.id.txtType)
        val status: TextView = itemView.findViewById(R.id.txtStatus)
        val budget: TextView = itemView.findViewById(R.id.txtBudget)
        val lead: TextView = itemView.findViewById(R.id.txtLeads)
        val totalCalls: TextView = itemView.findViewById(R.id.txtTotalCalls)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.campaign_item, parent, false)
        return CampaignViewHolder(view)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        val campaign = campaignList[position]

        holder.name.text = campaign.name
        holder.type.text = campaign.campaignCategoryName
        holder.status.text = campaign.status
        holder.budget.text = campaign.budget.toString()
        holder.lead.text = campaign.totalLeads.toString()
        holder.totalCalls.text = campaign.totalCalls.toString()

        holder.itemView.setOnClickListener {
            onItemClick(campaign)
        }
    }

    override fun getItemCount(): Int = campaignList.size

    fun updateData(newList: List<CampaignResponseItem>) {
        campaignList = newList
        notifyDataSetChanged()
    }
}
