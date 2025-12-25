package com.technfest.technfestcrm.utils

import android.content.Context
import android.telephony.TelephonyManager
import com.google.i18n.phonenumbers.PhoneNumberUtil

object EditedLeadNameStore {

    fun normalizeToE164(context: Context, raw: String?): String {
        if (raw.isNullOrBlank()) return ""
        return try {
            val cleaned = raw.replace("[^0-9+]".toRegex(), "")
            val phoneUtil = PhoneNumberUtil.getInstance()
            val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val region = (tm.networkCountryIso ?: tm.simCountryIso)
                ?.uppercase()
                ?.takeIf { it.isNotBlank() } ?: "IN"

            val proto = if (cleaned.startsWith("+")) phoneUtil.parse(cleaned, null)
            else phoneUtil.parse(cleaned, region)

            phoneUtil.format(proto, PhoneNumberUtil.PhoneNumberFormat.E164)
        } catch (e: Exception) {
            ""
        }
    }

    fun save(context: Context, number: String, name: String) {
        val e164 = normalizeToE164(context, number)
        if (e164.isBlank() || name.isBlank()) return

        context.getSharedPreferences("EditedLeadNames", Context.MODE_PRIVATE)
            .edit()
            .putString(e164, name.trim())
            .apply()
    }

    fun get(context: Context, number: String?): String? {
        val e164 = normalizeToE164(context, number)
        if (e164.isBlank()) return null

        return context.getSharedPreferences("EditedLeadNames", Context.MODE_PRIVATE)
            .getString(e164, null)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }
}
