package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Test

class BleObservationTest {
    @Test
    fun masksBluetoothAddressForDisplay() {
        assertEquals("AA:BB:••:••:••", maskBluetoothAddress("AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun hidesMalformedAddress() {
        assertEquals("ঠিকানা গোপন", maskBluetoothAddress("unknown"))
    }

    @Test
    fun formatsLastSeenAsClockTime() {
        assertEquals(8, formatBleLastSeen(0L).length)
    }

    @Test
    fun classifiesBleObservationFreshness() {
        assertEquals(BleFreshness.LIVE, classifyBleFreshness(90_000L, 100_000L))
        assertEquals(BleFreshness.RECENT, classifyBleFreshness(50_000L, 100_000L))
        assertEquals(BleFreshness.STALE, classifyBleFreshness(0L, 120_000L))
    }

    @Test
    fun futureTimestampIsClampedToLive() {
        assertEquals(BleFreshness.LIVE, classifyBleFreshness(110_000L, 100_000L))
    }

    @Test
    fun freshnessThresholdsAreStableAtExactBoundaries() {
        assertEquals(BleFreshness.RECENT, classifyBleFreshness(70_000L, 100_000L))
        assertEquals(BleFreshness.STALE, classifyBleFreshness(-20_000L, 100_000L))
    }
}
