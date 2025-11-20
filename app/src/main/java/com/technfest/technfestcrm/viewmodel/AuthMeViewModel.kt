package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.AuthMeResponseClass
import com.technfest.technfestcrm.repository.AuthMeRepository
import kotlinx.coroutines.launch

class AuthMeViewModel(private val repository: AuthMeRepository) : ViewModel() {

    val authMeResponse = MutableLiveData<AuthMeResponseClass?>()
    val error = MutableLiveData<String>()

    fun fetchAuthMe(token: String) {
        viewModelScope.launch {
            try {
                val response = repository.authMe("Bearer $token")
                if (response.isSuccessful) {
                    authMeResponse.value = response.body()
                } else {
                    error.value = "Failed: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                error.value = e.localizedMessage
            }
        }
    }
}
