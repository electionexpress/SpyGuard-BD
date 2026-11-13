package com.electionexpress.spyguardbd

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.concurrent.ConcurrentHashMap

internal data class LanServiceObservation(
    val serviceName: String,
    val serviceType: String,
    val hostAddress: String?,
    val port: Int,
    val categoryLabel: String
)

internal fun classifyLanServiceType(type: String): String {
    val normalized = type.lowercase()
    return when {
        "_googlecast" in normalized -> "মিডিয়া/কাস্ট ডিভাইস"
        "_airplay" in normalized || "_raop" in normalized -> "মিডিয়া/এয়ারপ্লে ডিভাইস"
        "_ipp" in normalized || "_printer" in normalized || "_pdl-datastream" in normalized -> "প্রিন্টার"
        "_ssh" in normalized || "_sftp-ssh" in normalized -> "কম্পিউটার/সার্ভার সেবা"
        "_smb" in normalized -> "ফাইল শেয়ারিং সেবা"
        "_http" in normalized || "_https" in normalized -> "ওয়েব সেবা"
        "_hap" in normalized -> "স্মার্ট-হোম ডিভাইস"
        "_rtsp" in normalized -> "মিডিয়া স্ট্রিমিং সেবা"
        else -> "অন্যান্য LAN সেবা"
    }
}

/**
 * Finds only services voluntarily advertised through local mDNS/DNS-SD.
 * It does not sweep IP ranges, authenticate, or connect to service endpoints.
 */
internal class LanServiceScanner(context: Context) {
    private val manager = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val handler = Handler(Looper.getMainLooper())
    private val found = ConcurrentHashMap<String, LanServiceObservation>()
    private val listeners = mutableListOf<Pair<String, NsdManager.DiscoveryListener>>()
    private var onUpdate: ((List<LanServiceObservation>) -> Unit)? = null
    private var onFinished: (() -> Unit)? = null
    private var pending = 0
    private var running = false

    private val serviceTypes = listOf(
        "_googlecast._tcp.", "_airplay._tcp.", "_raop._tcp.", "_ipp._tcp.",
        "_printer._tcp.", "_ssh._tcp.", "_smb._tcp.", "_http._tcp.",
        "_https._tcp.", "_hap._tcp.", "_rtsp._tcp."
    )

    fun start(onResults: (List<LanServiceObservation>) -> Unit, onComplete: () -> Unit) {
        if (running) return
        running = true
        found.clear()
        listeners.clear()
        onUpdate = onResults
        onFinished = onComplete
        pending = serviceTypes.size

        serviceTypes.forEach { type ->
            val listener = object : NsdManager.DiscoveryListener {
                override fun onDiscoveryStarted(serviceType: String) = Unit
                override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                    finishOne(type)
                }
                override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) = Unit
                override fun onDiscoveryStopped(serviceType: String) = Unit
                override fun onServiceLost(serviceInfo: NsdServiceInfo) {
                    val key = "${serviceInfo.serviceName}|${serviceInfo.serviceType}"
                    found.remove(key)
                    publish()
                }
                override fun onServiceFound(serviceInfo: NsdServiceInfo) {
                    if (serviceInfo.serviceType.substringBefore('.') != type.substringBefore('.')) return
                    @Suppress("DEPRECATION")
                    try {
                        manager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                            override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                                val key = "${info.serviceName}|${info.serviceType}"
                                found[key] = LanServiceObservation(
                                    info.serviceName, info.serviceType, null, info.port,
                                    classifyLanServiceType(info.serviceType)
                                )
                                publish()
                            }
                            override fun onServiceResolved(info: NsdServiceInfo) {
                                val key = "${info.serviceName}|${info.serviceType}"
                                found[key] = LanServiceObservation(
                                    info.serviceName, info.serviceType,
                                    info.host?.hostAddress, info.port,
                                    classifyLanServiceType(info.serviceType)
                                )
                                publish()
                            }
                        })
                    } catch (_: Exception) {
                        val key = "${serviceInfo.serviceName}|${serviceInfo.serviceType}"
                        found[key] = LanServiceObservation(
                            serviceInfo.serviceName, serviceInfo.serviceType, null,
                            serviceInfo.port, classifyLanServiceType(serviceInfo.serviceType)
                        )
                        publish()
                    }
                }
            }
            listeners.add(type to listener)
            try {
                manager.discoverServices(type, NsdManager.PROTOCOL_DNS_SD, listener)
            } catch (_: Exception) {
                finishOne(type)
            }
        }
        handler.postDelayed({ stop() }, 12_000L)
    }

    private fun publish() {
        val snapshot = found.values.sortedWith(compareBy({ it.categoryLabel }, { it.serviceName }))
        handler.post { onUpdate?.invoke(snapshot) }
    }

    private fun finishOne(type: String) {
        if (pending <= 0) return
        pending--
        if (pending == 0) stop()
    }

    fun stop() {
        if (!running) return
        running = false
        handler.removeCallbacksAndMessages(null)
        listeners.forEach { (_, listener) ->
            try { manager.stopServiceDiscovery(listener) } catch (_: Exception) {}
        }
        listeners.clear()
        onUpdate?.invoke(found.values.sortedBy { it.serviceName })
        onFinished?.invoke()
    }
}

