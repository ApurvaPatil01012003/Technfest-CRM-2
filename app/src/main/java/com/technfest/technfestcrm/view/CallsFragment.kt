package com.technfest.technfestcrm.view

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.telecom.Call
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.CallsAdapter
import com.technfest.technfestcrm.databinding.FragmentCallsBinding
import com.technfest.technfestcrm.model.Calls
import com.technfest.technfestcrm.model.RecentCallItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CallsFragment : Fragment() {

    private var _binding: FragmentCallsBinding? = null
    private val binding get() = _binding!!

    private lateinit var callAdapter: CallsAdapter
    private lateinit var allCalls: List<Calls>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true) // enable toolbar menu
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCallsBinding.inflate(inflater, container, false)



        val allRecent = loadAllRecentCalls()
        updateCallCounts(allRecent)


        allCalls = loadLatestCallsPerLead()

        callAdapter = CallsAdapter(allCalls) { selectedCall ->
            openLeadDetails(selectedCall)
        }

        binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = callAdapter

        return binding.root
    }
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {

            R.id.filter_all -> applyFilter("All")
            R.id.filter_today -> applyFilter("Today")
            R.id.filter_missed -> applyFilter("Missed")
            R.id.filter_unknown -> applyFilter("Unknown")
            R.id.filter_not_logged -> applyFilter("Not Logged")

            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_calls, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "Search calls..."

        searchView.setOnQueryTextListener(object :
            SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                filterBySearch(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBySearch(newText.orEmpty())
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }
    private fun filterBySearch(query: String) {

        if (query.isBlank()) {
            callAdapter.updateData(allCalls)
            return
        }

        val filtered = allCalls.filter {
            it.name.contains(query, ignoreCase = true) ||
                    it.number.contains(query) ||
                    it.CallType.contains(query, ignoreCase = true)
        }

        callAdapter.updateData(filtered)
    }


    // 🔹 Filtering logic
    private fun applyFilter(type: String) {

        val filtered = when (type) {

            "Today" -> allCalls.filter {
                it.day.equals("Today", true)
            }

            "Missed" -> allCalls.filter {
                it.CallType.contains("Missed", true)
            }

            "Unknown" -> allCalls.filter {
                it.name.isBlank()
            }

            "Not Logged" -> allCalls.filter {
                it.note.isBlank()
            }

            else -> allCalls
        }

        callAdapter.updateData(filtered)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadLatestCallsPerLead(): List<Calls> {
        val prefs = requireContext().getSharedPreferences("RecentCallsStore", Context.MODE_PRIVATE)
        val json = prefs.getString("recent_calls", null) ?: return emptyList()

        val type = object : TypeToken<List<RecentCallItem>>() {}.type
        val all: List<RecentCallItem> = try {
            Gson().fromJson(json, type) ?: emptyList()
        } catch (_: Exception) { emptyList() }

        // ✅ newest first
        val sorted = all.sortedByDescending { it.timestampMs }

        // ✅ keep only FIRST (latest) per lead/number
        val seen = HashSet<String>()
        val uniqueLatest = ArrayList<RecentCallItem>()

        for (c in sorted) {
            val key = buildUniqueKey(c)  // leadId if available else number
            if (seen.add(key)) {
                uniqueLatest.add(c)
            }
        }

        // ✅ convert to your Calls model (adapter)
        return uniqueLatest.map { c ->
            Calls(
                name = c.leadName ?: "Unknown",
                CallType = c.statusLabel
                    ?: "",     // "Incoming", "Missed", etc (you already create)
                number = c.number ?: "",
                day = formatDayLabel(c.timestampMs),
                time = formatTimeLabel(c.timestampMs),
                duration = formatDurationLabel(c.durationSec),
                note = "",
                // keep remaining fields as you have in Calls() constructor:
                img = "",
                a = "",
                b = "",
                c = "",
                initial = (c.leadName?.firstOrNull()?.toString() ?: "U"),
                leadId = c.leadId

            )
        }
    }

    private fun buildUniqueKey(c: RecentCallItem): String {
        // ✅ Best: leadId first (stable). If leadId 0, fallback to normalized number.
        return if (c.leadId > 0) {
            "L:${c.leadId}"
        } else {
            "N:${normalizeNumberKey(c.number)}"
        }
    }

    private fun normalizeNumberKey(num: String?): String {
        if (num.isNullOrBlank()) return ""
        return num.replace("[^0-9+]".toRegex(), "").takeLast(12) // safe basic key
    }

    private fun formatDayLabel(ts: Long): String {
        val cal = Calendar.getInstance()
        val today = Calendar.getInstance()

        cal.timeInMillis = ts

        val sdfDate = SimpleDateFormat("dd MMM", Locale.ENGLISH)

        return when {
            isSameDay(cal, today) -> "Today"
            isYesterday(cal) -> "Yesterday"
            else -> sdfDate.format(Date(ts))
        }
    }

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

    private fun isSameDay(a: Calendar, b: Calendar): Boolean {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR) &&
                a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)
    }

    private fun isYesterday(cal: Calendar): Boolean {
        val y = Calendar.getInstance()
        y.add(Calendar.DAY_OF_YEAR, -1)
        return isSameDay(cal, y)
    }

    private val callsUpdateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {

            val allRecent = loadAllRecentCalls()
            updateCallCounts(allRecent)

            allCalls = loadLatestCallsPerLead()
            callAdapter.updateData(allCalls)
        }
    }


    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.technfest.technfestcrm.CALL_ACTIVITY_UPDATED")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(callsUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            ContextCompat.registerReceiver(requireContext(), callsUpdateReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }
    }

    override fun onStop() {
        super.onStop()
        try { requireContext().unregisterReceiver(callsUpdateReceiver) } catch (_: Exception) {}
    }

    private fun loadAllRecentCalls(): List<RecentCallItem> {
        val prefs = requireContext().getSharedPreferences("RecentCallsStore", Context.MODE_PRIVATE)
        val json = prefs.getString("recent_calls", null) ?: return emptyList()

        val type = object : TypeToken<List<RecentCallItem>>() {}.type
        return try {
            Gson().fromJson<List<RecentCallItem>>(json, type) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }
    private fun updateCallCounts(all: List<RecentCallItem>) {

        val total = all.size

//        val outgoing = all.count { c ->
//            c.statusLabel?.contains("Outgoing", ignoreCase = true) == true
//        }

        val outgoing = all.count { c ->
            val label = c.statusLabel.orEmpty()
            label.contains("Outgoing", true) ||
                    label.equals("Not answered", true)
        }

        // Incoming answered => Incoming + NOT Missed + duration > 0 (best signal)
        val incomingAnswered = all.count { c ->
            val label = c.statusLabel.orEmpty()
            label.contains("Incoming", true) &&
                    !label.contains("Missed", true) &&
                    (c.durationSec ?: 0) > 0
        }

        // Missed incoming
        val missed = all.count { c ->
            c.statusLabel?.contains("Missed", ignoreCase = true) == true
        }

        binding.txtTotalCalls.text = total.toString()
        binding.txtOutgoingCalls.text = outgoing.toString()
        binding.txtIncomingCalls.text = incomingAnswered.toString()
        binding.txtMissedCalls.text = missed.toString()
    }

    private fun openLeadDetails(call: Calls) {
        val fragment = LeadDetailFragment()

        fragment.arguments = Bundle().apply {
            putInt("leadId", call.leadId)
            putString("mobile", call.number)
            putString("name", call.name)

            // if you have them in CallsFragment args/sharedpref
            putString("token", arguments?.getString("token") ?: "")
            putInt("workspaceId", arguments?.getInt("workspaceId") ?: 0)

        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("LeadDetails")
            .commit()
    }



}
