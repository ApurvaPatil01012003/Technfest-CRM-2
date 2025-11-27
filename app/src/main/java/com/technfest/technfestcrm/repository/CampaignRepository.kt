package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.CampaignCategory
import com.technfest.technfestcrm.model.CampaignResponse
import com.technfest.technfestcrm.model.CampaignResponseItem
import com.technfest.technfestcrm.model.EditCampaignRequest
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response


class CampaignRepository {
    suspend fun getCampaigns(token : String , workspaceId : Int): Response<CampaignResponse>
    {
        return RetrofitInstance.apiInterface.getCampaigns("Bearer $token", workspaceId)
    }
    suspend fun getCampaignCategories(token: String): Response<CampaignCategory> {
        return RetrofitInstance.apiInterface.getCampaignCategories("Bearer $token")
    }
    suspend fun editCampaign(
        token: String,
        campaignId: Int,
        request: EditCampaignRequest
    ): Response<CampaignResponseItem> {
        return RetrofitInstance.apiInterface.editCampaign("Bearer $token", campaignId, request)
    }

}