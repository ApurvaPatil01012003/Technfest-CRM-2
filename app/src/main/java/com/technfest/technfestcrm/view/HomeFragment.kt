package com.technfest.technfestcrm.view

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.HomeTaskAdapter
import com.technfest.technfestcrm.adapter.HotLeadAdapter
import com.technfest.technfestcrm.databinding.FragmentHomeBinding
import com.technfest.technfestcrm.model.LeadResponseItem
import com.technfest.technfestcrm.model.LocalTask
import com.technfest.technfestcrm.model.TaskResponseItem
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import android.telephony.SubscriptionManager
import android.widget.Switch


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var homeTaskAdapter: HomeTaskAdapter
    private lateinit  var fullName :String
    private val PREF_SIM_SYNC = "SimSyncPrefs"
    private val KEY_SYNCED_NUMBERS = "synced_numbers" // Set<String>


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        binding.leadsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        checkAndShowSimSyncDialog()

        var prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
         fullName = prefs.getString("fullName", "User").toString()

        prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
        val allLocalLeads = getLocalLeads(requireContext())
        val newLeads = allLocalLeads.filter { it.status.equals("new", ignoreCase = true) }

        binding.txtNewLeadCount.text = newLeads.size.toString()


        if (newLeads.isEmpty()) {
            binding.txtLeadNotAssign.visibility = View.VISIBLE
            binding.leadsRecyclerView.visibility = View.GONE
        } else {
            binding.txtLeadNotAssign.visibility = View.GONE
            binding.leadsRecyclerView.visibility = View.VISIBLE
        }

        setupHotLeads(newLeads)

//        binding.leadsRecyclerView.adapter = HotLeadAdapter(newLeads) { clickedLead ->
//            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
//            val fragment = LeadsFragment().apply {
//                arguments = Bundle().apply {
//                    putInt("leadId", clickedLead.id)
//                    putString("Token", prefs.getString("token", ""))
//                    putInt("WorkspaceId", prefs.getInt("workspaceId", -1))
//                    putString("Name", prefs.getString("fullName", "User"))
//                    putString("WorkspaceName", prefs.getString("workspaceName", ""))
//                }
//            }
//            loadFragment(fragment)
//        }


        binding.taskRecyclerView.layoutManager = LinearLayoutManager(requireContext())

