package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.LoginRequest
import com.technfest.technfestcrm.model.LoginResponse
import com.technfest.technfestcrm.repository.LoginRepository
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: LoginRepository) : ViewModel() {

    val loginResponseLiveData = MutableLiveData<LoginResponse?>()
    val errorLiveData = MutableLiveData<String>()

    fun loginUser(email: String, password: String) {

        viewModelScope.launch {
            try {
                val response = repository.loginUser(LoginRequest(email, password))

                if (response.isSuccessful) {
                    loginResponseLiveData.postValue(response.body())
                } else {
                    errorLiveData.postValue("Invalid email or password")
                }

            } catch (e: Exception) {
                errorLiveData.postValue("Error: ${e.localizedMessage}")
            }
        }
    }
}
