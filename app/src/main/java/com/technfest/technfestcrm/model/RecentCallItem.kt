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
    val id: Long = 0L,
    val leadId: Int = 0,
    val leadName: String = "",
    val number: String = "",
    val direction: String = "",
    val status: String = "",
    val statusLabel: String = "",
    val durationSec: Int = 0,
    val startIso: String = "",
    val endIso: String = "",
    val timestampMs: Long = 0L,

    val phoneAccountId: String? = null,
    val phoneAccountComponent: String? = null,
    val usedSubId: Int? = null
)
