package com.electionexpress.spyguardbd

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.util.UUID
import kotlin.concurrent.thread

internal data class OnvifDiscoveryResult(
    val sourceAddress: String,
    val serviceAddresses: List<String>
)

/** Extracts advertised ONVIF XAddrs without trusting or executing device-provided XML. */
internal fun parseOnvifDiscoveryResponse(xml: String, sourceAddress: String): OnvifDiscoveryResult {
    val match = Regex("(?is)<[^>]*XAddrs[^>]*>(.*?)</[^>]*XAddrs>").find(xml)
    val addresses = match?.groupValues?.getOrNull(1)
        ?.replace("&amp;", "&")
        ?.trim()
        ?.split(Regex("\\s+"))
        ?.filter { it.startsWith("http://") || it.startsWith("https://") }
        ?.distinct()
        .orEmpty()
    return OnvifDiscoveryResult(sourceAddress, addresses)
}

/**
 * Sends one standards-based ONVIF WS-Discovery Probe on the current Wi-Fi LAN.
 * It does not scan IP ranges, authenticate, open video streams, or inspect other networks.
 */
internal fun discoverOnvifDevices(context: Context): List<OnvifDiscoveryResult> {
    val wifi = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    val lock = wifi.createMulticastLock("SpyGuardBD-OnvifDiscovery").apply {
        setReferenceCounted(false)
        acquire()
    }
    val found = linkedMapOf<String, OnvifDiscoveryResult>()
    val group = InetAddress.getByName("239.255.255.250")
    val port = 3702
    val probe = """<?xml version="1.0" encoding="UTF-8"?>
<e:Envelope xmlns:e="http://www.w3.org/2003/05/soap-envelope"
 xmlns:w="http://schemas.xmlsoap.org/ws/2004/08/addressing"
 xmlns:d="http://schemas.xmlsoap.org/ws/2005/04/discovery"
 xmlns:dn="http://www.onvif.org/ver10/network/wsdl">
 <e:Header>
  <w:MessageID>uuid:${UUID.randomUUID()}</w:MessageID>
  <w:To>urn:schemas-xmlsoap-org:ws:2005:04:discovery</w:To>
  <w:Action>http://schemas.xmlsoap.org/ws/2005/04/discovery/Probe</w:Action>
 </e:Header>
 <e:Body><d:Probe><d:Types>dn:NetworkVideoTransmitter</d:Types></d:Probe></e:Body>
</e:Envelope>""".trimIndent().toByteArray(Charsets.UTF_8)

    try {
        MulticastSocket(null).use { socket ->
            socket.reuseAddress = true
            socket.bind(InetSocketAddress(0))
            socket.soTimeout = 500
            socket.joinGroup(group)
            val packet = DatagramPacket(probe, probe.size, group, port)
            socket.send(packet)

            val deadline = System.currentTimeMillis() + 4500L
            val buffer = ByteArray(8192)
            while (System.currentTimeMillis() < deadline) {
                try {
                    val response = DatagramPacket(buffer, buffer.size)
                    socket.receive(response)
                    val body = String(response.data, response.offset, response.length, Charsets.UTF_8)
                    val item = parseOnvifDiscoveryResponse(body, response.address.hostAddress ?: "Unknown")
                    val key = "${item.sourceAddress}|${item.serviceAddresses.joinToString()}"
                    found[key] = item
                } catch (_: java.net.SocketTimeoutException) {
                    // Continue until the bounded discovery window expires.
                }
            }
            socket.leaveGroup(group)
        }
    } finally {
        if (lock.isHeld) lock.release()
    }
    return found.values.toList()
}

@Composable
fun CameraNetworkDiscoveryPanel() {
    val context = LocalContext.current
    var scanning by remember { mutableStateOf(false) }
    var status by remember { mutableStateOf("শুধু একই Wi-Fi/LAN-এ ONVIF সাড়া দেওয়া ডিভাইস খোঁজা হবে।") }
    val results = remember { mutableStateListOf<OnvifDiscoveryResult>() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C))
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("LOCAL NETWORK CAMERA DISCOVERY", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                "আপনার বর্তমান Wi-Fi নেটওয়ার্কে ONVIF WS-Discovery সমর্থিত ক্যামেরা/ভিডিও ডিভাইসের ঘোষণা খোঁজে। স্ক্যানটি ব্যবহারকারী নিজে শুরু করবেন এবং প্রায় ৫ সেকেন্ডে শেষ হবে।",
                color = Color(0xFFB8C9C2), fontSize = 12.sp
            )
            Button(
                enabled = !scanning,
                onClick = {
                    scanning = true
                    results.clear()
                    status = "ONVIF multicast probe পাঠানো হচ্ছে…"
                    thread(name = "spyguard-onvif-discovery") {
                        val outcome = try {
                            Pair(discoverOnvifDevices(context.applicationContext), null as String?)
                        } catch (error: Exception) {
                            Pair(emptyList(), "স্ক্যান ব্যর্থ: ${error.localizedMessage ?: "নেটওয়ার্ক ত্রুটি"}")
                        }
                        android.os.Handler(android.os.Looper.getMainLooper()).post {
                            results.clear()
                            results.addAll(outcome.first)
                            status = outcome.second ?: if (outcome.first.isEmpty()) {
                                "এই স্ক্যানে কোনো ONVIF উত্তর পাওয়া যায়নি। এটি ক্যামেরা নেই—এমন প্রমাণ নয়।"
                            } else {
                                "${outcome.first.size}টি ONVIF response পাওয়া গেছে; প্রতিটি ডিভাইস আলাদাভাবে যাচাই করুন।"
                            }
                            scanning = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (scanning) "স্ক্যান চলছে…" else "এই Wi-Fi নেটওয়ার্ক স্ক্যান করুন") }
            Text(status, color = Color(0xFFFFD18A), fontSize = 12.sp)
            results.forEach { result ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("ডিভাইস IP: ${result.sourceAddress}", color = Color.White, fontSize = 13.sp)
                    if (result.serviceAddresses.isEmpty()) {
                        Text("ONVIF response পাওয়া গেছে; XAddr প্রকাশ করা হয়নি।", color = Color(0xFFB8C9C2), fontSize = 11.sp)
                    } else {
                        result.serviceAddresses.forEach { address ->
                            Text(address, color = Color(0xFF53E0A0), fontSize = 11.sp)
                        }
                    }
                }
            }
            Text(
                "সীমাবদ্ধতা: ONVIF বন্ধ/অসমর্থিত, wired-only, offline, আলাদা VLAN, router client-isolation বা multicast-blocked ডিভাইস ধরা নাও পড়তে পারে। একটি ONVIF response নিজে থেকে গোপন ক্যামেরা বা recording প্রমাণ করে না; এখানে কোনো stream খোলা বা login করা হয় না।",
                color = Color(0xFFB8C9C2), fontSize = 11.sp
            )
        }
    }
}
