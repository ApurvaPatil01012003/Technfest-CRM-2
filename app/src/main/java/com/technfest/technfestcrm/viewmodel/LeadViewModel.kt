package com.technfest.technfestcrm.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.CreatesLeadResponse
import com.technfest.technfestcrm.model.LeadMetaItem
import com.technfest.technfestcrm.model.LeadRequest
import com.technfest.technfestcrm.model.LeadResponseItem
import com.technfest.technfestcrm.repository.LeadRepository
import kotlinx.coroutines.launch


class LeadViewModel(private val repository: LeadRepository) : ViewModel() {

    val leadsLiveData = MutableLiveData<List<LeadResponseItem>>()
    val errorLiveData = MutableLiveData<String>()
    val loadingLiveData = MutableLiveData<Boolean>()
    val sourceLiveData = MutableLiveData<List<LeadMetaItem>>()
    val statusLiveData = MutableLiveData<List<LeadMetaItem>>()
    val stageLiveData = MutableLiveData<List<LeadMetaItem>>()
    val priorityLiveData = MutableLiveData<List<LeadMetaItem>>()
    val teamLiveData = MutableLiveData<List<LeadMetaItem>>()
    val assignTypeLiveData = MutableLiveData<List<LeadMetaItem>>()

    fun fetchLeads(token: String, workspaceId: Int) {
        viewModelScope.launch {
            loadingLiveData.value = true
            try {
                val response = repository.getLeads(token, workspaceId)
                if (response.isSuccessful) {
                    leadsLiveData.value = response.body() ?: emptyList()
                } else {
                    errorLiveData.value = "Error: ${response.code()} - ${response.message()}"
                }
            } catch (e: Exception) {
                errorLiveData.value = e.localizedMessage ?: "Unknown error"
            }
            loadingLiveData.value = false
        }
    }


    val successLiveData = MutableLiveData<Boolean>()
    val leadResponseLiveData = MutableLiveData<CreatesLeadResponse?>()

    fun createLead(token: String, leadRequest: LeadRequest) {
        viewModelScope.launch {
            loadingLiveData.value = true
            try {
                val response = repository.createLeads(token, leadRequest)
                if (response.isSuccessful) {
                    val leadResponse = response.body()
                    leadResponseLiveData.value = leadResponse
                    successLiveData.value = true
                } else {
                    val errorBody = response.errorBody()?.string() // actual server error
                    Log.e("LeadError", "Code: ${response.code()} ErrorBody: $errorBody")
                    errorLiveData.value = errorBody ?: "Error: ${response.code()} - ${response.message()}"
                    successLiveData.value = false
                }
            } catch (e: Exception) {
                Log.e("LeadError", "Exception: ${e.message}")
                errorLiveData.value = e.localizedMessage ?: "Unknown error"
                successLiveData.value = false
            }
            loadingLiveData.value = false
        }
    }



    fun fetchLeadMeta(token: String, workspaceId: Int, category: String) {
        Log.d("LeadMetaCheck", "Token = $token, workspaceId = $workspaceId")

        viewModelScope.launch {
            try {
                val response = repository.getLeadMeta(token, workspaceId, category)
                Log.d("LeadMetaCheck", "URL: lead-meta?workspace_id=$workspaceId&category=$category")

                Log.d("LeadMeta", "$category response code: ${response.code()} body: ${response.body()}")
                if (response.isSuccessful) {
                    val list = response.body() ?: emptyList()
                    Log.d("LeadMeta", "$category list size: ${list.size}")
                    when(category){
                        "source" -> sourceLiveData.value = list
                        "status" -> statusLiveData.value = list
                        "stage" -> stageLiveData.value = list
                        "priority" -> priorityLiveData.value = list
                        "team" -> teamLiveData.value = list
                        "assign_type" -> assignTypeLiveData.value = list
                    }
                } else {
                    errorLiveData.value = "Error ${response.code()}"
                }
            } catch (e: Exception) {
                Log.e("LeadMeta", "Exception in $category: ${e.message}")
                errorLiveData.value = e.localizedMessage
            }
        }
    }



}
