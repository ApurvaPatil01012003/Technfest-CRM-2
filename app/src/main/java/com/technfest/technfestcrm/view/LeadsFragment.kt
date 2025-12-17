package com.technfest.technfestcrm.view

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
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
import java.util.Locale


class LeadsFragment : Fragment() {
    private var selectedLeadId = -1

    private var _binding: FragmentLeadsBinding? = null
    private val binding get() = _binding!!

    private lateinit var leadAdapter: LeadAdapter
    private lateinit var leadViewModel: LeadViewModel
    private lateinit var workspaceViewModel: GetWorkspacesViewModel

    private var fullLeadList = mutableListOf<LeadResponseItem>()
    private var leadList = mutableListOf<LeadResponseItem>()
    private fun String?.safeLower() = this?.trim()?.lowercase() ?: ""
    private fun String?.safeString() = this ?: ""
    fun Int?.safeInt(): Int {
        return this ?: 0
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentLeadsBinding.inflate(inflater, container, false)
        selectedLeadId = arguments?.getInt("leadId", -1) ?: -1

        val fullName = arguments?.getString("Name")
        val token = arguments?.getString("Token")
        val workspaceId = arguments?.getInt("WorkspaceId", -1)
        val workspaceNameFromBundle = arguments?.getString("WorkspaceName")

//        binding.userName.text = fullName
//        binding.userInitialCircle.text = getInitials(fullName ?: "")

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

//        if (!workspaceNameFromBundle.isNullOrEmpty()) {
//          //  binding.toolbarTitle.text = workspaceNameFromBundle
//
//        } else if (!token.isNullOrEmpty() && workspaceId != null && workspaceId != -1) {
//
//            workspaceViewModel
//                .fetchWorkspaces("Bearer $token")
//                .observe(viewLifecycleOwner) { response ->
//
////                    if (response.isSuccessful) {
////                        val workspace = response.body()?.find { it.id == workspaceId }
////                        binding.toolbarTitle.text = workspace?.name ?: "Workspace"
////                    } else {
////                        binding.toolbarTitle.text = "Workspace"
////                    }
//                }
//        }
        leadAdapter = LeadAdapter(leadList) { selectedLead ->

            val tokenStr = token ?: ""
            val wsId = workspaceId ?: 0
            val ownerUserId = selectedLead.ownerUserId ?: 0
            val campaignId = selectedLead.campaignId ?:0

            val detailFragment =
                LeadDetailFragment.newInstance( selectedLead,
                    tokenStr,
                    wsId,
                    selectedLead.campaignCategoryId ?: 0,
                    ownerUserId ,
                   campaignId)

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = leadAdapter

        setupFilters()
        updateSelected(binding.filterAll)


        if (!token.isNullOrEmpty() && workspaceId != null && workspaceId != -1) {
            leadViewModel.fetchLeads(token, workspaceId)
        }

        leadViewModel.leadsLiveData.observe(viewLifecycleOwner) { data ->

            fullLeadList.clear()
            fullLeadList.addAll(data)

            leadList.clear()
            leadList.addAll(data)
            updateFilteredList(fullLeadList)
            //ownerUserId
            //campaignId
            //campaincodeN-> campaignCategoryId


            Log.d("LeadData", data.toString())


            binding.txtCountLead.text = "Leads Count: ${data.size}"

            if (selectedLeadId != -1) {
                val position = fullLeadList.indexOfFirst { it.id == selectedLeadId }

                if (position != -1) {
                    binding.leadRecyclerView.post {
                        binding.leadRecyclerView.scrollToPosition(position)
                        leadAdapter.setSelectedLead(selectedLeadId)
                    }
                }
            }
            saveLeadCache(requireContext(), data)

        }



        binding.fabAddLead.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val workspaceId = prefs.getInt("workspaceId", -1)
            val userId = prefs.getInt("userId", 0)

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


//        binding.userInitialCircle.setOnClickListener {
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, ProfileFragment())
//                .addToBackStack(null)
//                .commit()
//        }



        return binding.root
    }

//    override fun onResume() {
//        super.onResume()
//
//        (activity as? MainActivity)?.setToolbar(
//            title = "Leads",
//            subtitle = "Today's summary & follow-ups"
//        )
//    }
//    override fun onPause() {
//        super.onPause()
//        (activity as? MainActivity)?.setToolbar("Technfest Workspace", "Today's summary & follow-ups")
//    }


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

