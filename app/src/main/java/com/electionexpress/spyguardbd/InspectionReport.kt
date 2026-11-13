package com.electionexpress.spyguardbd

/**
 * A user-authored inspection note. It records observations only and never
 * represents them as an automated camera detection.
 */
data class InspectionReport(
    val roomLabel: String,
    val observedAt: String,
    val observations: OpticalInspection,
    val notes: String = ""
) {
    fun toPlainText(): String = buildString {
        appendLine("SpyGuard BD — Inspection Report")
        appendLine("সময়: $observedAt")
        appendLine("স্থান/রুম: ${roomLabel.ifBlank { "উল্লেখ করা হয়নি" }}")
        appendLine()
        appendLine("হাতে করা পর্যবেক্ষণ:")
        appendLine("- লেন্সের মতো উপাদান: ${yesNo(observations.lensLikeElement)}")
        appendLine("- একাধিক কোণ থেকে পুনরাবৃত্ত ঝলক: ${yesNo(observations.repeatableGlintFromMultipleAngles)}")
        appendLine("- বিদ্যুৎ/ডেটা সংযোগের চিহ্ন: ${yesNo(observations.visiblePowerOrDataConnection)}")
        appendLine("- স্বাধীনভাবে পুনরায় যাচাই: ${yesNo(observations.independentlyRechecked)}")
        appendLine()
        appendLine("অতিরিক্ত নোট:")
        appendLine(notes.ifBlank { "কোনো অতিরিক্ত নোট নেই" })
        appendLine()
        appendLine("সতর্কতা: এটি ব্যবহারকারীর নথিভুক্ত পর্যবেক্ষণ; স্বয়ংক্রিয় ক্যামেরা শনাক্তকরণ নয়। কোনো প্রতিফলন বা একক লক্ষণ ক্যামেরার প্রমাণ নয়। প্রয়োজনে নিরাপদ দূরত্ব থেকে পুনরায় যাচাই করুন।")
    }

    private fun yesNo(value: Boolean): String = if (value) "হ্যাঁ" else "না"
}
