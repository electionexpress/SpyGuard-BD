package com.electionexpress.spyguardbd

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import kotlinx.coroutines.delay

private val Bg = Color(0xFF07110F)
private val Panel = Color(0xFF12201C)
private val Mint = Color(0xFF53E0A0)
private val Muted = Color(0xFFB8C9C2)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpyGuardApp() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SpyGuardApp() {
    val context = LocalContext.current
    val wifi = remember { context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager }
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager.adapter }

    var permissionState by remember { mutableStateOf("অনুমতি প্রয়োজন হলে স্ক্যানের সময় চাওয়া হবে") }
    var wifiState by remember { mutableStateOf("এখনো স্ক্যান করা হয়নি") }
    var wifiNetworks by remember { mutableStateOf<List<String>>(emptyList()) }
    var bleState by remember { mutableStateOf("এখনো স্ক্যান করা হয়নি") }
    val bleDevices = remember { mutableStateMapOf<String, String>() }
    var isBleScanning by remember { mutableStateOf(false) }
    var showIrGuide by remember { mutableStateOf(false) }

    val requiredPermissions = remember {
        buildList {
            if (Build.VERSION.SDK_INT >= 31) {
                add(Manifest.permission.BLUETOOTH_SCAN)
                add(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                add(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            add(Manifest.permission.ACCESS_FINE_LOCATION)
        }.distinct().toTypedArray()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        val denied = grants.filterValues { !it }.keys
        permissionState = if (denied.isEmpty()) "প্রয়োজনীয় অনুমতি দেওয়া হয়েছে" else "কিছু অনুমতি দেওয়া হয়নি: ${denied.joinToString()}"
    }

    fun hasPermission(permission: String): Boolean =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    val wifiReceiver = remember {
        object : BroadcastReceiver() {
            @SuppressLint("MissingPermission")
            override fun onReceive(ctx: Context?, intent: Intent?) {
                    try {
                        wifiNetworks = wifi.scanResults
                            .sortedByDescending { it.level }
                            .take(30)
                            .map { result ->
                                val name = result.SSID.ifBlank { "নামহীন নেটওয়ার্ক" }
                                "$name • ${result.frequency} MHz • ${result.level} dBm • BSSID ${result.BSSID}"
                            }
                        wifiState = if (wifiNetworks.isEmpty()) "কোনো ফল পাওয়া যায়নি; Location চালু আছে কি না দেখুন" else "${wifiNetworks.size}টি দৃশ্যমান Wi-Fi নেটওয়ার্ক পাওয়া গেছে"
                    } catch (_: SecurityException) {
                        wifiState = "অনুমতি নেই — Location permission দিন"
                    }
            }
        }
    }

    DisposableEffect(Unit) {
        val filter = IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        if (Build.VERSION.SDK_INT >= 33) {
            context.registerReceiver(wifiReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            @Suppress("DEPRECATION")
            context.registerReceiver(wifiReceiver, filter)
        }
        onDispose {
            try { context.unregisterReceiver(wifiReceiver) } catch (_: Exception) {}
        }
    }

    val bleCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val address = result.device.address ?: "Unknown"
                val label = try {
                    result.device.name ?: result.scanRecord?.deviceName ?: "নামহীন Bluetooth ডিভাইস"
                } catch (_: SecurityException) { "নাম-অনুমতি নেই" }
                bleDevices[address] = "$label • ${result.rssi} dBm"
                bleState = "${bleDevices.size}টি Bluetooth বিজ্ঞাপন পাওয়া গেছে"
            }
            override fun onScanFailed(errorCode: Int) {
                isBleScanning = false
                bleState = "Bluetooth স্ক্যান ব্যর্থ (কোড $errorCode)"
            }
        }
    }

    LaunchedEffect(isBleScanning) {
        if (isBleScanning) {
            delay(15_000)
            try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleCallback) } catch (_: SecurityException) {}
            isBleScanning = false
            bleState = "১৫ সেকেন্ডের স্ক্যান সম্পন্ন — ${bleDevices.size}টি ডিভাইস পাওয়া গেছে"
        }
    }

    DisposableEffect(isBleScanning) {
        onDispose {
            if (isBleScanning) {
                try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleCallback) } catch (_: Exception) {}
            }
        }
    }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel)) {
        Scaffold(containerColor = Bg, topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SPYGUARD BD", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("নিকটবর্তী ইলেকট্রনিক ডিভাইস শনাক্তকরণ", color = Muted, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }) { padding ->
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(22.dp)) {
                        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, null, tint = Mint)
                                Text("  নিরাপত্তা ড্যাশবোর্ড", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                            }
                            Text("ডিভাইস: ${Build.MANUFACTURER} ${Build.MODEL}", color = Muted)
                            Text("Android ${Build.VERSION.RELEASE} • SpyGuard BD 0.7.0", color = Muted)
                            Text(permissionState, color = Muted, fontSize = 12.sp)
                            OutlinedButton(onClick = { permissionLauncher.launch(requiredPermissions) }) {
                                Text("প্রয়োজনীয় অনুমতি যাচাই")
                            }
                        }
                    }
                }
                item { CameraInspectionPanel() }
                item { CameraEvidenceAssessmentPanel() }
                item { CameraNetworkDiscoveryPanel() }
                item { LanServiceDiscoveryPanel() }
                item {
                    SectionCard(
                        title = "Wi-Fi নেটওয়ার্ক স্ক্যান",
                        subtitle = "আশপাশে সম্প্রচারিত Wi-Fi নেটওয়ার্কের নাম, ফ্রিকোয়েন্সি ও সিগন্যাল শক্তি দেখুন। এগুলো ক্যামেরা বলে প্রমাণ করে না।",
                        icon = { Icon(Icons.Default.Wifi, null, tint = Mint) }
                    ) {
                        Button(onClick = {
                            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            } else {
                                try {
                                    wifiState = if (wifi.startScan()) "স্ক্যান চলছে…" else "Android স্ক্যান সীমিত করেছে; কিছুক্ষণ পর আবার চেষ্টা করুন"
                                } catch (_: SecurityException) {
                                    wifiState = "অনুমতি নেই"
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Wi-Fi স্ক্যান শুরু") }
                        Text(wifiState, color = Muted, fontSize = 13.sp)
                        wifiNetworks.forEach { Text("• $it", color = Color.White, fontSize = 12.sp) }
                    }
                }
                item {
                    SectionCard(
                        title = "Bluetooth Low Energy স্ক্যান",
                        subtitle = "নিকটবর্তী BLE বিজ্ঞাপনকারী ডিভাইস দেখা যায়। নিষ্ক্রিয়, লুকানো বা BLE ব্যবহার না করা ডিভাইস দেখা যাবে না।",
                        icon = { Icon(Icons.Default.Bluetooth, null, tint = Mint) }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                enabled = !isBleScanning,
                                onClick = {
                                    val scanPermission = if (Build.VERSION.SDK_INT >= 31) Manifest.permission.BLUETOOTH_SCAN else Manifest.permission.ACCESS_FINE_LOCATION
                                    if (!hasPermission(scanPermission)) {
                                        permissionLauncher.launch(arrayOf(scanPermission))
                                    } else if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled) {
                                        bleState = "Bluetooth চালু করুন"
                                    } else {
                                        try {
                                            val scanner = bluetoothAdapter.bluetoothLeScanner
                                            if (scanner == null) {
                                                bleState = "এই ডিভাইসে Bluetooth LE scanner পাওয়া যায়নি"
                                            } else {
                                                bleDevices.clear()
                                                scanner.startScan(bleCallback)
                                                isBleScanning = true
                                                bleState = "স্ক্যান চলছে — ১৫ সেকেন্ড পরে স্বয়ংক্রিয়ভাবে বন্ধ হবে"
                                            }
                                        } catch (_: SecurityException) {
                                            bleState = "Bluetooth স্ক্যান অনুমতি নেই"
                                        } catch (_: Exception) {
                                            bleState = "এই ডিভাইসে BLE স্ক্যান শুরু করা যায়নি"
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("স্ক্যান শুরু") }
                            OutlinedButton(
                                enabled = isBleScanning,
                                onClick = {
                                    try { bluetoothAdapter?.bluetoothLeScanner?.stopScan(bleCallback) } catch (_: Exception) {}
                                    isBleScanning = false
                                    bleState = "স্ক্যান বন্ধ"
                                }
                            ) { Text("বন্ধ") }
                        }
                        Text(bleState, color = Muted, fontSize = 13.sp)
                        val categoryCounts = deviceCategorySummary(bleDevices.values.map { it.substringBefore(" • ") })
                        if (bleDevices.isNotEmpty()) {
                            Text("নামভিত্তিক সম্ভাব্য শ্রেণি: " + categoryCounts.entries.joinToString(" • ") { "${it.key.labelBn}: ${it.value}" }, color = Mint, fontSize = 12.sp)
                        }
                        bleDevices.entries.take(30).forEach { (address, detail) ->
                            val classification = classifyNearbyDevice(detail.substringBefore(" • "))
                            Text("• ${classification.category.labelBn} — $detail\n  $address\n  ${classification.confidenceLabel}: ${classification.explanation}", color = Color.White, fontSize = 12.sp)
                        }
                    }
                }
                item { NearbyDeviceInventoryPanel(bleDevices = bleDevices.toMap(), wifiNetworks = wifiNetworks) }
                item {
                    SectionCard(
                        title = "আশপাশের Android ফোনের Camera Activity",
                        subtitle = "অন্য ফোনে ক্যামেরা চালু হলেই সাধারণ SpyGuard অ্যাপ সরাসরি জানতে পারে না। Android এই তথ্য তৃতীয় পক্ষের অ্যাপকে দূর থেকে দেয় না।",
                        icon = { Icon(Icons.Default.Radar, null, tint = Mint) }
                    ) {
                        Text(
                            "বর্তমান Wi-Fi/Bluetooth স্ক্যান শুধু ডিভাইস বা বিজ্ঞাপন শনাক্ত করে—ক্যামেরা চালু আছে তা নয়। ভবিষ্যতে সম্মতিসহ Companion Mode যোগ করা যাবে: পর্যবেক্ষণাধীন Android ফোনে SpyGuard Companion ইনস্টল থাকবে; সেই ফোন নিজস্ব CameraManager status থেকে ‘camera busy’ সিগন্যাল দেবে। এটি recording-এর প্রমাণ নয় এবং BLE/Wi-Fi বন্ধ থাকলে সংকেত নাও আসতে পারে।",
                            color = Muted, fontSize = 12.sp
                        )
                        Text(
                            "এখনকার সংস্করণে এই দূরবর্তী Camera Activity alert চালু নয়। ভুল সতর্কতা এড়াতে কোনো অজানা ফোনকে ক্যামেরা চালু বলে চিহ্নিত করা হবে না।",
                            color = Color(0xFFFFD18A), fontSize = 12.sp, fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                item {
                    SectionCard(
                        title = "ইনফ্রারেড আলো পরীক্ষা",
                        subtitle = "ঘরের আলো কমিয়ে সন্দেহজনক লেন্স/LED-এ নজর দিন। ফোনের ক্যামেরা কিছু IR আলো দেখাতে পারে, আবার IR-cut filter-এর কারণে নাও পারে।",
                        icon = { Icon(Icons.Default.CameraAlt, null, tint = Mint) }
                    ) {
                        OutlinedButton(onClick = { showIrGuide = !showIrGuide }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (showIrGuide) "নির্দেশনা বন্ধ করুন" else "পরীক্ষার নির্দেশনা দেখুন")
                        }
                        if (showIrGuide) {
                            Text("১. ঘরের আলো কমান। ২. সন্দেহজনক বস্তু/ডিটেক্টরের দিকে ফোনের ক্যামেরা তাক করুন। ৩. ছোট ঝলমলে বিন্দু দেখলে সেটি অন্য আলোর উৎসও হতে পারে—নিশ্চিত ক্যামেরা নয়। ৪. সামনে ও পেছনের ক্যামেরা আলাদাভাবে পরীক্ষা করুন।", color = Color.White, fontSize = 13.sp)
                        }
                    }
                }
                item {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF19251F)), shape = RoundedCornerShape(18.dp)) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                            Icon(Icons.Default.Info, tint = Mint, contentDescription = null)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                "গুরুত্বপূর্ণ সীমাবদ্ধতা: ফোনের Wi-Fi/Bluetooth স্ক্যান কেবল দৃশ্যমান রেডিও ডিভাইস দেখায়। অফলাইন ক্যামেরা, SD-card রেকর্ডার, তারযুক্ত মাইক্রোফোন বা সব RF ট্রান্সমিটার শনাক্ত করতে পারে না। এই অ্যাপ নিরাপত্তা সহায়ক, নিশ্চয়তা নয়।",
                                color = Muted, fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionCard(
    title: String,
    subtitle: String,
    icon: @Composable () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(colors = CardDefaults.cardColors(containerColor = Panel), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                icon()
                Spacer(Modifier.width(10.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Text(subtitle, color = Muted, fontSize = 12.sp)
            content()
        }
    }
}
