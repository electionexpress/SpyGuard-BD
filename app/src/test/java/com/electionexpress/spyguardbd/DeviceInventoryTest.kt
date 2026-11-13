package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceInventoryTest {
    @Test fun blePhoneObservationKeepsSourceAndHint() {
        val item = bleObservation("AA:BB", "Samsung Galaxy • -61 dBm")
        assertEquals(ObservationSource.BLUETOOTH_LE, item.source)
        assertEquals(ObservationKind.POSSIBLE_PHONE, item.kind)
        assertEquals(-61, item.signalDbm)
    }

    @Test fun wifiIsNotMisrepresentedAsClientPhone() {
        val item = wifiObservation("HomeRouter • 2412 MHz • -48 dBm")
        assertEquals(ObservationKind.WIFI_NETWORK, item.kind)
        assertTrue(item.detail.contains("ক্লায়েন্টের তালিকা নয়"))
    }

    @Test fun inventoryDeduplicatesByObservationKey() {
        val rows = buildNearbyInventory(
            mapOf("AA:BB" to "Galaxy • -60 dBm", "CC:DD" to "Galaxy • -70 dBm"),
            listOf("Home • 2412 MHz • -50 dBm", "Home • 2412 MHz • -50 dBm")
        )
        assertEquals(3, rows.size)
    }

    @Test fun reportExplainsLimitationsAndEscapesText() {
        val report = nearbyInventoryReport(listOf(wifiObservation("Guest \"WiFi\" • -40 dBm")), "2026-11-13T00:00:00Z")
        assertTrue(report.contains("complete device inventory"))
        assertTrue(report.contains("\\\"WiFi\\\""))
    }
}
