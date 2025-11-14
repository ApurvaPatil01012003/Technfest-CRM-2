package com.technfest.technfestcrm.view

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.model.Lead

class LeadDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_lead_detail, container, false)
        val toolbar =
            view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        // Handle back arrow click
        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        // Bind views
//        val nameText: TextView = view.findViewById(R.id.tvLeadName)
//        val companyText: TextView = view.findViewById(R.id.tvCompany)
//        val locationText: TextView = view.findViewById(R.id.tvLocation)
//        val statusText: TextView = view.findViewById(R.id.tvStatus)
//        val sourceText: TextView = view.findViewById(R.id.tvSource)
//        val taskText: TextView = view.findViewById(R.id.tvTask)
//        val stageText: TextView = view.findViewById(R.id.tvStage)
//        val ownerText: TextView = view.findViewById(R.id.tvOwner)
        val btnReschedule: MaterialButton=view.findViewById(R.id.btnReschedule)
        val btnAddNote: MaterialButton=view.findViewById(R.id.btnAddNote)
        val btnCreateTask: MaterialButton=view.findViewById(R.id.btnCreateTask)
        btnReschedule.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_reschedule, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        btnAddNote.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_add_note, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        btnCreateTask.setOnClickListener {
            val dialogView = layoutInflater.inflate(R.layout.alert_lead_create_lead_task, null)
            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(true)
                .create()
            dialog.show()
        }
        // Get data from bundle
        val args = arguments
//        nameText.text = args?.getString("name")
//        companyText.text = args?.getString("company")
//        locationText.text = args?.getString("location")
//        statusText.text = args?.getString("status")
//        sourceText.text = args?.getString("source")
//        taskText.text = args?.getString("task")
//        stageText.text = args?.getString("stage")
//        ownerText.text = args?.getString("owner")

        return view
    }

    // ✅ This companion object provides the newInstance() function
    companion object {
        fun newInstance(lead: Lead): LeadDetailFragment {
            val fragment = LeadDetailFragment()
            val args = Bundle().apply {
                putString("name", lead.name)
                putString("company", lead.company)
                putString("location", lead.location)
                putString("status", lead.status)
                putString("source", lead.Source)
                putString("task", lead.task)
                putString("stage", lead.stage)
                putString("owner", lead.owner)
            }
            fragment.arguments = args
            return fragment
        }
    }
}
