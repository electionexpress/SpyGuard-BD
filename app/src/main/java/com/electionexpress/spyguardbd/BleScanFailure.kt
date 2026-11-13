package com.electionexpress.spyguardbd

/**
 * Converts Android BLE ScanCallback failure codes into actionable, cautious UI guidance.
 * The raw code is retained in the message for diagnostics; it is not a device-detection result.
 */
fun bleScanFailureMessage(errorCode: Int): String {
    val explanation = when (errorCode) {
        1 -> "স্ক্যান আগে থেকেই চালু আছে"
        2 -> "Bluetooth scanner নিবন্ধন করা যায়নি; Bluetooth বন্ধ করে আবার চালু করুন"
        3 -> "Android-এর অভ্যন্তরীণ Bluetooth সমস্যা; কিছুক্ষণ পরে আবার চেষ্টা করুন"
        4 -> "এই ডিভাইসে BLE স্ক্যান সমর্থিত নয়"
        5 -> "Bluetooth স্ক্যানের হার্ডওয়্যার রিসোর্স ব্যস্ত; পরে চেষ্টা করুন"
        6 -> "খুব ঘন ঘন স্ক্যান করা হয়েছে; কিছুক্ষণ অপেক্ষা করুন"
        else -> "অজানা Bluetooth স্ক্যান সমস্যা; Bluetooth ও অনুমতি যাচাই করুন"
    }
    return "$explanation (কোড $errorCode)"
}
