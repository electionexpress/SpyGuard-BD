package com.electionexpress.spyguardbd.autoscan

enum class CheckStatus { NOT_STARTED, RUNNING, COMPLETED, UNAVAILABLE, PERMISSION_DENIED, FAILED }
enum class EvidenceKind { WIFI_NETWORK, BLUETOOTH_ADVERTISEMENT, SENSOR_READING, OPTICAL_CANDIDATE, CAMERA_PROTOCOL_ENDPOINT }

data class ScanCheck(
    val source: String,
    val status: CheckStatus,
    val observationCount: Int = 0,
    val detail: String = "",
    val evidenceKind: EvidenceKind? = null
)

enum class ScanVerdict(val titleBn: String, val explanationBn: String) {
    POSSIBLE_CAMERA_SIGNAL(
        "সম্ভাব্য ক্যামেরা সংকেত পাওয়া গেছে",
        "ক্যামেরা-সংশ্লিষ্ট নির্দিষ্ট প্রমাণ বা দৃশ্যমান সম্ভাব্য লেন্স পাওয়া গেছে। সরাসরি যাচাই প্রয়োজন।"
    ),
    NO_SUSPICIOUS_SIGNAL(
        "সন্দেহজনক সংকেত পাওয়া যায়নি",
        "সম্পন্ন হওয়া পরীক্ষাগুলোতে সন্দেহজনক সংকেত মেলেনি। এটি সব ধরনের ক্যামেরা বা রেকর্ডার নেই—এমন নিশ্চয়তা নয়।"
    ),
    UNDETERMINED(
        "নিশ্চিত হওয়া যায়নি",
        "কিছু পরীক্ষা অসম্পূর্ণ, অনুপলব্ধ বা অনুমতিবিহীন; তাই নিরাপদ/ঝুঁকিমুক্ত সিদ্ধান্ত দেওয়া যাচ্ছে না।"
    )
}

data class AutoScanReport(
    val verdict: ScanVerdict,
    val checks: List<ScanCheck>,
    val totalObservations: Int
)

/**
 * Conservative evidence aggregation.
 * Generic Wi-Fi/BLE presence and phone sensor readings never count as camera proof.
 */
object AutoScanEngine {
    fun evaluate(
        checks: List<ScanCheck>,
        requiredSources: Set<String> = setOf("Wi-Fi", "Bluetooth", "Optical")
    ): AutoScanReport {
        val directCameraEvidence = checks.any {
            it.status == CheckStatus.COMPLETED &&
                (it.evidenceKind == EvidenceKind.CAMERA_PROTOCOL_ENDPOINT ||
                 it.evidenceKind == EvidenceKind.OPTICAL_CANDIDATE)
        }
        val allRequiredComplete = requiredSources.all { source ->
            checks.any { it.source == source && it.status == CheckStatus.COMPLETED }
        }
        val verdict = when {
            directCameraEvidence -> ScanVerdict.POSSIBLE_CAMERA_SIGNAL
            allRequiredComplete -> ScanVerdict.NO_SUSPICIOUS_SIGNAL
            else -> ScanVerdict.UNDETERMINED
        }
        return AutoScanReport(
            verdict = verdict,
            checks = checks,
            totalObservations = checks.sumOf { it.observationCount.coerceAtLeast(0) }
        )
    }
}
