package com.technfest.technfestcrm.model

import com.google.gson.annotations.SerializedName

data class CreatesLeadResponse(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    val mobile: String?,
    val email: String?,
    val company: String?
)