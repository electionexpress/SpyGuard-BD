package com.electionexpress.spyguardbd

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class BleObservation(
    val address: String,
    val displayName: String,
    val rssiDbm: Int,
    val lastSeenAtMillis: Long
)

/** Masks the hardware address in the UI; the raw address remains an in-memory scan key only. */
fun maskBluetoothAddress(address: String): String {
    val parts = address.split(":")
    if (parts.size != 6 || parts.any { !it.matches(Regex("[0-9A-Fa-f]{2}")) }) {
        return "ঠিকানা গোপন"
    }
    return "${parts[0].uppercase(Locale.ROOT)}:${parts[1].uppercase(Locale.ROOT)}:••:••:••"
}

fun formatBleLastSeen(timestampMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))

/** Freshness describes advertisement recency, not device identity or camera status. */
enum class BleFreshness { LIVE, RECENT, STALE }

fun classifyBleFreshness(lastSeenAtMillis: Long, nowMillis: Long): BleFreshness {
    val ageMillis = (nowMillis - lastSeenAtMillis).coerceAtLeast(0L)
    return when {
        ageMillis < 30_000L -> BleFreshness.LIVE
        ageMillis < 120_000L -> BleFreshness.RECENT
        else -> BleFreshness.STALE
    }
}

fun bleFreshnessLabel(freshness: BleFreshness): String = when (freshness) {
    BleFreshness.LIVE -> "সাম্প্রতিক সিগনাল"
    BleFreshness.RECENT -> "কিছুক্ষণ আগে দেখা"
    BleFreshness.STALE -> "পুরোনো পর্যবেক্ষণ"
}
