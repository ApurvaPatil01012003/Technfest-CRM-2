//package com.technfest.technfestcrm.worker
//
//import android.content.ContentValues
//import android.content.Context
//import android.net.Uri
//import android.os.Build
//import android.provider.DocumentsContract
//import android.provider.MediaStore
//import android.util.Base64
//import android.util.Log
//import androidx.annotation.RequiresApi
//import androidx.documentfile.provider.DocumentFile
//import androidx.work.CoroutineWorker
//import androidx.work.WorkerParameters
//import com.technfest.technfestcrm.model.RecordingUploadRequest
//import com.technfest.technfestcrm.repository.RecordingsRepository
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.delay
//import kotlinx.coroutines.withContext
//import java.util.Locale
//
//class MoveRecordingsWorker(
//    private val ctx: Context,
//    params: WorkerParameters
//) : CoroutineWorker(ctx, params) {
//
//    private val TAG = "MoveWorker"
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    override suspend fun doWork(): Result {
//        return try {
//            Log.d(TAG, "Starting recording move + upload…")
//
//            val prefsRec = ctx.getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
//            val treeUriStr = prefsRec.getString("folder_path", null)
//            if (treeUriStr.isNullOrEmpty()) {
//                Log.d(TAG, "No recordings folder selected, skipping.")
//                return Result.success()
//            }
//
//            val sessionPrefs = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
//            val token = sessionPrefs.getString("token", null)
//            if (token.isNullOrEmpty()) {
//                Log.e(TAG, "No token found, cannot upload.")
//                return Result.success()
//            }
//
//            val callLogPrefs = ctx.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE)
//            val callLogId =
//                inputData.getInt("CALL_LOG_ID", 0).takeIf { it > 0 }
//                    ?: callLogPrefs.getInt("lastCallLogId", 0)
//
//            if (callLogId == 0) {
//                Log.e(TAG, "No callLogId found, skipping.")
//                return Result.success()
//            }
//
//            val callEndMs = inputData.getLong("CALL_END_MS", 0L)
//
//            val root = DocumentFile.fromTreeUri(ctx, Uri.parse(treeUriStr))
//                ?: run {
//                    Log.e(TAG, "Root DocumentFile is null.")
//                    return Result.success()
//                }
//
//            val movedPrefs = ctx.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
//            val repo = RecordingsRepository()
//
////            // ✅ wait until dialer actually writes the file (Samsung delay)
////            val candidates = waitAndFindRecentRecordings(root, callEndMs)
////
////            if (candidates.isEmpty()) {
////                Log.d(TAG, "No new recordings found after waiting. Done.")
////                return Result.success()
////            }
////
////            var anySuccess = false
//
////            for (file in candidates) {
////                val name = file.name ?: continue
////                val recordingId = file.uri.toString()
////
////                if (movedPrefs.getBoolean(recordingId, false)) {
////                    Log.d(TAG, "SKIP already moved/uploaded: $name")
////                    continue
////                }
////
////                // 1) Copy to Downloads/downloaded_rom (local)
////                val localUri = saveToDownloadsDownloadedRom(file, name)
////                if (localUri == null) {
////                    Log.e(TAG, "Failed to save locally: $name")
////                    continue
////                }
////                Log.d(TAG, "Saved locally → $localUri")
////
////                // 2) Upload to server (your existing base64 API)
////                val mimeType = ctx.contentResolver.getType(file.uri) ?: guessAudioMime(name)
////                val base64 = withContext(Dispatchers.IO) { fileToBase64(file.uri, mimeType) }
////                if (base64.isEmpty()) {
////                    Log.e(TAG, "Failed to encode base64: $name")
////                    continue
////                }
////
////                val request = RecordingUploadRequest(
////                    fileBase64 = base64,
////                    filename = name
////                )
////
////                try {
////                    val resp = repo.uploadRecording(token, callLogId, request)
////                    if (resp.isSuccessful) {
////                        anySuccess = true
////                        movedPrefs.edit().putBoolean(recordingId, true).apply()
////
////                        val body = resp.body()
////                        Log.d(TAG, "Uploaded for callLogId=$callLogId url=${body?.recordingUrl}")
////
////                        // optional: store last url
////                        body?.recordingUrl?.let { url ->
////                            callLogPrefs.edit().putString("lastRecordingUrl", url).apply()
////                        }
////                    } else {
////                        Log.e(TAG, "Upload failed $name: code=${resp.code()} err=${resp.errorBody()?.string()}")
////                        // NOTE: still kept local copy in Downloads/downloaded_rom
////                    }
////                } catch (e: Exception) {
////                    Log.e(TAG, "Exception uploading $name", e)
////                    // NOTE: still kept local copy in Downloads/downloaded_rom
////                }
////            }
//
//            //  Log.d(TAG, "Completed. anySuccess=$anySuccess")
//            val candidates = waitAndFindRecentRecordings(root, callEndMs)
//            if (candidates.isEmpty()) {
//                Log.d(TAG, "No new recordings found after waiting. Done.")
//                return Result.success()
//            }
//
//// ✅ pick only latest
//            val latest = candidates.maxByOrNull { getLastModifiedMs(it.uri) }
//                ?: return Result.success()
//
//            processOne(latest, callLogId, callEndMs)
//
//            Result.success()
//
//        } catch (e: Exception) {
//            Log.e(TAG, "Worker error", e)
//            Result.retry()
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private suspend fun waitAndFindRecentRecordings(
//        root: DocumentFile,
//        callEndMs: Long
//    ): List<DocumentFile> {
//        // check up to ~45 seconds
//        repeat(6) {
//            val files = root.listFiles().filter { f ->
//                val n = f.name?.lowercase(Locale.getDefault()) ?: ""
//                val isAudio = n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".wav")
//                if (!isAudio) return@filter false
//
//                if (callEndMs <= 0L) return@filter true // fallback: take all audio
//
//                val lm = getLastModifiedMs(f.uri)
//                lm >= (callEndMs - 60_000) // within last 1 min window
//            }
//
//            if (files.isNotEmpty()) return files
//
//            delay(7_000)
//        }
//        return emptyList()
//    }
//
//    private fun getLastModifiedMs(uri: Uri): Long {
//        var ts = 0L
//        val cursor = ctx.contentResolver.query(
//            uri,
//            arrayOf(DocumentsContract.Document.COLUMN_LAST_MODIFIED),
//            null, null, null
//        )
//        cursor?.use { if (it.moveToFirst()) ts = it.getLong(0) }
//        return ts
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private fun saveToDownloadsDownloadedRom(file: DocumentFile, displayName: String): Uri? {
//        return try {
//            val resolver = ctx.contentResolver
//
//            val values = ContentValues().apply {
//                put(MediaStore.Downloads.DISPLAY_NAME, safeName(displayName))
//                put(MediaStore.Downloads.MIME_TYPE, resolver.getType(file.uri) ?: guessAudioMime(displayName))
//                put(MediaStore.Downloads.RELATIVE_PATH, "Download/downloaded_rom")
//                put(MediaStore.Downloads.IS_PENDING, 1)
//            }
//
//            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
//            val outUri = resolver.insert(collection, values) ?: return null
//
//            resolver.openOutputStream(outUri)?.use { out ->
//                resolver.openInputStream(file.uri)?.use { input ->
//                    input.copyTo(out)
//                } ?: return null
//            } ?: return null
//
//            values.clear()
//            values.put(MediaStore.Downloads.IS_PENDING, 0)
//            resolver.update(outUri, values, null, null)
//
//            outUri
//        } catch (e: Exception) {
//            Log.e(TAG, "saveToDownloadsDownloadedRom error", e)
//            null
//        }
//    }
//
//    private fun safeName(name: String): String {
//        // keep extension
//        return name.replace(Regex("[\\\\/:*?\"<>|]"), "_")
//    }
//
//    private fun guessAudioMime(name: String): String {
//        val lower = name.lowercase()
//        return when {
//            lower.endsWith(".mp3") -> "audio/mpeg"
//            lower.endsWith(".m4a") -> "audio/mp4"
//            lower.endsWith(".aac") -> "audio/aac"
//            lower.endsWith(".wav") -> "audio/wav"
//            else -> "audio/*"
//        }
//    }
//
//    private fun fileToBase64(uri: Uri, mimeType: String): String {
//        return try {
//            val inputStream = ctx.contentResolver.openInputStream(uri)
//            val bytes = inputStream?.readBytes() ?: return ""
//            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
//            "data:$mimeType;base64,$base64"
//        } catch (e: Exception) {
//            Log.e(TAG, "fileToBase64 error", e)
//            ""
//        }
//    }
//
//    @RequiresApi(Build.VERSION_CODES.Q)
//    private suspend fun processOne(file: DocumentFile, callLogId: Int, callEndMs: Long) {
//        val callLogPrefs = ctx.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE)
//        val movedPrefs = ctx.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
//        val localSavedPrefs = ctx.getSharedPreferences("LocalSavedRecordings", Context.MODE_PRIVATE)
//
//        val sessionPrefs = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
//        val token = sessionPrefs.getString("token", null) ?: return
//
//        val repo = RecordingsRepository()
//
//        val name = file.name ?: return
//        val recordingId = file.uri.toString()
//
//        val lm = getLastModifiedMs(file.uri)
//        val ext = name.substringAfterLast('.', "")
//        val outName = "CALL_${callLogId}_${lm}.${ext}"
//
//        // val localKey = "call_${callLogId}_lm_${lm}"
//        val localKey = "call_${callLogId}_src_${recordingId.hashCode()}"
//
//        if (localSavedPrefs.getBoolean(localKey, false)) {
//            Log.d(TAG, "SKIP local already saved → $localKey")
//        } else {
//            val localUri = saveToDownloadsDownloadedRom(file, outName)
//            if (localUri != null) {
//                Log.d(TAG, "Saved locally → $localUri")
//                localSavedPrefs.edit().putBoolean(localKey, true).apply()
//            } else {
//                Log.e(TAG, "Failed to save locally: $outName")
//            }
//        }
//
//        // ✅ 2) Prevent double upload for same source uri
//        if (movedPrefs.getBoolean(recordingId, false)) {
//            Log.d(TAG, "SKIP already uploaded: $recordingId")
//            return
//        }
//
//        // ✅ Upload to server
//        val mimeType = ctx.contentResolver.getType(file.uri) ?: guessAudioMime(name)
//        val base64 = withContext(Dispatchers.IO) { fileToBase64(file.uri, mimeType) }
//        if (base64.isEmpty()) {
//            Log.e(TAG, "Failed to encode base64: $name")
//            return
//        }
//
//        val request = RecordingUploadRequest(
//            fileBase64 = base64,
//            filename = outName // send stable name to server also
//        )
//
//        try {
//            val resp = repo.uploadRecording(token, callLogId, request)
//            if (resp.isSuccessful) {
//                movedPrefs.edit().putBoolean(recordingId, true).apply()
//                val body = resp.body()
//                Log.d(TAG, "Uploaded for callLogId=$callLogId url=${body?.recordingUrl}")
//
//                body?.recordingUrl?.let { url ->
//                    callLogPrefs.edit().putString("lastRecordingUrl", url).apply()
//                }
//            } else {
//                Log.e(TAG, "Upload failed: code=${resp.code()} err=${resp.errorBody()?.string()}")
//            }
//        } catch (e: Exception) {
//            Log.e(TAG, "Exception uploading", e)
//        }
//    }
//
//}


package com.technfest.technfestcrm.worker

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.technfest.technfestcrm.model.RecordingUploadRequest
import com.technfest.technfestcrm.repository.RecordingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.util.Locale

class MoveRecordingsWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val TAG = "MoveWorker"

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        return try {
            val prefsRec = ctx.getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
            val treeUriStr = prefsRec.getString("folder_path", null)
            if (treeUriStr.isNullOrEmpty()) {
                Log.d(TAG, "No recordings folder selected, skipping.")
                return Result.success()
            }

            val callLogId = inputData.getInt("CALL_LOG_ID", 0)
            if (callLogId == 0) return Result.success()

            val callEndMs = inputData.getLong("CALL_END_MS", 0L)

            val root = DocumentFile.fromTreeUri(ctx, Uri.parse(treeUriStr))
                ?: return Result.success()

            val localSavedPrefs = ctx.getSharedPreferences("LocalSavedRecordings", Context.MODE_PRIVATE)
            val movedPrefs = ctx.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
            val sessionPrefs = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = sessionPrefs.getString("token", null)
            if (token.isNullOrEmpty()) return Result.success()

            val repo = RecordingsRepository()

            // Wait and get only new files not yet processed
            val candidates = waitAndFindRecentRecordings(root, callEndMs, localSavedPrefs, callLogId)
            if (candidates.isEmpty()) {
                Log.d(TAG, "No new recordings found after waiting. Done.")
                return Result.success()
            }

            // Process each candidate (or pick only latest)
            val latest = candidates.maxByOrNull { getLastModifiedMs(it.uri) } ?: return Result.success()
            processRecording(latest, callLogId, callEndMs, localSavedPrefs, movedPrefs, token, repo)

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Worker error", e)
            Result.retry()
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun waitAndFindRecentRecordings(
        root: DocumentFile,
        callEndMs: Long,
        localSavedPrefs: android.content.SharedPreferences,
        callLogId: Int
    ): List<DocumentFile> {
        repeat(6) { // ~42s total
            val files = root.listFiles().filter { f ->
                val n = f.name?.lowercase(Locale.getDefault()) ?: ""
                val isAudio = n.endsWith(".mp3") || n.endsWith(".m4a") || n.endsWith(".aac") || n.endsWith(".wav")
                if (!isAudio) return@filter false

                val lm = getLastModifiedMs(f.uri)
                if (callEndMs > 0L && lm < (callEndMs - 60_000)) return@filter false

                // ✅ Only include if not already saved locally
                val localKey = "call_${callLogId}_src_${f.uri.toString().hashCode()}"
                !localSavedPrefs.getBoolean(localKey, false)
            }

            if (files.isNotEmpty()) return files
            delay(7_000)
        }
        return emptyList()
    }

    private fun getLastModifiedMs(uri: Uri): Long {
        var ts = 0L
        val cursor = ctx.contentResolver.query(
            uri,
            arrayOf(android.provider.DocumentsContract.Document.COLUMN_LAST_MODIFIED),
            null, null, null
        )
        cursor?.use { if (it.moveToFirst()) ts = it.getLong(0) }
        return ts
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private suspend fun processRecording(
        file: DocumentFile,
        callLogId: Int,
        callEndMs: Long,
        localSavedPrefs: android.content.SharedPreferences,
        movedPrefs: android.content.SharedPreferences,
        token: String,
        repo: RecordingsRepository
    ) {
        val name = file.name ?: return
        val recordingId = file.uri.toString()
        val localKey = "call_${callLogId}_src_${recordingId.hashCode()}"

        // Save locally
        if (!localSavedPrefs.getBoolean(localKey, false)) {
            val outName = "CALL_${callLogId}_${getLastModifiedMs(file.uri)}.${name.substringAfterLast('.', "")}"
            val localUri = saveToDownloadsDownloadedRom(file, outName)
            if (localUri != null) {
                localSavedPrefs.edit().putBoolean(localKey, true).apply()
                Log.d(TAG, "Saved locally → $localUri")
            } else {
                Log.e(TAG, "Failed to save locally: $outName")
                return
            }
        }
        if (!movedPrefs.getBoolean(recordingId, false)) {
            val mimeType = ctx.contentResolver.getType(file.uri) ?: guessAudioMime(name)
            val base64 = withContext(Dispatchers.IO) { fileToBase64(file.uri, mimeType) }
            if (base64.isEmpty()) return

            val request = RecordingUploadRequest(fileBase64 = base64, filename = name)
            try {
                val resp = repo.uploadRecording(token, callLogId, request)
                if (resp.isSuccessful) {
                    movedPrefs.edit().putBoolean(recordingId, true).apply()
                    Log.d(TAG, "Uploaded recording successfully: $name")
                } else {
                    Log.e(TAG, "Upload failed: code=${resp.code()} err=${resp.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception uploading $name", e)
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun saveToDownloadsDownloadedRom(file: DocumentFile, displayName: String): Uri? {
        return try {
            val resolver = ctx.contentResolver
            val values = ContentValues().apply {
                put(MediaStore.Downloads.DISPLAY_NAME, displayName)
                put(MediaStore.Downloads.MIME_TYPE, resolver.getType(file.uri) ?: guessAudioMime(displayName))
                put(MediaStore.Downloads.RELATIVE_PATH, "Download/downloaded_rom")
                put(MediaStore.Downloads.IS_PENDING, 1)
            }

            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val outUri = resolver.insert(collection, values) ?: return null

            resolver.openOutputStream(outUri)?.use { out ->
                resolver.openInputStream(file.uri)?.use { input ->
                    input.copyTo(out)
                } ?: return null
            } ?: return null

            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(outUri, values, null, null)

            outUri
        } catch (e: Exception) {
            Log.e(TAG, "saveToDownloadsDownloadedRom error", e)
            null
        }
    }

    private fun guessAudioMime(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".m4a") -> "audio/mp4"
            lower.endsWith(".aac") -> "audio/aac"
            lower.endsWith(".wav") -> "audio/wav"
            else -> "audio/*"
        }
    }

    private fun fileToBase64(uri: Uri, mimeType: String): String {
        return try {
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return ""
            "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"
        } catch (e: Exception) {
            Log.e(TAG, "fileToBase64 error", e)
            ""
        }
    }
}
