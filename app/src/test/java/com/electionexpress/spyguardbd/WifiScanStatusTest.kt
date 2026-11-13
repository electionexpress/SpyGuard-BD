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
    fun scanStartExplainsMissingLocationPermission() {
        assertEquals(
            "Location permission দেওয়া নেই — অনুমতি দিন",
            wifiScanStartStatus(false, true, true, true)
        )
    }

    @Test
    fun scanStartExplainsWifiAndLocationDisabled() {
        assertEquals("Wi-Fi বন্ধ — ফোনের Wi-Fi চালু করুন", wifiScanStartStatus(true, false, true, true))
        assertEquals("Android-এর Location বন্ধ — Location চালু করুন", wifiScanStartStatus(true, true, false, true))
    }

    @Test
    fun rejectedScanRequestIsNotReportedAsEmptyScan() {
        assertEquals(
            "Android নতুন স্ক্যান গ্রহণ করেনি — scan throttling বা system policy হতে পারে; পুরোনো ফল দেখানো হতে পারে",
            wifiScanStartStatus(true, true, true, false)
        )
    }

    @Test
    fun acceptedScanRequestIsNotReportedAsCompleted() {
        assertEquals(
            "স্ক্যানের অনুরোধ গৃহীত — নতুন ফল আসা পর্যন্ত অপেক্ষা করুন",
            wifiScanStartStatus(true, true, true, true)
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
