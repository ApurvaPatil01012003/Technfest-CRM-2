package com.technfest.technfestcrm.model

data class Campaign(
    var name : String,
    val type : String,
    var status : String,
    val ROI:String,
    val date_range : String,
    val owner:String,
    var budget : String,
    var spend :String,
    val leads:String,
    val lead_cpl : String,

    )
