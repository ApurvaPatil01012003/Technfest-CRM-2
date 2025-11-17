package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.databinding.ItemRecordingBinding

class RecordingsAdapter(private val list: List<String>,
                        private val onItemClick: (String) -> Unit
) :
    RecyclerView.Adapter<RecordingsAdapter.RecordingViewHolder>() {

    inner class RecordingViewHolder(val binding: ItemRecordingBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecordingViewHolder {
        val binding = ItemRecordingBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RecordingViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecordingViewHolder, position: Int) {
        val name = list[position]
        holder.binding.tvRecordingName.text = name

        holder.binding.root.setOnClickListener {
            onItemClick(name)
        }
    }


    override fun getItemCount(): Int = list.size
}