@Composable
fun LanServiceDiscoveryPanel() {
    val context = LocalContext.current
    val observations = remember { mutableStateListOf<LanServiceObservation>() }
    var status by remember { mutableStateOf("একই Wi-Fi/LAN-এ নিজে থেকে সেবা ঘোষণা করা ডিভাইস খোঁজে।") }
    var scanning by remember { mutableStateOf(false) }
    val scanner = remember(context) { LanServiceScanner(context) }

    DisposableEffect(scanner) {
        onDispose { scanner.stop() }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LOCAL NETWORK DEVICE DISCOVERY", color = Color.White, fontSize = 16.sp)
            Text(
                "mDNS/DNS-SD-তে সম্প্রচারিত Chromecast, AirPlay, printer, SSH, SMB, web ও smart-home সেবা খোঁজে। কোনো IP range sweep বা login করা হয় না।",
                color = Color(0xFFB8C9C2), fontSize = 12.sp
            )
            Button(
                enabled = !scanning,
                onClick = {
                    observations.clear()
                    scanning = true
                    status = "১২ সেকেন্ড পর্যন্ত LAN service discovery চলছে…"
                    scanner.start(
                        onResults = { items ->
                            observations.clear()
                            observations.addAll(items)
                        },
                        onComplete = {
                            scanning = false
                            status = if (observations.isEmpty()) {
                                "কোনো mDNS service পাওয়া যায়নি। এর মানে LAN-এ ডিভাইস নেই—এমন নয়।"
                            } else {
                                "${observations.size}টি বিজ্ঞাপিত service endpoint পাওয়া গেছে। একই ডিভাইস একাধিক service দিতে পারে।"
                            }
                        }
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (scanning) "স্ক্যান চলছে…" else "LAN ডিভাইস/সেবা খুঁজুন") }
            if (scanning) {
                Button(onClick = { scanner.stop() }, modifier = Modifier.fillMaxWidth()) { Text("স্ক্যান বন্ধ") }
            }
            Text(status, color = Color(0xFFFFD18A), fontSize = 12.sp)
            observations.forEach { item ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("${item.categoryLabel}: ${item.serviceName}", color = Color.White, fontSize = 13.sp)
                    Text("${item.hostAddress ?: "IP পাওয়া যায়নি"}:${item.port} • ${item.serviceType}", color = Color(0xFF53E0A0), fontSize = 11.sp)
                }
            }
            Text(
                "সীমাবদ্ধতা: শুধু mDNS/DNS-SD service বিজ্ঞাপনকারী ডিভাইস ধরা পড়ে। বিজ্ঞাপন বন্ধ, client isolation, VLAN, multicast block বা offline ডিভাইস দেখা যাবে না। এটি পুরো নেটওয়ার্কের ডিভাইস তালিকা নয়; service endpoint-ও ডিভাইসের পরিচয়/মডেল নিশ্চিত করে না।",
                color = Color(0xFFB8C9C2), fontSize = 11.sp
            )
        }
    }
}
