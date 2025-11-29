package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.AuthMeResponseClass
import com.technfest.technfestcrm.model.CallLogRequest
import com.technfest.technfestcrm.model.CallLogResponse
import com.technfest.technfestcrm.model.CampaignCategory
import com.technfest.technfestcrm.model.CampaignResponse
import com.technfest.technfestcrm.model.CampaignResponseItem
import com.technfest.technfestcrm.model.CreateTaskResponse
import com.technfest.technfestcrm.model.CreatesLeadResponse
import com.technfest.technfestcrm.model.EditCampaignRequest
import com.technfest.technfestcrm.model.GetWorkspacesClass
import com.technfest.technfestcrm.model.LeadMetaItem
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.LeadResponse
import com.technfest.technfestcrm.model.LoginRequest
import com.technfest.technfestcrm.model.LoginResponse
import com.technfest.technfestcrm.model.RecordingResponse
import com.technfest.technfestcrm.model.RecordingUploadRequest
import com.technfest.technfestcrm.model.RecordingUrlUpdateRequest
import com.technfest.technfestcrm.model.TaskRequest
import com.technfest.technfestcrm.model.TaskResponse
import com.technfest.technfestcrm.model.TaskTypeResponse
import com.technfest.technfestcrm.model.UsersResponse
import com.technfest.technfestcrm.model.WorkspaceResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiInterface {

    @POST("api/auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @GET("api/auth/me")
    suspend fun authMe(
        @Header("Authorization") token: String
    ): Response<AuthMeResponseClass>

    @GET("api/workspaces")
    suspend fun getWorkspaces(
        @Header("Authorization") token: String
    ): Response<GetWorkspacesClass>


    @POST("api/leads")
    suspend fun createLeads(
        @Header("Authorization") token: String,
        @Body request: LeadRequest
    ): Response<CreatesLeadResponse>


    @GET("api/leads")
    suspend fun getLeads(
        @Header("Authorization") token: String,
        @Query("workspaceId") workspaceId: Int
    ): Response<LeadResponse>

    @GET("api/lead-meta")
    suspend fun getLeadMeta(
        @Header("Authorization") token: String,
        @Query("workspaceId") workspaceId: Int,
        @Query("category") category: String
    ): Response<List<LeadMetaItem>>

    @GET("api/campaigns")
    suspend fun getCampaigns(
        @Header("Authorization") token: String,
        @Query("workspaceId") workspaceId: Int
    ): Response<CampaignResponse>

    @GET("api/campaigns/categories")
    suspend fun getCampaignCategories(
        @Header("Authorization") token: String
    ): Response<CampaignCategory>

    @GET("api/tasks")
    suspend fun getTasks(
        @Header("Authorization") token: String,
        @Query("workspaceId") workspaceId: Int
    ): Response<TaskResponse>


    @POST("api/tasks")
    suspend fun createTask(
        @Header("Authorization") token: String,
        @Body taskRequest: TaskRequest
    ): Response<CreateTaskResponse>

    @GET("api/tasks/types")
    suspend fun getTaskType(
        @Header("Authorization") token: String,
        @Query("workspaceId") workspaceId: Int
    ): Response<TaskTypeResponse>


    @GET("api/users")
    suspend fun getUsers(
        @Header("Authorization") token: String,
    ): Response<UsersResponse>

    @GET("api/workspaces")
    suspend fun getWorkspace(
        @Header("Authorization") token: String
    ): Response<WorkspaceResponse>

    @PATCH("api/campaigns/{id}")
    suspend fun editCampaign(
        @Header("Authorization") token: String,
        @Path("id") campaignId: Int,
        @Body request: EditCampaignRequest
    ): Response<CampaignResponseItem>

    @POST("api/webhooks/call-log")
    suspend fun sendCallLog(
        @Header("X-API-KEY") apiKey: String,
        @Body request: CallLogRequest
    ):Response<CallLogResponse>


    @POST("api/call-logs/{id}/recording")
    suspend fun uploadRecording(
        @Header("Authorization") token: String,
        @Path("id") callLogId: Int,
        @Body request: RecordingUploadRequest
    ): Response<RecordingResponse>

    @PATCH("api/call-logs/{id}/recording-url")
    suspend fun updateRecordingUrl(
        @Header("Authorization") token: String,
        @Path("id") callLogId: Int,
        @Body body: RecordingUrlUpdateRequest
    ): Response<RecordingResponse>

}