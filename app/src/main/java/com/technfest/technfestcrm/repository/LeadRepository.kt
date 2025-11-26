package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.CreatesLeadResponse
import com.technfest.technfestcrm.model.LeadMetaItem
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.LeadResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response
class LeadRepository {
    suspend fun getLeads(token: String, workspaceId: Int): Response<LeadResponse> {
        return RetrofitInstance.apiInterface.getLeads("Bearer $token", workspaceId)
    }
    suspend fun createLeads(token: String, request: LeadRequest): Response<CreatesLeadResponse> {
        return RetrofitInstance.apiInterface.createLeads(
            "Bearer $token",
            request
        )
    }

    suspend fun getLeadMeta(token: String, workspaceId: Int, category: String): Response<List<LeadMetaItem>> {
        return RetrofitInstance.apiInterface.getLeadMeta(
            token = "Bearer $token",
            workspaceId = workspaceId,
            category = category
        )
    }




}