package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.UsersResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response

class UsersRepository {
    suspend fun getUsers(token:String):Response<UsersResponse>{
        return RetrofitInstance.apiInterface.getUsers("Bearer $token")
    }

}