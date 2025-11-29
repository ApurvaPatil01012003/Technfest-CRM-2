package com.technfest.technfestcrm.model

data class CallLogResponse(

    val id: Int,
    val workspaceId: Int,
    val userId: Int?,
    val leadId: Int?,
    val campaignId: Int?
)