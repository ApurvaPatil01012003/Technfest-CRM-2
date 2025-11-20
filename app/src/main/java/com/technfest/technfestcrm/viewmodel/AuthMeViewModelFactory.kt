package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.repository.AuthMeRepository

class AuthMeViewModelFactory(private val repository: AuthMeRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthMeViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthMeViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
