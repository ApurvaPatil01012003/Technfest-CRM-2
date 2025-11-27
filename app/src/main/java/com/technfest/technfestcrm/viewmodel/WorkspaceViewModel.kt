package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.TaskResponseItem
import com.technfest.technfestcrm.model.WorkspaceResponseItem
import com.technfest.technfestcrm.repository.WorkspaceRepository
import kotlinx.coroutines.launch

class WorkspaceViewModel(private val repository: WorkspaceRepository) : ViewModel() {

    val workspaceResult = MutableLiveData<WorkspaceResponseItem?>()

    fun getWorkspaceDetails(token: String, workspaceId: Int) {
        viewModelScope.launch {
            try {
                val response = repository.getWorkspace(token)
                if (response.isSuccessful && response.body() != null) {

                    val workspaceList = response.body()!!

                    val matchedWorkspace = workspaceList.find { it.id == workspaceId }

                    workspaceResult.postValue(matchedWorkspace)

                } else {
                    workspaceResult.postValue(null)
                }
            } catch (e: Exception) {
                workspaceResult.postValue(null)
            }
        }
    }
}
