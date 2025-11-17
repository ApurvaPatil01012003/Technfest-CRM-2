package com.technfest.technfestcrm.view

import android.content.Intent
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityMainBinding
import androidx.core.content.edit

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Apply ONLY top inset to the main container (status bar)
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)

            // Remove bottom padding from BottomNavigationView
            binding.bottomNavigationView.setPadding(0, 0, 0, 0)

            insets
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

}