//        homeTaskAdapter = HomeTaskAdapter(emptyList()) { task ->
//            val fragment = TaskFragment()
//            val bundle = Bundle().apply {
//                putInt("highlightTaskId", task.id)
//            }
//            fragment.arguments = bundle
//            loadFragment(fragment)
//        }
//        binding.taskRecyclerView.adapter = homeTaskAdapter



        val allLocalTasks = getLocalTasks(requireContext())

        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val todayPendingTasks = allLocalTasks.filter { task ->
            try {
                val taskDate = LocalDateTime.parse(task.dueAt, formatter).toLocalDate()
                taskDate == today && task.status.equals("pending", ignoreCase = true)
            } catch (e: Exception) {
                false
            }
        }.sortedBy { it.dueAt }

        binding.txtTodayPendingTaskCount.text = todayPendingTasks.size.toString()

        //homeTaskAdapter.updateList(todayPendingTasks)
        setupTodayTasks(todayPendingTasks)

        if (todayPendingTasks.isEmpty()) {
            binding.txtTaskNotAssign.visibility = View.VISIBLE
            binding.taskRecyclerView.visibility = View.GONE
        } else {
            binding.txtTaskNotAssign.visibility = View.GONE
            binding.taskRecyclerView.visibility = View.VISIBLE
        }





        return binding.root
    }

    private fun loadFragment(fragment: Fragment) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun getLocalTasks(context: Context): List<TaskResponseItem> {

        val prefs = context.getSharedPreferences("LocalTasks", Context.MODE_PRIVATE)
        val json = prefs.getString("task_list", null) ?: return emptyList()

        val type = object :
            com.google.gson.reflect.TypeToken<List<LocalTask>>() {}.type

        val localTasks: List<LocalTask> =
            com.google.gson.Gson().fromJson(json, type)

        return localTasks.map { it.toTaskResponseItem() }
    }

    private fun LocalTask.toTaskResponseItem(): TaskResponseItem {
        return TaskResponseItem(
            assignedEmployeeName = "Self",
            assignedToEmployeeId = 0,
            assignedToUserId = 0,
            assignedUserName = "Self",
            completedAt = "",
            createdAt = "",
            createdByName = "System",
            createdByUserId = 0,
            currentVersion = 1,
            departmentId = "",
            departmentName = "",
            description = this.description,
            dueAt = this.dueAt.toString(),
            dueDate = this.dueAt.toString(),
            estimatedHours = "0",
            id = this.id,
            isActive = true,
            lastActivityAt = "",
            leadId = 0,
            leadName = this.leadName.toString(),
            priority = this.priority.toString(),
            projectId = 0,
            projectName = "",
            status = this.status.toString(),
            taskType = this.taskType,
            title = this.title,
            totalLoggedMinutes = 0,
            updatedAt = "",
            workspaceId = 0
        )
    }
    private fun getLocalLeads(context: Context): List<com.technfest.technfestcrm.model.LeadResponseItem> {
        val localLeads = com.technfest.technfestcrm.localdatamanager.LocalLeadManager.getLeads(context)
        return localLeads.mapIndexed { index, lead ->
            com.technfest.technfestcrm.localdatamanager.LocalLeadMapper.toResponse(lead, index + 1)
        }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()

        // Refresh Leads
        val allLocalLeads = getLocalLeads(requireContext())
        val newLeads = allLocalLeads.filter { it.status.equals("new", true) }
        binding.txtNewLeadCount.text = newLeads.size.toString()
        setupHotLeads(newLeads)

        // Refresh Today Tasks
        val allLocalTasks = getLocalTasks(requireContext())
        val today = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        val todayPendingTasks = allLocalTasks.filter {
            try {
                LocalDateTime.parse(it.dueAt, formatter).toLocalDate() == today &&
                        it.status.equals("pending", true)
            } catch (e: Exception) {
                false
            }
        }

        setupTodayTasks(todayPendingTasks)

        (activity as? MainActivity)?.setupDrawer()
    }

    private fun setupHotLeads(newLeads: List<LeadResponseItem>) {
        binding.leadsRecyclerView.adapter = HotLeadAdapter(newLeads) { clickedLead ->
            val prefs = requireActivity().getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val fragment = LeadsFragment().apply {
                arguments = Bundle().apply {
                    putInt("leadId", clickedLead.id)
                    putString("Token", prefs.getString("token", ""))
                    putInt("WorkspaceId", prefs.getInt("workspaceId", -1))
                    putString("Name", prefs.getString("fullName", "User"))
                    putString("WorkspaceName", prefs.getString("workspaceName", ""))
                }
            }
            loadFragment(fragment)
        }
    }
    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupTodayTasks(todayPendingTasks: List<TaskResponseItem>) {

        homeTaskAdapter = HomeTaskAdapter(todayPendingTasks) { task ->
            val fragment = TaskFragment().apply {
                arguments = Bundle().apply {
                    putInt("highlightTaskId", task.id)
                }
            }
            loadFragment(fragment)
        }

        binding.taskRecyclerView.adapter = homeTaskAdapter
    }

//    @RequiresApi(Build.VERSION_CODES.O)
//    private fun checkAndShowSimSyncDialog() {
//        val prefs = requireContext().getSharedPreferences(PREF_SIM_SYNC, Context.MODE_PRIVATE)
//        val syncedNumbers = prefs.getStringSet(KEY_SYNCED_NUMBERS, emptySet())
//
//        // If no SIM synced → show dialog
//        if (syncedNumbers.isNullOrEmpty()) {
//            checkPermissionAndShowDialog()
//        }
//    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun checkAndShowSimSyncDialog() {
        val synced = com.technfest.technfestcrm.utils.SimSyncStore.getSynced(requireContext())
        if (synced.isEmpty()) checkPermissionAndShowDialog()
    }

    private fun checkPermissionAndShowDialog() {
        if (
            ActivityCompat.checkSelfPermission(
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
                201
            )
        } else {
            showSimSyncDialog()
        }
    }

