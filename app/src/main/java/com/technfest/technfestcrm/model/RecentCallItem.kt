package com.technfest.technfestcrm.model

data class RecentCallItem(
    val id: Long,
    val leadId: Int,
    val leadName: String,
    val number: String,
    val direction: String,
    val status: String,
    val statusLabel: String,
    val durationSec: Int,
    val startIso: String,
    val endIso: String,
    val timestampMs: Long
)
