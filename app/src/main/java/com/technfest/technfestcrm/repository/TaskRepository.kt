package com.technfest.technfestcrm.repository

import com.technfest.technfestcrm.model.CreateTaskResponse
import com.technfest.technfestcrm.model.TaskRequest
import com.technfest.technfestcrm.model.TaskResponse
import com.technfest.technfestcrm.model.TaskTypeResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import retrofit2.Response

class TaskRepository {
    suspend fun getTasks(token: String, workspaceId: Int): Response<TaskResponse> {
        return RetrofitInstance.apiInterface.getTasks(
            token = "Bearer $token",
            workspaceId = workspaceId
        )
    }

    suspend fun createTask(token: String, taskRequest: TaskRequest): Response<CreateTaskResponse> {
        return RetrofitInstance.apiInterface.createTask(
            token = "Bearer $token",
            taskRequest = taskRequest
        )
    }

    suspend fun getTaskType(token: String,workspaceId: Int): Response<TaskTypeResponse> {
        return RetrofitInstance.apiInterface.getTaskType(
            token ="Bearer $token",
            workspaceId =workspaceId
        )
    }


}
