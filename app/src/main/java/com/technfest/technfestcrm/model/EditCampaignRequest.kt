package com.technfest.technfestcrm.model

data class EditCampaignRequest(
    val name: String?,
    val campaignCategoryName: String?,
    val description: String?,
    val startDate: String?,
    val endDate: String?,
    val budget: Double?,
    val spentAmount: Double?,
    val cpl: Double?,
    val totalLeads: Int?,
//    val status: String?,
    val tags: List<String>?
)
