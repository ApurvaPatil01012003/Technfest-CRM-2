package com.technfest.technfestcrm.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentEditWorkspaceBinding
import kotlinx.coroutines.flow.combine

class EditWorkspaceFragment : Fragment() {
    private var _binding: FragmentEditWorkspaceBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentEditWorkspaceBinding.inflate(inflater, container, false)

        return binding.root
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
        }

        val Name = arguments?.getString("WorkspaceName")
        val Code = arguments?.getString("WorkspaceCode")
        val Type = arguments?.getString("WorkspaceType")
        val timeZone = arguments?.getString("TimeZone")
        val workingHours = arguments?.getString("WorkingHours")
        val holiday = arguments?.getString("Holiday")
        binding.edtWorkspaceName.setText(Name)
        binding.edtCode.setText(Code)
        binding.edtType.setText(Type)
        binding.edtTimeZone.setText(timeZone)
        binding.edtWorkingHours.setText(workingHours)
        binding.edtWeekend.setText(holiday)


    }
}