package com.technfest.technfestcrm.viewmodel

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.technfest.technfestcrm.model.CampaignCategoryItem
import com.technfest.technfestcrm.model.CampaignResponseItem
import com.technfest.technfestcrm.repository.CampaignRepository
import kotlinx.coroutines.launch

class CampaignsViewModel(private val repository: CampaignRepository) : ViewModel() {

    val campaignList = MutableLiveData<List<CampaignResponseItem>>()
    val categoriesLiveData = MutableLiveData<List<CampaignCategoryItem>>()
    val error = MutableLiveData<String>()

    fun fetchCampaigns(token: String, workspaceId: Int) {
        Log.d("CAMPAIGN_VM", "fetchCampaigns() called")

        viewModelScope.launch {
            try {
                Log.d("CAMPAIGN_VM", "Calling API getCampaigns()...")
                val response = repository.getCampaigns(token, workspaceId)

                Log.d("CAMPAIGN_VM", "getCampaigns() response code = ${response.code()}")

                if (response.isSuccessful && response.body() != null) {
                    Log.d("CAMPAIGN_VM", "Campaign data received, size = ${response.body()!!.size}")
                    campaignList.postValue(response.body())
                } else {
                    Log.e("CAMPAIGN_VM", "Failed: ${response.message()}")
                    error.postValue("Failed: ${response.message()}")
                }

            } catch (e: Exception) {
                Log.e("CAMPAIGN_VM", "Exception in fetchCampaigns: ${e.localizedMessage}")
                error.postValue("Error: ${e.localizedMessage}")
            }
        }
    }

    fun fetchCategories(token: String) {
        Log.d("CAMPAIGN_VM", "fetchCategories() called")

        viewModelScope.launch {
            try {
                Log.d("CAMPAIGN_VM", "Calling API getCampaignCategories()...")
                val result = repository.getCampaignCategories(token)

                Log.d("CAMPAIGN_VM", "getCampaignCategories() response code = ${result.code()}")

                if (result.isSuccessful && result.body() != null) {
                    Log.d("CAMPAIGN_VM", "Categories received, size = ${result.body()!!.size}")
                    categoriesLiveData.postValue(result.body())
                } else {
                    Log.e("CAMPAIGN_VM", "Category load failed: ${result.message()}")
                }

            } catch (e: Exception) {
                Log.e("CAMPAIGN_VM", "Exception in fetchCategories: ${e.message}")
                error.postValue(e.message)
            }
        }
    }
}
