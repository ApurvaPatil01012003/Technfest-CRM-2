package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.LoginRequest
import com.technfest.technfestcrm.model.LoginResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiInterface {

    @POST("auth/login")
    suspend fun loginUser( @Body request: LoginRequest): Response<LoginResponse>

}