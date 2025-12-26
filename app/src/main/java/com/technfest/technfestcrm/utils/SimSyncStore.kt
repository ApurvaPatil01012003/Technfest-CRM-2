//package com.technfest.technfestcrm.utils
//
//import android.content.Context
//
//object SimSyncStore {
//    private const val PREF_SIM_SYNC = "SimSyncPrefs"
//    private const val KEY_SYNCED_NUMBERS = "synced_numbers" // Set<String>
//
//    fun getSyncedNumbers(ctx: Context): Set<String> {
//        val prefs = ctx.getSharedPreferences(PREF_SIM_SYNC, Context.MODE_PRIVATE)
//        return prefs.getStringSet(KEY_SYNCED_NUMBERS, emptySet()) ?: emptySet()
//    }
//
//    fun saveSyncedNumbers(ctx: Context, numbers: Set<String>) {
//        val prefs = ctx.getSharedPreferences(PREF_SIM_SYNC, Context.MODE_PRIVATE)
//        prefs.edit().putStringSet(KEY_SYNCED_NUMBERS, numbers).apply()
//    }
//
//    fun isAnySynced(ctx: Context): Boolean = getSyncedNumbers(ctx).isNotEmpty()
//}


package com.technfest.technfestcrm.utils

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object SimSyncStore {
    private const val PREF = "SimSyncPrefs"
    private const val KEY = "sim_list"

    data class SyncedSim(
        val subId: Int,
        val slotIndex: Int,
        val displayName: String,
        val number: String?,
        val isSynced: Boolean
    )

    fun saveAll(context: Context, list: List<SyncedSim>) {
        val json = com.google.gson.Gson().toJson(list)
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY, json)
            .commit()   // ✅ IMPORTANT (sync write)
    }

    fun getAll(context: Context): List<SyncedSim> {
        val json = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return emptyList()

        val type = object : com.google.gson.reflect.TypeToken<List<SyncedSim>>() {}.type
        return try { com.google.gson.Gson().fromJson(json, type) } catch (_: Exception) { emptyList() }
    }

    fun getSynced(context: Context): List<SyncedSim> {
        return getAll(context).filter { it.isSynced }
    }
}
