package com.technfest.technfestcrm.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentSettingBinding
import androidx.core.content.edit
import androidx.core.net.toUri

class SettingFragment : Fragment() {

    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    private var isExpanded = false
    private var isSimExpanded = false
    private var isProfileExpanded = false
    private var isWorkspaceExpanded = false
    private var ignoreListenerUpdates = false
    private var isCallExpanded = false

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        binding.headerLayout.setOnClickListener {
            isExpanded = !isExpanded
            binding.permissionOptions.visibility = if (isExpanded) View.VISIBLE else View.GONE
            binding.imgArrow.setImageResource(
                if (isExpanded) R.drawable.baseline_arrow_right_24
                else R.drawable.baseline_arrow_drop_down_24
            )
        }

        binding.headerSIMLayout.setOnClickListener {
            isSimExpanded = !isSimExpanded
            if (isSimExpanded) {
                checkAndShowSimList()
                binding.simOptionsLayout.visibility = View.VISIBLE
                binding.imgSIMArrow.setImageResource(R.drawable.baseline_arrow_right_24)
            } else {
                binding.simOptionsLayout.visibility = View.GONE
                binding.imgSIMArrow.setImageResource(R.drawable.baseline_arrow_drop_down_24)
            }
        }

        binding.headerProfileLayout.setOnClickListener {
            isProfileExpanded = !isProfileExpanded
            binding.layoutProfileDetails.visibility =
                if (isProfileExpanded) View.VISIBLE else View.GONE
            binding.imgProfileSetting.setImageResource(
                if (isProfileExpanded) R.drawable.baseline_arrow_right_24
                else R.drawable.baseline_arrow_drop_down_24
            )
        }

