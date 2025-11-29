package com.technfest.technfestcrm.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.TaskResponseItem
import com.technfest.technfestcrm.model.TaskTypeResponse
import com.technfest.technfestcrm.repository.TaskRepository
import kotlinx.coroutines.launch
class TaskViewModel(private val repository: TaskRepository) : ViewModel() {

    val taskResult = MutableLiveData<List<TaskResponseItem>>()
    val errorMsg = MutableLiveData<String>()
    val loading = MutableLiveData<Boolean>()

    fun fetchTasks(token: String, workspaceId: Int) {
        viewModelScope.launch {
            loading.value = true

            try {
                val response = repository.getTasks(token, workspaceId)
                if (response.isSuccessful && response.body() != null) {
                    taskResult.value = response.body()
                } else {
                    errorMsg.value = "Failed: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                errorMsg.value = e.message
            }

            loading.value = false
        }
    }

    val taskTypeResult = MutableLiveData<List<String>>()

    fun fetchTaskType(token: String,workspaceId: Int)
    {
     viewModelScope.launch {
         try {

             val response = repository.getTaskType(token,workspaceId)
             if(response.isSuccessful)
             {
                 taskTypeResult.postValue(response.body())
             }
         }catch (e: Exception)
         {

         }
     }
    }

}

