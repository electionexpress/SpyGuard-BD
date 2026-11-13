package com.electionexpress.spyguardbd

import org.junit.Assert.assertEquals
import org.junit.Test

class BleScanFailureTest {
    @Test
    fun mapsKnownAndroidFailureCodesToActionableMessages() {
        assertEquals("স্ক্যান আগে থেকেই চালু আছে (কোড 1)", bleScanFailureMessage(1))
        assertEquals(
            "Bluetooth scanner নিবন্ধন করা যায়নি; Bluetooth বন্ধ করে আবার চালু করুন (কোড 2)",
            bleScanFailureMessage(2)
        )
        assertEquals(
            "Android-এর অভ্যন্তরীণ Bluetooth সমস্যা; কিছুক্ষণ পরে আবার চেষ্টা করুন (কোড 3)",
            bleScanFailureMessage(3)
        )
        assertEquals("এই ডিভাইসে BLE স্ক্যান সমর্থিত নয় (কোড 4)", bleScanFailureMessage(4))
        assertEquals(
            "Bluetooth স্ক্যানের হার্ডওয়্যার রিসোর্স ব্যস্ত; পরে চেষ্টা করুন (কোড 5)",
            bleScanFailureMessage(5)
        )
        assertEquals(
            "খুব ঘন ঘন স্ক্যান করা হয়েছে; কিছুক্ষণ অপেক্ষা করুন (কোড 6)",
            bleScanFailureMessage(6)
        )
    }

    @Test
    fun preservesUnknownCodeForDiagnostics() {
        assertEquals(
            "অজানা Bluetooth স্ক্যান সমস্যা; Bluetooth ও অনুমতি যাচাই করুন (কোড 99)",
            bleScanFailureMessage(99)
        )
    }
}
