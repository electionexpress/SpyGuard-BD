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
    if (parts.size != 6) return "ঠিকানা গোপন"
    return "${parts[0]}:${parts[1]}:••:••:••"
}

fun formatBleLastSeen(timestampMillis: Long): String =
    SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))
