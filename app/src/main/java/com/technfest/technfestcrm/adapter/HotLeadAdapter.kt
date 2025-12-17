package com.technfest.technfestcrm.adapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.LeadResponseItem

class HotLeadAdapter(private val leadList: List<LeadResponseItem>,
                     private val onItemClick: (LeadResponseItem) -> Unit) :
    RecyclerView.Adapter<HotLeadAdapter.LeadViewHolder>() {

    inner class LeadViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val leadName: TextView = itemView.findViewById(R.id.leadName)
        val leadAvatar : TextView = itemView.findViewById(R.id.leadAvatar)
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
        setAvatar(holder.leadAvatar, lead.fullName)

        holder.itemView.setOnClickListener {
            onItemClick(lead)
        }
    }

    override fun getItemCount(): Int = leadList.size


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
