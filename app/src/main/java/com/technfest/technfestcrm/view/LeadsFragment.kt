package com.technfest.technfestcrm.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.LeadAdapter
import com.technfest.technfestcrm.databinding.FragmentLeadsBinding
import com.technfest.technfestcrm.model.Lead

class LeadsFragment : Fragment() {

    private var _binding: FragmentLeadsBinding? = null
    private val binding get() = _binding!!

    private lateinit var leadAdapter: LeadAdapter
    private lateinit var leadList: MutableList<Lead>
    private lateinit var fullLeadList: MutableList<Lead>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLeadsBinding.inflate(inflater, container, false)

        fullLeadList = mutableListOf(
            Lead(
                name = "Amit Sharma",
                location = "New Delhi",
                Source = "Facebook",
                task = "Follow up",
                status = "High",
                stage = "Demo Scheduled",
                company = "TechCorp",
                owner = "Rahul Verma"
            ),
            Lead(
                name = "Neha Singh",
                location = "Mumbai",
                Source = "Instagram",
                task = "Call",
                status = "Medium",
                stage = "Lead Generated",
                company = "SoftSolutions",
                owner = "Priya Sharma"
            ),
            Lead(
                name = "Ravi Kumar",
                location = "Chennai",
                Source = "LinkedIn",
                task = "Email",
                status = "Low",
                stage = "Contacted",
                company = "Innovatech",
                owner = "Amit Jain"
            )
        )

        leadList = fullLeadList.toMutableList()

        // ✅ Setup RecyclerView
        leadAdapter = LeadAdapter(leadList) { selectedLead ->
            val detailFragment = LeadDetailFragment.newInstance(selectedLead)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = leadAdapter

        // ✅ Setup SearchView properly
        setupSearchView(binding.searchView)

        // ✅ FAB click to add new lead
        binding.fabAddLead.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddNewLeadFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar =
            view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
    private fun setupSearchView(searchView: SearchView) {
        searchView.queryHint = "Search by name, phone, city, company, source..."
        searchView.isIconified = false
        searchView.clearFocus()

        val searchEditText = searchView.findViewById<EditText>(
            androidx.appcompat.R.id.search_src_text
        )
        searchEditText.hint = "Search by name, phone, city, company, source..."
        searchEditText.setHintTextColor(Color.GRAY)
        searchEditText.setTextColor(Color.BLACK)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val text = newText?.trim()?.lowercase() ?: ""
                leadList.clear()

                if (text.isEmpty()) {
                    leadList.addAll(fullLeadList)
                } else {
                    leadList.addAll(
                        fullLeadList.filter { lead ->
                            lead.name.lowercase().contains(text) ||
                                    lead.location.lowercase().contains(text) ||
                                    lead.company.lowercase().contains(text) ||
                                    lead.Source.lowercase().contains(text) ||
                                    lead.owner.lowercase().contains(text)
                        }
                    )
                }

                leadAdapter.notifyDataSetChanged()
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
