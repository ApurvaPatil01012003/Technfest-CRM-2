package com.technfest.technfestcrm.model

data class Calls(
    val name : String,
    val CallType : String,
    val number : String,
    val day : String,
    val time : String,
    val duration : String ,
    val assign_user :String,
    val assign_number : String,
    val linkedLink : String,
    val note : String,
    val initial :String
    )
