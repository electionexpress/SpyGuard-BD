package com.electionexpress.spyguardbd

/**
 * A Wi-Fi access point observed by the device during a scan.
 * BSSID is used only to collapse repeated observations in one scan;
 * it is not treated as proof of a unique physical device.
 */
data class WifiObservation(
    val ssid: String,
    val bssid: String,
    val frequencyMhz: Int,
    val rssiDbm: Int
)

/**
 * Keeps the strongest observation for each observed access-point identity.
 * When BSSID is unavailable, SSID + frequency is a conservative fallback.
 */
fun deduplicateWifiObservations(
    observations: List<WifiObservation>,
    limit: Int = 30
): List<WifiObservation> {
    if (limit <= 0) return emptyList()

    return observations
        .groupBy { observation ->
            observation.bssid.takeIf { it.isNotBlank() }
                ?.let { "bssid:$it" }
                ?: "fallback:${observation.ssid}|${observation.frequencyMhz}"
        }
        .values
        .map { group -> group.maxBy { it.rssiDbm } }
        .sortedByDescending { it.rssiDbm }
        .take(limit)
}
