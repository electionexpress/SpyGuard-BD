package com.electionexpress.spyguardbd

/**
 * Describes whether Android delivered fresh Wi-Fi scan results.
 * Cached results must never be presented as a successful new scan.
 */
fun wifiScanStatus(resultsUpdated: Boolean, resultCount: Int): String {
    if (!resultsUpdated) {
        return "নতুন ফল নিশ্চিত নয় — Android-এর পুরোনো cached ফল থাকতে পারে"
    }
    return if (resultCount == 0) {
        "নতুন স্ক্যান সম্পন্ন — কোনো দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া যায়নি"
    } else {
        "নতুন স্ক্যানে ${resultCount}টি দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া গেছে"
    }
}

/**
 * Explains why a new Wi-Fi scan could not be requested.
 * A rejected request does not mean that no networks exist; Android may throttle scans.
 */
fun wifiScanStartStatus(
    hasLocationPermission: Boolean,
    wifiEnabled: Boolean,
    locationEnabled: Boolean,
    requestAccepted: Boolean
): String = when {
    !hasLocationPermission -> "Location permission দেওয়া নেই — অনুমতি দিন"
    !wifiEnabled -> "Wi-Fi বন্ধ — ফোনের Wi-Fi চালু করুন"
    !locationEnabled -> "Android-এর Location বন্ধ — Location চালু করুন"
    !requestAccepted -> "Android নতুন স্ক্যান গ্রহণ করেনি — scan throttling বা system policy হতে পারে; পুরোনো ফল দেখানো হতে পারে"
    else -> "স্ক্যানের অনুরোধ গৃহীত — নতুন ফল আসা পর্যন্ত অপেক্ষা করুন"
}
