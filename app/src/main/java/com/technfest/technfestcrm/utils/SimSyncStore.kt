package com.technfest.technfestcrm.utils

import android.content.Context

object SimSyncStore {
    private const val PREF_SIM_SYNC = "SimSyncPrefs"
    private const val KEY_SYNCED_NUMBERS = "synced_numbers" // Set<String>

    fun getSyncedNumbers(ctx: Context): Set<String> {
        val prefs = ctx.getSharedPreferences(PREF_SIM_SYNC, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_SYNCED_NUMBERS, emptySet()) ?: emptySet()
    }

    fun saveSyncedNumbers(ctx: Context, numbers: Set<String>) {
        val prefs = ctx.getSharedPreferences(PREF_SIM_SYNC, Context.MODE_PRIVATE)
        prefs.edit().putStringSet(KEY_SYNCED_NUMBERS, numbers).apply()
    }

    fun isAnySynced(ctx: Context): Boolean = getSyncedNumbers(ctx).isNotEmpty()
}
