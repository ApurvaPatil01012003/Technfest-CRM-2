package com.technfest.technfestcrm.localdatamanager

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.technfest.technfestcrm.model.LeadRequest

object LocalLeadManager {

    private const val PREF_NAME = "local_leads_pref"
    private const val KEY_LEADS = "leads_list"

    fun saveLead(context: Context, lead: LeadRequest) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        val existingLeads = getLeads(context).toMutableList()
        existingLeads.add(lead)

        prefs.edit()
            .putString(KEY_LEADS, gson.toJson(existingLeads))
            .apply()
    }

    fun getLeads(context: Context): List<LeadRequest> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()
        val json = prefs.getString(KEY_LEADS, null) ?: return emptyList()

        val type = object : TypeToken<List<LeadRequest>>() {}.type
        return gson.fromJson(json, type)
    }

    fun clearLeads(context: Context) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit()
            .remove(KEY_LEADS)
            .apply()
    }


}

