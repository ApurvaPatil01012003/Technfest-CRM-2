package com.technfest.technfestcrm.view

import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.HotLeadAdapter
import com.technfest.technfestcrm.databinding.FragmentHomeBinding
import com.technfest.technfestcrm.model.Calls
import com.technfest.technfestcrm.model.HotLead

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: HotLeadAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.leadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val sampleLeads = listOf(
            HotLead("Amit Sharma", "Billing software demo", "demo_scheduled"),
            HotLead("Kunal Dresses", "Follow-up on ERP quote", "followup"),
            HotLead("VRP Sons", "Price negotiation", "connected")
        )


        adapter = HotLeadAdapter(sampleLeads)
        binding.leadsRecyclerView.adapter = adapter


        binding.addNewLead.setOnClickListener {
            loadFragment(AddNewLeadFragment())
        }

        binding.myLead.setOnClickListener {
            loadFragment(LeadsFragment())
        }


        binding.task.setOnClickListener {
            loadFragment(TaskFragment())
        }

        binding.callsCampaign.setOnClickListener {
            loadFragment(CallsCampaignFragment())
        }
        binding.calls.setOnClickListener {
            loadFragment(CallsFragment())
        }
        binding.report.setOnClickListener {
            loadFragment(ReportFragment())
        }
        binding.userInitialCircle.setOnClickListener {
            loadFragment(ProfileFragment())
        }
        return binding.root
    }


    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }



}
