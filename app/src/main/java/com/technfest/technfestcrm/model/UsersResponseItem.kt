package com.technfest.technfestcrm.model

data class UsersResponseItem(
    val branch_name: String,
    val created_at: String,
    val department_name: String,
    val email: String,
    val employee_code: String,
    val employee_name: String,
    val full_name: String,
    val id: Int,
    val last_login_at: String,
    val last_login_ip: Any,
    val mobile: String,
    val status: String,
    val user_type: String,
    val uuid: String,
    val workspace_code: String,
    val workspace_id: Int,
    val workspace_name: String
)