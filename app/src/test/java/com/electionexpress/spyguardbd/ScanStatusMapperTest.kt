package com.electionexpress.spyguardbd

import com.electionexpress.spyguardbd.autoscan.CheckStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class ScanStatusMapperTest {
    @Test fun bleObservationDuringScanIsStillRunning() {
        assertEquals(
            CheckStatus.RUNNING,
            bluetoothCheckStatus("3টি Bluetooth বিজ্ঞাপন পাওয়া গেছে")
        )
    }

    @Test fun bleScanIsCompleteOnlyAfterBoundedWindowEnds() {
        assertEquals(
            CheckStatus.COMPLETED,
            bluetoothCheckStatus("১৫ সেকেন্ডের স্ক্যান সম্পন্ন — 3টি ডিভাইস পাওয়া গেছে")
        )
    }

    @Test fun failedBleScanDoesNotLookClean() {
        assertEquals(
            CheckStatus.FAILED,
            bluetoothCheckStatus("Bluetooth অভ্যন্তরীণ সমস্যা (কোড 3)")
        )
    }

    @Test fun deniedBluetoothPermissionIsExplicit() {
        assertEquals(
            CheckStatus.PERMISSION_DENIED,
            bluetoothCheckStatus("Bluetooth স্ক্যানের অনুমতি নেই")
        )
    }

    @Test fun wifiRequestAcceptedMeansStillRunning() {
        assertEquals(
            CheckStatus.RUNNING,
            wifiCheckStatus("স্ক্যানের অনুরোধ গৃহীত — নতুন ফল আসা পর্যন্ত অপেক্ষা করুন")
        )
    }

    @Test fun cachedWifiResultsAreNotACompletedScan() {
        assertEquals(
            CheckStatus.FAILED,
            wifiCheckStatus("নতুন ফল নিশ্চিত নয় — Android-এর পুরোনো cached ফল থাকতে পারে")
        )
    }

    @Test fun freshWifiResultsCanCompleteScan() {
        assertEquals(
            CheckStatus.COMPLETED,
            wifiCheckStatus("নতুন স্ক্যানে 2টি দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া গেছে")
        )
    }

    @Test fun unavailableWifiIsNotReportedAsZeroThreat() {
        assertEquals(
            CheckStatus.UNAVAILABLE,
            wifiCheckStatus("Wi-Fi/Location অনুপলব্ধ — এই অংশ পরীক্ষা করা যায়নি")
        )
    }
}
