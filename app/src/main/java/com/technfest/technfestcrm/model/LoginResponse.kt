package com.technfest.technfestcrm.model

data class LoginResponse(
    val success: Boolean,
    val token: String,
    val user: LoginUser
)