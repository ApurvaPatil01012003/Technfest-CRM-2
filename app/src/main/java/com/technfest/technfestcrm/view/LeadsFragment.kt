package com.technfest.technfestcrm.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.LeadAdapter
import com.technfest.technfestcrm.databinding.FragmentLeadsBinding
import com.technfest.technfestcrm.model.Lead
import com.technfest.technfestcrm.network.RetrofitInstance
import com.technfest.technfestcrm.repository.GetWorkspacesRepository
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModel
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModelFactory

class LeadsFragment : Fragment() {

    private var _binding: FragmentLeadsBinding? = null
    private val binding get() = _binding!!

    private lateinit var leadAdapter: LeadAdapter
    private lateinit var leadList: MutableList<Lead>
    private lateinit var fullLeadList: MutableList<Lead>

    private lateinit var viewModel: GetWorkspacesViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLeadsBinding.inflate(inflater, container, false)

        val fullName = arguments?.getString("Name")
        val token = arguments?.getString("Token")
        val workspaceId = arguments?.getInt("WorkspaceId", -1)
        val workspaceNameFromBundle = arguments?.getString("WorkspaceName")

        binding.userName.text = fullName
        binding.userInitialCircle.text = getInitials(fullName ?: "")

        val repository = GetWorkspacesRepository(RetrofitInstance.apiInterface)
        viewModel = ViewModelProvider(this, GetWorkspacesViewModelFactory(repository))
            .get(GetWorkspacesViewModel::class.java)

        if (!workspaceNameFromBundle.isNullOrEmpty()) {

            binding.toolbarTitle.text = workspaceNameFromBundle
        } else if (!token.isNullOrEmpty() && workspaceId != null && workspaceId != -1) {
            viewModel.fetchWorkspaces("Bearer $token").observe(viewLifecycleOwner) { response ->
                if (response.isSuccessful) {
                    val workspace = response.body()?.find { it.id == workspaceId }
                    binding.toolbarTitle.text = workspace?.name ?: "Workspace"
                } else {
                    binding.toolbarTitle.text = "Workspace"
                }
            }
        } else {
            binding.toolbarTitle.text = "Workspace"
        }
        fullLeadList = mutableListOf(
            Lead("Amit Sharma", "New Delhi", "Facebook", "Follow up", "High", "Demo Scheduled", "TechCorp", "Rahul Verma"),
            Lead("Neha Singh", "Mumbai", "Instagram", "Call", "Medium", "Lead Generated", "SoftSolutions", "Priya Sharma"),
            Lead("Ravi Kumar", "Chennai", "LinkedIn", "Email", "Low", "Contacted", "Innovatech", "Amit Jain")
        )
        leadList = fullLeadList.toMutableList()

        leadAdapter = LeadAdapter(leadList) { selectedLead ->
            val detailFragment = LeadDetailFragment.newInstance(selectedLead)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = leadAdapter

        setupSearchView(binding.searchView)

        binding.fabAddLead.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddNewLeadFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.userInitialCircle.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, ProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
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

        val searchEditText = searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.hint = "Search by name, phone, city, company, source..."
        searchEditText.setHintTextColor(Color.GRAY)
        searchEditText.setTextColor(Color.BLACK)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                val text = newText?.trim()?.lowercase() ?: ""
                leadList.clear()
                leadList.addAll(
                    if (text.isEmpty()) fullLeadList
                    else fullLeadList.filter { lead ->
                        lead.name.lowercase().contains(text) ||
                                lead.location.lowercase().contains(text) ||
                                lead.company.lowercase().contains(text) ||
                                lead.Source.lowercase().contains(text) ||
                                lead.owner.lowercase().contains(text)
                    }
                )
                leadAdapter.notifyDataSetChanged()
                return true
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getInitials(fullName: String): String {
        return fullName.split(" ")
            .filter { it.isNotEmpty() }
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    }
}
