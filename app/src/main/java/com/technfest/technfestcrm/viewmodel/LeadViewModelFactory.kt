package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.repository.LeadRepository

class LeadViewModelFactory(val repository: LeadRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {

        if (modelClass.isAssignableFrom(LeadViewModel::class.java)) {
            return LeadViewModel(repository) as T

        }
        throw IllegalArgumentException("Unknown viewmodel class")
    }
}