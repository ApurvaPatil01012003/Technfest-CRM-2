package com.technfest.technfestcrm.model

data class RecordingUploadRequest(
    val fileBase64: String,
    val filename: String
)
