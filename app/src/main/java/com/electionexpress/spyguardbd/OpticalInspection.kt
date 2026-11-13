package com.electionexpress.spyguardbd

/**
 * Records observations made by a person during a visual room inspection.
 *
 * These are not machine detections. No observation, alone or combined, proves
 * that an object contains a camera.
 */
data class OpticalInspection(
    val lensLikeElement: Boolean = false,
    val repeatableGlintFromMultipleAngles: Boolean = false,
    val visiblePowerOrDataConnection: Boolean = false,
    val independentlyRechecked: Boolean = false
) {
    val recordedObservationCount: Int
        get() = listOf(
            lensLikeElement,
            repeatableGlintFromMultipleAngles,
            visiblePowerOrDataConnection,
            independentlyRechecked
        ).count { it }

    fun guidance(): String = when (recordedObservationCount) {
        0 -> "কোনো পর্যবেক্ষণ নথিভুক্ত হয়নি। ঘরটি ধীরে ধীরে দেখুন; এটি নিরাপদ হওয়ার প্রমাণ নয়।"
        1 -> "একটি বিষয় নথিভুক্ত হয়েছে। ভিন্ন কোণ ও আলোতে আবার যাচাই করুন; এটি ক্যামেরার প্রমাণ নয়।"
        else -> "একাধিক বিষয় নথিভুক্ত হয়েছে। বস্তুটি নিরাপদ দূরত্ব থেকে পুনরায় পরীক্ষা করুন এবং প্রয়োজনে সরাসরি যাচাই করুন। এটি স্বয়ংক্রিয় শনাক্তকরণ নয়।"
    }
}
