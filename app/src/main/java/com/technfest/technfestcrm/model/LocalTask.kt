package com.technfest.technfestcrm.model

data class LocalTask(
    val id: Int,
    val title: String,
    val description: String,
    val dueAt: String? = null,
    val status: String? ,
    val priority: String? ,
    val taskType: String ,   // "general" or "CALL_FOLLOW_UP"
    val source: String,       // "Manual" or "Auto"
    val leadName: String? ,
    val leadNumber: String? = null,
    val assignedToUser: String? ,  // user assigned
    val estimatedHours: String?        // estimated hours
)


