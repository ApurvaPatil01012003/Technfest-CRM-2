package com.technfest.technfestcrm.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import androidx.core.graphics.drawable.toDrawable
import com.technfest.technfestcrm.model.CampaignResponseItem
import org.w3c.dom.Text

class CampaignAdapter(private var campaignList: List<CampaignResponseItem>) :
    RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {
    class CampaignViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.campaignName)
        val type: TextView = itemView.findViewById(R.id.txtType)
        val status: TextView = itemView.findViewById(R.id.txtStatus)
        val ROI: TextView = itemView.findViewById(R.id.txtROI)
        val date_range: TextView = itemView.findViewById(R.id.txtDateRange)
        val owner: TextView = itemView.findViewById(R.id.txtOwnerName)
        val budget: TextView = itemView.findViewById(R.id.txtBudget)
        val spend: TextView = itemView.findViewById(R.id.txtSpend)
        val lead: TextView = itemView.findViewById(R.id.txtLeads)
        val cpl: TextView = itemView.findViewById(R.id.txtCpl)
        val tagsContainer: LinearLayout = itemView.findViewById(R.id.tagsContainer)
        val totalCalls : TextView = itemView.findViewById(R.id.txtTotalCalls)
        val description :  TextView=itemView.findViewById(R.id.txtDescription)
        val createdBy: TextView=itemView.findViewById(R.id.txtCreatedBy)

        val btnStart: MaterialButton = itemView.findViewById(R.id.btnStart)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)

    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CampaignViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.campaign_item, parent, false)
        return CampaignViewHolder(view)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {

        val c = campaignList[position]
        // Clear old tags first
        holder.tagsContainer.removeAllViews()

// Convert tags list to strings
        val tagList = c.tags?.map { it.toString() } ?: emptyList()

        if (tagList.isEmpty()) {
            // Show "null" if no tags
            val textView = TextView(holder.itemView.context).apply {
                text = "null"
                setTextColor(Color.parseColor("#716E6E"))
                textSize = holder.itemView.resources.getDimension(R.dimen._4ssp)
                setPadding(
                    holder.itemView.resources.getDimensionPixelSize(R.dimen._8sdp),
                    holder.itemView.resources.getDimensionPixelSize(R.dimen._4sdp),
                    holder.itemView.resources.getDimensionPixelSize(R.dimen._8sdp),
                    holder.itemView.resources.getDimensionPixelSize(R.dimen._4sdp)
                )
                setBackgroundResource(R.drawable.tag_chip_bg)
            }
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                rightMargin = holder.itemView.resources.getDimensionPixelSize(R.dimen._4sdp)
            }
            holder.tagsContainer.addView(textView, params)

        } else {
            // Show all tags
            tagList.forEach { tag ->
                val textView = TextView(holder.itemView.context).apply {
                    text = tag
                    setTextColor(Color.parseColor("#3F51B5"))
                    textSize = holder.itemView.resources.getDimension(R.dimen._4ssp)
                    setPadding(
                        holder.itemView.resources.getDimensionPixelSize(R.dimen._8sdp),
                        holder.itemView.resources.getDimensionPixelSize(R.dimen._4sdp),
                        holder.itemView.resources.getDimensionPixelSize(R.dimen._8sdp),
                        holder.itemView.resources.getDimensionPixelSize(R.dimen._4sdp)
                    )
                    setBackgroundResource(R.drawable.tag_chip_bg)
                }
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    rightMargin = holder.itemView.resources.getDimensionPixelSize(R.dimen._4sdp)
                }
                holder.tagsContainer.addView(textView, params)
            }
        }


        holder.name.text = c.name
        holder.type.text = c.campaignCategoryName
        holder.status.text = c.status

        holder.date_range.text =
            "${formatDate(c.startDate)} → ${formatDate(c.endDate.toString()) ?: "Ongoing"}"
        holder.owner.text = c.ownerUserName?.toString() ?: "null"
        holder.budget.text = c.budget.toString()
        holder.spend.text = c.spentAmount.toString()
        holder.lead.text = c.totalLeads.toString()
        holder.cpl.text = c.cpl.toString()
        holder.totalCalls.text = c.totalCalls.toString()
        holder.description.text = c.description.toString()
        holder.createdBy.text = c.createdByName?.toString() ?: "null"

        holder.btnEdit.setOnClickListener {
            val context = holder.itemView.context

            val dialogView = LayoutInflater.from(context)
                .inflate(R.layout.campaign_edit, null)

            val dialog = AlertDialog.Builder(context)
                .setView(dialogView)
                .create()

            dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            dialog.show()

            val etCampaignName = dialogView.findViewById<EditText>(R.id.etCampaignName)
            val etBudget = dialogView.findViewById<EditText>(R.id.etBudget)
            val spinnerStatus = dialogView.findViewById<Spinner>(R.id.spinnerStatus)
            val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            etCampaignName.setText(c.name)
            etBudget.setText(c.budget.toString())

            val statusList = listOf("Active", "Paused", "Completed")
            val adapter =
                ArrayAdapter(context, android.R.layout.simple_spinner_dropdown_item, statusList)
            spinnerStatus.adapter = adapter
            spinnerStatus.setSelection(statusList.indexOf(c.status))

            btnCancel.setOnClickListener { dialog.dismiss() }

            btnSave.setOnClickListener {
                c.name = etCampaignName.text.toString()
                c.budget = etBudget.text.toString()
                c.status = spinnerStatus.selectedItem.toString()

                notifyItemChanged(position)
                dialog.dismiss()
            }
        }
    }


    override fun getItemCount(): Int = campaignList.size
    fun updateData(newList: List<CampaignResponseItem>) {
        campaignList = newList
        notifyDataSetChanged()
    }


    private fun formatDate(dateString: String?): String {
        if (dateString.isNullOrEmpty()) return "Ongoing"

        return try {
            val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            val outputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())

            val date = inputFormat.parse(dateString)
            outputFormat.format(date!!)
        } catch (e: Exception) {
            dateString
        }
    }

}