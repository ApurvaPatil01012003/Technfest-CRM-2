package com.technfest.technfestcrm.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.TaskAdapter
import com.technfest.technfestcrm.model.Task

class TaskFragment : Fragment() {

    private lateinit var taskRecyclerView: RecyclerView
    private lateinit var taskAdapter: TaskAdapter
    private lateinit var taskList: MutableList<Task>
    private lateinit var fullTaskList: MutableList<Task>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task, container, false)
        taskRecyclerView = view.findViewById(R.id.taskRecyclerView)

        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        // Dummy list (you can later load from DB or API)
        fullTaskList = mutableListOf(
            Task(
                heading = "Call Amit about billing demo",
                leadName = "Amit",
                city = "Delhi",
                time = "11 AM",
                status = "Open",
                priority = "High",
                channelName = "Tech",
                Assign_name = "Sagar",
                summary = "Confirm time for today's billing + inventory software demo."
            ),
            Task(
                heading = "Follow up with Neha",
                leadName = "Neha",
                city = "Mumbai",
                time = "2 PM",
                status = "Pending",
                priority = "Medium",
                channelName = "CRM",
                Assign_name = "Priya",
                summary = "Discuss proposal feedback."
            )
        )

        taskList = fullTaskList.toMutableList()

        taskAdapter = TaskAdapter(taskList) { selectedTask ->
            val detailFragment = TaskDetailFragment.newInstance(selectedTask)
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, detailFragment)
                .addToBackStack(null)
                .commit()
        }

        taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        taskRecyclerView.adapter = taskAdapter

        return view
    }
}
