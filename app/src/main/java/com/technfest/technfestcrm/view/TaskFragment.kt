package com.technfest.technfestcrm.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.TaskAdapter
import com.technfest.technfestcrm.model.TaskResponseItem
import com.technfest.technfestcrm.repository.TaskRepository
import com.technfest.technfestcrm.viewmodel.TaskViewModel
import com.technfest.technfestcrm.viewmodel.TaskViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskFragment : Fragment() {

    private lateinit var taskAdapter: TaskAdapter
    private lateinit var viewModel: TaskViewModel
    private var originalList: List<TaskResponseItem> = emptyList()

    private lateinit var filterAll: TextView
    private lateinit var filterToday: TextView
    private lateinit var filterPending: TextView
    private lateinit var filterCompleted: TextView
    private lateinit var filterHighPriority: TextView

    private var token: String? = null
    private var workspaceId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        arguments?.let {
            token = it.getString("token")
            workspaceId = it.getInt("workspaceId", -1)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_task, container, false)

        val recyclerView = view.findViewById<RecyclerView>(R.id.taskRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val repo = TaskRepository()
        val factory = TaskViewModelFactory(repo)
        viewModel = ViewModelProvider(this, factory)[TaskViewModel::class.java]

        taskAdapter = TaskAdapter(emptyList()) { task ->
            val detailFragment = TaskDetailFragment.newInstance(task)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        recyclerView.adapter = taskAdapter
        filterAll = view.findViewById(R.id.filterAll)
        filterToday = view.findViewById(R.id.filterToday)
        filterPending = view.findViewById(R.id.filterPending)
        filterCompleted = view.findViewById(R.id.filterCompleted)
        filterHighPriority = view.findViewById(R.id.filterHighPriority)

        setupFilters()

        viewModel.taskResult.observe(viewLifecycleOwner) { list ->
            originalList = list
            applyFilter("All") // Default
        }

        viewModel.errorMsg.observe(viewLifecycleOwner) {
            Toast.makeText(requireContext(), it, Toast.LENGTH_SHORT).show()
        }

        if (!token.isNullOrEmpty() && workspaceId != -1) {
            viewModel.fetchTasks(token!!, workspaceId)
        } else {
            Toast.makeText(requireContext(), "Invalid workspace/token", Toast.LENGTH_SHORT).show()
        }

        return view
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
        }}

    private fun setupFilters() {

        filterAll.setOnClickListener { applyFilter("All") }
        filterToday.setOnClickListener { applyFilter("Today") }
        filterPending.setOnClickListener { applyFilter("Pending") }
        filterCompleted.setOnClickListener { applyFilter("Completed") }
        filterHighPriority.setOnClickListener { applyFilter("HighPriority") }
    }
    private fun applyFilter(type: String) {

        highlightSelected(type)

        val filteredList = when (type) {

            "Today" -> {
                val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                originalList.filter { it.dueDate?.startsWith(today) == true }
            }

            "Pending" -> originalList.filter { it.status.equals("Pending", true) }

            "Completed" -> originalList.filter { it.status.equals("Completed", true) }

            "HighPriority" -> originalList.filter { it.priority.equals("High", true) }

            else -> originalList   // ALL
        }

        taskAdapter.updateList(filteredList)
    }
    private fun highlightSelected(type: String) {
        val defaultBg = R.drawable.unselected_campaign_bg
        val selectedBg = R.drawable.selected_campaign_bg

        filterAll.setBackgroundResource(if (type == "All") selectedBg else defaultBg)
        filterToday.setBackgroundResource(if (type == "Today") selectedBg else defaultBg)
        filterPending.setBackgroundResource(if (type == "Pending") selectedBg else defaultBg)
        filterCompleted.setBackgroundResource(if (type == "Completed") selectedBg else defaultBg)
        filterHighPriority.setBackgroundResource(if (type == "HighPriority") selectedBg else defaultBg)
    }
}
