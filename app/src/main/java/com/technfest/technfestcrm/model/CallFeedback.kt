package com.technfest.technfestcrm.model

data class CallFeedback(
    val number: String,
    val leadName: String?,
    val callStatus: String,
    val rating: Int,
    val note: String,
    val followUp: String?,
    val receivedBy: String?,
    val timestamp: Long
)
