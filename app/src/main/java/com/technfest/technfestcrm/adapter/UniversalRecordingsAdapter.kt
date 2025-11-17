package com.technfest.technfestcrm.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.databinding.ItemRecordingBinding

class UniversalRecordingsAdapter(
    private val list: List<DocumentFile>,
    private val onClick: (DocumentFile) -> Unit
) :
    RecyclerView.Adapter<UniversalRecordingsAdapter.RecVH>() {

    inner class RecVH(val b: ItemRecordingBinding) :
        RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecVH {
        return RecVH(
            ItemRecordingBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
        )
    }

    override fun onBindViewHolder(holder: RecVH, position: Int) {
        val file = list[position]
        holder.b.tvRecordingName.text = file.name ?: "Unknown"

        holder.b.root.setOnClickListener { onClick(file) }
    }

    override fun getItemCount() = list.size
}
