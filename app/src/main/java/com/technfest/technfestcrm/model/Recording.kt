package com.technfest.technfestcrm.model

data class Recording(
    val name: String,       // filename
    val path: String,       // full path
    val phoneNumber: String, // number of caller/receiver
    val timestamp: Long      // creation/modification time in milliseconds
)
