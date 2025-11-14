package com.technfest.technfestcrm.model

data class TaskStatusData (val completed: Float,
                           val notCompleted: Float,
                           val allTask: Float? = null)