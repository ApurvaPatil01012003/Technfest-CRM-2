package com.technfest.technfestcrm.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentCallAutomationBinding

class CallAutomationFragment : Fragment() {
private var  _binding : FragmentCallAutomationBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
_binding= FragmentCallAutomationBinding.inflate(inflater, container, false)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.edtStartTime.setOnClickListener {
            showTimePicker(binding.edtStartTime)
        }

        binding.edtEndTime.setOnClickListener {
            showTimePicker(binding.edtEndTime)
        }
    }
    private fun showTimePicker(targetView: android.widget.EditText) {
        val c = java.util.Calendar.getInstance()
        val hour = c.get(java.util.Calendar.HOUR_OF_DAY)
        val minute = c.get(java.util.Calendar.MINUTE)

        val timePicker = android.app.TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                targetView.setText(formattedTime)
            },
            hour,
            minute,
            true


        )

        timePicker.show()
    }


}