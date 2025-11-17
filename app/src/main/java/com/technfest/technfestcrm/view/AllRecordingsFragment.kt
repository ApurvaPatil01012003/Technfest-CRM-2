package com.technfest.technfestcrm.view

import android.content.Context

import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.provider.DocumentsContract
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.button.MaterialButton
import com.technfest.technfestcrm.R
import com.technfest.technfestcrm.adapter.UniversalRecordingsAdapter
import com.technfest.technfestcrm.databinding.FragmentAllRecordingsBinding
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

class AllRecordingsFragment : Fragment() {

    private var _binding: FragmentAllRecordingsBinding? = null
    private val binding get() = _binding!!

    private var mediaPlayer: MediaPlayer? = null
    private var isPlaying = false

//    @RequiresApi(Build.VERSION_CODES.Q)
//    private val folderPicker =
//        registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
//            if (uri != null) {
//                requireContext().contentResolver.takePersistableUriPermission(
//                    uri,
//                    Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
//                )
//
//                val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
//                prefs.edit { putString("tree_uri", uri.toString()) }
//
//                refreshList()
//            }
//        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAllRecordingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.recyclerCallRecordings.layoutManager = LinearLayoutManager(requireContext())

        refreshList()

//        binding.btnMoveRecordings.setOnClickListener {
//            moveAllRecordings()
//        }

//        binding.btnSelectFolder.setOnClickListener {
//            folderPicker.launch(null)
//        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun refreshList() {
        val list = readRecordings()
        binding.recyclerCallRecordings.adapter = UniversalRecordingsAdapter(list) { file ->
            showPlayDialog(file)
        }
    }

    private fun readRecordings(): List<DocumentFile> {
        val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        val treeUri = prefs.getString("folder_path", null) ?: return emptyList()

        val root = DocumentFile.fromTreeUri(requireContext(), Uri.parse(treeUri)) ?: return emptyList()

        return root.listFiles().filter {
            val n = it.name?.lowercase() ?: ""
            n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".wav")
        }
    }

    private fun showPlayDialog(file: DocumentFile) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_recording_player, null)
        val tvName = dialogView.findViewById<TextView>(R.id.tvRecordingName)
        val btnPlay = dialogView.findViewById<ImageButton>(R.id.btnPlayPause)
        val seekBar = dialogView.findViewById<SeekBar>(R.id.seekBarAudio)
        val btnClose = dialogView.findViewById<MaterialButton>(R.id.btnClose)

        tvName.text = file.name ?: "Recording"

        val dialog = AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnClose.setOnClickListener {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            dialog.dismiss()
        }

        dialog.show()

        playRecording(file.uri, seekBar, btnPlay)
    }

    private fun playRecording(uri: Uri, seekBar: SeekBar, play: ImageButton) {
        mediaPlayer?.release()
        mediaPlayer = MediaPlayer().apply {
            setDataSource(requireContext(), uri)
            prepare()
            start()
        }

        isPlaying = true
        play.setImageResource(R.drawable.baseline_pause_circle_outline_24)

        seekBar.max = mediaPlayer!!.duration
        updateSeek(seekBar)

        play.setOnClickListener {
            if (isPlaying) {
                mediaPlayer?.pause()
                play.setImageResource(R.drawable.baseline_play_circle_outline_24)
            } else {
                mediaPlayer?.start()
                play.setImageResource(R.drawable.baseline_pause_circle_outline_24)
                updateSeek(seekBar)
            }
            isPlaying = !isPlaying
        }

        mediaPlayer!!.setOnCompletionListener {
            isPlaying = false
            play.setImageResource(R.drawable.baseline_play_circle_outline_24)
            seekBar.progress = 0
        }
    }

    private fun updateSeek(seekBar: SeekBar) {
        val handler = Handler()
        handler.post(object : Runnable {
            override fun run() {
                mediaPlayer?.let {
                    seekBar.progress = it.currentPosition
                    if (it.isPlaying) handler.postDelayed(this, 150)
                }
            }
        })

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) mediaPlayer?.seekTo(progress)
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
    }
    @RequiresApi(Build.VERSION_CODES.Q)
    private fun moveAllRecordings() {
        val prefs = requireContext().getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        val treeUri = prefs.getString("folder_path", null) ?: return

        val root = DocumentFile.fromTreeUri(requireContext(), treeUri.toUri()) ?: return

        val targetFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "downloaded_rom"
        )
        if (!targetFolder.exists()) targetFolder.mkdirs()

        root.listFiles().forEach { file ->
            val name = file.name ?: return@forEach

            if (!name.endsWith(".mp3", true) &&
                !name.endsWith(".m4a", true) &&
                !name.endsWith(".aac", true) &&
                !name.endsWith(".wav", true)
            ) return@forEach

            val recordingId = file.uri.toString()
            if (isAlreadyMoved(recordingId)) {
                Log.d("Recordings", "SKIP (already moved by URI) → $recordingId")
                return@forEach
            }

            val phone = extractValidPhoneNumber(name) ?: "unknown"
            val timestamp = getFileTimestamp(requireContext(), file.uri)
            val outName = "$phone-$timestamp.mp3"
            val outFile = File(targetFolder, outName)

            if (outFile.exists()) {
                markMoved(recordingId)
                Log.d("Recordings", "File already exists, just marking moved → $outName")
                return@forEach
            }

            requireContext().contentResolver.openInputStream(file.uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }

            markMoved(recordingId)
            Log.d("Recordings", "MOVED: $name → $outName")
        }

        refreshList()
    }

    private fun markMoved(recordingId: String) {
        val prefs = requireContext().getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
        prefs.edit { putBoolean(recordingId, true) }
    }

    private fun isAlreadyMoved(recordingId: String): Boolean {
        val prefs = requireContext().getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
        return prefs.getBoolean(recordingId, false)
    }

    private fun getFileTimestamp(ctx: Context, uri: Uri): Long {
        var result = System.currentTimeMillis()
        val cursor = ctx.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null, null, null
        )
        cursor?.use {
            if (it.moveToFirst()) result = it.getLong(0)
        }
        return result
    }




    override fun onDestroyView() {
        super.onDestroyView()
        mediaPlayer?.release()
        mediaPlayer = null
        _binding = null
    }
    fun extractValidPhoneNumber(filename: String): String? {
        val groups = Regex("\\d+").findAll(filename)
            .map { it.value }
            .toList()

        if (groups.isEmpty()) return null
        val longest = groups.maxByOrNull { it.length } ?: return null
        if (longest.length < 10) return null

        return when {
            longest.length == 10 -> longest
            longest.length in 11..15 -> longest.takeLast(10)
            else -> null
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    fun autoMoveRecordings() {
        moveAllRecordings()
    }


}
