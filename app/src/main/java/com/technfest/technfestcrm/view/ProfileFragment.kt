package com.technfest.technfestcrm.view

import android.graphics.Color
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.appbar.AppBarLayout
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentProfileBinding

class ProfileFragment : Fragment() {
  private var _binding : FragmentProfileBinding?=null
    private val binding get() = _binding!!
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
      _binding= FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.appBarLayout.addOnOffsetChangedListener(
            AppBarLayout.OnOffsetChangedListener { appBarLayout, verticalOffset ->
                val totalScrollRange = appBarLayout.totalScrollRange
                if (Math.abs(verticalOffset) >= totalScrollRange) {
                    binding.txtToolbarInitial.visibility = View.VISIBLE
                    binding.txtToolbarName.visibility = View.VISIBLE
                } else {
                    binding.txtToolbarInitial.visibility = View.GONE
                    binding.txtToolbarName.visibility = View.GONE
                }
            }
        )

        binding.toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.toolbar.inflateMenu(R.menu.profilr_menu)
        binding.toolbar.overflowIcon?.setTint(Color.WHITE)


        binding.toolbar.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.edit -> {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, EditProfileFragment())
                        .addToBackStack(null)
                        .commit()
                    true
                }
                else -> false
            }
        }
    }




}