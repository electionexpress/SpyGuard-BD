package com.electionexpress.spyguardbd

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.Instant

@Composable
fun NearbyDeviceInventoryPanel(bleDevices: Map<String, String>, wifiNetworks: List<String>) {
    val context = LocalContext.current
    val observations = remember(bleDevices.toMap(), wifiNetworks.toList()) {
        buildNearbyInventory(bleDevices, wifiNetworks)
    }
    val counts = observations.groupingBy { it.kind }.eachCount()
    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Text("NEARBY DEVICE INVENTORY", color = Color.White, fontSize = 17.sp)
            Text("এই সেশনে " + observations.size + "টি পর্যবেক্ষণ • BLE " + bleDevices.size + " • Wi-Fi " + wifiNetworks.size,
                color = Color(0xFF53E0A0), fontSize = 13.sp)
            counts.forEach { (kind, count) ->
                Text("• " + kind.labelBn + ": " + count, color = Color.White, fontSize = 12.sp)
            }
            observations.take(40).forEach { item ->
                val signal = item.signalDbm?.let { " • " + it + " dBm" } ?: ""
                Text(item.displayName + " — " + item.kind.labelBn + "\nউৎস: " + item.source.labelBn + signal +
                    "\n" + item.confidence + ": " + item.detail, color = Color(0xFFB8C9C2), fontSize = 11.sp)
            }
            Button(enabled = observations.isNotEmpty(), onClick = {
                val report = nearbyInventoryReport(observations, Instant.now().toString())
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(Intent.EXTRA_SUBJECT, "SpyGuard BD — Nearby Device Report")
                    putExtra(Intent.EXTRA_TEXT, report)
                }
                context.startActivity(Intent.createChooser(share, "রিপোর্ট শেয়ার করুন"))
            }, modifier = Modifier.fillMaxWidth()) { Text("স্থানীয় স্ক্যান রিপোর্ট শেয়ার করুন") }
            Text("রিপোর্টটি বর্তমান স্ক্যানের ফল; এটি পূর্ণাঙ্গ ডিভাইস তালিকা নয়। তথ্য স্বয়ংক্রিয়ভাবে সার্ভারে পাঠানো হয় না。",
                color = Color(0xFFFFD18A), fontSize = 11.sp)
        }
    }
}
