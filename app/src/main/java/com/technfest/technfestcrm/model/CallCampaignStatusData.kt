package com.technfest.technfestcrm.model

data class CallCampaignStatusData(val connected: Float,
                                  val notConnected: Float,
                                  val callLater: Float,
                                  val allCallCampaign: Float? = null)
