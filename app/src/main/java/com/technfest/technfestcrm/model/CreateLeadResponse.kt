package com.technfest.technfestcrm.model

import com.google.gson.annotations.SerializedName

data class CreatesLeadResponse(
    val id: Int,
    @SerializedName("full_name")
    val fullName: String,
    val mobile: String?,                   // optional
    val email: String?,                    // optional
    val company: String?                   // optional
    // add more fields if your API returns them
)