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
        "নতুন স্ক্যানে $resultCountটি দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া গেছে"
    }
}
