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

    @Test
    fun parsesOnvifXAddrsAndDecodesAmpersands() {
        val xml = """<tds:ProbeMatch><d:XAddrs>http://192.168.1.20/onvif/device_service?x=1&amp;y=2 https://192.168.1.20/onvif</d:XAddrs></tds:ProbeMatch>"""
        val result = parseOnvifDiscoveryResponse(xml, "192.168.1.20")
        assertEquals("192.168.1.20", result.sourceAddress)
        assertEquals(
            listOf("http://192.168.1.20/onvif/device_service?x=1&y=2", "https://192.168.1.20/onvif"),
            result.serviceAddresses
        )
    }

    @Test
    fun ignoresNonHttpAdvertisedAddresses() {
        val xml = """<d:XAddrs>file:///tmp/camera ftp://192.168.1.20/onvif</d:XAddrs>"""
        val result = parseOnvifDiscoveryResponse(xml, "192.168.1.20")
        assertEquals(emptyList<String>(), result.serviceAddresses)
    }
}
