package com.simplemobiletools.contacts.pro.models

import android.annotation.SuppressLint
import android.os.Build
import android.telephony.SubscriptionInfo
import android.telephony.TelephonyManager
import androidx.annotation.RequiresApi

data class SimCard(
    val id: String,
    val subscriptionId: Int,
    val displayName: String,
) {

    val hasValidSubscriptionId: Boolean
        get() = subscriptionId != NO_SUBSCRIPTION_ID

    companion object {
        private const val NO_SUBSCRIPTION_ID = -1

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP_MR1)
        fun create(info: SubscriptionInfo): SimCard {
            return SimCard(
                info.iccId, info.subscriptionId,
                info.displayName.toString(),
            )
        }

        @SuppressLint("MissingPermission", "HardwareIds")
        fun create(telephony: TelephonyManager, displayLabel: String): SimCard {
            return if (telephony.simState == TelephonyManager.SIM_STATE_READY) {
                SimCard(
                    telephony.simSerialNumber.toString(),
                    NO_SUBSCRIPTION_ID,
                    displayLabel,
                )
            } else {
                // This should never happen but in case it does just fallback to an "empty" instance
                SimCard("", NO_SUBSCRIPTION_ID, "")
            }
        }
    }
}
