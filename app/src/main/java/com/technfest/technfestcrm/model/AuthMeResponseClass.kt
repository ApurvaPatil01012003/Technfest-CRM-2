package com.technfest.technfestcrm.model

data class AuthMeResponseClass(
    val companyId: Int,
    val createdAt: String,
    val email: String,
    val employeeId: Int,
    val fullName: String,
    val id: Int,
    val lastLoginAt: String,
    val lastLoginIp: Any,
    val mobile: String,
    val resellerId: Any,
    val status: String,
    val updatedAt: String,
    val userType: String,
    val uuid: String,
    val workspaceId: Int
)