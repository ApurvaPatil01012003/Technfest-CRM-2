package com.technfest.technfestcrm.view

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentEditProfileBinding

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val name = arguments?.getString("name")
        val role = arguments?.getString("role")
        val team = arguments?.getString("team")
        val email = arguments?.getString("email")
        val phone = arguments?.getString("phone")

        // Display values in EditTexts
        binding.edtName.setText(name)
        binding.edtRole.setText(role)
        binding.edtTeam.setText(team)
        binding.edtEmail.setText(email)
        binding.edtMobile.setText(phone)
    }
}