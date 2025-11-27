package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.repository.WorkspaceRepository

class WorkspaceViewModelFactory(val repository: WorkspaceRepository): ViewModelProvider.Factory{
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      if(modelClass.isAssignableFrom(WorkspaceViewModel::class.java))
      {
        return  WorkspaceViewModel(repository)  as T
      }
        throw IllegalArgumentException("View model not found")
    }
}