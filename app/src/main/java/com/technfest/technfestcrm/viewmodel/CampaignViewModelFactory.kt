package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.repository.CampaignRepository

class CampaignViewModelFactory(private val repository: CampaignRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CampaignsViewModel::class.java)) {
            return CampaignsViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel class not found")
    }
}
