package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.AuthMeResponseClass
import com.technfest.technfestcrm.model.GetWorkspacesClass
import com.technfest.technfestcrm.model.LoginRequest
import com.technfest.technfestcrm.model.LoginResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

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

}