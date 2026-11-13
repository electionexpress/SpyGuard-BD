package com.electionexpress.spyguardbd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun LocalServiceDiscoveryPanel() {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.NSD_SERVICE) as NsdManager }
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("একই LAN-এ DNS-SD HTTP service খোঁজা হবে।") }
    val services = remember { mutableStateListOf<String>() }

    val listener = remember {
        object : NsdManager.DiscoveryListener {
            override fun onDiscoveryStarted(serviceType: String) {
                status = "লোকাল সার্ভিস আবিষ্কার চলছে…"
            }
            override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                try {
                    manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                        override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                            val row = info.serviceName + " • resolve failed (" + errorCode + ")"
                            if (!services.contains(row)) services.add(row)
                        }
                        override fun onServiceResolved(info: NsdServiceInfo) {
                            val host = info.host?.hostAddress ?: "IP অজানা"
                            val row = info.serviceName + " • " + host + ":" + info.port
                            if (!services.contains(row)) services.add(row)
                        }
                    })
                } catch (_: Exception) {
                    val row = serviceInfo.serviceName + " • resolve শুরু করা যায়নি"
                    if (!services.contains(row)) services.add(row)
                }
            }
            override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                // Keep discovered observations in this short-lived scan snapshot.
            }
            override fun onDiscoveryStopped(serviceType: String) {
                status = "লোকাল সার্ভিস স্ক্যান বন্ধ"
            }
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                scanning = false
                status = "স্ক্যান শুরু হয়নি (কোড " + errorCode + ")"
                try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                status = "স্ক্যান বন্ধ করা যায়নি (কোড " + errorCode + ")"
                try { manager.stopServiceDiscovery(this) } catch (_: Exception) {}
            }
        }
    }

    LaunchedEffect(scanning) {
        if (scanning) {
            delay(12_000)
            try { manager.stopServiceDiscovery(listener) } catch (_: Exception) {}
            scanning = false
            status = "১২ সেকেন্ডের সার্ভিস স্ক্যান শেষ • " + services.size + "টি service response"
        }
    }

    Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C))) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("LOCAL NETWORK SERVICE DISCOVERY", color = Color.White, fontSize = 16.sp)
            Text("Android DNS-SD/NSD দিয়ে একই নেটওয়ার্কে বিজ্ঞাপিত HTTP service খোঁজে। কিছু router, printer বা web-enabled device পাওয়া যেতে পারে; সব ডিভাইস নয়।",
                color = Color(0xFFB8C9C2), fontSize = 12.sp)
            Button(enabled = !scanning, onClick = {
                services.clear()
                scanning = true
                status = "স্ক্যান শুরু হচ্ছে…"
                try {
                    manager.discoverServices("_http._tcp.", NsdManager.PROTOCOL_DNS_SD, listener)
                } catch (error: Exception) {
                    scanning = false
                    status = "স্ক্যান ব্যর্থ: " + (error.localizedMessage ?: "ত্রুটি")
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text(if (scanning) "স্ক্যান চলছে…" else "লোকাল HTTP service খুঁজুন")
            }
            Text(status, color = Color(0xFFFFD18A), fontSize = 12.sp)
            services.take(30).forEach { Text("• " + it, color = Color.White, fontSize = 11.sp) }
            Text("সীমাবদ্ধতা: service discovery-তে সাড়া না দেওয়া, multicast বন্ধ, client isolation বা আলাদা VLAN-এ থাকা ডিভাইস দেখা যাবে না। HTTP service পাওয়া মানেই ক্যামেরা বা সন্দেহজনক ডিভাইস নয়।",
                color = Color(0xFFB8C9C2), fontSize = 11.sp)
        }
    }
}
