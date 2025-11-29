package com.technfest.technfestcrm.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.CallLogRequest
import com.technfest.technfestcrm.repository.CallLogRepository
import kotlinx.coroutines.launch


class CallLogViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = CallLogRepository()

    fun sendCallLog(
        token: String,
        number: String,
        name: String,
        leadId: Int,
        workspaceId: Int,
        duration: Long
    ) {

        viewModelScope.launch {
            try {
                repo.sendCallLog(
                    token,
                    CallLogRequest(
                        callStatus = "completed",
                        campaignCode = "",
                        campaignId = 0,
                        customerName = name,
                        customerNumber = number,
                        direction = "outgoing",
                        durationSeconds = duration.toInt(),
                        endTime = System.currentTimeMillis().toString(),
                        externalEventId = "",
                        leadId = leadId,
                        notes = "",
                        recordingUrl = "",
                        source = "mobile",
                        startTime = (System.currentTimeMillis() - (duration * 1000)).toString(),
                        userId = 0,
                        workspaceId = workspaceId
                    )
                )
            } catch (e: Exception) {
                Log.e("CALL_LOG", "ERROR: ${e.message}")
            }
        }
    }
}
