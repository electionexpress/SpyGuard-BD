package com.electionexpress.spyguardbd

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A user-authored inspection note. It records observations only and never
 * represents them as an automated camera detection.
 */
data class InspectionReport(
    val roomLabel: String,
    val observedAt: String,
    val observations: OpticalInspection,
    val notes: String = "",
    val nearbyWifiNetworks: List<String> = emptyList(),
    val nearbyBluetoothDevices: List<String> = emptyList(),
    val opticalSweep: OpticalSweepSession = OpticalSweepSession()
) {
    fun toPlainText(): String = buildString {
        appendLine("SpyGuard BD — Inspection Report")
        appendLine("সময়: $observedAt")
        appendLine("স্থান/রুম: ${roomLabel.ifBlank { "উল্লেখ করা হয়নি" }}")
        appendLine()
        appendLine("হাতে করা পর্যবেক্ষণ (${observations.recordedObservationCount}/4):")
        appendLine("- লেন্সের মতো উপাদান: ${yesNo(observations.lensLikeElement)}")
        appendLine("- একাধিক কোণ থেকে পুনরাবৃত্ত ঝলক: ${yesNo(observations.repeatableGlintFromMultipleAngles)}")
        appendLine("- বিদ্যুৎ/ডেটা সংযোগের চিহ্ন: ${yesNo(observations.visiblePowerOrDataConnection)}")
        appendLine("- স্বাধীনভাবে পুনরায় যাচাই: ${yesNo(observations.independentlyRechecked)}")
        appendLine("পর্যবেক্ষণ নির্দেশনা: ${observations.guidance()}")
        appendLine()
        appendLine(
            "নির্দেশিত রুম সুইপ (" + opticalSweep.checkedCount + "/" + opticalSweep.totalCount +
                " অংশ পরীক্ষা; " + opticalSweep.followUpCount + "টি পুনরায় যাচাই প্রয়োজন):"
        )
        opticalSweep.orderedEntries.forEach { entry ->
            val area = OpticalSweepCatalog.areas.first { it.id == entry.areaId }
            val time = entry.observedAtMillis?.let { formatOpticalSweepTimestamp(it) } ?: "সময় নথিভুক্ত হয়নি"
            appendLine("- " + area.title + ": " + entry.result.reportText + " (সময়: " + time + ")")
        }
        appendLine("রুম সুইপ হাতে করা পর্যবেক্ষণ; প্রতিফলন বা কোনো বস্তু একা ক্যামেরার প্রমাণ নয়।")
        appendLine()
        appendLine("স্ক্যানের সময় দৃশ্যমান Wi-Fi নেটওয়ার্ক (${nearbyWifiNetworks.size}):")
        if (nearbyWifiNetworks.isEmpty()) appendLine("- কোনো Wi-Fi ফলাফল সংযুক্ত করা হয়নি")
        else nearbyWifiNetworks.forEach { appendLine("- $it") }
        appendLine()
        appendLine("স্ক্যানের সময় দৃশ্যমান Bluetooth LE ডিভাইস (${nearbyBluetoothDevices.size}):")
        if (nearbyBluetoothDevices.isEmpty()) appendLine("- কোনো BLE ফলাফল সংযুক্ত করা হয়নি")
        else nearbyBluetoothDevices.forEach { appendLine("- $it") }
        appendLine()
        appendLine("অতিরিক্ত নোট:")
        appendLine(notes.ifBlank { "কোনো অতিরিক্ত নোট নেই" })
        appendLine()
        appendLine("সতর্কতা: এটি ব্যবহারকারীর নথিভুক্ত পর্যবেক্ষণ; স্বয়ংক্রিয় ক্যামেরা শনাক্তকরণ নয়। Wi-Fi/Bluetooth ফলাফল কেবল দৃশ্যমান নেটওয়ার্ক/বিজ্ঞাপন, ক্যামেরার প্রমাণ নয়। কোনো প্রতিফলনও ক্যামেরার প্রমাণ নয়। অফলাইন, বন্ধ, তারযুক্ত বা সম্প্রচার না করা ডিভাইস তালিকায় নাও আসতে পারে.")
    }

    private fun yesNo(value: Boolean): String = if (value) "হ্যাঁ" else "না"
}

private fun formatOpticalSweepTimestamp(timestampMillis: Long): String =
    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date(timestampMillis))
