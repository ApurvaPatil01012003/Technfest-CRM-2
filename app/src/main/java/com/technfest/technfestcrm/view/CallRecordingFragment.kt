package com.technfest.technfestcrm.view

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.databinding.FragmentCallRecordingBinding
import androidx.core.content.edit

class CallRecordingFragment : Fragment() {
    private var _binding: FragmentCallRecordingBinding? = null
    private val binding get() = _binding!!
    private var token: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        token = arguments?.getString("Token")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCallRecordingBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val savedPath = getSavedFolderPath()

        if (!savedPath.isNullOrEmpty()) {
            updateUI(savedPath)
        }

        binding.btnSelectFolder.setOnClickListener {
            folderPickerLauncher.launch(null)
        }

        binding.btnRemoveFolder.setOnClickListener {
            removeFolder()
            binding.tvFolderStatus.text = "No folder selected"
            binding.btnSelectFolder.visibility = View.VISIBLE
            binding.btnRemoveFolder.visibility = View.GONE
        }
        binding.tvRecordingUploads.setOnClickListener {
            val fragment = AllRecordingsFragment()
            val bundle = Bundle()
            bundle.putString("Token", token)
            fragment.arguments = bundle

            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit()
        }

    }


    private val folderPickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->

            if (uri != null) {

                requireContext().contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                )

                saveFolderPath(uri.toString())
                updateUI(uri.toString())
                showSyncDialog()
            }
        }


    private fun saveFolderPath(path: String) {
        val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        prefs.edit() { putString("folder_path", path) }
    }

    private fun getSavedFolderPath(): String? {
        val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        return prefs.getString("folder_path", null)
    }

    private fun removeFolder() {
        val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        prefs.edit() { remove("folder_path") }
    }

    private fun updateUI(folderPath: String) {
        binding.tvFolderStatus.text = folderPath
        binding.btnSelectFolder.visibility = View.GONE
        binding.layoutRemoveFolder.visibility = View.VISIBLE
    }

    private fun showSyncDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Call Recording Path Added")
            .setMessage("Do you want to sync your call recordings?")
            .setCancelable(false)
            .setPositiveButton("Yes") { dialog, _ ->

                val recordingsList = getAllRecordingsFromFolder()
                saveRecordingsList(recordingsList)
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
    private fun saveRecordingsList(list: List<String>) {
        val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)

        prefs.edit().putStringSet("recordings_list", list.toSet()).apply()
    }
    private fun getAllRecordingsFromFolder(): List<String> {
        val savedPath = getSavedFolderPath() ?: return emptyList()
        val treeUri = android.net.Uri.parse(savedPath)

        val recordings = mutableListOf<String>()

        try {
            // Extract the document ID from tree URI
            val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)

            // Build the children URI
            val childrenUri = android.provider.DocumentsContract.buildChildDocumentsUriUsingTree(
                treeUri,
                docId
            )

            val cursor = requireContext().contentResolver.query(
                childrenUri,
                arrayOf(
                    android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                    android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE
                ),
                null,
                null,
                null
            )

            cursor?.use {
                val nameIndex =
                    it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIndex =
                    it.getColumnIndex(android.provider.DocumentsContract.Document.COLUMN_MIME_TYPE)

                while (it.moveToNext()) {
                    val name = it.getString(nameIndex)
                    val mime = it.getString(mimeIndex)

                    if (mime != null && mime.startsWith("audio")) {
                        recordings.add(name)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return recordings
    }


    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onResume() {
        super.onResume()

        try {
            val fragment = parentFragmentManager.findFragmentByTag("AllRecordingsAuto")
            if (fragment == null) {
                val autoFragment = AllRecordingsFragment()
                parentFragmentManager.beginTransaction()
                    .add(autoFragment, "AllRecordingsAuto")
                    .commitNowAllowingStateLoss()

                autoFragment.autoMoveRecordings()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }




}