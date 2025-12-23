package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LeadResponseItem
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat

class LeadAdapter(
    private var leadList: List<LeadResponseItem>,
    private val onItemClick: (LeadResponseItem) -> Unit
) : RecyclerView.Adapter<LeadAdapter.LeadViewHolder>() {

    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadName: TextView = itemView.findViewById(R.id.leadName)
        val leadNumber: TextView = itemView.findViewById(R.id.leadNumber)

        //        val location: TextView = itemView.findViewById(R.id.leadLocation)
        val priority: TextView = itemView.findViewById(R.id.leadPriority)
        val leadAvatar: TextView = itemView.findViewById(R.id.leadAvatar)

        //  val source: TextView = itemView.findViewById(R.id.source)
        val status: TextView = itemView.findViewById(R.id.status)
        val leadStage: TextView = itemView.findViewById(R.id.leadStage)
//        val companyName: TextView = itemView.findViewById(R.id.companyName)
//        val leadOwner: TextView = itemView.findViewById(R.id.leadOwner)
    }


    private var selectedLeadId = -1

    fun setSelectedLead(id: Int) {
        selectedLeadId = id
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LeadViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lead_items, parent, false)
        return LeadViewHolder(view)
    }

    override fun onBindViewHolder(holder: LeadViewHolder, position: Int) {
        val lead = leadList[position]

        holder.leadName.text = lead.fullName
//        holder.leadNumber.text = "+91 ${lead.mobile}"
        holder.leadNumber.text = formatInternationalNumber(lead.mobile)
      //  holder.location.text = "Location : ${lead.location?.toString() ?: "Not availble"}"
        holder.priority.text = "Priority : ${lead.priority}"
       // holder.source.text ="Source : ${lead.source?.toString() ?: "Not availble" }"
        holder.status.text = "Status : ${lead.status}"
        holder.leadStage.text = "Stage : ${lead.stage ?: "Not availble"}"
        setAvatar(holder.leadAvatar, lead.fullName)
//        holder.companyName.text = "Company Name : ${lead.company?.toString() ?: "Not availble"}"
//        holder.leadOwner.text = "Owner Name : ${lead.ownerName?.toString() ?: "Not availble"}"


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
        if (lead.id == selectedLeadId) {
            holder.itemView.setBackgroundResource(R.color.legend_all_leads)
        } else {
            holder.itemView.setBackgroundResource(R.color.white)
        }

        holder.itemView.setOnClickListener { onItemClick(lead) }
    }

    override fun getItemCount() = leadList.size

    private val avatarBackgrounds = listOf(
        R.drawable.gradient_circle_blue,
        R.drawable.gradient_circle_green,
        R.drawable.gradient_circle_orange,
        R.drawable.gradient_circle,
        R.drawable.gradient_circle_blue_cyan,
        R.drawable.gradient_circle_purple_pink,
        R.drawable.gradient_circle_red_orange

    )

    private fun setAvatar(textView: TextView, name: String?) {
        if (name.isNullOrBlank()) {
            textView.text = "?"
            textView.setBackgroundResource(avatarBackgrounds[0])
            return
        }

        val cleanName = name.trim().replace("\\s+".toRegex(), " ")
        val parts = cleanName.split(" ")

        val initials = when {
            parts.size >= 2 -> {
                "${parts[0][0]}${parts[1][0]}"
            }
            else -> {
                "${parts[0][0]}"
            }
        }

        textView.text = initials.uppercase()

        // Telegram-style stable background
        val index = kotlin.math.abs(cleanName.hashCode()) % avatarBackgrounds.size
        textView.setBackgroundResource(avatarBackgrounds[index])
    }
}


    fun formatInternationalNumber(rawNumber: String?): String {
        if (rawNumber.isNullOrBlank()) return ""

        return try {
            val phoneUtil = PhoneNumberUtil.getInstance()

            // Clean unwanted characters
            val cleaned = rawNumber.replace("[^0-9+]".toRegex(), "")

            // Parse with IN as default region
            val numberProto = phoneUtil.parse(cleaned, "IN")

            // Always return FULL international format
            phoneUtil.format(numberProto, PhoneNumberFormat.INTERNATIONAL)

        } catch (e: Exception) {
            rawNumber  // fallback
        }






}
