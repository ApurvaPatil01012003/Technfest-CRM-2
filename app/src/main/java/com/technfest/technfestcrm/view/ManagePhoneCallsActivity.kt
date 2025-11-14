package com.technfest.technfestcrm.view

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.technfest.technfestcrm.databinding.ActivityManagePhoneCallsBinding

class ManagePhoneCallsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManagePhoneCallsBinding

    // Request multiple permissions (READ_PHONE_STATE and CALL_PHONE)
    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val allGranted = permissions.entries.all { it.value }
            if (allGranted) {
                goToNextActivity()
            } else {
                showPermissionDeniedDialog()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManagePhoneCallsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnAllow.setOnClickListener {
            checkAndRequestPermission()
        }
    }

    private fun checkAndRequestPermission() {
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )

        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        when {
            // ✅ All permissions already granted
            notGranted.isEmpty() -> {
                goToNextActivity()
            }

            // 🚫 Some permissions not granted → request them
            else -> {
                requestPermissionLauncher.launch(permissions)
            }
        }
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permission Required")
            .setMessage("Technfest CRM requires phone call permissions. Please enable them in App Settings.")
            .setCancelable(false)
            .setPositiveButton("Open Settings") { dialog, _ ->
                dialog.dismiss()
                openAppSettings()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun openAppSettings() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    private fun goToNextActivity() {
        startActivity(Intent(this, DisplayOverlayActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()

        // 🔄 When user comes back from Settings
        val permissions = arrayOf(
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.CALL_PHONE
        )

        val allGranted = permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            goToNextActivity()
        }
    }
}
