package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import com.technfest.technfestcrm.model.RecordingUploadRequest
import com.technfest.technfestcrm.repository.RecordingsRepository

class RecordingViewModel(private val repo: RecordingsRepository) : ViewModel() {
    suspend fun uploadRecording(token: String, callLogId: Int, body: RecordingUploadRequest)
            = repo.uploadRecording(token, callLogId, body)
    suspend fun linkRecordingUrl(
        token: String,
        callLogId: Int,
        url: String
    ) = repo.linkRecordingUrl(token, callLogId, url)
}