//    @SuppressLint("MissingPermission")
//    private fun showSimSyncDialog() {
//
//        val dialogView = layoutInflater.inflate(R.layout.alert_sim_sync, null)
//        val container = dialogView.findViewById<LinearLayout>(R.id.layoutSimContainer)
//        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
//        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)
//
//        val dialog = AlertDialog.Builder(requireContext())
//            .setView(dialogView)
//            .setCancelable(false)
//            .create()
//
//        val subscriptionManager =
//            requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
//
//        val subscriptions = subscriptionManager.activeSubscriptionInfoList
//        val selectedNumbers = mutableSetOf<String>()
//
//        container.removeAllViews()
//
//        if (subscriptions.isNullOrEmpty()) {
//            val tv = TextView(requireContext()).apply {
//                text = "No SIM cards detected"
//            }
//            container.addView(tv)
//        } else {
//            for (info in subscriptions) {
//
//                val simView = layoutInflater.inflate(
//                    R.layout.item_sim_sync,
//                    container,
//                    false
//                )
//
//                val txtName = simView.findViewById<TextView>(R.id.txtSimName)
//                val txtNumber = simView.findViewById<TextView>(R.id.txtSimNumber)
//                val switchSync = simView.findViewById<Switch>(R.id.switchSync)
//
//                val simName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}"
//                val simNumber = info.number?.takeIf { it.isNotEmpty() } ?: "Unknown"
//
//                txtName.text = simName
//                txtNumber.text = simNumber
//
//                switchSync.setOnCheckedChangeListener { _, isChecked ->
//                    if (isChecked) {
//                        selectedNumbers.add(simNumber)
//                    } else {
//                        selectedNumbers.remove(simNumber)
//                    }
//                }
//
//                container.addView(simView)
//            }
//        }


    @SuppressLint("MissingPermission")
    private fun showSimSyncDialog() {

        val dialogView = layoutInflater.inflate(R.layout.alert_sim_sync, null)
        val container = dialogView.findViewById<LinearLayout>(R.id.layoutSimContainer)
        val btnSave = dialogView.findViewById<Button>(R.id.btnSave)
        val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        val subscriptionManager =
            requireContext().getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager

        val subscriptions = subscriptionManager.activeSubscriptionInfoList
        val newList = mutableListOf<com.technfest.technfestcrm.utils.SimSyncStore.SyncedSim>()

        container.removeAllViews()

        if (subscriptions.isNullOrEmpty()) {
            val tv = TextView(requireContext()).apply { text = "No SIM cards detected" }
            container.addView(tv)
        } else {

            // load old saved list (so switches show correctly)
            val saved = com.technfest.technfestcrm.utils.SimSyncStore.getAll(requireContext())

            for (info in subscriptions) {

                val simView = layoutInflater.inflate(R.layout.item_sim_sync, container, false)

                val txtName = simView.findViewById<TextView>(R.id.txtSimName)
                val txtNumber = simView.findViewById<TextView>(R.id.txtSimNumber)
                val switchSync = simView.findViewById<Switch>(R.id.switchSync)

                val simName = info.displayName?.toString() ?: "SIM ${info.simSlotIndex + 1}"
                val simNumber = info.number?.takeIf { it.isNotBlank() } // can be null
                val subId = info.subscriptionId
                val slot = info.simSlotIndex

                txtName.text = simName
                txtNumber.text = simNumber ?: "Unknown"

                val alreadySynced = saved.any { it.subId == subId && it.isSynced }
                switchSync.isChecked = alreadySynced

                switchSync.setOnCheckedChangeListener { _, isChecked ->
                    // we will rebuild list on Save
                }

                container.addView(simView)

                // store initial (we’ll update isSynced on save by reading switches)
                newList.add(
                    com.technfest.technfestcrm.utils.SimSyncStore.SyncedSim(
                        subId = subId,
                        slotIndex = slot,
                        displayName = simName,
                        number = simNumber,
                        isSynced = alreadySynced
                    )
                )
            }
        }

        btnSave.setOnClickListener {
            // rebuild from UI switches
            val finalList = mutableListOf<com.technfest.technfestcrm.utils.SimSyncStore.SyncedSim>()
            for (i in 0 until container.childCount) {
                val row = container.getChildAt(i)
                val sw = row.findViewById<Switch>(R.id.switchSync) ?: continue

                val sim = newList.getOrNull(i) ?: continue
                finalList.add(sim.copy(isSynced = sw.isChecked))
            }

            val syncedCount = finalList.count { it.isSynced }
            if (syncedCount == 0) {
                Toast.makeText(requireContext(), "Please sync at least one SIM", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            com.technfest.technfestcrm.utils.SimSyncStore.saveAll(requireContext(), finalList)
            dialog.dismiss()
        }

        btnCancel.setOnClickListener { dialog.dismiss() }

        dialog.show()
    }

//    btnSave.setOnClickListener {
//            if (selectedNumbers.isNotEmpty()) {
//                saveSyncedSims(selectedNumbers)
//                dialog.dismiss()
//            } else {
//                Toast.makeText(
//                    requireContext(),
//                    "Please sync at least one SIM",
//                    Toast.LENGTH_SHORT
//                ).show()
//            }
//        }
//
//        btnCancel.setOnClickListener {
//            // User skipped → dialog will appear next time again
//            dialog.dismiss()
//        }
//
//        dialog.show()
//    }
//    private fun saveSyncedSims(numbers: Set<String>) {
//        val prefs = requireContext().getSharedPreferences(PREF_SIM_SYNC, Context.MODE_PRIVATE)
//        prefs.edit()
//            .putStringSet(KEY_SYNCED_NUMBERS, numbers)
//            .apply()
//    }
//    override fun onRequestPermissionsResult(
//        requestCode: Int,
//        permissions: Array<out String>,
//        grantResults: IntArray
//    ) {
//        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
//
//        if (requestCode == 201 &&
//            grantResults.isNotEmpty() &&
//            grantResults.all { it == PackageManager.PERMISSION_GRANTED }
//        ) {
//            showSimSyncDialog()
//        }
//    }


}
