package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.technfest.technfestcrm.repository.GetWorkspacesRepository

class GetWorkspacesViewModel(private val repo: GetWorkspacesRepository) : ViewModel() {

    fun fetchWorkspaces(token: String) = liveData {
        val response = repo.getWorkspaces(token)
        emit(response)
    }
}
