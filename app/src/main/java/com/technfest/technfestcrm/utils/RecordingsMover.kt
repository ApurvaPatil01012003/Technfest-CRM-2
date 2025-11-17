package com.technfest.technfestcrm.utils

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import java.io.File
import java.io.FileOutputStream
import androidx.core.net.toUri

object RecordingsMover {

    fun moveLatestRecording(context: Context) {

        val prefs = context.getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        val folderPath = prefs.getString("folder_path", null)

        if (folderPath == null) {
            Log.e("AUTO_MOVE", "No SAF folder selected.")
            return
        }

        val root = DocumentFile.fromTreeUri(context, folderPath.toUri()) ?: return

        val audioFiles = root.listFiles().filter {
            it.name?.endsWith(".mp3") == true ||
                    it.name?.endsWith(".m4a") == true ||
                    it.name?.endsWith(".aac") == true ||
                    it.name?.endsWith(".wav") == true
        }

        if (audioFiles.isEmpty()) {
            Log.e("AUTO_MOVE", "No audio found")
            return
        }

        val latest = audioFiles.maxByOrNull { it.lastModified() } ?: return

        val phone = extractPhone(latest.name ?: "unknown")
        val time  = latest.lastModified()
        val outName = "${phone}-${time}.mp3"

        val targetFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "downloaded_rom"
        )
        if (!targetFolder.exists()) targetFolder.mkdirs()

        val outputFile = File(targetFolder, outName)

        context.contentResolver.openInputStream(latest.uri)?.use { input ->
            FileOutputStream(outputFile).use { output ->
                input.copyTo(output)
            }
        }

        Log.d("AUTO_MOVE", "MOVED → ${latest.name} → $outName")
    }

    private fun extractPhone(name: String): String {
        val digits = Regex("\\d+").findAll(name).map { it.value }.toList()
        val longest = digits.maxByOrNull { it.length } ?: return "unknown"
        return if (longest.length >= 10) longest.takeLast(10) else "unknown"
    }
}
