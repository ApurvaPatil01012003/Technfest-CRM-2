package com.technfest.technfestcrm.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.CallsAdapter
import com.technfest.technfestcrm.databinding.FragmentSimBinding
import com.technfest.technfestcrm.model.Calls
import com.technfest.technfestcrm.model.RecentCallItem
import com.technfest.technfestcrm.utils.SimSyncStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimFragment : Fragment() {

    private var _binding: FragmentSimBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentSimBinding.inflate(inflater, container, false)

        val simName = arguments?.getString("simName")
        val simNumber = arguments?.getString("simNumber")
        val slotIndex = arguments?.getInt("slotIndex") ?: -1
        val subId = arguments?.getInt("subId") ?: -1

        binding.tvSimName.text = simName ?: "SIM"
        binding.tvSimNumber.text = simNumber ?: "Number not available"

        binding.callRecyclerview.layoutManager = LinearLayoutManager(requireContext())

        val list = loadLast10CallsForThisSim(subId)

        binding.callRecyclerview.adapter = CallsAdapter(list) { selectedCall ->
            val fragment = LeadDetailFragment().apply {
                arguments = Bundle().apply {
                    putInt("leadId", selectedCall.leadId)
                    putString("mobile", selectedCall.number)
                    putString("name", selectedCall.name)
                }
            }
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack("LeadDetails")
                .commit()
        }

        val savedList = SimSyncStore.getAll(requireContext())
        val saved = savedList.firstOrNull { it.subId == subId }
        val isSynced = saved?.isSynced == true
        setStatusUI(isSynced)

        binding.switchStatus.setOnCheckedChangeListener { _, isChecked ->

            val current = SimSyncStore.getAll(requireContext()).toMutableList()
            val idx = current.indexOfFirst { it.subId == subId }

            if (idx >= 0) {
                current[idx] = current[idx].copy(isSynced = isChecked)
            } else {
                current.add(
                    SimSyncStore.SyncedSim(
                        subId = subId,
                        slotIndex = slotIndex,
                        displayName = simName ?: "SIM",
                        number = simNumber,
                        isSynced = isChecked
                    )
                )
            }

            SimSyncStore.saveAll(requireContext(), current)
            setStatusUI(isChecked)
        }

        Log.d("SIM_FRAG", "simName=$simName simNumber=$simNumber subId=$subId slot=$slotIndex")

        return binding.root
    }

    private fun setStatusUI(isSynced: Boolean) {
        binding.switchStatus.isChecked = isSynced

        if (isSynced) {

            binding.switchStatus.thumbTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#CDA6D3")
                )
            binding.switchStatus.trackTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#CDA6D3")
                )
        } else {
            binding.switchStatus.thumbTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#9E9E9E")
                )
            binding.switchStatus.trackTintList =
                android.content.res.ColorStateList.valueOf(
                    android.graphics.Color.parseColor("#E0E0E0")
                )
        }

        binding.tvStatus.text = if (isSynced) "Active" else "Deactive"
        binding.tvStatus.setBackgroundResource(
            if (isSynced) R.drawable.selected_campaign_bg else R.drawable.unselected_campaign_bg
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadLast10CallsForThisSim(subId: Int): List<Calls> {

        val prefs = requireContext().getSharedPreferences("RecentCallsStore", Context.MODE_PRIVATE)
        val json = prefs.getString("recent_calls", null) ?: return emptyList()

        val type = object : TypeToken<List<RecentCallItem>>() {}.type
        val all: List<RecentCallItem> = try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }

        // ✅ filter by SIM (your model uses usedSubId)
        val onlyThisSim = all.filter { (it.usedSubId ?: -1) == subId }

        // ✅ only lead calls (optional: remove unknown numbers)
        val leadOnly = onlyThisSim.filter { it.leadId > 0 }

        // ✅ newest first and take 10
        val top10 = leadOnly.sortedByDescending { it.timestampMs }.take(10)

        Log.d("SIM_FRAG", "total=${all.size} sim=${onlyThisSim.size} lead=${leadOnly.size} subId=$subId")

        return top10.map { c ->
            Calls(
                name = c.leadName.ifBlank { "Unknown" },
                CallType = c.statusLabel,
                number = c.number,
                day = formatDayLabel(c.timestampMs),
                time = formatTimeLabel(c.timestampMs),
                duration = formatDurationLabel(c.durationSec),
                note = "",
                img = "",
                a = "",
                b = "",
                c = "",
                initial = (c.leadName.firstOrNull()?.toString() ?: "U"),
                leadId = c.leadId
            )
        }
    }



    // ✅ INSIDE fragment, so requireContext works
    private fun findLeadByNumber(number: String): Pair<Int, String?>? {
        val leads = com.technfest.technfestcrm.localdatamanager.LocalLeadManager
            .getLeads(requireContext()) // ✅ only context

        val callKey = normalizeKey10(number)

        for (l in leads) {
            val leadKey = normalizeKey10(l.mobile)
            if (leadKey.isNotBlank() && leadKey == callKey) {
                val name = l.fullName.ifBlank { "Unknown" }
                return Pair(l.id, name)
            }
        }
        return null
    }

    private fun normalizeKey10(num: String?): String {
        if (num.isNullOrBlank()) return ""
        val digits = num.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }
}

// ✅ keep these helpers outside OR inside, both ok (no context use)
private fun formatTimeLabel(ts: Long): String {
    val sdf = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
    return sdf.format(Date(ts))
}

private fun formatDurationLabel(sec: Int): String {
    if (sec <= 0) return "0 sec"
    val m = sec / 60
    val s = sec % 60
    return if (m > 0) "${m} min ${s} sec" else "${s} sec"
}

private fun formatDayLabel(ts: Long): String {
    val cal = java.util.Calendar.getInstance()
    val today = java.util.Calendar.getInstance()
    cal.timeInMillis = ts

    val sdfDate = SimpleDateFormat("dd MMM", Locale.ENGLISH)

    return when {
        isSameDay(cal, today) -> "Today"
        isYesterday(cal) -> "Yesterday"
        else -> sdfDate.format(Date(ts))
    }
}

private fun isSameDay(a: java.util.Calendar, b: java.util.Calendar): Boolean {
    return a.get(java.util.Calendar.YEAR) == b.get(java.util.Calendar.YEAR) &&
            a.get(java.util.Calendar.DAY_OF_YEAR) == b.get(java.util.Calendar.DAY_OF_YEAR)
}

private fun isYesterday(cal: java.util.Calendar): Boolean {
    val y = java.util.Calendar.getInstance()
    y.add(java.util.Calendar.DAY_OF_YEAR, -1)
    return isSameDay(cal, y)
}
