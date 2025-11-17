package com.technfest.technfestcrm.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import androidx.core.content.edit
import androidx.core.net.toUri

class AllRecordingsAutoMover(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun autoMoveRecordings() {
        val prefs = context.getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        val treeUri = prefs.getString("folder_path", null) ?: return
        Log.d("AutoMover", "Folder Uri = $treeUri")

        val root = DocumentFile.fromTreeUri(context, treeUri.toUri()) ?: return

        val targetFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "downloaded_rom"
        )
        if (!targetFolder.exists()) targetFolder.mkdirs()

        root.listFiles().forEach { file ->

            val name = file.name ?: return@forEach
            val lower = name.lowercase()
            Log.d("AutoMover", "Processing file = $name")

            if (!lower.endsWith(".mp3") &&
                !lower.endsWith(".m4a") &&
                !lower.endsWith(".aac") &&
                !lower.endsWith(".wav")
            ) return@forEach

            val recordingId = file.uri.toString()
            if (isAlreadyMoved(recordingId)) {
                Log.d("AutoMover", "SKIP (already moved) → $recordingId")
                return@forEach
            }

            val phone = extractPhone(name) ?: "unknown"
            val timestamp = getTimestamp(file.uri)

            val outName = "$phone-$timestamp.mp3"
            val outFile = File(targetFolder, outName)
            Log.d("AutoMover", "Saving to = $outFile")

            // If file already exists, just mark as moved and skip copying
            if (outFile.exists()) {
                Log.d("AutoMover", "File already exists, marking moved → $outName")
                markMoved(recordingId)
                return@forEach
            }

            // Copy file
            context.contentResolver.openInputStream(file.uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }

            markMoved(recordingId)
            Log.d("AutoMover", "MOVED: $name → $outName")
        }
    }

    private fun extractPhone(filename: String): String? {
        val groups = Regex("\\d+").findAll(filename).map { it.value }.toList()
        val longest = groups.maxByOrNull { it.length } ?: return null
        return if (longest.length >= 10) longest.takeLast(10) else null
    }

    private fun getTimestamp(uri: Uri): Long {
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null, null, null
        )
        var ts = System.currentTimeMillis()
        cursor?.use {
            if (it.moveToFirst()) ts = it.getLong(0)
        }
        return ts
    }

    // 🔑 Now key = recordingId (uri string), not phone+timestamp
    private fun markMoved(recordingId: String) {
        val prefs = context.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
        prefs.edit { putBoolean(recordingId, true) }
    }

    private fun isAlreadyMoved(recordingId: String): Boolean {
        val prefs = context.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
        return prefs.getBoolean(recordingId, false)
    }
}