        binding.btnEdit.setOnClickListener {
            val editProfileFragment = EditProfileFragment()
            val bundle = Bundle().apply {
                putString("name", "Sagar Mohite")
                putString("role", "sales_caller")
                putString("team", "Team A")
                putString("email", "sagar@technfest.com")
                putString("phone", "+91 88578 08284")
            }
            editProfileFragment.arguments = bundle
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editProfileFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnChangePassword.setOnClickListener {
            val changePasswordFragment = ChangePasswordFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, changePasswordFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.btnEditWorkspace.setOnClickListener {
            val editWorkspaceFragment = EditWorkspaceFragment()
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, editWorkspaceFragment)
                .addToBackStack(null)
                .commit()
        }

        binding.headerWorkspaceLayout.setOnClickListener {
            isWorkspaceExpanded = !isWorkspaceExpanded
            binding.layoutWorkspaceDetails.visibility =
                if (isWorkspaceExpanded) View.VISIBLE else View.GONE
            binding.imgWorkspaceSetting.setImageResource(
                if (isWorkspaceExpanded) R.drawable.baseline_arrow_right_24
                else R.drawable.baseline_arrow_drop_down_24
            )
        }


        binding.headerCallLayout.setOnClickListener {
            isCallExpanded = !isCallExpanded
            binding.CallLayout.visibility =
                if (isCallExpanded) View.VISIBLE else View.GONE
            binding.imageCallSetting.setImageResource(
                if (isCallExpanded) R.drawable.baseline_arrow_right_24
                else R.drawable.baseline_arrow_drop_down_24
            )
            binding.callAutomation.setOnClickListener {
                val callAutomationFragment = CallAutomationFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, callAutomationFragment)
                    .addToBackStack(null)
                    .commit()
            }
            binding.callRecordings.setOnClickListener {
                val callRecordingFragment = CallRecordingFragment()
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, callRecordingFragment)
                    .addToBackStack(null)
                    .commit()
            }

        }

        binding.btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout Account")
                .setMessage("Do you want to Logout")
                .setPositiveButton("Yes") { dialog, _ ->
                    // TODO: clear session & go to login
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.cancel()
                }
                .create()
                .show()
        }

        setupSwitchListeners()
        refreshAllSwitchStates()

        return binding.root
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", requireContext().packageName, null)
        )
        startActivity(intent)  // no ActivityResult, we will detect in onResume()
    }

    private fun openOverlaySettings() {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            "package:${requireContext().packageName}".toUri()
        )
        startActivity(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndShowSimList() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_PHONE_NUMBERS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.READ_PHONE_NUMBERS
                ),
                101
            )
            return
        }
        showSimList()
    }

    @SuppressLint("MissingPermission")
    private fun showSimList() {
        val subscriptionManager =
            requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val subscriptions = subscriptionManager.activeSubscriptionInfoList

        binding.simOptionsLayout.removeAllViews()

        if (subscriptions.isNullOrEmpty()) {
            val txtNoSim = TextView(requireContext()).apply {
                text = "No SIM cards detected"
                setTextColor(ContextCompat.getColor(requireContext(), R.color.black))
                textSize = 14f
                setPadding(8, 8, 8, 8)
            }
            binding.simOptionsLayout.addView(txtNoSim)
            return
        }

        for (info in subscriptions) {
            val simView = layoutInflater.inflate(
                R.layout.item_sim_option,
                binding.simOptionsLayout,
                false
            )
            val txtSim = simView.findViewById<TextView>(R.id.txtSimName)

            val simName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}"
            val simNumber = info.number?.takeIf { it.isNotEmpty() } ?: "Number not available"

            // e.g. "SIM1: Airtel (98xxxxxxx)"
            val displayText = "SIM${info.simSlotIndex + 1}: $simName ($simNumber)"
            txtSim.text = displayText

            txtSim.setOnClickListener {
                val simFragment = SimFragment().apply {
                    arguments = Bundle().apply {
                        putString("simName", simName)
                        putString("simNumber", simNumber)
                        putInt("slotIndex", info.simSlotIndex)
                    }
                }
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, simFragment)
                    .addToBackStack(null)
                    .commit()
            }

            binding.simOptionsLayout.addView(simView)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.isNotEmpty() &&
            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
        ) {
            showSimList()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    // ───────────────── Toolbar & lifecycle ─────────────────

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar =
            view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        (activity as? androidx.appcompat.app.AppCompatActivity)?.apply {
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        toolbar.setNavigationOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        refreshAllSwitchStates()
    }

    override fun onResume() {
        super.onResume()

        refreshAllSwitchStates()

        val cameFromContactsToggle = consumeContactsToggleFlag()

        if (cameFromContactsToggle) {
            if (!PermissionHelper.hasReadContacts(requireContext())) {
                val intent = Intent(requireContext(), ReadContactsActivity::class.java).apply {
                    putExtra(ReadContactsActivity.EXTRA_FROM_SETTINGS, true)
                }
                startActivity(intent)
            }
        }
    }

    private fun markContactsToggleFlowStarted() {
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        prefs.edit() { putBoolean("contactsToggleFlow", true) }
    }

    private fun consumeContactsToggleFlag(): Boolean {
        val prefs = requireContext().getSharedPreferences("AppPrefs", Context.MODE_PRIVATE)
        val value = prefs.getBoolean("contactsToggleFlow", false)
        if (value) {
            prefs.edit() { putBoolean("contactsToggleFlow", false) }
        }
        return value
    }




    private fun setupSwitchListeners() {
        binding.switchReadCall.setOnCheckedChangeListener { button, isChecked ->
            if (ignoreListenerUpdates) return@setOnCheckedChangeListener

            val hasNow = PermissionHelper.hasReadCallLog(requireContext())

            if (isChecked && !hasNow) {
                startActivity(Intent(requireContext(), ReadCallLogsActivity::class.java))
                syncSwitchImmediate(button, false)
            } else if (!isChecked && hasNow) {
                openAppSettings()
                syncSwitchImmediate(button, true)
            }
        }

        binding.switchReadContacts.setOnCheckedChangeListener { button, isChecked ->
            if (ignoreListenerUpdates) return@setOnCheckedChangeListener

            val hasNow = PermissionHelper.hasReadContacts(requireContext())

            if (isChecked && !hasNow) {
                markContactsToggleFlowStarted()
                openAppSettings()
                syncSwitchImmediate(button, false)

            } else if (!isChecked && hasNow) {
                markContactsToggleFlowStarted()
                openAppSettings()
                syncSwitchImmediate(button, true)
            }
        }





        binding.switchManagePhoneCall.setOnCheckedChangeListener { button, isChecked ->
            if (ignoreListenerUpdates) return@setOnCheckedChangeListener

            val hasNow = PermissionHelper.hasCallPhone(requireContext())

            if (isChecked && !hasNow) {
                startActivity(Intent(requireContext(), ManagePhoneCallsActivity::class.java))
                syncSwitchImmediate(button, false)
            } else if (!isChecked && hasNow) {
                openAppSettings()
                syncSwitchImmediate(button, true)
            }
        }

        binding.switchOverlay.setOnCheckedChangeListener { button, isChecked ->
            if (ignoreListenerUpdates) return@setOnCheckedChangeListener

            val hasNow = PermissionHelper.hasOverlay(requireContext())

            if (isChecked && !hasNow) {
                val intent = Intent(requireContext(), DisplayOverlayActivity::class.java).apply {
                    putExtra(DisplayOverlayActivity.EXTRA_FROM_SETTINGS, true)
                }
                openOverlaySettings()
                syncSwitchImmediate(button, false)

            } else if (!isChecked && hasNow) {
                openOverlaySettings()
                syncSwitchImmediate(button, true)
            }
        }



    }

    private fun syncSwitchImmediate(button: CompoundButton, actualStateNow: Boolean) {
        ignoreListenerUpdates = true
        button.isChecked = actualStateNow
        ignoreListenerUpdates = false
    }

    private fun refreshAllSwitchStates() {
        if (!isAdded) return

        ignoreListenerUpdates = true

        binding.switchReadCall.isChecked =
            PermissionHelper.hasReadCallLog(requireContext())

        binding.switchReadContacts.isChecked =
            PermissionHelper.hasReadContacts(requireContext())

        binding.switchManagePhoneCall.isChecked =
            PermissionHelper.hasCallPhone(requireContext())

        binding.switchOverlay.isChecked =
            PermissionHelper.hasOverlay(requireContext())

        ignoreListenerUpdates = false
    }
}