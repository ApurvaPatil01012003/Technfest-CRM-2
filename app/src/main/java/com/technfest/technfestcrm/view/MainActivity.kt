package com.technfest.technfestcrm.view

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import android.util.Log
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.ActivityMainBinding
import com.technfest.technfestcrm.receiver.CallStateForegroundService
import com.technfest.technfestcrm.utils.AllRecordingsAutoMover
import com.technfest.technfestcrm.repository.AuthMeRepository
import com.technfest.technfestcrm.viewmodel.AuthMeViewModel

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var authMeViewModel: AuthMeViewModel

    data class DrawerItem(val title: String, val icon: Int, val badgeCount: Int = 0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        handleIntent(intent)

        startCallStateServiceSafely()
        supportFragmentManager.setFragmentResultListener(
            "lead_added",
            this
        ) { _, _ ->
            setupDrawer()
        }

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setHomeAsUpIndicator(R.drawable.baseline_menu_24)

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


        setDefaultDialer()
        handleLeadOpenIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "task_channel",
                "Task Notifications",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(android.app.NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        handleNotificationIntent(intent)


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
                != android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 2001)
            }
        }


    }


    private fun handleNotificationIntent(intent: Intent?): Boolean {
        val highlightTaskId = intent?.getIntExtra("highlightTaskId", -1) ?: -1
        if (highlightTaskId != -1) {
            openTaskFromNotification(highlightTaskId)
            return true
        }
        return false
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


    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        setupDrawer()
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


    internal fun setupDrawer() {
        val drawerBinding =
            com.technfest.technfestcrm.databinding.CustomDrawerBinding.bind(binding.customDrawer.root)
        val container = drawerBinding.drawerMenuContainer
        container.removeAllViews() // prevent duplicates

        val prefs = getSharedPreferences("UserSession", MODE_PRIVATE)
        val token = prefs.getString("token", null)
        val fullName = prefs.getString("fullName", "User")
        val workspaceId = prefs.getInt("workspaceId", -1)
        val workspaceName = prefs.getString("workspaceName", "Workspace")

        val allLocalLeads = com.technfest.technfestcrm.localdatamanager.LocalLeadManager.getLeads(this)
        val totalLeadsCount = allLocalLeads.size

        val allLocalTasksPrefs = getSharedPreferences("LocalTasks", MODE_PRIVATE)
        val tasksJson = allLocalTasksPrefs.getString("task_list", null)
        val totalTasksCount = if (tasksJson != null) {
            val type = object : com.google.gson.reflect.TypeToken<List<com.technfest.technfestcrm.model.LocalTask>>() {}.type
            val localTasks: List<com.technfest.technfestcrm.model.LocalTask> =
                com.google.gson.Gson().fromJson(tasksJson, type)
            localTasks.size
        } else 0



        val menuItems = listOf(
            DrawerItem("Dashboard", R.drawable.home),
            DrawerItem("Add Lead", R.drawable.plus),
            DrawerItem("My Leads", R.drawable.leadschange, totalLeadsCount),
            DrawerItem("Tasks", R.drawable.taskchange, totalTasksCount),
            DrawerItem("Campaigns", R.drawable.change),
            DrawerItem("Calls", R.drawable.calls),
            DrawerItem("Reports", R.drawable.report),
            DrawerItem("Setting", R.drawable.settings)
        )

        menuItems.forEachIndexed { index, item ->
            val view = layoutInflater.inflate(R.layout.drawer_item, container, false)

            view.findViewById<ImageView>(R.id.icon).setImageResource(item.icon)
            view.findViewById<TextView>(R.id.title).text = item.title

            val badge = view.findViewById<TextView>(R.id.badge)
            if (item.badgeCount > 0) {
                badge.text = item.badgeCount.toString()
                badge.visibility = android.view.View.VISIBLE
            }

            view.setOnClickListener {
                binding.homeDrawer.closeDrawer(androidx.core.view.GravityCompat.START)
                handleDrawerClick(item, token, fullName, workspaceId, workspaceName)
            }

            container.addView(view)

            // Add divider except after last item
            if (index < menuItems.size - 1) {
                val divider = android.view.View(this)
                divider.layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1
                )
                divider.setBackgroundColor(android.graphics.Color.parseColor("#DDDDDD"))
                container.addView(divider)
            }
        }
    }

    private fun handleDrawerClick(
        item: DrawerItem,
        token: String?,
        fullName: String?,
        workspaceId: Int,
        workspaceName: String?
    ) {
        when (item.title) {
            "Dashboard" -> openHomeFragment(fullName, token, workspaceId)
            "Add Lead" -> {
                val fragment = AddNewLeadFragment()
                fragment.arguments = Bundle().apply {
                    putString("token", token)
                    putInt("workspaceId", workspaceId)
                }
                loadFragment(fragment)
            }

            "My Leads" -> {
                val fragment = LeadsFragment()
                fragment.arguments = Bundle().apply {
                    putString("Token", token)
                    putInt("WorkspaceId", workspaceId)
                    putString("Name", fullName)
                    putString("WorkspaceName", workspaceName)
                }
                loadFragment(fragment)
            }

            "Tasks" -> {
                val fragment = TaskFragment()
                fragment.arguments = Bundle().apply {
                    putString("token", token)
                    putInt("workspaceId", workspaceId)
                }
                loadFragment(fragment)
            }

            "Campaigns" -> {
                val fragment = CallsCampaignFragment()
                fragment.arguments = Bundle().apply {
                    putString("token", token)
                    putInt("workspaceId", workspaceId)
                }
                loadFragment(fragment)
            }

            "Calls" -> loadFragment(CallsFragment())
            "Reports" -> loadFragment(ReportFragment())
            "Setting" -> loadFragment(SettingFragment())
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        binding.homeDrawer.openDrawer(androidx.core.view.GravityCompat.START)
        return true
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleLeadOpenIntent(intent)
        handleNotificationIntent(intent)
        handleIntent(intent)
    }

    private fun handleLeadOpenIntent(intent: Intent?) {
        val leadId = intent?.getIntExtra("OPEN_LEAD_ID", 0) ?: 0
        val workspaceId = intent?.getIntExtra("OPEN_LEAD_WORKSPACE_ID", 0) ?: 0
        val token = intent?.getStringExtra("OPEN_LEAD_TOKEN") ?: ""

        if (leadId > 0 && token.isNotBlank()) {

            val fragment = LeadDetailFragment.newInstanceById(
                leadId = leadId,
                token = token,
                workspaceId = workspaceId
            )

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }
    }

    private fun handleIntent(intent: Intent?) {
        intent ?: return

        val fragmentToOpen = intent.getStringExtra("openFragment")
        val highlightTaskId = intent.getIntExtra("highlightTaskId", -1)

        if (fragmentToOpen == "TASK") {
            val fragment = TaskFragment().apply {
                arguments = Bundle().apply {
                    if (highlightTaskId != -1) {
                        putInt("highlightTaskId", highlightTaskId)
                    }
                }
            }

            supportFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .commit()
        }
    }


    private fun openTaskFromNotification(taskId: Int) {
        val fragment = TaskFragment().apply {
            arguments = Bundle().apply {
                putInt("highlightTaskId", taskId)
            }
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }




}
