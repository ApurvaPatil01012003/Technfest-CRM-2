package com.technfest.technfestcrm.model

data class LeadStatusData( val interested: Float,
                           val notInterested: Float,
                           val justCurious: Float,
                           val dealClosed: Float,
                           val allLeads: Float? = null)
