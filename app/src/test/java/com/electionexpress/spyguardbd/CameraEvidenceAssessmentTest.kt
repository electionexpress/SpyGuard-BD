package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Test

class CameraEvidenceAssessmentTest {
    @Test
    fun radioOrReflectionAloneCannotVerifyCamera() {
        val result = assessCameraEvidence(
            CameraEvidenceSignals(suspiciousObject = true, lensVisuallyConfirmed = true)
        )
        assertEquals(CameraEvidenceLevel.VISUAL_CANDIDATE, result)
    }

    @Test
    fun multipleIndependentVisualCluesAreOnlyProbable() {
        val result = assessCameraEvidence(
            CameraEvidenceSignals(
                suspiciousObject = true,
                lensVisuallyConfirmed = true,
                concealmentOrCameraPlacement = true
            )
        )
        assertEquals(CameraEvidenceLevel.PROBABLE, result)
    }

    @Test
    fun protocolAndVisualLensEvidenceVerifyDeviceButNotActivity() {
        val result = assessCameraEvidence(
            CameraEvidenceSignals(
                lensVisuallyConfirmed = true,
                cameraSpecificProtocolConfirmed = true
            )
        )
        assertEquals(CameraEvidenceLevel.VERIFIED, result)
    }

    @Test
    fun directStreamEvidenceIsRequiredForActiveSurveillanceLevel() {
        val result = assessCameraEvidence(
            CameraEvidenceSignals(
                lensVisuallyConfirmed = true,
                cameraSpecificProtocolConfirmed = true,
                liveStreamOrRecordingDirectlyConfirmed = true
            )
        )
        assertEquals(CameraEvidenceLevel.ACTIVE_SURVEILLANCE, result)
    }

    @Test
    fun noSignalsRemainUnverified() {
        assertEquals(CameraEvidenceLevel.UNVERIFIED, assessCameraEvidence(CameraEvidenceSignals()))
    }
}
