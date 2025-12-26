package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.RecentActivityItem

class RecentActivityAdapter(
    private var list: List<RecentActivityItem>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_CALL = 1
        private const val TYPE_FEEDBACK = 2
    }

    override fun getItemViewType(position: Int): Int {
        return when (list[position]) {
            is RecentActivityItem.CallItem -> TYPE_CALL
            is RecentActivityItem.FeedbackItem -> TYPE_FEEDBACK
        }
    }

    inner class CallVH(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.txtTitle)
        val line1 = view.findViewById<TextView>(R.id.txtLine1)
        val line2 = view.findViewById<TextView>(R.id.txtLine2)
        val line3 = view.findViewById<TextView>(R.id.txtLine3)
        val dateTime = view.findViewById<TextView>(R.id.txtDateTime)
    }

    inner class FeedbackVH(view: View) : RecyclerView.ViewHolder(view) {
        val status = view.findViewById<TextView>(R.id.txtStatus)
        val rating = view.findViewById<TextView>(R.id.txtRating)
        val receivedBy = view.findViewById<TextView>(R.id.txtReceivedBy)
        val note = view.findViewById<TextView>(R.id.txtNote)
        val followUp = view.findViewById<TextView>(R.id.txtFollowUp)
        val dateTime = view.findViewById<TextView>(R.id.txtDateTime)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_CALL) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_call, parent, false)
            CallVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_recent_feedback, parent, false)
            FeedbackVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = list[position]) {

            is RecentActivityItem.CallItem -> {
                val h = holder as CallVH
                h.title.text = "Call"
                h.line1.text = "Lead name: ${item.leadName}"
                h.line2.text = "Lead number: ${item.leadNumber}"
                h.line3.text =
                    "Call status: ${item.callStatusLabel}\nStart: ${item.startIso}\nEnd: ${item.endIso}\nDuration: ${item.durationSec}s"
                h.dateTime.text = formatDateTime(item.timestamp)
            }

            is RecentActivityItem.FeedbackItem -> {
                val f = item.feedback
                val h = holder as FeedbackVH
                h.status.text = "Call status : ${f.callStatus}"
                h.rating.text = "Rating : ${"⭐".repeat(f.rating)}"
                h.receivedBy.text = "Call received by : ${f.receivedBy ?: "N/A"}"
                h.note.text = "Note : ${f.note}"
                h.followUp.text = "Next follow-up : ${f.followUp ?: "N/A"}"
                h.dateTime.text = formatDateTime(f.timestamp)
            }
        }
    }

    override fun getItemCount() = list.size

    fun update(newList: List<RecentActivityItem>) {
        list = newList
        notifyDataSetChanged()
    }

    private fun formatDateTime(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy hh:mm a", java.util.Locale.getDefault())
        return sdf.format(java.util.Date(timestamp))
    }
}
