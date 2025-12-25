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
    private const val KEY = "synced_sims_json" // JSON list

    data class SyncedSim(
        val subId: Int,
        val slotIndex: Int,
        val displayName: String,
        val number: String?,   // can be null/unknown
        val isSynced: Boolean
    )

    fun getAll(context: Context): MutableList<SyncedSim> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<SyncedSim>>() {}.type
        return Gson().fromJson(json, type) ?: mutableListOf()
    }

    fun getSynced(context: Context): List<SyncedSim> {
        return getAll(context).filter { it.isSynced }
    }

    fun saveAll(context: Context, list: List<SyncedSim>) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY, Gson().toJson(list)).apply()
    }

    fun setSynced(context: Context, subId: Int, synced: Boolean) {
        val all = getAll(context)
        val idx = all.indexOfFirst { it.subId == subId }
        if (idx >= 0) {
            all[idx] = all[idx].copy(isSynced = synced)
            saveAll(context, all)
        }
    }

    // helpful for old code which expects number list
    fun getSyncedNumbers(context: Context): List<String> {
        return getSynced(context).mapNotNull { it.number }.filter { it.isNotBlank() && !it.equals("Unknown", true) }
    }
}
