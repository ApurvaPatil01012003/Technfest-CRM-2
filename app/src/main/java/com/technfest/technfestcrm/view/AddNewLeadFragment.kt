package com.technfest.technfestcrm.view

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.databinding.FragmentAddNewLeadBinding
import com.technfest.technfestcrm.model.CampaignResponseItem
import com.technfest.technfestcrm.model.LeadMetaItem
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.repository.CampaignRepository
import com.technfest.technfestcrm.repository.LeadRepository
import com.technfest.technfestcrm.viewmodel.CampaignViewModelFactory
import com.technfest.technfestcrm.viewmodel.CampaignsViewModel
import com.technfest.technfestcrm.viewmodel.LeadViewModel
import com.technfest.technfestcrm.viewmodel.LeadViewModelFactory
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AddNewLeadFragment : Fragment() {

    private var _binding: FragmentAddNewLeadBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: LeadViewModel
    private lateinit var repository: LeadRepository

    private var token: String? = null
    private var workspaceId: Int = -1
    private var campaignList: List<CampaignResponseItem> = emptyList()
    private var selectedCampaignId: Int = 0
    private var teamList: List<LeadMetaItem> = emptyList()
    private var selectedFollowupDate: Calendar? = null

    private lateinit var campaignsViewModel: CampaignsViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            token = it.getString("token")
            workspaceId = it.getInt("workspaceId")
            Log.d("Token", token.toString())

        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddNewLeadBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val toolbar = binding.toolbar
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // ViewModel setup
        repository = LeadRepository()
        viewModel = ViewModelProvider(this, LeadViewModelFactory(repository))[LeadViewModel::class.java]

        loadMetaData()
        observeMetaData()


        val campaignRepo = CampaignRepository()
        val factory = CampaignViewModelFactory(campaignRepo)
        campaignsViewModel = ViewModelProvider(this, factory)[CampaignsViewModel::class.java]

        token?.let { t -> campaignsViewModel.fetchCampaigns(t, workspaceId) }


        campaignsViewModel.campaignList.observe(viewLifecycleOwner) { list ->
            campaignList = list
            val names = list.map { it.name ?: "" }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spnCampaignName.adapter = adapter
        }

        viewModel.successLiveData.observe(viewLifecycleOwner) { success ->
            if (success) {
                Toast.makeText(requireContext(), "Lead Created Successfully", Toast.LENGTH_SHORT).show()
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
        }

        viewModel.leadResponseLiveData.observe(viewLifecycleOwner) { leadResponse ->
            leadResponse?.let {
                Log.d("LeadCreated", "Lead ID: ${it.id}, Name: ${it.fullName}")
               // Toast.makeText(requireContext(), "Lead Created: ${it.fullName}", Toast.LENGTH_SHORT).show()
            }
        }


        binding.spnCampaignName.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedCampaignId = campaignList[position].id
            }
            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedCampaignId = 0
            }
        }
        binding.edtFollowUpdate.setOnClickListener {
            val calendar = Calendar.getInstance()
            DatePickerDialog(
                requireContext(),
                { _, year, month, dayOfMonth ->
                    val selectedDate = Calendar.getInstance()
                    selectedDate.set(year, month, dayOfMonth)

                    TimePickerDialog(
                        requireContext(),
                        { _, hourOfDay, minute ->
                            selectedDate.set(Calendar.HOUR_OF_DAY, hourOfDay)
                            selectedDate.set(Calendar.MINUTE, minute)
                            selectedDate.set(Calendar.SECOND, 0)

                            selectedFollowupDate = selectedDate // save it here

                            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                            binding.edtFollowUpdate.setText(sdf.format(selectedDate.time))

                        },
                        calendar.get(Calendar.HOUR_OF_DAY),
                        calendar.get(Calendar.MINUTE),
                        true
                    ).show()
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }




        binding.btnSaveLead.setOnClickListener {
            saveLead()
        }
    }

    private fun loadMetaData() {
        token?.let { t ->
            viewModel.fetchLeadMeta(t, workspaceId, "source")
            viewModel.fetchLeadMeta(t, workspaceId, "status")
            viewModel.fetchLeadMeta(t, workspaceId, "stage")
            viewModel.fetchLeadMeta(t, workspaceId, "priority")
            viewModel.fetchLeadMeta(t, workspaceId, "team")
            viewModel.fetchLeadMeta(t, workspaceId, "assign_type")
        }
    }


    private fun observeMetaData() {
        viewModel.sourceLiveData.observe(viewLifecycleOwner) {
            setSpinner(binding.spnSource, it)
        }

        viewModel.statusLiveData.observe(viewLifecycleOwner) { setSpinner(binding.spnStatus, it) }
        viewModel.stageLiveData.observe(viewLifecycleOwner) { setSpinner(binding.spnStages, it) }
        viewModel.priorityLiveData.observe(viewLifecycleOwner) { setSpinner(binding.spnPriority, it) }
        viewModel.teamLiveData.observe(viewLifecycleOwner) { list ->
            teamList = list
            val names = list.map { it.name }
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
            binding.spnAssignToTeam.adapter = adapter
        }
        viewModel.assignTypeLiveData.observe(viewLifecycleOwner) { setSpinner(binding.spnAssignType, it) }

        viewModel.errorLiveData.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }
    }


    private fun saveLead() {
        val name = binding.edtLeadName.text.toString().trim()
        val phone = binding.edtleadNumber.text.toString().trim()

        if (name.isEmpty() || phone.isEmpty()) {
            Toast.makeText(requireContext(), "Name & Phone required", Toast.LENGTH_SHORT).show()
            return
        }

        val tagsInput = binding.edtTags.text.toString()
        val tagsList = if (tagsInput.isNotEmpty()) {
            tagsInput.split(",").map { it.trim() }
        } else emptyList()


        val selectedTeamPosition = binding.spnAssignToTeam.selectedItemPosition
        val selectedTeam = if (selectedTeamPosition in teamList.indices) teamList[selectedTeamPosition] else null

        val selectedSource = viewModel.sourceLiveData.value?.get(binding.spnSource.selectedItemPosition)
        val selectedStage = viewModel.stageLiveData.value?.get(binding.spnStages.selectedItemPosition)

        val followupDatesList = selectedFollowupDate?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            listOf(sdf.format(it.time))
        } ?: emptyList()
        val nextFollowup = selectedFollowupDate?.let {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(it.time)
        }
        val selectedStatus = viewModel.statusLiveData.value?.get(binding.spnStatus.selectedItemPosition)
        val statusValue = selectedStatus?.name ?: ""
        val selectedPriority = viewModel.priorityLiveData.value?.get(binding.spnPriority.selectedItemPosition)
        val priorityValue = selectedPriority?.name ?: ""

        val leadRequest = LeadRequest(

            assigned_to = binding.edtUserName.text.toString(),
            campaignId = selectedCampaignId,
            campaignName = campaignList.find { it.id == selectedCampaignId }?.name ?: "",
            company = binding.edtCompanyname.text.toString(),
            email = binding.edtEmail.text.toString(),
            followupDates = followupDatesList,
            fullName = name,
            leadRequirement = binding.edtLeadRequest.text.toString(),
            location = binding.edtLocation.text.toString(),
            mobile = phone,
            nextFollowupAt = nextFollowup,
            priority = priorityValue,
            source = selectedSource?.name?.toString() ?: "",
            sourceDetails = "",
            stage = selectedStage?.name?.toString() ?: "",
            status = statusValue,
            tags = tagsList,
            teamId = selectedTeam?.id ?: 0,
            teamName = selectedTeam?.name ?: ""
        )

        val gson = com.google.gson.Gson()
        Log.d("LeadRequestJSON", gson.toJson(leadRequest))

        token?.let { t ->
            viewModel.createLead(t, leadRequest)
        }
    }



    private fun setSpinner(spinner: Spinner, data: List<LeadMetaItem>) {
        val names = data.map { it.name }
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, names)
        spinner.adapter = adapter
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
