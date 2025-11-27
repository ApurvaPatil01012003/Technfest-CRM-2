package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.LeadResponse
import com.technfest.technfestcrm.model.WorkspaceResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response

class WorkspaceRepository {
    suspend fun getWorkspace(token:String): Response<WorkspaceResponse>{
        return RetrofitInstance.apiInterface.getWorkspace("Bearer $token")
    }

}