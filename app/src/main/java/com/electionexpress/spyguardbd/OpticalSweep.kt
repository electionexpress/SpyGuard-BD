package com.electionexpress.spyguardbd

/** A visible room area included in the user-guided optical sweep. */
data class OpticalSweepArea(
    val id: String,
    val title: String,
    val guidance: String
)

object OpticalSweepCatalog {
    val areas = listOf(
        OpticalSweepArea("ceiling", "সিলিং, লাইট ও স্মোক ডিটেক্টর", "শুধু দৃশ্যমান অংশ দেখুন; কোনো কভার খুলবেন না।"),
        OpticalSweepArea("wall-decor", "দেয়ালঘড়ি, ফ্রেম ও সাজসজ্জা", "ছোট ছিদ্র, অস্বাভাবিক কাচ বা নতুন বস্তু লক্ষ্য করুন।"),
        OpticalSweepArea("vents", "ভেন্ট, গ্রিল ও এয়ারকন্ডিশনার", "নিরাপদ দূরত্ব থেকে গ্রিলের দৃশ্যমান অংশ পরীক্ষা করুন।"),
        OpticalSweepArea("power", "চার্জার, অ্যাডাপ্টার ও মাল্টিপ্লাগ", "অচেনা বৈদ্যুতিক বস্তু খুলবেন না বা স্পর্শ করবেন না।"),
        OpticalSweepArea("furniture", "শেলফ, আসবাব ও ছোট ডেকর", "বস্তুটির স্বাভাবিক নকশার সঙ্গে অস্বাভাবিক অংশ তুলনা করুন।"),
        OpticalSweepArea("mirrors", "আয়না, কাচ ও চকচকে পৃষ্ঠ", "বিভিন্ন কোণে দেখুন; সাধারণ প্রতিফলনও হতে পারে।"),
        OpticalSweepArea("electronics", "টিভি, স্পিকার ও অন্যান্য ইলেকট্রনিকস", "দৃশ্যমান লেন্সসদৃশ অংশ থাকলে নোট করুন; সেটি ক্যামেরা প্রমাণ করে না।"),
        OpticalSweepArea("doors-windows", "দরজা, জানালা ও অন্যান্য দৃশ্যমান স্থান", "শুধু নিরাপদে দেখা যায় এমন অংশ পর্যবেক্ষণ করুন।")
    )
}

enum class OpticalSweepResult(
    val displayLabel: String,
    val reportText: String,
    val needsFollowUp: Boolean
) {
    NOT_CHECKED("পরীক্ষা হয়নি", "এই অংশ পরীক্ষা করা হয়নি", false),
    NO_VISIBLE_CANDIDATE(
        "দৃশ্যমান কিছু চোখে পড়েনি",
        "এই অংশে দৃশ্যমান সন্দেহজনক কিছু চোখে পড়েনি; এটি নিরাপত্তার নিশ্চয়তা নয়",
        false
    ),
    POSSIBLE_REFLECTION(
        "সম্ভাব্য আলোর প্রতিফলন",
        "সম্ভাব্য প্রতিফলন; আলো ও কোণ বদলে পুনরায় যাচাই করুন—এটি ক্যামেরার প্রমাণ নয়",
        true
    ),
    NEEDS_MANUAL_RECHECK(
        "বস্তুটি পুনরায় যাচাই প্রয়োজন",
        "বস্তুটি পুনরায় যাচাই প্রয়োজন; ক্যামেরা হিসেবে নিশ্চিত নয়",
        true
    )
}

data class OpticalSweepEntry(
    val areaId: String,
    val result: OpticalSweepResult = OpticalSweepResult.NOT_CHECKED,
    val observedAtMillis: Long? = null
) {
    init {
        require((result == OpticalSweepResult.NOT_CHECKED) == (observedAtMillis == null)) {
            "Only a recorded result may have an observation timestamp"
        }
    }
}

/**
 * Immutable, user-entered room-sweep record. It does not score risk or infer
 * that a camera exists. A "no visible candidate" result is not an all-clear.
 */
data class OpticalSweepSession(
    val entries: List<OpticalSweepEntry> = OpticalSweepCatalog.areas.map { OpticalSweepEntry(it.id) }
) {
    init {
        require(entries.map { it.areaId }.distinct().size == entries.size) {
            "Room-sweep area entries must be unique"
        }
        require(entries.all { entry -> OpticalSweepCatalog.areas.any { it.id == entry.areaId } }) {
            "Unknown room-sweep area"
        }
    }

    val totalCount: Int
        get() = OpticalSweepCatalog.areas.size

    val checkedCount: Int
        get() = entries.count { it.result != OpticalSweepResult.NOT_CHECKED }

    val followUpCount: Int
        get() = entries.count { it.result.needsFollowUp }

    val orderedEntries: List<OpticalSweepEntry>
        get() = OpticalSweepCatalog.areas.map { area ->
            entries.firstOrNull { it.areaId == area.id } ?: OpticalSweepEntry(area.id)
        }

    fun entryFor(areaId: String): OpticalSweepEntry {
        require(OpticalSweepCatalog.areas.any { it.id == areaId }) { "Unknown room-sweep area" }
        return entries.firstOrNull { it.areaId == areaId } ?: OpticalSweepEntry(areaId)
    }

    fun record(
        areaId: String,
        result: OpticalSweepResult,
        observedAtMillis: Long
    ): OpticalSweepSession {
        require(OpticalSweepCatalog.areas.any { it.id == areaId }) { "Unknown room-sweep area" }
        require(observedAtMillis >= 0L) { "Observation timestamp cannot be negative" }
        val entry = OpticalSweepEntry(
            areaId = areaId,
            result = result,
            observedAtMillis = if (result == OpticalSweepResult.NOT_CHECKED) null else observedAtMillis
        )
        val updated = entries.toMutableList()
        val index = updated.indexOfFirst { it.areaId == areaId }
        if (index >= 0) updated[index] = entry else updated.add(entry)
        return copy(
            entries = OpticalSweepCatalog.areas.mapNotNull { area ->
                updated.firstOrNull { it.areaId == area.id }
            }
        )
    }
}
