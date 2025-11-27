package com.technfest.technfestcrm.view

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.HomeTaskAdapter
import com.technfest.technfestcrm.adapter.HotLeadAdapter
import com.technfest.technfestcrm.databinding.FragmentHomeBinding
import com.technfest.technfestcrm.model.HotLead
import com.technfest.technfestcrm.network.RetrofitInstance
import com.technfest.technfestcrm.repository.GetWorkspacesRepository
import com.technfest.technfestcrm.repository.LeadRepository
import com.technfest.technfestcrm.repository.TaskRepository
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModel
import com.technfest.technfestcrm.viewmodel.GetWorkspacesViewModelFactory
import com.technfest.technfestcrm.viewmodel.LeadViewModel
import com.technfest.technfestcrm.viewmodel.LeadViewModelFactory
import com.technfest.technfestcrm.viewmodel.TaskViewModel
import com.technfest.technfestcrm.viewmodel.TaskViewModelFactory
import okhttp3.internal.concurrent.Task
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: GetWorkspacesViewModel
    private lateinit var homeTaskAdapter: HomeTaskAdapter
    private lateinit var taskViewModel: TaskViewModel


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.leadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        var prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        var token = prefs.getString("token", null)
        var workspaceId = prefs.getInt("workspaceId", -1)
        val fullName = prefs.getString("fullName", "User")
        val savedWorkspaceName = prefs.getString("workspaceName", null)

        binding.userName.text = fullName
        binding.userInitialCircle.text = getInitials(fullName ?: "")

        if (!savedWorkspaceName.isNullOrEmpty()) {
            binding.toolbarTitle.text = savedWorkspaceName
        }

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

        binding.myLead.setOnClickListener {
            (activity as MainActivity).openLeadsFromHome()
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val workspaceId = prefs.getInt("workspaceId", -1)


            val f = LeadsFragment()
            val bundle = Bundle().apply {
                putString("Name", fullName)
                putString("Token", token)
                putInt("WorkspaceId", workspaceId)
                putString("WorkspaceName", binding.toolbarTitle.text.toString())
            }
            f.arguments = bundle

            loadFragment(f)

        }

        prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        token = prefs.getString("token", null)
        workspaceId = prefs.getInt("workspaceId", -1)

        val leadRepository = LeadRepository()
        val leadFactory = LeadViewModelFactory(leadRepository)
        val leadViewModel = ViewModelProvider(this, leadFactory)[LeadViewModel::class.java]

        if (!token.isNullOrEmpty() && workspaceId != -1) {
            leadViewModel.fetchLeads(token, workspaceId)
        }

        leadViewModel.leadsLiveData.observe(viewLifecycleOwner) { leads ->

            val hotLeads = leads.filter {
                it.status.equals("new", ignoreCase = true)
            }
            val newLeadCount = hotLeads.size
            binding.txtNewLeadCount.text = newLeadCount.toString()
            val adapter = HotLeadAdapter(hotLeads) { clickedLead ->

                val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)

                val token = prefs.getString("token", null)
                val workspaceId = prefs.getInt("workspaceId", -1)
                val fullName = prefs.getString("fullName", "User")
                val workspaceName = prefs.getString("workspaceName", "")

                val fragment = LeadsFragment()
                fragment.arguments = Bundle().apply {
                    putInt("leadId", clickedLead.id)
                    putString("Token", token)
                    putInt("WorkspaceId", workspaceId)
                    putString("Name", fullName)
                    putString("WorkspaceName", workspaceName)
                }

                loadFragment(fragment)

            }

            binding.leadsRecyclerView.adapter = adapter
        }


        binding.task.setOnClickListener {
            (activity as MainActivity).openTasksFromHome()
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val workspaceId = prefs.getInt("workspaceId", -1)

            val fragment = TaskFragment()
            fragment.arguments = Bundle().apply {
                putString("token", token)
                putInt("workspaceId", workspaceId)
            }

            loadFragment(fragment)

        }
        binding.callsCampaign.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val workspaceId = prefs.getInt("workspaceId", -1)

            val fragment = CallsCampaignFragment()
            fragment.arguments = Bundle().apply {
                putString("token", token)
                putInt("workspaceId", workspaceId)
            }

            loadFragment(fragment)
        }

        binding.addNewLead.setOnClickListener {
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val workspaceId = prefs.getInt("workspaceId", -1)

            val fragment = AddNewLeadFragment()
            fragment.arguments = Bundle().apply {
                putString("token", token)
                putInt("workspaceId", workspaceId)
            }

            loadFragment(fragment)
        }


        binding.calls.setOnClickListener { loadFragment(CallsFragment()) }
        binding.report.setOnClickListener { loadFragment(ReportFragment()) }
        binding.userInitialCircle.setOnClickListener { loadFragment(ProfileFragment()) }


        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        homeTaskAdapter = HomeTaskAdapter(emptyList()) { task ->
            val fragment = TaskFragment()
            val bundle = Bundle().apply {
                putString("token", token)
                putInt("workspaceId", workspaceId)
                putInt("highlightTaskId", task.id)
            }
            fragment.arguments = bundle
            loadFragment(fragment)
        }
        binding.taskRecyclerView.adapter = homeTaskAdapter

        val taskRepository = TaskRepository()
        val taskFactory = TaskViewModelFactory(taskRepository)
        taskViewModel = ViewModelProvider(this, taskFactory)[TaskViewModel::class.java]

        if (!token.isNullOrEmpty() && workspaceId != -1) {
            taskViewModel.fetchTasks(token, workspaceId)
        } else {
            Toast.makeText(requireContext(), "Invalid workspace/token", Toast.LENGTH_SHORT).show()
        }
        taskViewModel.taskResult.observe(viewLifecycleOwner) { tasks ->
            val today = java.time.LocalDate.now()
            val todayTasks = tasks.filter { task ->
                try {
                    task.dueAt?.let {
                        val taskDate = java.time.ZonedDateTime.parse(it).toLocalDate()
                        taskDate == today
                    } ?: false
                } catch (e: Exception) {
                    false
                }
            }
            homeTaskAdapter.updateList(todayTasks)
            val pendingTodayCount = todayTasks.count {
                it.status.equals("pending", ignoreCase = true)
            }
            if (todayTasks.isEmpty()) {
                binding.txtTaskNotAssign.visibility = View.VISIBLE
                binding.taskRecyclerView.visibility = View.GONE
            } else {
                binding.txtTaskNotAssign.visibility = View.GONE
                binding.taskRecyclerView.visibility = View.VISIBLE
            }

            binding.txtTodayPendingTaskCount.text = pendingTodayCount.toString()


        }



        return binding.root
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


}
