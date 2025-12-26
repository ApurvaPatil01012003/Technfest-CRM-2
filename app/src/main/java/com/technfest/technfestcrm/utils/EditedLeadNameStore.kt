package com.technfest.technfestcrm.utils

import android.content.Context
import android.telephony.TelephonyManager
import android.util.Log
import com.google.i18n.phonenumbers.PhoneNumberUtil

object EditedLeadNameStore {

    private fun key10(number: String?): String {
        if (number.isNullOrBlank()) return ""
        val digits = number.filter { it.isDigit() }
        return if (digits.length >= 10) digits.takeLast(10) else digits
    }

    fun save(context: Context, number: String, name: String) {
        val key = key10(number)
        if (key.isBlank() || name.isBlank()) return

        context.getSharedPreferences("EditedLeadNames", Context.MODE_PRIVATE)
            .edit()
            .putString(key, name.trim())
            .apply()

        Log.d("EDIT_NAME_STORE", "save key10='$key' name='${name.trim()}'")
    }

    fun get(context: Context, number: String?): String? {
        val key = key10(number)
        if (key.isBlank()) return null

        val value = context.getSharedPreferences("EditedLeadNames", Context.MODE_PRIVATE)
            .getString(key, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        Log.d("EDIT_NAME_STORE", "get key10='$key' value='$value'")
        return value
    }
}
