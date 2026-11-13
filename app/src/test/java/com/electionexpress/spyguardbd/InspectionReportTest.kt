package com.electionexpress.spyguardbd

import org.junit.Assert.assertTrue
import org.junit.Test

class InspectionReportTest {
    @Test
    fun reportIncludesUserLocationTimeAndNotes() {
        val report = InspectionReport(
            roomLabel = "Guest room",
            observedAt = "2026-11-13T09:00:00+06:00",
            observations = OpticalInspection(lensLikeElement = true),
            notes = "Shelf near the window"
        ).toPlainText()

        assertTrue(report.contains("Guest room"))
        assertTrue(report.contains("2026-11-13T09:00:00+06:00"))
        assertTrue(report.contains("Shelf near the window"))
        assertTrue(report.contains("লেন্সের মতো উপাদান: হ্যাঁ"))
    }

    @Test
    fun blankFieldsAreClearlyRepresentedAndNoDetectionIsClaimed() {
        val report = InspectionReport(
            roomLabel = "",
            observedAt = "time",
            observations = OpticalInspection()
        ).toPlainText()

        assertTrue(report.contains("উল্লেখ করা হয়নি"))
        assertTrue(report.contains("কোনো অতিরিক্ত নোট নেই"))
        assertTrue(report.contains("স্বয়ংক্রিয় ক্যামেরা শনাক্তকরণ নয়"))
        assertTrue(report.contains("ক্যামেরার প্রমাণ নয়"))
    }
    @Test
    fun reportIncludesVisibleWirelessObservationsWithoutCallingThemCameras() {
        val report = InspectionReport(
            roomLabel = "Meeting room",
            observedAt = "2026-11-13 12:00:00",
            observations = OpticalInspection(),
            nearbyWifiNetworks = listOf("Office Wi-Fi • 2412 MHz • -55 dBm"),
            nearbyBluetoothDevices = listOf("BLE tag • -68 dBm")
        ).toPlainText()

        assertTrue(report.contains("Office Wi-Fi"))
        assertTrue(report.contains("BLE tag"))
        assertTrue(report.contains("এগুলো ক্যামেরা শনাক্ত করে না"))
        assertTrue(report.contains("অফলাইন, বন্ধ, তারযুক্ত"))
    }

}
