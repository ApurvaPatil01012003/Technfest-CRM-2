package com.technfest.technfestcrm.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CallActivityStore {

    private const val PREF = "CallActivityStore"
    private const val KEY = "call_activity_list"

    data class CallActivityItem(
        val id: Long,
        val leadId: Int?,          // null if unknown outgoing and you don’t want create lead
        val leadName: String,
        val number: String,
        val direction: String,     // incoming/outgoing
        val status: String,        // answered/unanswered
        val startIso: String,
        val endIso: String,
        val durationSec: Int,
        val timestampMs: Long
    )

    fun getAll(ctx: Context): MutableList<CallActivityItem> {
        val prefs = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<CallActivityItem>>() {}.type
        return Gson().fromJson(json, type) ?: mutableListOf()
    }

    fun add(ctx: Context, item: CallActivityItem) {
        val list = getAll(ctx)
        list.add(item)
        // keep last 500
        val trimmed = list.sortedByDescending { it.timestampMs }.take(500)
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, Gson().toJson(trimmed))
            .apply()
    }

    fun getByLeadId(ctx: Context, leadId: Int): List<CallActivityItem> {
        return getAll(ctx).filter { it.leadId == leadId }.sortedByDescending { it.timestampMs }
    }
}
