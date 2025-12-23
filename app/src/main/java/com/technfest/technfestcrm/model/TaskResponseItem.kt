package com.technfest.technfestcrm.model

data class TaskResponseItem(
    val id: Int,
    val title: String,
    val description: String,
    val dueAt: String,
    val status: String,
    val priority: String,
    val taskType: String,
    val leadName: String,
    val assignedEmployeeName: String,


    val assignedToEmployeeId: Int? = null,
    val assignedToUserId: Int? = null,
    val assignedUserName: String? = null,
    val completedAt: Any? = null,
    val createdAt: String? = null,
    val createdByName: String? = null,
    val createdByUserId: Int? = null,
    val currentVersion: Int? = null,
    val departmentId: Any? = null,
    val departmentName: Any? = null,
    val dueDate: String? = null,
    val estimatedHours: String? = null,
    val isActive: Boolean? = null,
    val lastActivityAt: Any? = null,
    val leadId: Int? = null,
    val projectId: Int? = null,
    val projectName: String? = null,
    val totalLoggedMinutes: Int? = null,
    val updatedAt: String? = null,
    val workspaceId: Int? = null
)
