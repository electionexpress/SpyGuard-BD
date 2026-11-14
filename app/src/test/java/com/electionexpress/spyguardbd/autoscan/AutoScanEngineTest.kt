package com.electionexpress.spyguardbd.autoscan

import org.junit.Assert.assertEquals
import org.junit.Test

class AutoScanEngineTest {
    @Test fun wifiAndBluetoothPresenceNeverProveCamera() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED, 4, evidenceKind = EvidenceKind.WIFI_NETWORK),
            ScanCheck("Bluetooth", CheckStatus.COMPLETED, 2, evidenceKind = EvidenceKind.BLUETOOTH_ADVERTISEMENT)
        ))
        assertEquals(ScanVerdict.UNDETERMINED, report.verdict)
    }

    @Test fun opticalCandidateProducesOnlyPossibleSignal() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Optical", CheckStatus.COMPLETED, 1, evidenceKind = EvidenceKind.OPTICAL_CANDIDATE)
        ))
        assertEquals(ScanVerdict.POSSIBLE_CAMERA_SIGNAL, report.verdict)
    }

    @Test fun noSuspiciousSignalRequiresEveryRequiredCheckCompleted() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED),
            ScanCheck("Bluetooth", CheckStatus.COMPLETED),
            ScanCheck("Optical", CheckStatus.COMPLETED)
        ))
        assertEquals(ScanVerdict.NO_SUSPICIOUS_SIGNAL, report.verdict)
    }

    @Test fun deniedPermissionMeansUndetermined() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED),
            ScanCheck("Bluetooth", CheckStatus.PERMISSION_DENIED),
            ScanCheck("Optical", CheckStatus.COMPLETED)
        ))
        assertEquals(ScanVerdict.UNDETERMINED, report.verdict)
    }

    @Test fun incompleteOpticalCandidateDoesNotCountAsEvidence() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Optical", CheckStatus.RUNNING, 1, evidenceKind = EvidenceKind.OPTICAL_CANDIDATE),
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED),
            ScanCheck("Bluetooth", CheckStatus.COMPLETED)
        ))
        assertEquals(ScanVerdict.UNDETERMINED, report.verdict)
    }

    @Test fun failedRequiredSourceCannotProduceCleanVerdict() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED),
            ScanCheck("Bluetooth", CheckStatus.FAILED),
            ScanCheck("Optical", CheckStatus.COMPLETED)
        ))
        assertEquals(ScanVerdict.UNDETERMINED, report.verdict)
    }

    @Test fun unavailableRequiredSourceCannotProduceCleanVerdict() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED),
            ScanCheck("Bluetooth", CheckStatus.UNAVAILABLE),
            ScanCheck("Optical", CheckStatus.COMPLETED)
        ))
        assertEquals(ScanVerdict.UNDETERMINED, report.verdict)
    }

    @Test fun negativeObservationCountsDoNotReduceOrInflateTotals() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED, -8),
            ScanCheck("Bluetooth", CheckStatus.COMPLETED, 3),
            ScanCheck("Optical", CheckStatus.COMPLETED, 1)
        ))
        assertEquals(4, report.totalObservations)
    }

    @Test fun genericSensorReadingDoesNotProveCamera() {
        val report = AutoScanEngine.evaluate(listOf(
            ScanCheck("Optical", CheckStatus.COMPLETED),
            ScanCheck("Wi-Fi", CheckStatus.COMPLETED),
            ScanCheck("Bluetooth", CheckStatus.COMPLETED),
            ScanCheck("Sensors", CheckStatus.COMPLETED, evidenceKind = EvidenceKind.SENSOR_READING)
        ))
        assertEquals(ScanVerdict.NO_SUSPICIOUS_SIGNAL, report.verdict)
    }
}
