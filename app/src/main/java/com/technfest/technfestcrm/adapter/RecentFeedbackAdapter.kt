package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.CallFeedback

class RecentFeedbackAdapter(
    private var list: List<CallFeedback>
) : RecyclerView.Adapter<RecentFeedbackAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val status = view.findViewById<TextView>(R.id.txtStatus)
        val rating = view.findViewById<TextView>(R.id.txtRating)
        val receivedBy = view.findViewById<TextView>(R.id.txtReceivedBy)
        val note = view.findViewById<TextView>(R.id.txtNote)
        val followUp = view.findViewById<TextView>(R.id.txtFollowUp)
        val dateTime = view.findViewById<TextView>(R.id.txtDateTime)

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_recent_feedback, parent, false)
        return VH(view)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = list[position]

        holder.status.text = "Call status : ${f.callStatus}"
        holder.rating.text = "Rating : ${"⭐".repeat(f.rating)}"
        holder.receivedBy.text = "Call received by : ${f.receivedBy ?: "N/A"}"
        holder.note.text = "Note : ${f.note}"
        holder.followUp.text = "Next follow-up : ${f.followUp ?: "N/A"}"

        holder.dateTime.text = formatDateTime(f.timestamp)
    }

    override fun getItemCount() = list.size

    fun update(newList: List<CallFeedback>) {
        list = newList
        notifyDataSetChanged()
    }
    private fun formatDateTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat(
            "dd/MM/yyyy hh:mm a",
            java.util.Locale.getDefault()
        )
        return sdf.format(java.util.Date(timestamp))
    }

}
