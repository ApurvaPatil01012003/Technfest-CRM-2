package com.technfest.technfestcrm.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityDisplayOverlayBinding

class DisplayOverlayActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_SETTINGS = "from_settings_overlay"
    }

    private lateinit var binding: ActivityDisplayOverlayBinding
    private val OVERLAY_PERMISSION_REQUEST_CODE = 101

    // true when opened from SettingFragment; false in onboarding flow
    private val fromSettings: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val sharedPref = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val isFirstTime = sharedPref.getBoolean("isFirstTimeOverlay", true)

        // ✅ Only auto-skip in onboarding — NOT when opened from Settings
        if (!fromSettings && !isFirstTime) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        enableEdgeToEdge()
        binding = ActivityDisplayOverlayBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Allow button → check/request permission
        binding.btnAllow.setOnClickListener {
            checkOverlayPermission()
        }

        // Skip button
        binding.txtSkip.setOnClickListener {
            sharedPref.edit { putBoolean("isFirstTimeOverlay", false) }

            if (fromSettings) {
                // From Settings → just go back
                finish()
            } else {
                // From onboarding → continue app flow
                goToNextActivity()
            }
        }
    }

    private fun checkOverlayPermission() {
        if (Settings.canDrawOverlays(this)) {
            saveAndGoNext()
        } else {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                "package:$packageName".toUri()
            )
            startActivityForResult(intent, OVERLAY_PERMISSION_REQUEST_CODE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == OVERLAY_PERMISSION_REQUEST_CODE) {
            if (Settings.canDrawOverlays(this)) {
                saveAndGoNext()
            } else {
                Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveAndGoNext() {
        // Mark first-time as done
        getSharedPreferences("AppPrefs", MODE_PRIVATE)
            .edit { putBoolean("isFirstTimeOverlay", false) }

        if (fromSettings) {
            // ✅ From settings → just close and go back to SettingFragment
            finish()
        } else {
            // ✅ Onboarding → go to next activity (your flow)
            goToNextActivity()
        }
    }

    private fun goToNextActivity() {
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}
