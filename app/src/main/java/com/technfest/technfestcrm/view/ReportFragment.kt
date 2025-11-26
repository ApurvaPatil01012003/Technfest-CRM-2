package com.technfest.technfestcrm.view

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentReportBinding
import com.technfest.technfestcrm.model.CallCampaignStatusData
import com.technfest.technfestcrm.model.LeadStatusData
import com.technfest.technfestcrm.model.TaskStatusData
import com.technfest.technfestcrm.model.WhatsAppCampaignStatusData

class ReportFragment : Fragment() {

    private var _binding: FragmentReportBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupFilters()

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

    private fun setupFilters() = with(binding) {
        val filters = listOf(tvToday, tvYesterday, tvThisWeek, tvCustom)

        fun setSelected(selected: View) {
            filters.forEach {
                it.background = ContextCompat.getDrawable(
                    requireContext(),
                    R.drawable.bg_filter_unselected
                )
            }
            selected.background = ContextCompat.getDrawable(
                requireContext(),
                R.drawable.bg_filter_selected
            )
        }

        tvToday.setOnClickListener {
            setSelected(tvToday)
            showRange()
            updateAll(
                "Today",
                LeadStatusData(105f, 56f, 15f, 67f, 200f),
                CallCampaignStatusData(105f, 56f, 15f, 200f),
                WhatsAppCampaignStatusData(105f, 56f, 161f),
               TaskStatusData(105f, 56f, 161f)
            )
        }

        tvYesterday.setOnClickListener {
            setSelected(tvYesterday)
            showRange()
            updateAll(
                "Yesterday",
                LeadStatusData(80f, 40f, 12f, 35f, 170f),
                CallCampaignStatusData(95f, 45f, 10f, 150f),
                WhatsAppCampaignStatusData(90f, 30f, 120f),
                TaskStatusData(60f, 30f, 90f)
            )
        }

        tvThisWeek.setOnClickListener {
            setSelected(tvThisWeek)
            showRange()
            updateAll(
                "This Week",
                LeadStatusData(420f, 210f, 60f, 160f, 900f),
                CallCampaignStatusData(400f, 150f, 50f, 600f),
                WhatsAppCampaignStatusData(350f, 100f, 450f),
                TaskStatusData(300f, 120f, 420f)
            )
        }

        tvCustom.setOnClickListener {
            setSelected(tvCustom)
            showCustomPicker()
        }

        tvToday.post { tvToday.performClick() } // default select today
    }
    private fun updateAll(
        rangeLabel: String,
        lead: LeadStatusData,
        call: CallCampaignStatusData,
        whatsApp: WhatsAppCampaignStatusData,
        task: TaskStatusData
    ) {
        updateLeadStatusChart(lead, rangeLabel)
        updateCallCampaignStatusChart(call, rangeLabel)
        updateWhatsAppStatusChart(whatsApp, rangeLabel)
        updateTaskStatusChart(task, rangeLabel)

        binding.tvDateRange.text = rangeLabel
        binding.tvCallCampRange.text = rangeLabel
        binding.txtDateRangeWhatsappCamp.text = rangeLabel
        binding.txtDateRangeTask.text = rangeLabel
    }



    private fun showRange() {
        binding.dateRange.visibility = View.GONE
    }

