package com.technfest.technfestcrm.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.CallsAdapter
import com.technfest.technfestcrm.databinding.FragmentSimBinding
import com.technfest.technfestcrm.model.Calls

class SimFragment : Fragment() {

    private var _binding: FragmentSimBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSimBinding.inflate(inflater, container, false)
        val simName = arguments?.getString("simName")
        val simNumber = arguments?.getString("simNumber")
        val slotIndex = arguments?.getInt("slotIndex")
        Log.d("SIM FRag","$simNumber")
        binding.tvSimName.text = simName
        binding.tvSimNumber.text = simNumber
        Log.d("SIM FRag","$simName")
        Log.d("SIM FRag","$simNumber")
        val allCalls = listOf(
            Calls("Abc", "Outgoing", "0909090909", "Today", "11:00 AM", "3 min", "User 1", "1234567890", "Lead Linked", "Note A", "A"),
            Calls("Pqr", "Incoming", "0808080808", "Yesterday", "10:00 AM", "2 min", "User 2", "1234567890", "Lead Linked", "Note B", "P"),
            Calls("Xyz", "Missed", "0707070707", "Today", "09:00 AM", "0 min", "User 3", "1234567890", "Lead Linked", "Note C", "X"),
            // Add more calls here…
        )

        val recentCalls = allCalls.take(10)

        // 🔹 Setup RecyclerView
        binding.callRecyclerview.layoutManager = LinearLayoutManager(requireContext())
        binding.callRecyclerview.adapter = CallsAdapter(recentCalls)

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}
