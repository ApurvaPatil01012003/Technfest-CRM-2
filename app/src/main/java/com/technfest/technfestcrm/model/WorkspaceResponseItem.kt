package com.technfest.technfestcrm.model

data class WorkspaceResponseItem(
    val createdAt: String,
    val id: Int,
    val name: String,
    val status: String,
    val timezone: String,
    val type: String,
    val updatedAt: String,
    val weekends: String,
    val workingHoursEnd: String,
    val workingHoursStart: String,
    val workspaceCode: String
)