    private fun showCustomPicker() {
        binding.dateRange.visibility = View.VISIBLE

        val etFrom = binding.etPhone
        val etTo = binding.etEmail

        val calendar = java.util.Calendar.getInstance()

        // Helper to show date picker and set text
        fun showDatePicker(target: android.widget.EditText) {
            val year = calendar.get(java.util.Calendar.YEAR)
            val month = calendar.get(java.util.Calendar.MONTH)
            val day = calendar.get(java.util.Calendar.DAY_OF_MONTH)

            val datePicker = android.app.DatePickerDialog(
                requireContext(),
                { _, y, m, d ->
                    val date = String.format("%02d/%02d/%04d", d, m + 1, y)
                    target.setText(date)

                    // Once both dates are selected, update charts
                    val fromDate = etFrom.text.toString()
                    val toDate = etTo.text.toString()
                    if (fromDate.isNotEmpty() && toDate.isNotEmpty()) {
                        val label = "$fromDate – $toDate"
                        updateAll(
                            label,
                            LeadStatusData(150f, 90f, 25f, 70f, 335f), // Replace with real filtered data
                            CallCampaignStatusData(120f, 60f, 25f, 205f),
                            WhatsAppCampaignStatusData(110f, 40f, 150f),
                            TaskStatusData(100f, 50f, 150f)
                        )
                    }
                },
                year, month, day
            )
            datePicker.show()
        }

        etFrom.setOnClickListener { showDatePicker(etFrom) }
        etTo.setOnClickListener { showDatePicker(etTo) }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ===================== 🔹 Charts =====================

    private fun updateLeadStatusChart(statusData: LeadStatusData, rangeLabel: String) {
        val entries = listOf(
            PieEntry(statusData.interested, "Interested"),
            PieEntry(statusData.notInterested, "Not Interested"),
            PieEntry(statusData.justCurious, "Just Curious"),
            PieEntry(statusData.dealClosed, "Deal Closed")
        )

        val colors = listOf(
            Color.parseColor("#4EB179"),
            Color.parseColor("#7C99C5"),
            Color.parseColor("#FFD54F"),
            Color.parseColor("#2E7D32")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            sliceSpace = 2f
        }

        val pieData = PieData(dataSet)
        binding.pieLeadRevenue.apply {
            data = pieData
            description.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = false
            holeRadius = 45f
            transparentCircleRadius = 50f
            setUsePercentValues(false)
            centerText = "Leads"
            setCenterTextSize(14f)
            legend.isEnabled = false
            animateY(800)
            invalidate()
        }

        binding.tvDateRange.text = rangeLabel
        binding.tvAllLead.text = "All Leads: ${statusData.allLeads?.toInt() ?: 0}"
        binding.tvInterested.text = "Interested: ${statusData.interested.toInt()}"
        binding.tvNotInterested.text = "Not Interested: ${statusData.notInterested.toInt()}"
        binding.tvCurious.text = "Just Curious: ${statusData.justCurious.toInt()}"
        binding.tvDealClosed.text = "Deal Closed: ${statusData.dealClosed.toInt()}"
    }

    private fun updateCallCampaignStatusChart(statusData: CallCampaignStatusData, rangeLabel: String) {
        val entries = listOf(
            PieEntry(statusData.connected, "Connected"),
            PieEntry(statusData.notConnected, "Not Connected"),
            PieEntry(statusData.callLater, "Call Later")
        )

        val colors = listOf(
            Color.parseColor("#4EB179"),
            Color.parseColor("#7C99C5"),
            Color.parseColor("#FFD54F")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            sliceSpace = 2f
        }

        val pieData = PieData(dataSet)
        binding.pieCallCampRevenue.apply {
            data = pieData
            description.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = false
            holeRadius = 45f
            transparentCircleRadius = 50f
            setUsePercentValues(false)
            centerText = "Call Campaign"
            setCenterTextSize(14f)
            legend.isEnabled = false
            animateY(800)
            invalidate()
        }
        binding.tvCallCampRange.text = rangeLabel
        binding.tvAllCallCamp.text = "All Calls: ${statusData.allCallCampaign?.toInt() ?: 0}"
        binding.tvConnected.text = "Connected: ${statusData.connected.toInt()}"
        binding.tvNotConnected.text = "Not Connected: ${statusData.notConnected.toInt()}"
        binding.tvCallLater.text = "Call Later: ${statusData.callLater.toInt()}"
    }

    private fun updateWhatsAppStatusChart(statusData: WhatsAppCampaignStatusData, rangeLabel: String) {
        val entries = listOf(
            PieEntry(statusData.msgSend, "Message Sent"),
            PieEntry(statusData.msgNotSend, "Message Not Sent")
        )

        val colors = listOf(
            Color.parseColor("#4EB179"),
            Color.parseColor("#7C99C5")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            sliceSpace = 2f
        }

        val pieData = PieData(dataSet)
        binding.pieWhatsAppCampRevenue.apply {
            data = pieData
            description.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = false
            holeRadius = 45f
            transparentCircleRadius = 50f
            setUsePercentValues(false)
            centerText = "WhatsApp Campaign"
            setCenterTextSize(14f)
            legend.isEnabled = false
            animateY(800)
            invalidate()
        }


        binding.txtDateRangeWhatsappCamp.text = rangeLabel
        binding.tvAllWhatsAppCamp.text = "All WhatsApp: ${statusData.allWhatsAppCampaign?.toInt() ?: 0}"
        binding.tvMsgSend.text = "Message Sent: ${statusData.msgSend.toInt()}"
        binding.tvMsgNotSend.text = "Message Not Sent: ${statusData.msgNotSend.toInt()}"
    }

    private fun updateTaskStatusChart(statusData: TaskStatusData, rangeLabel: String) {
        val entries = listOf(
            PieEntry(statusData.completed, "Completed"),
            PieEntry(statusData.notCompleted, "Not Completed")
        )

        val colors = listOf(
            Color.parseColor("#4EB179"),
            Color.parseColor("#7C99C5")
        )

        val dataSet = PieDataSet(entries, "").apply {
            this.colors = colors
            valueTextSize = 12f
            valueTextColor = Color.BLACK
            sliceSpace = 2f
        }

        val pieData = PieData(dataSet)
        binding.pieTaskRevenue.apply {
            data = pieData
            description.isEnabled = false
            setDrawEntryLabels(false)
            isRotationEnabled = false
            holeRadius = 45f
            transparentCircleRadius = 50f
            setUsePercentValues(false)
            centerText = "Tasks"
            setCenterTextSize(14f)
            legend.isEnabled = false
            animateY(800)
            invalidate()
        }
        binding.txtDateRangeTask.text = rangeLabel
        binding.tvAllTask.text = "All Tasks: ${statusData.allTask?.toInt() ?: 0}"
        binding.tvTaskcompleted.text = "Completed: ${statusData.completed.toInt()}"
        binding.tvTaskPending.text = "Pending: ${statusData.notCompleted.toInt()}"
    }


}
