package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.UsersResponseItem
import com.technfest.technfestcrm.repository.UsersRepository
import kotlinx.coroutines.launch


class UsersViewModel(private val repository: UsersRepository) : ViewModel() {

    val usersList = MutableLiveData<List<UsersResponseItem>>()

    fun fetchUsers(token: String) {
        viewModelScope.launch {
            try {
                val response = repository.getUsers(token)
                if (response.isSuccessful) {
                    usersList.postValue(response.body())
                }
            } catch (e: Exception) {
            }
        }
    }
}
