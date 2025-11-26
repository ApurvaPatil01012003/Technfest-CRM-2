package com.technfest.technfestcrm.model

data class LeadRequest(
    val assigned_to: String,
    val campaignId: Int,
    val campaignName: String,
    val company: String,
    val email: String,
    val followupDates: List<String>,
    val fullName: String,
    val leadRequirement: String,
    val location: String,
    val mobile: String,
    val nextFollowupAt: String?,
    val priority: String,
    val source: String,
    val sourceDetails: String,
    val stage: String,
    val status: String,
    val tags: List<String>,
    val teamId: Int,
    val teamName: String
)