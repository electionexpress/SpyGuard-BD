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
import com.electionexpress.spyguardbd.autoscan.AutoScanEngine
import com.electionexpress.spyguardbd.autoscan.CheckStatus
import com.electionexpress.spyguardbd.autoscan.EvidenceKind
import com.electionexpress.spyguardbd.autoscan.ScanCheck

@Composable
fun AutoScanSummaryCard(
    wifiState: String,
    wifiCount: Int,
    bluetoothState: String,
    bluetoothCount: Int
) {
    var opticalChecked by remember { mutableStateOf(false) }
    var opticalCandidate by remember { mutableStateOf(false) }

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
            if (opticalChecked) CheckStatus.COMPLETED else CheckStatus.NOT_STARTED,
            if (opticalCandidate) 1 else 0,
            if (opticalCandidate) "ব্যবহারকারী সম্ভাব্য লেন্স/অপটিক্যাল চিহ্ন লক্ষ্য করেছেন" else "ম্যানুয়াল অপটিক্যাল পরীক্ষা সম্পন্ন হয়নি",
            if (opticalCandidate) EvidenceKind.OPTICAL_CANDIDATE else null
        )
    )
    val report = AutoScanEngine.evaluate(checks)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF102B2B)),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("AUTO SCAN • সমন্বিত ফলাফল", color = Color(0xFF55E6C1), style = MaterialTheme.typography.labelLarge)
            Text(report.verdict.titleBn, color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(report.verdict.explanationBn, color = Color(0xFFAAC2C5), style = MaterialTheme.typography.bodySmall)
            Text("পর্যবেক্ষণ: ${report.totalObservations}টি • Wi-Fi: $wifiCount • Bluetooth: $bluetoothCount",
                color = Color.White, style = MaterialTheme.typography.bodyMedium)
            Text("অপটিক্যাল পরীক্ষা (ঘর/বস্তুর দৃশ্যমান পরিদর্শন)", color = Color.White, style = MaterialTheme.typography.titleSmall)
            Row {
                Checkbox(checked = opticalChecked, onCheckedChange = {
                    opticalChecked = it
                    if (!it) opticalCandidate = false
                })
                Text("আমি দৃশ্যমান লেন্স/সন্দেহজনক বস্তু পরীক্ষা করেছি", color = Color(0xFFAAC2C5), modifier = Modifier.padding(top = 12.dp))
            }
            if (opticalChecked) {
                Row {
                    Checkbox(checked = opticalCandidate, onCheckedChange = { opticalCandidate = it })
                    Text("সম্ভাব্য লেন্স/ক্যামেরা-সদৃশ চিহ্ন পেয়েছি", color = Color(0xFFFFC27A), modifier = Modifier.padding(top = 12.dp))
                }
            }
            Text(
                "Wi-Fi/Bluetooth নাম, সিগন্যাল শক্তি বা ফোনের সেন্সর একা ক্যামেরা শনাক্ত করে না। অপটিক্যাল পর্যবেক্ষণও কেবল সম্ভাব্য সূত্র—নিশ্চিত প্রমাণ নয়।",
                color = Color(0xFFAAC2C5), style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
