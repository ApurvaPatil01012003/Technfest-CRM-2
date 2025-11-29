package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.RecordingResponse
import com.technfest.technfestcrm.model.RecordingUploadRequest
import com.technfest.technfestcrm.model.RecordingUrlUpdateRequest
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response

class RecordingsRepository {
    suspend fun uploadRecording(
        token: String,
        callLogId: Int,
        body: RecordingUploadRequest
    ) = RetrofitInstance.apiInterface.uploadRecording("Bearer $token", callLogId, body)

    suspend fun linkRecordingUrl(
        token: String,
        callLogId: Int,
        recordingUrl: String
    ) = RetrofitInstance.apiInterface.updateRecordingUrl(
        "Bearer $token",
        callLogId,
        RecordingUrlUpdateRequest(recordingUrl)
    )
}

