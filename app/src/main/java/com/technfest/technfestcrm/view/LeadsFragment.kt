package com.technfest.technfestcrm.view

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.LeadAdapter
import com.technfest.technfestcrm.databinding.FragmentLeadsBinding
import com.technfest.technfestcrm.localdatamanager.LocalLeadManager
import com.technfest.technfestcrm.localdatamanager.LocalLeadMapper
import com.technfest.technfestcrm.model.LeadResponseItem


class LeadsFragment : Fragment() {
    private var selectedLeadId = -1

    private var _binding: FragmentLeadsBinding? = null
    private val binding get() = _binding!!

    private lateinit var leadAdapter: LeadAdapter
//    private lateinit var leadViewModel: LeadViewModel
//    private lateinit var workspaceViewModel: GetWorkspacesViewModel

    private var fullLeadList = mutableListOf<LeadResponseItem>()
    private var leadList = mutableListOf<LeadResponseItem>()
    private fun String?.safeLower() = this?.trim()?.lowercase() ?: ""

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

//        val token = arguments?.getString("Token")
//        val workspaceId = arguments?.getInt("WorkspaceId", -1)


//        val wsRepo = GetWorkspacesRepository(RetrofitInstance.apiInterface)
//        workspaceViewModel = ViewModelProvider(
//            this,
//            GetWorkspacesViewModelFactory(wsRepo)
//        )[GetWorkspacesViewModel::class.java]
//        val leadRepo = LeadRepository()
//        leadViewModel = ViewModelProvider(
//            this,
//            LeadViewModelFactory(leadRepo)
//        )[LeadViewModel::class.java]


//        leadAdapter = LeadAdapter(leadList) { selectedLead ->
//
////            val tokenStr = token ?: ""
////            val wsId = workspaceId ?: 0
//            val ownerUserId = selectedLead.ownerUserId ?: 0
//            val campaignId = selectedLead.campaignId ?:0
//
//
//
//            parentFragmentManager.beginTransaction()
//                .replace(R.id.fragmentContainer, LeadDetailFragment())
//                .addToBackStack(null)
//                .commit()
//        }


        leadAdapter = LeadAdapter(leadList) { selectedLead ->

            val fragment = LeadDetailFragment.newInstance(
                lead = selectedLead,
                token = "",          // local mode → empty
                workspaceId = 0,
                campaignCategoryId = 0,
                ownerUserId = selectedLead.ownerUserId ?: 0,
                campaignId = selectedLead.campaignId ?: 0
            )

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }


        binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = leadAdapter
        loadLocalLeads()

        setupFilters()
        updateSelected(binding.filterAll)

        binding.fabAddLead.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AddNewLeadFragment())
                .addToBackStack(null)
                .commit()
        }



        return binding.root
    }

    override fun onResume() {
        super.onResume()
        loadLocalLeads()
        if (selectedLeadId != -1) {
            val position = fullLeadList.indexOfFirst { it.id == selectedLeadId }
            if (position != -1) {
                binding.leadRecyclerView.post {
                    binding.leadRecyclerView.scrollToPosition(position)
                    leadAdapter.setSelectedLead(selectedLeadId)
                }
            }
        }




    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }


    private fun setupFilters() {

        // All Leads
        binding.filterAll.setOnClickListener {
            updateSelected(binding.filterAll)
            updateFilteredList(fullLeadList)
        }

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
binding.filterMyLead.setOnClickListener {
    updateSelected(binding.filterMyLead)

    val filtered = fullLeadList.filter {
        it.status?.equals("Contacted") == true
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
    private fun getEditedLeadName(context: Context, number: String?): String? {
        if (number.isNullOrBlank()) return null
        val e164 = normalizeForCompare(number)
        val prefs = context.getSharedPreferences("EditedLeadNames", Context.MODE_PRIVATE)
        return prefs.getString(e164, null)
    }

    private fun loadLocalLeads() {

        val localLeads = LocalLeadManager.getLeads(requireContext())

        fullLeadList.clear()

        localLeads.forEachIndexed { index, leadRequest ->
            val responseItem = LocalLeadMapper.toResponse(leadRequest, index + 1)

            val edited = com.technfest.technfestcrm.utils.EditedLeadNameStore.get(requireContext(), responseItem.mobile)
            if (!edited.isNullOrBlank()) {
                responseItem.fullName = edited
            }

            fullLeadList.add(responseItem)
        }


        leadList.clear()
        leadList.addAll(fullLeadList)
        leadAdapter.notifyDataSetChanged()

        binding.txtCountLead.text = "Leads Count: ${fullLeadList.size}"
    }

    private val nameUpdateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            loadLocalLeads()
        }
    }

    override fun onStart() {
        super.onStart()
        val filter = IntentFilter("com.technfest.technfestcrm.LEAD_NAME_UPDATED")

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            requireContext().registerReceiver(nameUpdateReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            androidx.core.content.ContextCompat.registerReceiver(
                requireContext(),
                nameUpdateReceiver,
                filter,
                androidx.core.content.ContextCompat.RECEIVER_NOT_EXPORTED
            )
        }
    }

    override fun onStop() {
        super.onStop()
        try { requireContext().unregisterReceiver(nameUpdateReceiver) } catch (_: Exception) {}
    }

}
