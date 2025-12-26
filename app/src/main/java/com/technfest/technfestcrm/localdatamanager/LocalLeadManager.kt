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
    fun upsertLead(context: Context, lead: LeadRequest) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        val list = getLeads(context).toMutableList()

        // Match by ID first (best), else by mobile (fallback)
        val idx = list.indexOfFirst { it.id == lead.id && lead.id != 0 }
            .takeIf { it >= 0 }
            ?: list.indexOfFirst {
                it.mobile?.filter { ch -> ch.isDigit() }?.takeLast(10) ==
                        lead.mobile?.filter { ch -> ch.isDigit() }?.takeLast(10)
            }

        if (idx >= 0) {
            list[idx] = lead
        } else {
            list.add(lead)
        }

        prefs.edit()
            .putString(KEY_LEADS, gson.toJson(list))
            .apply()
    }
    fun updateLeadName(context: Context, number: String, newName: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val gson = Gson()

        val list = getLeads(context).toMutableList()
        val key10 = number.filter { it.isDigit() }.takeLast(10)

        val idx = list.indexOfFirst {
            it.mobile?.filter { ch -> ch.isDigit() }?.takeLast(10) == key10
        }
        if (idx < 0) return

        val old = list[idx]
        list[idx] = old.copy(fullName = newName)

        prefs.edit()
            .putString(KEY_LEADS, gson.toJson(list))
            .apply()
    }



}

