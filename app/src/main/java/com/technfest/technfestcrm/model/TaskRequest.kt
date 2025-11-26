package com.technfest.technfestcrm.model

data class TaskRequest(
    val workspaceId: Int,
    val title: String,
    val description: String,
    val taskType: String = "general",
    val projectId: Int?,
    val assignedToEmployeeId: Int?,
    val estimatedHours: Int?,
    val dueDate: String?,
    val dueAt: String?,
    val projectName: String?,
    val status: String?,
    val priority: String?,
    val assignedUserName: String?,
    val leadName: String?,
    val leadId: Int?
)
