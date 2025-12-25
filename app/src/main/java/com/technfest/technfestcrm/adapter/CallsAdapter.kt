package com.technfest.technfestcrm.adapter

//import android.view.LayoutInflater
//import android.view.View
//import android.view.ViewGroup
//import android.widget.TextView
//import androidx.recyclerview.widget.RecyclerView
//import com.technfest.technfestcrm.R
//import com.technfest.technfestcrm.model.Calls
//
//class CallsAdapter(private var callsList: List<Calls>) :
//    RecyclerView.Adapter<CallsAdapter.CallsViewHolder>() {
//
//    class CallsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
//        val name: TextView = itemView.findViewById(R.id.tvName)
//        //val CallType: TextView = itemView.findViewById(R.id.tvCallType)
//       // val number: TextView = itemView.findViewById(R.id.tvNumber)
//        val time: TextView = itemView.findViewById(R.id.tvTime)
//        val day: TextView = itemView.findViewById(R.id.tvday)
//        val duration: TextView = itemView.findViewById(R.id.tvDuration)
//       // val assign_number: TextView = itemView.findViewById(R.id.tvAssignNumber)
//        //val note: TextView = itemView.findViewById(R.id.tvNoteAdded)
//        val initial: TextView = itemView.findViewById(R.id.tvInitials)
//    }
//
//    override fun onCreateViewHolder(
//        parent: ViewGroup,
//        viewType: Int
//    ): CallsViewHolder {
//        val view = LayoutInflater.from(parent.context).inflate(R.layout.calls_item, parent, false)
//        return CallsViewHolder(view)
//    }
//
//    override fun onBindViewHolder(
//        holder: CallsViewHolder,
//        position: Int
//    ) {
//        val call = callsList[position]
//        holder.name.text = call.name
//      //  holder.number.text = call.number
//       // holder.CallType.text = call.CallType
//        holder.time.text = call.time
//        //holder.day.text = call.day
//        holder.duration.text = call.duration
//       // holder.assign_number.text = call.assign_number
//       // holder.note.text = call.note
//    setAvatar(holder.initial, call.name)
//    }
//
//    override fun getItemCount(): Int = callsList.size
//    private val avatarBackgrounds = listOf(
//        R.drawable.gradient_circle_blue,
//        R.drawable.gradient_circle_green,
//        R.drawable.gradient_circle_orange,
//        R.drawable.gradient_circle,
//        R.drawable.gradient_circle_blue_cyan,
//        R.drawable.gradient_circle_purple_pink,
//        R.drawable.gradient_circle_red_orange
//
//    )
//
//    private fun setAvatar(textView: TextView, name: String?) {
//        if (name.isNullOrBlank()) {
//            textView.text = "?"
//            textView.setBackgroundResource(avatarBackgrounds[0])
//            return
//        }
//
//        val cleanName = name.trim().replace("\\s+".toRegex(), " ")
//        val parts = cleanName.split(" ")
//
//        val initials = when {
//            parts.size >= 2 -> {
//                "${parts[0][0]}${parts[1][0]}"
//            }
//            else -> {
//                "${parts[0][0]}"
//            }
//        }
//
//        textView.text = initials.uppercase()
//
//        // Telegram-style stable background
//        val index = kotlin.math.abs(cleanName.hashCode()) % avatarBackgrounds.size
//        textView.setBackgroundResource(avatarBackgrounds[index])
//    }
//    fun updateData(newList: List<Calls>) {
//        callsList = newList
//        notifyDataSetChanged()
//    }
//
//
//}




import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Calls

class CallsAdapter(private var callsList: List<Calls>,
                   private val onItemClick: (Calls) -> Unit) :

    RecyclerView.Adapter<CallsAdapter.CallsViewHolder>() {

    class CallsViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.tvName)
        val time: TextView = itemView.findViewById(R.id.tvTime)
        val day: TextView = itemView.findViewById(R.id.tvday)
        val duration: TextView = itemView.findViewById(R.id.tvDuration)
        val initial: TextView = itemView.findViewById(R.id.tvInitials)

        // ✅ add this
        val callTypeIcon: ImageView = itemView.findViewById(R.id.ivCallType)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CallsViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calls_item, parent, false)
        return CallsViewHolder(view)
    }

    override fun onBindViewHolder(holder: CallsViewHolder, position: Int) {
        val call = callsList[position]

        holder.name.text = call.name
        holder.time.text = call.time
        holder.day.text = call.day
        holder.duration.text = call.duration

        setAvatar(holder.initial, call.name)

        // ✅ bind icon + color
        bindCallTypeIcon(holder.callTypeIcon, call.CallType)
        holder.itemView.setOnClickListener { onItemClick(call) }
    }

    override fun getItemCount(): Int = callsList.size

    private fun bindCallTypeIcon(iv: ImageView, callType: String?) {
        val ctx = iv.context
        val label = callType?.lowercase().orEmpty()

        val isOutgoing = label.contains("outgoing")
        val isIncoming = label.contains("incoming")
        val isMissed = label.contains("missed")
        val isNotAnswered = label.contains("not answered") || label.contains("not picked") || label.contains("unanswered")

        val isAnswered = !(isMissed || isNotAnswered)

        // icon based on direction
        val iconRes = when {
            isOutgoing -> R.drawable.baseline_north_east_24
            isIncoming -> R.drawable.baseline_south_west_24
            else -> R.drawable.baseline_north_east_24 // fallback
        }

        // color based on status
        val color = when {
            isMissed || isNotAnswered -> android.graphics.Color.parseColor("#E53935") // red
            isAnswered -> android.graphics.Color.parseColor("#4BD151") // green
            else -> android.graphics.Color.parseColor("#E53935") // grey fallback
        }

        iv.setImageResource(iconRes)
        iv.imageTintList = ColorStateList.valueOf(color)
    }

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
            parts.size >= 2 -> "${parts[0][0]}${parts[1][0]}"
            else -> "${parts[0][0]}"
        }

        textView.text = initials.uppercase()
        val index = kotlin.math.abs(cleanName.hashCode()) % avatarBackgrounds.size
        textView.setBackgroundResource(avatarBackgrounds[index])
    }

    fun updateData(newList: List<Calls>) {
        callsList = newList
        notifyDataSetChanged()
    }

}
