package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.AuthMeResponseClass
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response

class AuthMeRepository {

    suspend fun authMe(token: String): Response<AuthMeResponseClass> {
        return RetrofitInstance.apiInterface.authMe(token)
    }
}
