package com.electionexpress.spyguardbd

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.electionexpress.spyguardbd.autoscan.AutoScanEngine
import com.electionexpress.spyguardbd.autoscan.CheckStatus
import com.electionexpress.spyguardbd.autoscan.EvidenceKind
import com.electionexpress.spyguardbd.autoscan.ScanCheck
import com.electionexpress.spyguardbd.autoscan.ScanVerdict

private val ScanMint = Color(0xFF62E6C4)
private val ScanTextMuted = Color(0xFFA9BEC5)
private val ScanPanel = Color(0xFF10242B)

@Composable
fun AutoScanSummaryCard(
    wifiState: String,
    wifiCount: Int,
    bluetoothState: String,
    bluetoothCount: Int,
    opticalInspection: OpticalInspection,
    opticalInspectionCompleted: Boolean
) {
    val opticalCandidate = opticalInspection.lensLikeElement || opticalInspection.repeatableGlintFromMultipleAngles

    val wifiStatus = when {
        wifiState.contains("সম্পন্ন") || wifiState.contains("নেটওয়ার্ক") && wifiCount > 0 -> CheckStatus.COMPLETED
        wifiState.contains("অনুমতি") -> CheckStatus.PERMISSION_DENIED
        wifiState.contains("চলছে") -> CheckStatus.RUNNING
        else -> CheckStatus.NOT_STARTED
    }
    val bluetoothStatus = when {
        bluetoothState.contains("সম্পন্ন") || bluetoothCount > 0 -> CheckStatus.COMPLETED
        bluetoothState.contains("অনুমতি") -> CheckStatus.PERMISSION_DENIED
        bluetoothState.contains("চলছে") -> CheckStatus.RUNNING
        else -> CheckStatus.NOT_STARTED
    }
    val checks = listOf(
        ScanCheck("Wi-Fi", wifiStatus, wifiCount, wifiState, EvidenceKind.WIFI_NETWORK),
        ScanCheck("Bluetooth", bluetoothStatus, bluetoothCount, bluetoothState, EvidenceKind.BLUETOOTH_ADVERTISEMENT),
        ScanCheck(
            "Optical",
            if (opticalInspectionCompleted) CheckStatus.COMPLETED else CheckStatus.NOT_STARTED,
            opticalInspection.recordedObservationCount,
            if (opticalCandidate) "ব্যবহারকারী লেন্স-সদৃশ উপাদান বা পুনরাবৃত্ত ঝলক নথিভুক্ত করেছেন" else opticalInspection.guidance(),
            if (opticalCandidate) EvidenceKind.OPTICAL_CANDIDATE else null
        )
    )
    val report = AutoScanEngine.evaluate(checks)
    val verdictColor = when (report.verdict) {
        ScanVerdict.POSSIBLE_CAMERA_SIGNAL -> Color(0xFFFFC078)
        ScanVerdict.NO_SUSPICIOUS_SIGNAL -> ScanMint
        ScanVerdict.UNDETERMINED -> Color(0xFFFFD166)
    }
    val completedCount = checks.count { it.status == CheckStatus.COMPLETED }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ScanPanel),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = ScanMint.copy(alpha = 0.13f), shape = RoundedCornerShape(12.dp)) {
                    Text("⌁", color = ScanMint, fontSize = 23.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 11.dp, vertical = 5.dp))
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("SMART AUTO SCAN", color = ScanMint, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                    Text("সমন্বিত নিরাপত্তা পরীক্ষা", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Surface(color = verdictColor.copy(alpha = 0.14f), shape = CircleShape) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Spacer(Modifier.size(6.dp).background(verdictColor, CircleShape))
                        Spacer(Modifier.width(6.dp))
                        Text("$completedCount/3 পরীক্ষা", color = verdictColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Surface(
                color = verdictColor.copy(alpha = 0.09f),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("বর্তমান মূল্যায়ন", color = ScanTextMuted, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                    Text(report.verdict.titleBn, color = verdictColor, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text(report.verdict.explanationBn, color = Color(0xFFD0DEE1), fontSize = 12.sp, lineHeight = 18.sp)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ScanMetric("Wi-Fi", wifiCount.toString(), wifiStatus == CheckStatus.COMPLETED, Modifier.weight(1f))
                ScanMetric("Bluetooth", bluetoothCount.toString(), bluetoothStatus == CheckStatus.COMPLETED, Modifier.weight(1f))
                ScanMetric("মোট পর্যবেক্ষণ", report.totalObservations.toString(), completedCount > 0, Modifier.weight(1f))
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("পরীক্ষার অবস্থা", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                ScanSourceRow("Wi-Fi নেটওয়ার্ক", wifiState, wifiStatus)
                ScanSourceRow("Bluetooth ডিভাইস", bluetoothState, bluetoothStatus)
                ScanSourceRow(
                    "লেন্স / অপটিক্যাল পর্যবেক্ষণ",
                    if (opticalInspectionCompleted) "হাতে করা অপটিক্যাল পরীক্ষা সম্পন্ন • ${opticalInspection.recordedObservationCount}/4 পর্যবেক্ষণ" else "অপটিক্যাল পরীক্ষা এখনো সম্পন্ন হিসেবে চিহ্নিত হয়নি",
                    if (opticalInspectionCompleted) CheckStatus.COMPLETED else CheckStatus.NOT_STARTED
                )
            }

            Surface(color = Color.White.copy(alpha = 0.035f), shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(13.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("অপটিক্যাল পর্যবেক্ষণ", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Text(
                        if (opticalInspectionCompleted) "ক্যামেরা পরিদর্শন স্ক্রিনে আপনার নথিভুক্ত পর্যবেক্ষণ এখানে প্রতিফলিত হচ্ছে।"
                        else "ক্যামেরা পরিদর্শন স্ক্রিনে পর্যবেক্ষণ সম্পন্ন চিহ্নিত করলে সমন্বিত মূল্যায়ন আপডেট হবে।",
                        color = ScanTextMuted, fontSize = 11.sp, lineHeight = 16.sp
                    )
                    Text(
                        if (opticalCandidate) "সম্ভাব্য অপটিক্যাল সূত্র নথিভুক্ত — সরাসরি যাচাই প্রয়োজন।"
                        else "এখন পর্যন্ত লেন্স-সদৃশ উপাদান বা পুনরাবৃত্ত ঝলক নথিভুক্ত হয়নি।",
                        color = if (opticalCandidate) Color(0xFFFFC078) else ScanTextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Text(
                "মনে রাখুন: Wi-Fi/Bluetooth ডিভাইসের সংখ্যা ক্যামেরার সংখ্যা নয়। এই ফলাফল কেবল উপলভ্য পরীক্ষার ভিত্তিতে প্রাথমিক মূল্যায়ন।",
                color = ScanTextMuted, fontSize = 11.sp, lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun ScanMetric(label: String, value: String, active: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, color = Color.White.copy(alpha = 0.045f), shape = RoundedCornerShape(15.dp)) {
        Column(Modifier.padding(horizontal = 10.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(value, color = if (active) ScanMint else Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Text(label, color = ScanTextMuted, fontSize = 10.sp, maxLines = 1)
        }
    }
}

@Composable
private fun ScanSourceRow(title: String, detail: String, status: CheckStatus) {
    val (label, tint) = when (status) {
        CheckStatus.COMPLETED -> "সম্পন্ন" to ScanMint
        CheckStatus.RUNNING -> "চলছে" to Color(0xFF7EC8FF)
        CheckStatus.PERMISSION_DENIED -> "অনুমতি নেই" to Color(0xFFFF8F8F)
        CheckStatus.UNAVAILABLE -> "অনুপলব্ধ" to Color(0xFFFFC078)
        CheckStatus.FAILED -> "ব্যর্থ" to Color(0xFFFF8F8F)
        CheckStatus.NOT_STARTED -> "অপেক্ষমাণ" to ScanTextMuted
    }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White.copy(alpha = 0.035f), RoundedCornerShape(12.dp)).padding(11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = ScanTextMuted, fontSize = 10.sp, maxLines = 2)
        }
        Surface(color = tint.copy(alpha = 0.12f), shape = CircleShape) {
            Text(label, color = tint, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
        }
    }
}
