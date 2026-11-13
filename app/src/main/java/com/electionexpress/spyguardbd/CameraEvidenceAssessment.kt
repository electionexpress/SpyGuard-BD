package com.electionexpress.spyguardbd

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal data class CameraEvidenceSignals(
    val suspiciousObject: Boolean = false,
    val lensVisuallyConfirmed: Boolean = false,
    val concealmentOrCameraPlacement: Boolean = false,
    val cameraSpecificProtocolConfirmed: Boolean = false,
    val liveStreamOrRecordingDirectlyConfirmed: Boolean = false
)

internal enum class CameraEvidenceLevel(val label: String) {
    NOT_TESTED("Not Tested"),
    UNVERIFIED("Unverified"),
    VISUAL_CANDIDATE("Level 1 — Visual Candidate"),
    PROBABLE("Level 2 — Probable Camera"),
    VERIFIED("Level 3 — Camera Device Verified"),
    ACTIVE_SURVEILLANCE("Level 4 — Active Surveillance Evidence")
}

/** Conservative policy: radio presence, reflection, or IR glow alone never verifies a camera. */
internal fun assessCameraEvidence(signals: CameraEvidenceSignals): CameraEvidenceLevel {
    if (signals.cameraSpecificProtocolConfirmed && signals.lensVisuallyConfirmed) {
        return if (signals.liveStreamOrRecordingDirectlyConfirmed) {
            CameraEvidenceLevel.ACTIVE_SURVEILLANCE
        } else CameraEvidenceLevel.VERIFIED
    }
    if (signals.suspiciousObject && signals.lensVisuallyConfirmed &&
        signals.concealmentOrCameraPlacement
    ) return CameraEvidenceLevel.PROBABLE

    if (signals.suspiciousObject || signals.lensVisuallyConfirmed ||
        signals.concealmentOrCameraPlacement
    ) return CameraEvidenceLevel.VISUAL_CANDIDATE

    return CameraEvidenceLevel.UNVERIFIED
}

@Composable
fun CameraEvidenceAssessmentPanel() {
    var suspiciousObject by remember { mutableStateOf(false) }
    var lensConfirmed by remember { mutableStateOf(false) }
    var concealment by remember { mutableStateOf(false) }
    var protocolConfirmed by remember { mutableStateOf(false) }
    var activeEndpoint by remember { mutableStateOf(false) }

    val level = assessCameraEvidence(
        CameraEvidenceSignals(suspiciousObject, lensConfirmed, concealment, protocolConfirmed, activeEndpoint)
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF172820))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("EVIDENCE VERIFICATION", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text("নিচের পর্যবেক্ষণগুলো হাতে-কলমে যাচাই করে নির্বাচন করুন। এটি স্বয়ংক্রিয় সেন্সর ফল নয়।", color = Color(0xFFB8C9C2), fontSize = 12.sp)
            EvidenceCheckRow("সন্দেহজনক বস্তু/ক্যামেরার মতো housing দেখা গেছে", suspiciousObject) { suspiciousObject = it }
            EvidenceCheckRow("কাছ থেকে লেন্স/অপটিক্যাল অংশ নিশ্চিতভাবে দেখা গেছে", lensConfirmed) { lensConfirmed = it }
            EvidenceCheckRow("বস্তুটি অস্বাভাবিকভাবে আড়াল করা বা ক্যামেরার উপযোগী স্থানে বসানো", concealment) { concealment = it }
            EvidenceCheckRow("ONVIF/RTSP বা camera-specific endpoint সরাসরি যাচাই করা হয়েছে", protocolConfirmed) { protocolConfirmed = it }
            EvidenceCheckRow("সরাসরি live stream বা recording status দেখা/যাচাই করা হয়েছে", activeEndpoint) { activeEndpoint = it }

            Text("বর্তমান মূল্যায়ন: ${level.label}", color = Color(0xFF53E0A0), style = MaterialTheme.typography.titleSmall)
            Text(
                when (level) {
                    CameraEvidenceLevel.ACTIVE_SURVEILLANCE -> "সক্রিয় stream/recording-এর প্রত্যক্ষ প্রমাণ নথিবদ্ধ করুন।"
                    CameraEvidenceLevel.VERIFIED -> "ক্যামেরা ডিভাইসের পক্ষে শক্ত প্রমাণ আছে; সক্রিয় রেকর্ডিং প্রমাণিত নয়।"
                    CameraEvidenceLevel.PROBABLE -> "একাধিক দৃশ্যমান সংকেত আছে; অতিরিক্ত যাচাই প্রয়োজন।"
                    CameraEvidenceLevel.VISUAL_CANDIDATE -> "এটি কেবল প্রাথমিক সন্দেহ; LED, আয়না, স্ক্রু বা প্রতিফলন ভুল সংকেত হতে পারে।"
                    CameraEvidenceLevel.UNVERIFIED -> "যথেষ্ট নির্ভরযোগ্য প্রমাণ নেই।"
                    CameraEvidenceLevel.NOT_TESTED -> "পরীক্ষা করা হয়নি।"
                },
                color = Color(0xFFFFD18A), fontSize = 12.sp
            )
            Text(
                "Wi-Fi/Bluetooth signal, SSID-এর নাম, IR আলো বা lens reflection একা Level 3/4 দিতে পারে না। এই checklist ব্যবহারকারীর ম্যানুয়াল পর্যবেক্ষণ; অ্যাপ এখনো ONVIF/RTSP স্বয়ংক্রিয়ভাবে স্ক্যান করছে না।",
                color = Color(0xFFB8C9C2), fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun EvidenceCheckRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = Color.White, fontSize = 12.sp, modifier = Modifier.padding(top = 12.dp))
    }
}
