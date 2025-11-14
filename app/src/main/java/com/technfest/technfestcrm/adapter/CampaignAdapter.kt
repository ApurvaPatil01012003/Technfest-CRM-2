package com.technfest.technfestcrm.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Campaign
import androidx.core.graphics.drawable.toDrawable
import org.w3c.dom.Text

class CampaignAdapter(private var campaignList: List<Campaign>) : RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {
    class CampaignViewHolder(itemView : View) : RecyclerView.ViewHolder(itemView)
    {
        val name : TextView = itemView.findViewById(R.id.campaignName)
        val type : TextView = itemView.findViewById(R.id.txtType)
        val status : TextView = itemView.findViewById(R.id.txtStatus)
        val ROI:TextView = itemView.findViewById(R.id.txtROI)
        val date_range : TextView = itemView.findViewById(R.id.txtDateRange)
        val owner:TextView = itemView.findViewById(R.id.txtOwnerName)
        val budget : TextView = itemView.findViewById(R.id.txtBudget)
        val spend : TextView = itemView.findViewById(R.id.txtSpend)
        val lead : TextView = itemView.findViewById(R.id.txtLeads)
        val cpl : TextView= itemView.findViewById(R.id.txtCpl)
        val btnStart : MaterialButton = itemView.findViewById(R.id.btnStart)
        val btnEdit : MaterialButton=itemView.findViewById(R.id.btnEdit)
    }
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CampaignViewHolder {
      val view = LayoutInflater.from(parent.context).inflate(R.layout.campaign_item,parent,false)
        return CampaignViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CampaignViewHolder,
        position: Int
    ) {
   val campaign = campaignList[position]
        holder.name.text=campaign.name
        holder.type.text =campaign.type
        holder.status.text=campaign.status
        holder.ROI.text=campaign.ROI
        holder.date_range.text=campaign.date_range
        holder.owner.text = campaign.owner
        holder.budget.text=campaign.budget
        holder.budget.text = campaign.spend
        holder.lead.text=campaign.leads
        holder.cpl.text=campaign.lead_cpl

        holder.btnEdit.setOnClickListener {
            val context = holder.itemView.context
            val dialogView = LayoutInflater.from(context).inflate(R.layout.campaign_edit, null)

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.show()


            val etCampaignName = dialogView.findViewById<EditText>(R.id.etCampaignName)
            val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
           // val etSpend = dialogView.findViewById<EditText>(R.id.etSpend)
            val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatus)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
            val txtCampaignName = dialogView.findViewById<TextView>(R.id.txtCampaignName)

            val campaign = campaignList[position]
            etCampaignName.setText(campaign.name)
            txtCampaignName.text = campaign.name
            etBudget.setText(campaign.budget.toString())
          //  etSpend.setText(campaign.budget.toString())

            val statusList = listOf("Active", "Paused", "Completed")
            val adapter = ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, statusList)
            spinnerStatus.adapter = adapter
            spinnerStatus.setSelection(statusList.indexOf(campaign.status))

            btnCancel.setOnClickListener { dialog.dismiss() }

            btnSave.setOnClickListener {

                campaign.name = etCampaignName.text.toString()
                campaign.budget = etBudget.text.toString().toIntOrNull().toString().toString()
             //   campaign.spend = etSpend.text.toString().toIntOrNull() ?: 0
                campaign.status = spinnerStatus.selectedItem.toString()

                notifyItemChanged(position)
                dialog.dismiss()
            }
        }

    }

    override fun getItemCount(): Int =campaignList.size
    fun updateData(newList: List<Campaign>) {
        campaignList = newList
        notifyDataSetChanged()
    }


}