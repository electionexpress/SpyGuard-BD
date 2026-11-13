package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OpticalInspectionTest {
    @Test
    fun emptyChecklistRecordsNoObservations() {
        val inspection = OpticalInspection()
        assertEquals(0, inspection.recordedObservationCount)
        assertTrue(inspection.guidance().contains("নিরাপদ হওয়ার প্রমাণ নয়"))
    }

    @Test
    fun eachObservationIsCountedOnce() {
        val inspection = OpticalInspection(
            lensLikeElement = true,
            repeatableGlintFromMultipleAngles = true,
            visiblePowerOrDataConnection = false,
            independentlyRechecked = true
        )
        assertEquals(3, inspection.recordedObservationCount)
    }

    @Test
    fun aSingleObservationDoesNotClaimCameraDetection() {
        val inspection = OpticalInspection(lensLikeElement = true)
        assertEquals(1, inspection.recordedObservationCount)
        assertTrue(inspection.guidance().contains("ক্যামেরার প্রমাণ নয়"))
    }

    @Test
    fun multipleObservationsRecommendFurtherVerificationNotAConfirmedThreat() {
        val inspection = OpticalInspection(
            lensLikeElement = true,
            repeatableGlintFromMultipleAngles = true
        )
        assertEquals(2, inspection.recordedObservationCount)
        assertTrue(inspection.guidance().contains("পুনরায় পরীক্ষা"))
        assertTrue(inspection.guidance().contains("স্বয়ংক্রিয় শনাক্তকরণ নয়"))
    }
}
