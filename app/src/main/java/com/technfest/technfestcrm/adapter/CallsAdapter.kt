package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Calls

class CallsAdapter(private val callsList: List<Calls>) :
    RecyclerView.Adapter<CallsAdapter.CallsViewHolder>() {

    class CallsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val CallType: TextView = itemView.findViewById(R.id.tvCallType)
        val number: TextView = itemView.findViewById(R.id.tvNumber)
        val time: TextView = itemView.findViewById(R.id.tvTime)
        val day: TextView = itemView.findViewById(R.id.tvday)
        val duration: TextView = itemView.findViewById(R.id.tvDuration)
        val assign_user: TextView = itemView.findViewById(R.id.tvAssignUser)
        val assign_number: TextView = itemView.findViewById(R.id.tvAssignNumber)
        val linkedLink: TextView = itemView.findViewById(R.id.tvLeadLinked)
        //val note: TextView = itemView.findViewById(R.id.tvNoteAdded)
        val initial: TextView = itemView.findViewById(R.id.tvInitials)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): CallsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calls_item, parent, false)
        return CallsViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CallsViewHolder,
        position: Int
    ) {
        val call = callsList[position]
        holder.name.text = call.name
        holder.number.text = call.number
        holder.CallType.text = call.CallType
        holder.time.text = call.time
        holder.day.text = call.day
        holder.duration.text = call.duration
        holder.assign_user.text=call.assign_user
        holder.assign_number.text = call.assign_number
        holder.linkedLink.text = call.linkedLink
       // holder.note.text = call.note
        holder.initial.text=call.initial
    }

    override fun getItemCount(): Int = callsList.size


}