package com.technfest.technfestcrm.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityMainBinding
import com.technfest.technfestcrm.receiver.AutoMoveForegroundService
import com.technfest.technfestcrm.receiver.CallStateForegroundService
import com.technfest.technfestcrm.utils.AllRecordingsAutoMover
import com.technfest.technfestcrm.repository.AuthMeRepository
import com.technfest.technfestcrm.viewmodel.AuthMeViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authMeViewModel: AuthMeViewModel

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
        startAutoMoveService()

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = prefs.getString("token", null)

        if (token.isNullOrEmpty()) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        authMeViewModel = AuthMeViewModel(AuthMeRepository())
        authMeViewModel.fetchAuthMe(token)
        authMeViewModel.authMeResponse.observe(this) { user ->
            if (user != null) {

                prefs.edit {
                    putString("fullName", user.fullName)
                    putInt("workspaceId", user.workspaceId)
                }
                openHomeFragment(user.fullName, token, user.workspaceId)
            }
        }
        authMeViewModel.error.observe(this) { msg ->
            Log.e("AuthMe", msg)
            openHomeFragment(
                prefs.getString("fullName", "User") ?: "User",
                token,
                prefs.getInt("workspaceId", -1)
            )
        }

        setupBottomNavigation()
        setDefaultDialer()
    }

    private fun startAutoMoveService() {
        val serviceIntent = Intent(this, AutoMoveForegroundService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
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

    private fun setDefaultDialer() {
        val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER)
        intent.putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, packageName)
        startActivity(intent)
    }

    private fun openHomeFragment(fullName: String?, token: String?, workspaceId: Int) {
        val homeFragment = HomeFragment()
        val bundle = Bundle().apply {
            putString("FullName", fullName)
            putString("Token", token)
            putInt("WorkSpaceId", workspaceId)
        }
        homeFragment.arguments = bundle
        loadFragment(homeFragment)
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.itemIconTintList = null
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
            val token = prefs.getString("token", null)
            val fullName = prefs.getString("fullName", "User")
            val workspaceId = prefs.getInt("workspaceId", -1)
            val workspaceName = prefs.getString("workspaceName", "Workspace")

            when (item.itemId) {
                R.id.nav_home -> openHomeFragment(fullName, token, workspaceId)
                R.id.nav_leads -> {
                    val leadsFragment = LeadsFragment()
                    val bundle = Bundle().apply {
                        putString("Token", token)
                        putInt("WorkspaceId", workspaceId)
                        putString("Name", fullName)
                        putString("WorkspaceName", workspaceName)
                    }
                    leadsFragment.arguments = bundle
                    loadFragment(leadsFragment)
                }
                R.id.nav_task -> {
                    val taskFragment = TaskFragment()
                    val bundle = Bundle().apply {
                        putString("token", token)
                        putInt("workspaceId", workspaceId)
                    }
                    taskFragment.arguments = bundle
                    loadFragment(taskFragment)
                }

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
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                AllRecordingsAutoMover(this).autoMoveRecordings()
                Log.d("MainActivity", "Auto move recordings triggered on resume")
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error in autoMoveRecordings", e)
        }
    }

    @Deprecated("This method has been deprecated in favor of using the\n      {@link OnBackPressedDispatcher} via {@link #getOnBackPressedDispatcher()}.\n      The OnBackPressedDispatcher controls how back button events are dispatched\n      to one or more {@link OnBackPressedCallback} objects.")
    override fun onBackPressed() {
        val currentFragment = supportFragmentManager.findFragmentById(R.id.fragmentContainer)
        if (currentFragment is HomeFragment) {
            finishAffinity()
        } else {
            super.onBackPressed()
        }
    }
    fun openLeadsFromHome() {
        binding.bottomNavigationView.selectedItemId = R.id.nav_leads
    }
    fun openTasksFromHome() {
        binding.bottomNavigationView.selectedItemId = R.id.nav_task
    }

}
