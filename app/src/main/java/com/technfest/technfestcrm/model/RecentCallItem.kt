//package com.technfest.technfestcrm.model
//
//data class RecentCallItem(
//    val id: Long,
//    val leadId: Int,
//    val leadName: String,
//    val number: String,
//    val direction: String,
//    val status: String,
//    val statusLabel: String,
//    val durationSec: Int,
//    val startIso: String,
//    val endIso: String,
//    val timestampMs: Long
//)


package com.technfest.technfestcrm.model

data class RecentCallItem(
    val id: Long,
    val leadId: Int,
    val leadName: String,
    val number: String,
    val direction: String,     // incoming/outgoing
    val status: String,        // answered/unanswered
    val statusLabel: String,   // display label
    val durationSec: Int,
    val startIso: String,
    val endIso: String,
    val timestampMs: Long,

    // for SIM filtering
    val phoneAccountId: String? = null,
    val phoneAccountComponent: String? = null,
    val usedSubId: Int? = null
)
