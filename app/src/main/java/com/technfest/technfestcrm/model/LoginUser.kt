package com.technfest.technfestcrm.model

data class LoginUser(
    val branch: LoginBranch,
    val email: String,
    val id: Int,
    val name: String,
    val roles: List<String>
)