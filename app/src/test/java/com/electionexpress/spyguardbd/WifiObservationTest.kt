package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Test

class WifiObservationTest {
    @Test
    fun deduplicatesByBssidAndKeepsStrongestSignal() {
        val result = deduplicateWifiObservations(
            listOf(
                WifiObservation("Office", "aa:bb", 2412, -70),
                WifiObservation("Office", "aa:bb", 2412, -45),
                WifiObservation("Office", "cc:dd", 2412, -60)
            )
        )

        assertEquals(2, result.size)
        assertEquals(-45, result.first().rssiDbm)
        assertEquals("aa:bb", result.first().bssid)
    }

    @Test
    fun fallsBackToSsidAndFrequencyWhenBssidIsBlank() {
        val result = deduplicateWifiObservations(
            listOf(
                WifiObservation("Guest", "", 2412, -75),
                WifiObservation("Guest", "", 2412, -50),
                WifiObservation("Guest", "", 5180, -65)
            )
        )

        assertEquals(2, result.size)
        assertEquals(-50, result.first().rssiDbm)
    }

    @Test
    fun sortsStrongestFirstAndHonorsLimit() {
        val result = deduplicateWifiObservations(
            listOf(
                WifiObservation("A", "a", 2412, -80),
                WifiObservation("B", "b", 2412, -30),
                WifiObservation("C", "c", 2412, -55)
            ),
            limit = 2
        )

        assertEquals(listOf("B", "C"), result.map { it.ssid })
    }

    @Test
    fun nonPositiveLimitReturnsEmptyList() {
        assertEquals(emptyList<WifiObservation>(), deduplicateWifiObservations(emptyList(), 0))
    }
}
