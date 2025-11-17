package com.technfest.technfestcrm.utils

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import android.provider.DocumentsContract
import java.io.File
import java.io.FileOutputStream

class AllRecordingsAutoMover(private val context: Context) {

    @RequiresApi(Build.VERSION_CODES.Q)
    fun autoMoveRecordings() {
        val prefs = context.getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
        val treeUri = prefs.getString("folder_path", null) ?: return

        val root = DocumentFile.fromTreeUri(context, Uri.parse(treeUri)) ?: return

        val targetFolder = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
            "downloaded_rom"
        )
        if (!targetFolder.exists()) targetFolder.mkdirs()

        root.listFiles().forEach { file ->

            val name = file.name ?: return@forEach

            if (!name.endsWith(".mp3") && !name.endsWith(".m4a")) return@forEach

            val phone = extractPhone(name) ?: "unknown"
            val timestamp = getTimestamp(file.uri)

            val uniqueKey = "$phone-$timestamp"

            if (isAlreadyMoved(uniqueKey)) {
                return@forEach
            }

            val outFile = File(targetFolder, "$uniqueKey.mp3")

            // Copy file
            context.contentResolver.openInputStream(file.uri)?.use { input ->
                FileOutputStream(outFile).use { output ->
                    input.copyTo(output)
                }
            }

            markMoved(uniqueKey)
        }
    }

    private fun extractPhone(filename: String): String? {
        val groups = Regex("\\d+").findAll(filename).map { it.value }.toList()
        val longest = groups.maxByOrNull { it.length } ?: return null
        return longest.takeLast(10)
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

    private fun markMoved(key: String) {
        val prefs = context.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
        prefs.edit().putBoolean(key, true).apply()
    }

    private fun isAlreadyMoved(key: String): Boolean {
        val prefs = context.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
        return prefs.getBoolean(key, false)
    }
}
