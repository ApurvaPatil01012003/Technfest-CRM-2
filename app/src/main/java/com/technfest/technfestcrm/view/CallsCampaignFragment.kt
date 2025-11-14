package com.technfest.technfestcrm.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.adapter.CampaignAdapter
import com.technfest.technfestcrm.databinding.FragmentCallsCampaignBinding
import com.technfest.technfestcrm.model.Campaign
import androidx.core.graphics.toColorInt

class CallsCampaignFragment : Fragment() {

    private var _binding: FragmentCallsCampaignBinding? = null
    private val binding get() = _binding!!

    private lateinit var campaignAdapter: CampaignAdapter
    private lateinit var allCampaigns: List<Campaign>

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallsCampaignBinding.inflate(inflater, container, false)

        // Spinner setup
        val filters = listOf("Select filter", "Active", "Pause", "Completed", "High ROI", "Low CPL")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnFilters.adapter = adapter

        // All campaigns list
        allCampaigns = listOf(
            Campaign("AAA", "Call", "Active", "ROI : medium", "01 Nov 2025 → Ongoing", "Sagar", "₹8,000"," ₹3,100", "24 leads"," ₹129"),
            Campaign("BBB", "WhatsApp", "Active", "ROI : medium", "01 Nov 2025 → Ongoing", "Sagar", "₹8,000","₹3,100", "30 leads"," ₹110"),
            Campaign("CCC", "Call", "Pause", "ROI : low", "25 Oct 2025 → 30 Oct 2025", "Amit", "₹5,000","₹1,200", "10 leads"," ₹120")
        )

        // RecyclerView setup
        campaignAdapter = CampaignAdapter(allCampaigns)
        binding.campaignsRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.campaignsRecyclerview.adapter = campaignAdapter

        setupFilterButtons()

        return binding.root
    }

    private fun setupFilterButtons() {
        binding.tvAll.setOnClickListener {
            campaignAdapter.updateData(allCampaigns)
            highlightSelected("all")
        }

        binding.tvCall.setOnClickListener {
            val filtered = allCampaigns.filter { it.type.equals("Call", ignoreCase = true) }
            campaignAdapter.updateData(filtered)
            highlightSelected("call")
        }

        binding.tvWhatsApp.setOnClickListener {
            val filtered = allCampaigns.filter { it.type.equals("WhatsApp", ignoreCase = true) }
            campaignAdapter.updateData(filtered)
            highlightSelected("whatsapp")
        }
    }

    private fun highlightSelected(selected: String) {
        val activeColor = "#EDEBFF".toColorInt()
        val defaultColor = requireContext().getColor(android.R.color.transparent)

        binding.tvAll.setBackgroundColor(if (selected == "all") activeColor else defaultColor)
        binding.tvCall.setBackgroundColor(if (selected == "call") activeColor else defaultColor)
        binding.tvWhatsApp.setBackgroundColor(if (selected == "whatsapp") activeColor else defaultColor)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}
