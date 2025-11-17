package com.technfest.technfestcrm.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.LoginRequest
import com.technfest.technfestcrm.model.LoginResponse
import com.technfest.technfestcrm.network.RetrofitInstance
import com.technfest.technfestcrm.repository.LoginRepository
import kotlinx.coroutines.launch

class LoginViewModel(val repository: LoginRepository): ViewModel() {

    val loginResult = MutableLiveData<String>()
    val tokenLiveData = MutableLiveData<String>()
    val userNameLiveData = MutableLiveData<String>()
    fun loginUser(email: String, password: String) {
        viewModelScope.launch {
            try {
                val response = repository.loginUser(LoginRequest(email, password))
                if (response.isSuccessful) {
                    val token = response.body()?.token ?: "No token"
                    val userName = response.body()?.user?.name ?: "User"
                    loginResult.postValue("Login successful!")
                    tokenLiveData.postValue(token)
                    userNameLiveData.postValue(userName)
                } else {
                    loginResult.postValue("Login failed")
                }
            } catch (e: Exception) {
                loginResult.postValue("Error: ${e.localizedMessage}")
            }
        }
    }

}