package com.electionexpress.spyguardbd

internal enum class ObservationSource(val labelBn: String) {
    BLUETOOTH_LE("Bluetooth LE"), WIFI_ACCESS_POINT("Wi-Fi access point")
}
internal enum class ObservationKind(val labelBn: String) {
    POSSIBLE_PHONE("সম্ভাব্য ফোন/ট্যাবলেট"), POSSIBLE_COMPUTER("সম্ভাব্য কম্পিউটার"),
    POSSIBLE_CAMERA("সম্ভাব্য ক্যামেরা"), POSSIBLE_NETWORK_EQUIPMENT("সম্ভাব্য নেটওয়ার্ক যন্ত্র"),
    POSSIBLE_AUDIO("সম্ভাব্য অডিও ডিভাইস"), POSSIBLE_WEARABLE("সম্ভাব্য ওয়্যারেবল"),
    WIFI_NETWORK("Wi-Fi নেটওয়ার্ক/অ্যাক্সেস পয়েন্ট"), UNKNOWN_RADIO("অজানা রেডিও পর্যবেক্ষণ")
}
internal data class DeviceObservation(
    val stableKey: String, val displayName: String, val source: ObservationSource,
    val kind: ObservationKind, val signalDbm: Int? = null, val detail: String,
    val confidence: String = "সীমিত"
)
internal fun bleObservation(address: String, detail: String): DeviceObservation {
    val name = detail.substringBefore(" • ").trim().ifBlank { "নাম প্রকাশ করেনি" }
    val rssi = Regex("(-?\\d+)\\s*dBm").find(detail)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val c = classifyNearbyDevice(name)
    val kind = when (c.category) {
        DeviceCategory.PHONE_OR_TABLET -> ObservationKind.POSSIBLE_PHONE
        DeviceCategory.COMPUTER -> ObservationKind.POSSIBLE_COMPUTER
        DeviceCategory.CAMERA_OR_SURVEILLANCE -> ObservationKind.POSSIBLE_CAMERA
        DeviceCategory.NETWORK_EQUIPMENT -> ObservationKind.POSSIBLE_NETWORK_EQUIPMENT
        DeviceCategory.AUDIO -> ObservationKind.POSSIBLE_AUDIO
        DeviceCategory.WEARABLE -> ObservationKind.POSSIBLE_WEARABLE
        DeviceCategory.UNKNOWN -> ObservationKind.UNKNOWN_RADIO
    }
    return DeviceObservation("ble:$address", name, ObservationSource.BLUETOOTH_LE, kind, rssi, c.explanation, c.confidenceLabel)
}
internal fun wifiObservation(row: String): DeviceObservation {
    val name = row.substringBefore(" • ").ifBlank { "নামহীন Wi-Fi নেটওয়ার্ক" }
    val rssi = Regex("(-?\\d+)\\s*dBm").find(row)?.groupValues?.getOrNull(1)?.toIntOrNull()
    val bssid = Regex("BSSID ([0-9A-Fa-f:]{17})").find(row)?.groupValues?.getOrNull(1)?.uppercase()
    return DeviceObservation("wifi:${bssid ?: name}", name, ObservationSource.WIFI_ACCESS_POINT, ObservationKind.WIFI_NETWORK,
        rssi, "এটি Wi-Fi নেটওয়ার্ক/অ্যাক্সেস পয়েন্ট; সংযুক্ত ফোন বা ক্লায়েন্টের তালিকা নয়", "নেটওয়ার্ক উপস্থিতি")
}
internal fun buildNearbyInventory(ble: Map<String, String>, wifiRows: List<String>): List<DeviceObservation> =
    (ble.map { (address, detail) -> bleObservation(address, detail) } + wifiRows.map(::wifiObservation))
        .distinctBy { it.stableKey }
        .sortedWith(compareBy<DeviceObservation> { it.source.ordinal }.thenBy { it.displayName.lowercase() })

internal fun nearbyInventoryReport(observations: List<DeviceObservation>, createdAt: String): String {
    fun esc(value: String): String =
        "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\""
    val rows = observations.joinToString(",\n") { item ->
        "    {" +
            "\"name\":" + esc(item.displayName) +
            ",\"source\":" + esc(item.source.labelBn) +
            ",\"kind\":" + esc(item.kind.labelBn) +
            ",\"signal_dbm\":" + (item.signalDbm?.toString() ?: "null") +
            ",\"confidence\":" + esc(item.confidence) +
            ",\"detail\":" + esc(item.detail) + "}"
    }
    return "{\n" +
        "  \"app\": \"SpyGuard BD\",\n" +
        "  \"report_type\": \"local_nearby_radio_observations\",\n" +
        "  \"created_at\": " + esc(createdAt) + ",\n" +
        "  \"limitations\": \"Observations are not a complete device inventory. Wi-Fi rows are access points, BLE names are self-advertised hints, and devices with radios disabled or no discoverable service may be absent.\",\n" +
        "  \"observation_count\": " + observations.size + ",\n" +
        "  \"observations\": [\n" + rows + "\n  ]\n}"
}
