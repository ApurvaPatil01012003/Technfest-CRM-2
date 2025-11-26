package com.technfest.technfestcrm.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.LeadAdapter
import com.technfest.technfestcrm.databinding.FragmentLeadsBinding
import com.technfest.technfestcrm.model.LeadResponseItem
import com.technfest.technfestcrm.network.RetrofitInstance
import com.technfest.technfestcrm.repository.GetWorkspacesRepository
import com.technfest.technfestcrm.repository.LeadRepository
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModel
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModelFactory
import com.technfest.technfestcrm.viewmodel.LeadViewModel
import com.technfest.technfestcrm.viewmodel.LeadViewModelFactory

class LeadsFragment : Fragment() {

    private var _binding: FragmentLeadsBinding? = null
    private val binding get() = _binding!!

    private lateinit var leadAdapter: LeadAdapter
    private lateinit var leadViewModel: LeadViewModel
    private lateinit var workspaceViewModel: GetWorkspacesViewModel

    private var fullLeadList = mutableListOf<LeadResponseItem>()
    private var leadList = mutableListOf<LeadResponseItem>()

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

        val wsRepo = GetWorkspacesRepository(RetrofitInstance.apiInterface)
        workspaceViewModel = ViewModelProvider(
            this,
            GetWorkspacesViewModelFactory(wsRepo)
        )[GetWorkspacesViewModel::class.java]
        val leadRepo = LeadRepository()
        leadViewModel = ViewModelProvider(
            this,
            LeadViewModelFactory(leadRepo)
        )[LeadViewModel::class.java]

        if (!workspaceNameFromBundle.isNullOrEmpty()) {
            binding.toolbarTitle.text = workspaceNameFromBundle

        } else if (!token.isNullOrEmpty() && workspaceId != null && workspaceId != -1) {

            workspaceViewModel
                .fetchWorkspaces("Bearer $token")
                .observe(viewLifecycleOwner) { response ->

                    if (response.isSuccessful) {
                        val workspace = response.body()?.find { it.id == workspaceId }
                        binding.toolbarTitle.text = workspace?.name ?: "Workspace"
                    } else {
                        binding.toolbarTitle.text = "Workspace"
                    }
                }
        }
        leadAdapter = LeadAdapter(leadList) { selectedLead ->

            val tokenStr = token ?: ""
            val wsId = workspaceId ?: 0

            val detailFragment =
                LeadDetailFragment.newInstance(selectedLead, tokenStr, wsId)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = leadAdapter

        setupSearchView(binding.searchView)

        if (!token.isNullOrEmpty() && workspaceId != null && workspaceId != -1) {
            leadViewModel.fetchLeads(token, workspaceId)
        }

        leadViewModel.leadsLiveData.observe(viewLifecycleOwner, Observer { data ->

            fullLeadList.clear()
            fullLeadList.addAll(data)

            leadList.clear()
            leadList.addAll(data)

            leadAdapter.notifyDataSetChanged()
        })


        binding.fabAddLead.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val workspaceId = prefs.getInt("workspaceId", -1)

            val fragment = AddNewLeadFragment()
            fragment.arguments = Bundle().apply {
                putString("token", token)
                putInt("workspaceId", workspaceId)
            }

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
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
        searchView.queryHint = "Search leads..."
        searchView.isIconified = false
        searchView.clearFocus()

        val searchEditText =
            searchView.findViewById<EditText>(androidx.appcompat.R.id.search_src_text)
        searchEditText.hint = "Search leads..."
        searchEditText.setHintTextColor(Color.GRAY)
        searchEditText.setTextColor(Color.BLACK)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?) = false

            override fun onQueryTextChange(newText: String?): Boolean {
                val text = newText?.trim()?.lowercase() ?: ""

                leadList.clear()
                leadList.addAll(
                    if (text.isEmpty()) fullLeadList
                    else fullLeadList.filter { lead ->
                        lead.fullName.orEmpty().lowercase().contains(text) ||
                                lead.mobile.orEmpty().lowercase().contains(text) ||
                                lead.sourceDetails.orEmpty().lowercase().contains(text)
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

    private fun getInitials(name: String): String {
        return name.split(" ")
            .filter { it.isNotEmpty() }
            .map { it[0].uppercaseChar() }
            .joinToString("")
    }

}
