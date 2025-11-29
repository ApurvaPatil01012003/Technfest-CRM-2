package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.CallLogRequest
import com.technfest.technfestcrm.model.CallLogResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response




    class CallLogRepository {
        suspend fun sendCallLog(apiKey: String, request: CallLogRequest)
                = RetrofitInstance.apiInterface.sendCallLog(apiKey, request)
    }


