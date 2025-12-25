package com.technfest.technfestcrm.model

data class Calls(
    val name: String,
    val CallType: String,
    val number: String,
    val day: String,
    val time: String,
    val duration: String,
    val note: String,
    val img: String = "",
    val a: String = "",
    val b: String = "",
    val c: String = "",
    val initial: String = "",
    val leadId: Int = 0,
    val campaignId: Int = 0,
    val ownerUserId: Int = 0
)


