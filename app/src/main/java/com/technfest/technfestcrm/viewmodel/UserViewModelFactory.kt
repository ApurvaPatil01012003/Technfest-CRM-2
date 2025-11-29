package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.technfest.technfestcrm.repository.UsersRepository

class UserViewModelFactory(val repository: UsersRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if(modelClass.isAssignableFrom(UsersViewModel::class.java))
    {
       return UsersViewModel(repository) as T
    }
        throw IllegalArgumentException("ViewModel not found")
    }
}