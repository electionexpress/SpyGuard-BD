package com.electionexpress.spyguardbd

/**
 * Conservative classification of nearby radio observations.
 * A BLE advertisement is not proof of a physical product model or device purpose.
 */
internal enum class DeviceCategory(val labelBn: String) {
    PHONE_OR_TABLET("মোবাইল/ট্যাবলেট সম্ভাব্য"),
    COMPUTER("কম্পিউটার সম্ভাব্য"),
    NETWORK_EQUIPMENT("নেটওয়ার্ক যন্ত্র সম্ভাব্য"),
    CAMERA_OR_SURVEILLANCE("ক্যামেরা/নজরদারি সম্ভাব্য"),
    AUDIO("অডিও ডিভাইস সম্ভাব্য"),
    WEARABLE("ওয়্যারেবল সম্ভাব্য"),
    UNKNOWN("অজানা BLE ডিভাইস")
}

internal data class DeviceClassification(
    val category: DeviceCategory,
    val confidenceLabel: String,
    val explanation: String
)

internal fun classifyNearbyDevice(advertisedName: String?): DeviceClassification {
    val name = advertisedName.orEmpty().trim().lowercase()
    if (name.isBlank() || name.contains("নামহীন") || name == "unknown") {
        return DeviceClassification(DeviceCategory.UNKNOWN, "কম", "নাম বা পরিচয় প্রকাশ করা হয়নি")
    }
    val rules = listOf(
        DeviceCategory.CAMERA_OR_SURVEILLANCE to listOf("camera", "webcam", "cctv", "ipcam", "nvr", "dvr", "onvif", "cam-"),
        DeviceCategory.PHONE_OR_TABLET to listOf("iphone", "ipad", "android", "galaxy", "pixel", "redmi", "xiaomi", "oneplus", "oppo", "vivo", "realme", "huawei"),
        DeviceCategory.COMPUTER to listOf("laptop", "notebook", "macbook", "thinkpad", "desktop", "windows pc"),
        DeviceCategory.NETWORK_EQUIPMENT to listOf("router", "access point", "hotspot", "mesh"),
        DeviceCategory.AUDIO to listOf("headphone", "earbuds", "airpods", "speaker", "soundbar", "headset"),
        DeviceCategory.WEARABLE to listOf("watch", "band", "fitbit", "garmin")
    )
    val match = rules.firstOrNull { (_, tokens) -> tokens.any(name::contains) }
    return if (match == null) {
        DeviceClassification(DeviceCategory.UNKNOWN, "কম", "শুধু বিজ্ঞাপিত নাম থেকে ধরন নিশ্চিত করা যায়নি")
    } else {
        DeviceClassification(match.first, "ইঙ্গিতভিত্তিক", "বিজ্ঞাপিত নামের শব্দের ভিত্তিতে অনুমান; মডেল/পরিচয় নিশ্চিত নয়")
    }
}

internal fun deviceCategorySummary(names: Collection<String>): Map<DeviceCategory, Int> =
    names.map { classifyNearbyDevice(it).category }.groupingBy { it }.eachCount()
