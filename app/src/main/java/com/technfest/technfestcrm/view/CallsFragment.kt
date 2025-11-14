package com.technfest.technfestcrm.view

import android.os.Bundle
import android.telecom.Call
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.CallsAdapter
import com.technfest.technfestcrm.databinding.FragmentCallsBinding
import com.technfest.technfestcrm.model.Calls


class CallsFragment : Fragment() {
    private var _binding: FragmentCallsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_calls, container, false)
        _binding = FragmentCallsBinding.inflate(inflater, container, false)
        val callUser = listOf(
            Calls(
                "Abc",
                "outgoing",
                "0909090909",
                "Today",
                "11:00 AM",
                "3 min",
                "asdf",
                "from 1234567890",
                "Lead linked",
                "noteeee",
                "A"
            ),
            Calls(
                "Pqr",
                "outgoing",
                "0909090909",
                "Yesturday",
                "11:00 AM",
                "3 min",
                "asdf",
                "from 1234567890",
                "Lead linked",
                "noteeee",
                "P"
            ),

            )
       binding.leadRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.leadRecyclerView.adapter = CallsAdapter(callUser)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.setDisplayShowHomeEnabled(true)
        }

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }
}