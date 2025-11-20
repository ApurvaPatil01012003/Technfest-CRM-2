package com.technfest.technfestcrm.model

data class LoginResponse(
    val employeeId: Int,
    val expiresIn: Int,
    val fullName: String,
    val id: Int,
    val token: String,
    val userType: String,
    val uuid: String,
    val workspaceId: Int
)