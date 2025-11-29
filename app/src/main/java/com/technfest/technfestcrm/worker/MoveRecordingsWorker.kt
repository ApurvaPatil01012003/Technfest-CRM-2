package com.technfest.technfestcrm.worker

import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.technfest.technfestcrm.model.RecordingUploadRequest
import com.technfest.technfestcrm.repository.RecordingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64

class MoveRecordingsWorker(
    private val ctx: Context,
    params: WorkerParameters
) : CoroutineWorker(ctx, params) {

    private val TAG = "MoveWorker"

    @RequiresApi(Build.VERSION_CODES.Q)
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting auto-link recordings…")

            val prefsRec = ctx.getSharedPreferences("recordings_prefs", Context.MODE_PRIVATE)
            val treeUriStr = prefsRec.getString("folder_path", null)
            if (treeUriStr.isNullOrEmpty()) {
                Log.d(TAG, "No recordings folder selected, skipping.")
                return Result.success()
            }

            val sessionPrefs = ctx.getSharedPreferences("UserSession", Context.MODE_PRIVATE)
            val token = sessionPrefs.getString("token", null)
            if (token.isNullOrEmpty()) {
                Log.e(TAG, "No token found in UserSession, cannot link.")
                return Result.success()
            }

            val callLogPrefs = ctx.getSharedPreferences("CallLogPrefs", Context.MODE_PRIVATE)
            val callLogId = callLogPrefs.getInt("lastCallLogId", 0)
            if (callLogId == 0) {
                Log.e(TAG, "No lastCallLogId found, skipping link.")
                return Result.success()
            }

            val root = DocumentFile.fromTreeUri(ctx, Uri.parse(treeUriStr))
                ?: run {
                    Log.e(TAG, "Root DocumentFile is null.")
                    return Result.success()
                }

            val movedPrefs = ctx.getSharedPreferences("MovedRecordings", Context.MODE_PRIVATE)
            val repo = RecordingsRepository()

            var anySuccess = false

            root.listFiles().forEach { file ->
                val name = file.name ?: return@forEach
                val lower = name.lowercase()

                if (!lower.endsWith(".mp3") &&
                    !lower.endsWith(".m4a") &&
                    !lower.endsWith(".aac") &&
                    !lower.endsWith(".wav")
                ) return@forEach

                val recordingId = file.uri.toString()
                if (movedPrefs.getBoolean(recordingId, false)) {
                    Log.d(TAG, "SKIP (already uploaded) → $recordingId")
                    return@forEach
                }

                Log.d(TAG, "Processing new recording → $name")

                // 1) Encode to base64
                val mimeType = ctx.contentResolver.getType(file.uri) ?: "audio/wav"
                val base64 = withContext(Dispatchers.IO) {
                    fileToBase64(file.uri, mimeType)
                }
                if (base64.isEmpty()) {
                    Log.e(TAG, "Failed to encode file: $name")
                    return@forEach
                }

                // 2) Prepare request
                val request = RecordingUploadRequest(
                    fileBase64 = base64,
                    filename = name
                )

                try {
                    val resp = repo.uploadRecording(token, callLogId, request)
                    if (resp.isSuccessful) {
                        anySuccess = true
                        movedPrefs.edit().putBoolean(recordingId, true).apply()

                        val body = resp.body()
                        Log.d(TAG, "Uploaded recording for callLogId=$callLogId → url=${body?.recordingUrl}")

                        if (body?.recordingUrl != null) {
                            callLogPrefs.edit()
                                .putString("lastRecordingUrl", body.recordingUrl)
                                .apply()
                        }
                    } else {
                        Log.e(TAG, "Upload failed for $name: code=${resp.code()} msg=${resp.errorBody()?.string()}")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Exception uploading $name", e)
                }
            }


            Log.d(TAG, "Auto link completed. anySuccess=$anySuccess")
            Result.success()

        } catch (e: Exception) {
            Log.e(TAG, "Error in auto-link", e)
            Result.retry()
        }
    }


    private fun fileToBase64(uri: Uri, mimeType: String): String {
        return try {
            val inputStream = ctx.contentResolver.openInputStream(uri)
            val bytes = inputStream?.readBytes() ?: return ""
            val base64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            "data:$mimeType;base64,$base64"
        } catch (e: Exception) {
            Log.e(TAG, "fileToBase64 error", e)
            ""
        }
    }
}
