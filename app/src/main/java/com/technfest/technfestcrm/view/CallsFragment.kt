package com.technfest.technfestcrm.view

import android.os.Bundle
import android.telecom.Call
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.CallsAdapter
import com.technfest.technfestcrm.databinding.FragmentCallsBinding
import com.technfest.technfestcrm.model.Calls

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

        // Dummy data
        allCalls = listOf(
            Calls("Abc", "Outgoing Answered", "0909090909", "Today", "11:00 AM", "3 min", "", "", "", "", "A"),
            Calls("Pqr", "Outgoing Unanswered", "0909090909", "Yesterday", "11:00 AM", "3 min", "", "", "", "", "P"),
            Calls("Ankita", "Missed", "9322779404", "Yesterday", "2:17 PM", "12 sec", "", "", "", "", "A"),
            Calls("Ac", "Incoming", "0909790909", "Today", "11:00 AM", "3 min", "", "", "", "", "A"),
            Calls("Apurva", "Outgoing Answered", "7367896543", "10 oct", "11:00 AM", "3 min", "", "", "", "", "A"),
            Calls("Pqr", "Outgoing Unanswered", "0909090909", "Yesterday", "11:00 AM", "3 min", "", "", "", "", "P"),
            Calls("Ankita", "Missed", "9322779404", "Yesterday", "2:17 PM", "12 sec", "", "", "", "", "A"),
            Calls("Ac", "Incoming", "0909790909", "Today", "11:00 AM", "3 min", "", "", "", "", "A"),
            Calls("Abc", "Outgoing Answered", "0909090909", "Today", "11:00 AM", "3 min", "", "", "", "", "A"),
            Calls("Pqr", "Outgoing Unanswered", "0909090909", "Yesterday", "11:00 AM", "3 min", "", "", "", "", "P"),
            Calls("Ankita", "Missed", "9322779404", "Yesterday", "2:17 PM", "12 sec", "", "", "", "", "A"),
            Calls("Ac", "Incoming", "0909790909", "Today", "11:00 AM", "3 min", "", "", "", "", "A")
        )

        callAdapter = CallsAdapter(allCalls)

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
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        searchView.queryHint = "Search calls..."

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {

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

}
