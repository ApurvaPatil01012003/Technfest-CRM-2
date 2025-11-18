package com.technfest.technfestcrm.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityMainBinding
import androidx.core.content.edit
import com.technfest.technfestcrm.receiver.AutoMoveForegroundService
import com.technfest.technfestcrm.receiver.CallStateForegroundService
import com.technfest.technfestcrm.utils.AllRecordingsAutoMover

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            binding.bottomNavigationView.setPadding(0, 0, 0, 0)

            insets
        }
        startCallStateServiceSafely()
        val serviceIntent = Intent(this, AutoMoveForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }


        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
        intent.putExtra(
            TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME,
            this.packageName
        )

        startActivity(intent)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottomNavigationView)
        bottomNavigationView.itemIconTintList = null
        loadFragment(HomeFragment())

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> loadFragment(HomeFragment())
                R.id.nav_leads -> loadFragment(LeadsFragment())
                R.id.nav_task -> loadFragment(TaskFragment())
               R.id.nav_settings -> loadFragment(SettingFragment())
            }
            true
        }

    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }
    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            try {
                AllRecordingsAutoMover(this).autoMoveRecordings()
                Log.d("MainActivity", "Auto move recordings triggered on resume")
            } catch (e: Exception) {
                Log.e("MainActivity", "Error in autoMoveRecordings", e)
            }
        }
        val prefs = getSharedPreferences("AppPrefs", MODE_PRIVATE)
        val cameFromToggle = prefs.getBoolean("contactsToggleFlow", false)

        if (cameFromToggle) {

            prefs.edit() { putBoolean("contactsToggleFlow", false) }

            if (!PermissionHelper.hasReadContacts(this)) {
                val intent = Intent(this, ReadContactsActivity::class.java)
                intent.putExtra(ReadContactsActivity.EXTRA_FROM_SETTINGS, true)
                startActivity(intent)
            }
        }
    }
    private fun startCallStateServiceSafely() {
        val intent = Intent(this, CallStateForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}