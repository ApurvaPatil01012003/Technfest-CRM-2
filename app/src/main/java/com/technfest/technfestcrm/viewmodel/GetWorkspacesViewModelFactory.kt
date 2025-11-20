package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.repository.GetWorkspacesRepository

class GetWorkspacesViewModelFactory(
    private val repository: GetWorkspacesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GetWorkspacesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return GetWorkspacesViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
