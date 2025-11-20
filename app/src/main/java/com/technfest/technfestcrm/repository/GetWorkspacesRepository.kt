package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.GetWorkspacesClass
import retrofit2.Response

class GetWorkspacesRepository(private val api: ApiInterface) {

    suspend fun getWorkspaces(token: String): Response<GetWorkspacesClass> {
        return api.getWorkspaces(token)
    }
}