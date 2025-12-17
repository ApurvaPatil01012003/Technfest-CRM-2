package com.technfest.technfestcrm.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.LeadAdapter
import com.technfest.technfestcrm.databinding.FragmentCallsCampaignDetailBinding
import com.technfest.technfestcrm.model.LeadResponseItem

class CallsCampaignDetailFragment : Fragment() {
    private var _binding: FragmentCallsCampaignDetailBinding? = null
    private val binding get() = _binding!!

    private var campaignId: String? = null
    private lateinit var leadsAdapter: LeadAdapter
    private var fullLeadList = mutableListOf<LeadResponseItem>()
    private var leadList = mutableListOf<LeadResponseItem>()
    private val manualLeads = mutableListOf(
        LeadResponseItem(
            campaignCategoryId = 1,
            campaignCategoryName = "Category A",
            campaignId = 101,
            campaignName = "Campaign 1",
            company = "ABC Pvt Ltd",
            completedAt = "2025-12-15",
            createdAt = "2025-12-15",
            email = "amit@example.com",
            followupStatus = "new",
            fullName = "Amit Sharma",
            id = 1,
            leadRequirement = "no",
            location = "Delhi",
            mobile = "911111111111",
            nextFollowupAt = "2025-12-18",
            notes = "Important lead",
            ownerName = "Ravi Kumar",
            ownerUserId = 1,
            priority = "High",
            rescheduledAt = "",
            source = "Referral",
            sourceDetails = "Friend",
            stage = "Demo Scheduled",
            status = "Active",
            teamName = "Team Alpha",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W1",
            workspaceId = 1,
            workspaceName = "Workspace 1"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt = "2025-12-15",
            createdAt = "2025-12-15",
            email = "sneha@example.com",
            followupStatus = "new",
            fullName = "Sneha Singh",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Ravi Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt = "2025-12-15",
            createdAt = "2025-12-15",
            email = "snea@example.com",
            followupStatus = "new",
            fullName = "Ankita Gholap",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222229",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Ravi Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt = "2025-12-15",
            createdAt = "2025-12-15",
            email = "john@example.com",
            followupStatus = "new",
            fullName = "John Singh",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Ravi Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt = "2025-12-15",
            createdAt = "2025-12-15",
            email = "sneha@example.com",
            followupStatus = "new",
            fullName = "Sneha Singh",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Rohan Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt = "2025-12-15",
            createdAt = "2025-12-15",
            email = "sneha@example.com",
            followupStatus = "new",
            fullName = "Riya",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Ravi Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt ="2025-12-15",
            createdAt = "2025-12-15",
            email = "sneha@example.com",
            followupStatus = "new",
            fullName = "Sneha Singh",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Pooja patil",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt ="2025-12-15",
            createdAt = "2025-12-15",
            email = "sneha@example.com",
            followupStatus = "new",
            fullName = "Ashish Gaikwad",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Ravi Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        ),
        LeadResponseItem(
            campaignCategoryId = 2,
            campaignCategoryName = "Category B",
            campaignId = 102,
            campaignName = "Campaign 2",
            company = "XYZ Ltd",
            completedAt ="2025-12-15",
            createdAt = "2025-12-15",
            email = "sneha@example.com",
            followupStatus = "new",
            fullName = "Apurva Patil",
            id = 2,
            leadRequirement = "",
            location = "Mumbai",
            mobile = "9222222222",
            nextFollowupAt = "",
            notes = "Follow up after 2 days",
            ownerName = "Ravi Kumar",
            ownerUserId = 2,
            priority = "Medium",
            rescheduledAt = "",
            source = "Website",
            sourceDetails = "Contact Form",
            stage = "Initial Contact",
            status = "Inactive",
            teamName = "Team Beta",
            updatedAt = "2025-12-15",
            whatsappSentAt = "",
            workspaceCode = "W2",
            workspaceId = 2,
            workspaceName = "Workspace 2"
        )

    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

            setHasOptionsMenu(true)
        campaignId = arguments?.getString("campaignId")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding = FragmentCallsCampaignDetailBinding.inflate(inflater, container, false)

        // Step 1: Initialize fullLeadList with manual leads
        fullLeadList.clear()
        fullLeadList.addAll(manualLeads)

        // Step 2: Initialize leadList (current displayed list)
        leadList.clear()
        leadList.addAll(manualLeads)

        // Step 3: Setup adapter with leadList
        leadsAdapter = LeadAdapter(leadList) { lead ->
            Toast.makeText(requireContext(), "Clicked: ${lead.fullName}", Toast.LENGTH_SHORT).show()
        }

        binding.leadsRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.leadsRecyclerview.adapter = leadsAdapter

        return binding.root
    }

    private fun filterLeads(query: String) {
        if (query.isBlank()) {
            leadList.clear()
            leadList.addAll(fullLeadList)
        } else {
            val filtered = fullLeadList.filter { lead ->
                lead.fullName.orEmpty().contains(query, ignoreCase = true) ||
                        lead.mobile.orEmpty().contains(query) ||
                        lead.sourceDetails.orEmpty().contains(query, ignoreCase = true)
            }
            leadList.clear()
            leadList.addAll(filtered)
        }

        leadsAdapter.notifyDataSetChanged()
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

}