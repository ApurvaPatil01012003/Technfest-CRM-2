package com.technfest.technfestcrm.model

data class CallLogRequest(
    val workspaceId: Int,
    val userId: Int?,
    val leadId: Int?,
    val campaignId: Int?,
    val campaignCode: String?,
    val customerNumber: String,
    val customerName: String?,
    val direction: String,
    val callStatus: String,
    val startTime: String,
    val endTime: String,
    val durationSeconds: Int,
    val recordingUrl: String?,
    val source: String,
    val notes: String?,
    val externalEventId: String?
)