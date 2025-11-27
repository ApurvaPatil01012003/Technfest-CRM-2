package com.technfest.technfestcrm.model

data class CampaignResponseItem(
    var budget: String? = null,
    val campaignCategoryId: Int,
    var campaignCategoryName: String? = "",
    val cpl: Any? = null,
    val createdAt: String,
    val createdBy: Int,
    val createdByName: String,
    var description: String?,
    val endDate: Any? = null,
    val id: Int,
    var name: String? = "",
    val ownerUserId: Any? = null,
    val ownerUserName: Any? = null,
    val runDurationDays: Any? = null,
    val spentAmount: Any? = null,
    val startDate: String,
    var status: String? = "",
    var tags: List<Any>?,
    val totalCalls: Int,
    val totalLeads: Int,
    val updatedAt: String,
    val workspaceCode: String,
    val workspaceId: Int,
    val workspaceName: String
)
