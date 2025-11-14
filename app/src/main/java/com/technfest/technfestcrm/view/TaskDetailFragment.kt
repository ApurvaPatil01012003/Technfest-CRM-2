package com.technfest.technfestcrm.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.appbar.MaterialToolbar
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Task

class TaskDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_task_detail, container, false)

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        return view
    }

    companion object {
        fun newInstance(task: Task): TaskDetailFragment {
            val fragment = TaskDetailFragment()
            val args = Bundle().apply {
                putString("heading", task.heading)
                putString("leadName", task.leadName)
                putString("city", task.city)
                putString("time", task.time)
                putString("status", task.status)
                putString("priority", task.priority)
                putString("channelName", task.channelName)
                putString("assignName", task.Assign_name)
                putString("summary", task.summary)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
