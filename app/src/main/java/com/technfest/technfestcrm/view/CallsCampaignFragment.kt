package com.technfest.technfestcrm.view

import com.technfest.technfestcrm.R
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.adapter.CampaignAdapter
import com.technfest.technfestcrm.databinding.FragmentCallsCampaignBinding
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.model.CampaignResponseItem
import com.technfest.technfestcrm.repository.CampaignRepository
import com.technfest.technfestcrm.viewmodel.CampaignViewModelFactory
import com.technfest.technfestcrm.viewmodel.CampaignsViewModel

class CallsCampaignFragment : Fragment() {

    private var _binding: FragmentCallsCampaignBinding? = null
    private val binding get() = _binding!!

    private lateinit var campaignAdapter: CampaignAdapter
    private lateinit var allCampaigns: List<CampaignResponseItem>

    private lateinit var viewModel: CampaignsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCallsCampaignBinding.inflate(inflater, container, false)

        val token = arguments?.getString("token")
        val workspaceId = arguments?.getInt("workspaceId", -1) ?: -1

        Log.d("CAMPAIGN_FRAGMENT", "Received Token = $token")
        Log.d("CAMPAIGN_FRAGMENT", "Received WorkspaceId = $workspaceId")

        val filters = listOf("All", "Active", "Pause", "Completed", "High ROI", "Low CPL")
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, filters)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spnFilters.adapter = adapter

        binding.spnFilters.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selected = parent?.getItemAtPosition(position).toString()
                applyFilter(selected)
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }

        setupViewModel()
        setupRecyclerView()
        observeData()
        viewModel.fetchCategories(token.toString())

        viewModel.fetchCampaigns(token.toString(), workspaceId)

        return binding.root
    }

    private fun setupViewModel() {

        val repository = CampaignRepository()
        val factory = CampaignViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory).get(CampaignsViewModel::class.java)
    }

    private fun setupRecyclerView() {
        campaignAdapter = CampaignAdapter(emptyList())
        binding.campaignsRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.campaignsRecyclerview.adapter = campaignAdapter
    }

    private fun observeData() {

        viewModel.categoriesLiveData.observe(viewLifecycleOwner) { cats ->
            Log.d("API_CATEGORIES", "Categories received: ${cats?.size}")
        }

        viewModel.campaignList.observe(viewLifecycleOwner) { campaigns ->

            val categories = viewModel.categoriesLiveData.value ?: emptyList()

            allCampaigns = campaigns.map { apiItem ->
                val matchedCat =
                    categories.find { it.id == apiItem.campaignCategoryId }?.name
                        ?: apiItem.campaignCategoryName

                apiItem.campaignCategoryName = matchedCat
                apiItem
            }


            campaignAdapter.updateData(allCampaigns)
        }

        viewModel.error.observe(viewLifecycleOwner) { err ->
            Log.e("API_ERROR", "Error: $err")
        }
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

    private fun applyFilter(filter: String) {

        if (!::allCampaigns.isInitialized) {
            Log.w("FILTER", "Campaign list not loaded yet")
            return
        }

        Log.d("FILTER", "Applying filter = $filter")

        val filteredList = when (filter) {

            "All" -> allCampaigns

            "Active" -> allCampaigns.filter {
                it.status.equals("Active", ignoreCase = true)
            }

            "Pause" -> allCampaigns.filter {
                it.status.equals("Pause", ignoreCase = true)
            }

            "Completed" -> allCampaigns.filter {
                it.status.equals("Completed", ignoreCase = true)
            }

//            "High ROI" -> allCampaigns.sortedByDescending {
//                it.roi?.toDoubleOrNull() ?: 0.0
//            }

            "Low CPL" -> allCampaigns.sortedBy {
                (it.cpl as? Number)?.toDouble() ?: Double.MAX_VALUE

            }

            else -> allCampaigns
        }


        campaignAdapter.updateData(filteredList)
    }


    override fun onDestroyView() {
        super.onDestroyView()
        Log.d("CAMPAIGN_FRAGMENT", "Fragment destroyed")
        _binding = null
    }
}
