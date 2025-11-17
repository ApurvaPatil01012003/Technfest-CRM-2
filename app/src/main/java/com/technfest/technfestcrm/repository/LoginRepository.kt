package com.technfest.technfestcrm.repository

import android.util.Log
import com.technfest.technfestcrm.model.LoginRequest
import com.technfest.technfestcrm.model.LoginResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response

class LoginRepository {

    suspend fun loginUser(request: LoginRequest) =
        RetrofitInstance.apiInterface.loginUser(request)

}