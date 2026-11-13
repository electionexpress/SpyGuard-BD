package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceClassificationTest {
    @Test fun cameraNameIsOnlyAPossibleCamera() {
        val result = classifyNearbyDevice("IP Camera")
        assertEquals(DeviceCategory.CAMERA_OR_SURVEILLANCE, result.category)
        assertTrue(result.explanation.contains("অনুমান"))
    }
    @Test fun phoneNameIsClassifiedAsPossiblePhone() {
        assertEquals(DeviceCategory.PHONE_OR_TABLET, classifyNearbyDevice("Samsung Galaxy").category)
    }
    @Test fun unnamedAdvertisementStaysUnknown() {
        assertEquals(DeviceCategory.UNKNOWN, classifyNearbyDevice(null).category)
    }
    @Test fun unknownNameIsNotGuessed() {
        assertEquals(DeviceCategory.UNKNOWN, classifyNearbyDevice("SG-48A").category)
    }
    @Test fun summaryCountsCategories() {
        val result = deviceCategorySummary(listOf("iPhone", "Galaxy", "IP Camera", ""))
        assertEquals(2, result[DeviceCategory.PHONE_OR_TABLET])
        assertEquals(1, result[DeviceCategory.CAMERA_OR_SURVEILLANCE])
        assertEquals(1, result[DeviceCategory.UNKNOWN])
    }
}
