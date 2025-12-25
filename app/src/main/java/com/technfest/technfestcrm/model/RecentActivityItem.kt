package com.technfest.technfestcrm.model

sealed class RecentActivityItem {
    abstract val timestamp: Long

    data class CallItem(
        val leadId: Int,
        val leadName: String,
        val leadNumber: String,
        val callStatusLabel: String, // "Outgoing Answered" / "Missed" etc
        val startIso: String,
        val endIso: String,
        val durationSec: Int,
        override val timestamp: Long
    ) : RecentActivityItem()

    data class FeedbackItem(
        val feedback: CallFeedback,
        override val timestamp: Long
    ) : RecentActivityItem()
}
