package com.technfest.technfestcrm.view

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityReadContactsBinding
import androidx.core.content.edit

class ReadContactsActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_FROM_SETTINGS = "from_settings_contacts"
    }

    private lateinit var binding: ActivityReadContactsBinding
    private val CONTACT_PERMISSION_CODE = 1001


    private val fromSettings: Boolean by lazy {
        intent.getBooleanExtra(EXTRA_FROM_SETTINGS, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val pref = getSharedPreferences("AppPrefs", MODE_PRIVATE)

        // ⛔ Removed the wrong finish()

        // If coming back from Settings AND permission is already granted → close
        if (fromSettings &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            finish()
            return
        }

        // Not from settings → skip if already completed first time flow
        if (!fromSettings) {
            val firstTime = pref.getBoolean("isFirstTimeContacts", true)
            if (!firstTime) {
                goToNextActivity()
                finish()
                return
            }
        }

        enableEdgeToEdge()
        binding = ActivityReadContactsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        binding.btnAllow.setOnClickListener { checkContactPermission() }

        binding.txtSkip.setOnClickListener {
            pref.edit { putBoolean("isFirstTimeContacts", false) }
            if (fromSettings) {
                finish()
            } else {
                goToNextActivity()
            }
        }
    }

    private fun checkContactPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            saveAndGoNext()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_CONTACTS),
                CONTACT_PERMISSION_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CONTACT_PERMISSION_CODE) {
            if (grantResults.isNotEmpty() &&
                grantResults[0] == PackageManager.PERMISSION_GRANTED
            ) {
                saveAndGoNext()
            }
        }
    }

    private fun saveAndGoNext() {
        getSharedPreferences("AppPrefs", MODE_PRIVATE).edit {
            putBoolean("isFirstTimeContacts", false)
        }

        if (fromSettings) {
            finish()
        } else {
            goToNextActivity()
        }
    }

    private fun goToNextActivity() {
        startActivity(Intent(this, ManagePhoneCallsActivity::class.java))
        finish()
    }

    override fun onResume() {
        super.onResume()

        // If user allowed permission from settings → close
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            saveAndGoNext()
        }
    }
}
