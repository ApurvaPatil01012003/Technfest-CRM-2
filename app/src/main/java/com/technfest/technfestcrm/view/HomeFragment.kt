package com.technfest.technfestcrm.view

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.HotLeadAdapter
import com.technfest.technfestcrm.databinding.FragmentHomeBinding
import com.technfest.technfestcrm.model.HotLead
import com.technfest.technfestcrm.network.RetrofitInstance
import com.technfest.technfestcrm.repository.GetWorkspacesRepository
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModel
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModelFactory

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private val PREFS_NAME = "MyPrefs"
    private val FULLNAME_KEY = "FullName"
    private val WORKSPACE_NAME_KEY = "WorkspaceName"

    private lateinit var viewModel: GetWorkspacesViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.leadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        // Load session from SharedPreferences
        val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val workspaceId = prefs.getInt("workspaceId", -1)
        val fullName = prefs.getString("fullName", "User")
        val savedWorkspaceName = prefs.getString("workspaceName", null)

        // Display name & initials
        binding.userName.text = fullName
        binding.userInitialCircle.text = getInitials(fullName ?: "")

        // Display saved workspace name if exists
        if (!savedWorkspaceName.isNullOrEmpty()) {
            binding.toolbarTitle.text = savedWorkspaceName
        }

        // Setup ViewModel
        val repository = GetWorkspacesRepository(RetrofitInstance.apiInterface)
        viewModel = ViewModelProvider(
            this,
            GetWorkspacesViewModelFactory(repository)
        ).get(GetWorkspacesViewModel::class.java)

        if (!token.isNullOrEmpty() && workspaceId != -1 && savedWorkspaceName.isNullOrEmpty()) {
            viewModel.fetchWorkspaces("Bearer $token").observe(viewLifecycleOwner) { response ->
                if (response.isSuccessful) {
                    val workspace = response.body()?.find { it.id == workspaceId }
                    workspace?.let {
                        binding.toolbarTitle.text = it.name
                        prefs.edit().apply {
                            putString("workspaceName", it.name)
                            apply()
                        }

                    }
                } else {
                    Log.e("WorkspaceError", response.message().toString())
                }
            }
        }

        // Leads button
        binding.myLead.setOnClickListener {
            val f = LeadsFragment()
            val bundle = Bundle().apply {
                putString("Name", fullName)
                putString("WorkspaceName", binding.toolbarTitle.text.toString())
            }
            f.arguments = bundle
            loadFragment(f)
        }

        // Sample leads
        val sampleLeads = listOf(
            HotLead("Amit Sharma", "Billing software demo", "demo_scheduled"),
            HotLead("Kunal Dresses", "ERP Quote Followup", "followup"),
            HotLead("VRP Sons", "Negotiation", "connected")
        )
        binding.leadsRecyclerView.adapter = HotLeadAdapter(sampleLeads)

        // Navigation clicks
        binding.addNewLead.setOnClickListener { loadFragment(AddNewLeadFragment()) }
        binding.task.setOnClickListener { loadFragment(TaskFragment()) }
        binding.callsCampaign.setOnClickListener { loadFragment(CallsCampaignFragment()) }
        binding.calls.setOnClickListener { loadFragment(CallsFragment()) }
        binding.report.setOnClickListener { loadFragment(ReportFragment()) }
        binding.userInitialCircle.setOnClickListener { loadFragment(ProfileFragment()) }

        return binding.root
    }

    private fun saveFullName(fullName: String) {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(FULLNAME_KEY, fullName).apply()
    }

    private fun loadFullName(): String? {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(FULLNAME_KEY, null)
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun getInitials(fullName: String): String {
        return fullName.split(" ")
            .filter { it.isNotEmpty() }
            .joinToString("") { it.first().uppercase() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun saveWorkspaceName(name: String?) {
        if (name.isNullOrEmpty()) return
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        sharedPref.edit().putString(WORKSPACE_NAME_KEY, name).apply()
    }

    private fun loadWorkspaceName(): String? {
        val sharedPref = requireActivity().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return sharedPref.getString(WORKSPACE_NAME_KEY, null)
    }

}
