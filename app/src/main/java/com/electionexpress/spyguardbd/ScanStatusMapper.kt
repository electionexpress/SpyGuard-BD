package com.electionexpress.spyguardbd

import com.electionexpress.spyguardbd.autoscan.CheckStatus

/**
 * Maps human-readable platform status to a conservative scan state.
 * A device observation during a scan is not proof that the scan has finished.
 */
internal fun wifiCheckStatus(message: String): CheckStatus = when {
    message.contains("অনুমতি") -> CheckStatus.PERMISSION_DENIED
    message.contains("অনুপলব্ধ") || message.contains("বন্ধ") -> CheckStatus.UNAVAILABLE
    message.contains("নতুন স্ক্যান সম্পন্ন") || message.contains("নতুন স্ক্যানে") -> CheckStatus.COMPLETED
    message.contains("স্ক্যানের অনুরোধ গৃহীত") || message.contains("স্ক্যান চলছে") -> CheckStatus.RUNNING
    message.contains("নতুন ফল নিশ্চিত নয়") || message.contains("গ্রহণ করেনি") -> CheckStatus.FAILED
    else -> CheckStatus.NOT_STARTED
}

internal fun bluetoothCheckStatus(message: String): CheckStatus = when {
    message.contains("অনুমতি") -> CheckStatus.PERMISSION_DENIED
    message.contains("অনুপলব্ধ") || message.contains("BLE scanner নেই") || message.contains("Bluetooth বন্ধ") -> CheckStatus.UNAVAILABLE
    message.contains("স্ক্যান সম্পন্ন") -> CheckStatus.COMPLETED
    message.contains("স্ক্যান চলছে") || message.contains("বিজ্ঞাপন পাওয়া গেছে") -> CheckStatus.RUNNING
    message.contains("স্ক্যান") && message.contains("কোড") -> CheckStatus.FAILED
    message.contains("সমস্যা") || message.contains("ব্যস্ত") || message.contains("অপেক্ষা করুন") -> CheckStatus.FAILED
    else -> CheckStatus.NOT_STARTED
}
