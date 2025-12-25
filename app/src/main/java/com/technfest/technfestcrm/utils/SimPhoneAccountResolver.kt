package com.technfest.technfestcrm.utils

import android.Manifest
import android.content.Context
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.telephony.SubscriptionManager
import androidx.annotation.RequiresPermission

object SimPhoneAccountResolver {

    @RequiresPermission(Manifest.permission.READ_PHONE_STATE)
    fun resolvePhoneAccountHandleForSubId(ctx: Context, subId: Int): PhoneAccountHandle? {
        val telecom = ctx.getSystemService(Context.TELECOM_SERVICE) as TelecomManager
        val subMgr = ctx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE) as SubscriptionManager
        val targetInfo = subMgr.activeSubscriptionInfoList?.firstOrNull { it.subscriptionId == subId }

        val handles = telecom.callCapablePhoneAccounts ?: return null

        for (h in handles) {
            val acc = telecom.getPhoneAccount(h) ?: continue
            val label = acc.label?.toString()?.lowercase() ?: ""

            if (targetInfo != null) {
                val disp = (targetInfo.displayName?.toString() ?: "").lowercase()
                val slot = targetInfo.simSlotIndex + 1

                if (disp.isNotBlank() && label.contains(disp)) return h
                if (label.contains("sim$slot") || label.contains("sim $slot")) return h
            }
        }

        return handles.firstOrNull()
    }
}