    private fun setupFilters() {

        // All Leads
        binding.filterAll.setOnClickListener {
            updateSelected(binding.filterAll)
            updateFilteredList(fullLeadList)
        }

//        binding.filterMyLead.setOnClickListener {
//            updateSelected(binding.filterMyLead)
//
//            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
//            val currentUserId = prefs.getInt("userId", -1)
//
//            val filtered = fullLeadList.filter {
//                it.ownerUserId.safeInt() == currentUserId
//            }
//
//            updateFilteredList(filtered)
//        }

        binding.filterNew.setOnClickListener {
            updateSelected(binding.filterNew)

            val filtered = fullLeadList.filter {
                it.status.safeLower() == "new"
            }

            updateFilteredList(filtered)
        }

        // Demo Status
        binding.filterDemo.setOnClickListener {
            updateSelected(binding.filterDemo)

            val filtered = fullLeadList.filter {
                it.stage?.equals("Demo Scheduled") == true
            }

            updateFilteredList(filtered)
        }

        // High Priority
        binding.filterHigh.setOnClickListener {
            updateSelected(binding.filterHigh)

            val filtered = fullLeadList.filter {
                it.priority.safeLower() == "high"
            }

            updateFilteredList(filtered)
        }
    }

    private fun updateSelected(selected: TextView) {

        val allButtons = listOf(
            binding.filterAll,
            binding.filterMyLead,
            binding.filterNew,
            binding.filterDemo,
            binding.filterHigh
        )

        allButtons.forEach {
            it.setBackgroundResource(R.drawable.textview_bg)
            it.setTextColor(requireContext().getColor(R.color.black))
        }

        selected.setBackgroundResource(R.drawable.selected_campaign_bg)
        selected.setTextColor(requireContext().getColor(R.color.white))
    }
    private fun updateFilteredList(filtered: List<LeadResponseItem>) {
        leadList.clear()
        leadList.addAll(filtered)
        leadAdapter.notifyDataSetChanged()
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.clear()
        inflater.inflate(R.menu.menu_search, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as androidx.appcompat.widget.SearchView

        searchView.queryHint = "Search leads..."

        searchView.setOnQueryTextListener(object :
            androidx.appcompat.widget.SearchView.OnQueryTextListener {

            override fun onQueryTextSubmit(query: String?): Boolean {
                filterLeads(query.orEmpty())
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterLeads(newText.orEmpty())
                return true
            }
        })

        super.onCreateOptionsMenu(menu, inflater)
    }
    private fun filterLeads(query: String) {

        if (query.isBlank()) {
            leadList.clear()
            leadList.addAll(fullLeadList)
            leadAdapter.notifyDataSetChanged()
            return
        }

        val filtered = fullLeadList.filter { lead ->
            lead.fullName.orEmpty().contains(query, ignoreCase = true) ||
                    lead.mobile.orEmpty().contains(query) ||
                    lead.sourceDetails.orEmpty().contains(query, ignoreCase = true)
        }

        leadList.clear()
        leadList.addAll(filtered)
        leadAdapter.notifyDataSetChanged()
    }
    private fun saveLeadCache(ctx: Context, leads: List<LeadResponseItem>) {
        val map = HashMap<String, LeadCacheItem>()

        for (l in leads) {
            val raw = l.mobile ?: continue
            val e164 = normalizeForCompare(raw)
            if (e164.isBlank()) continue

            map[e164] = LeadCacheItem(
                id = l.id,
                name = l.fullName ?: "",
                campaignId = l.campaignId ?: 0,
                campaignCode =  "",
                customerNumber = raw
            )
        }

        val json = com.google.gson.Gson().toJson(map)
        ctx.getSharedPreferences("LeadCache", Context.MODE_PRIVATE)
            .edit()
            .putString("lead_map", json)
            .apply()
    }

    data class LeadCacheItem(
        val id: Int,
        val name: String,
        val campaignId: Int,
        val campaignCode: String,
        val customerNumber: String
    )

    private fun normalizeForCompare(number: String?, defaultRegion: String = "IN"): String {
        if (number.isNullOrBlank()) return ""
        return try {
            val phoneUtil = com.google.i18n.phonenumbers.PhoneNumberUtil.getInstance()
            val cleaned = number.replace("[^0-9+]".toRegex(), "")
            val region = if (cleaned.startsWith("+")) null else defaultRegion
            val proto = phoneUtil.parse(cleaned, region)
            phoneUtil.format(proto, com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            val digits = number.filter { it.isDigit() }
            if (digits.length > 10) digits.takeLast(10) else digits
        }
    }


}
