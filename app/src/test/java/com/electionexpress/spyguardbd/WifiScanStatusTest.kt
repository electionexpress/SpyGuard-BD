package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiScanStatusTest {
    @Test
    fun cachedResultsAreNotReportedAsFresh() {
        assertEquals(
            "নতুন ফল নিশ্চিত নয় — Android-এর পুরোনো cached ফল থাকতে পারে",
            wifiScanStatus(resultsUpdated = false, resultCount = 4)
        )
    }

    @Test
    fun freshEmptyScanIsReportedClearly() {
        assertEquals(
            "নতুন স্ক্যান সম্পন্ন — কোনো দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া যায়নি",
            wifiScanStatus(resultsUpdated = true, resultCount = 0)
        )
    }

    @Test
    fun freshScanReportsObservedCount() {
        assertEquals(
            "নতুন স্ক্যানে 3টি দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া গেছে",
            wifiScanStatus(resultsUpdated = true, resultCount = 3)
        )
    }
}
