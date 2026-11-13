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
}
