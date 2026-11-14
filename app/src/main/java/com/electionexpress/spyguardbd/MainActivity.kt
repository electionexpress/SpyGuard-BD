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
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.net.Uri
import android.content.ActivityNotFoundException
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Home
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
import kotlinx.coroutines.launch

private val Bg = Color(0xFF07151A)
private val Panel = Color(0xFF10242B)
private val Mint = Color(0xFF55E6C1)
private val Muted = Color(0xFFAAC2C5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpyGuardApp() }
    }
}

@Suppress("DEPRECATION")
private fun isLocationServiceEnabled(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            manager.isLocationEnabled
        } else {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.LOCATION_MODE,
                Settings.Secure.LOCATION_MODE_OFF
            ) != Settings.Secure.LOCATION_MODE_OFF
        }
    } catch (_: Exception) {
        false
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
    val bleDevices = remember { mutableStateMapOf<String, BleObservation>() }
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
                        val resultsUpdated = intent?.getBooleanExtra(
                            WifiManager.EXTRA_RESULTS_UPDATED,
                            false
                        ) == true
                        if (resultsUpdated) {
                            wifiNetworks = deduplicateWifiObservations(
                                wifi.scanResults.map { result ->
                                    WifiObservation(
                                        ssid = result.SSID.ifBlank { "নামহীন নেটওয়ার্ক" },
                                        bssid = result.BSSID.orEmpty(),
                                        frequencyMhz = result.frequency,
                                        rssiDbm = result.level
                                    )
                                }
                            ).map { observation ->
                                "${observation.ssid} • ${observation.frequencyMhz} MHz • ${observation.rssiDbm} dBm • স্ক্যান সময় ${formatBleLastSeen(System.currentTimeMillis())}"
                            }
                        }
                        wifiState = wifiScanStatus(resultsUpdated, wifiNetworks.size)
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
                try {
                    val address = result.device.address ?: "Unknown"
                    val label = result.device.name ?: result.scanRecord?.deviceName ?: "নামহীন Bluetooth ডিভাইস"
                    bleDevices[address] = BleObservation(address, label, result.rssi, System.currentTimeMillis())
                    bleState = "${bleDevices.size}টি Bluetooth বিজ্ঞাপন পাওয়া গেছে"
                } catch (_: SecurityException) {
                    isBleScanning = false
                    bleState = "Bluetooth ডিভাইসের তথ্য পড়ার অনুমতি নেই — Nearby devices অনুমতি যাচাই করুন"
                }
            }
            override fun onScanFailed(errorCode: Int) {
                isBleScanning = false
                bleState = bleScanFailureMessage(errorCode)
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

    // Request only the permissions needed for nearby-device scanning on first launch.
    LaunchedEffect(Unit) {
        permissionLauncher.launch(requiredPermissions)
    }

    // Once the user grants access, start a bounded Wi-Fi + BLE scan automatically.
    LaunchedEffect(permissionState) {
        if (permissionState == "প্রয়োজনীয় অনুমতি দেওয়া হয়েছে") {
            if (hasPermission(Manifest.permission.ACCESS_FINE_LOCATION) &&
                wifi.isWifiEnabled && isLocationServiceEnabled(context)
            ) {
                try {
                    wifi.startScan()
                    wifiState = "স্বয়ংক্রিয় Wi-Fi স্ক্যান চলছে"
                } catch (_: SecurityException) {
                    wifiState = "Wi-Fi স্ক্যানের অনুমতি নেই"
                } catch (_: Exception) {
                    wifiState = "Wi-Fi স্ক্যান শুরু করা যায়নি"
                }
            } else {
                wifiState = "Wi-Fi/Location অনুপলব্ধ — এই অংশ পরীক্ষা করা যায়নি"
            }

            val scanPermissionsGranted = if (Build.VERSION.SDK_INT >= 31) {
                hasPermission(Manifest.permission.BLUETOOTH_SCAN) &&
                    hasPermission(Manifest.permission.BLUETOOTH_CONNECT)
            } else {
                hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)
            }
            if (scanPermissionsGranted && bluetoothAdapter?.isEnabled == true) {
                try {
                    val scanner = bluetoothAdapter.bluetoothLeScanner
                    if (scanner != null && !isBleScanning) {
                        bleDevices.clear()
                        scanner.startScan(bleCallback)
                        isBleScanning = true
                        bleState = "স্বয়ংক্রিয় BLE স্ক্যান চলছে — ১৫ সেকেন্ডের মধ্যে বন্ধ হবে"
                    } else if (scanner == null) {
                        bleState = "এই ফোনে BLE scanner নেই"
                    }
                } catch (_: SecurityException) {
                    bleState = "Bluetooth স্ক্যানের অনুমতি নেই"
                } catch (_: Exception) {
                    bleState = "Bluetooth স্ক্যান শুরু করা যায়নি"
                }
            } else {
                bleState = "Bluetooth বন্ধ/অনুপলব্ধ বা অনুমতি নেই"
            }
        }
    }

    val dashboardListState = rememberLazyListState()
    val dashboardScope = rememberCoroutineScope()
    var selectedDestination by remember { mutableIntStateOf(0) }
    var opticalInspection by remember { mutableStateOf(OpticalInspection()) }
    var opticalInspectionCompleted by remember { mutableStateOf(false) }

    MaterialTheme(colorScheme = darkColorScheme(primary = Mint, background = Bg, surface = Panel)) {
        Scaffold(containerColor = Bg, topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("SPYGUARD BD", fontWeight = FontWeight.Bold, color = Color.White)
                        Text("ব্যক্তিগত নিরাপত্তা স্ক্যানার", color = Muted, fontSize = 13.sp)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Bg)
            )
        }, bottomBar = {
            NavigationBar(containerColor = Color(0xFF0B1D23), contentColor = Mint) {
                val destinations = listOf(
                    Triple("হোম", Icons.Default.Home, 0),
                    Triple("ক্যামেরা", Icons.Default.CameraAlt, 2),
                    Triple("ডিভাইস", Icons.Default.Radar, 4),
                    Triple("প্রাইভেসি", Icons.Default.Info, 6)
                )
                destinations.forEachIndexed { index, destination ->
                    NavigationBarItem(
                        selected = selectedDestination == index,
                        onClick = {
                            selectedDestination = index
                            dashboardScope.launch { dashboardListState.animateScrollToItem(destination.third) }
                        },
                        icon = { Icon(destination.second, contentDescription = destination.first) },
                        label = { Text(destination.first, fontSize = 10.sp) },
                        alwaysShowLabel = true,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Mint,
                            selectedTextColor = Mint,
                            indicatorColor = Mint.copy(alpha = 0.13f),
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }) { padding ->
            LazyColumn(
                state = dashboardListState,
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF102B2B)),
                        shape = RoundedCornerShape(26.dp)
                    ) {
                        Column(
                            Modifier.fillMaxWidth().padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = Mint.copy(alpha = 0.14f)
                                ) {
                                    Icon(
                                        Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Mint,
                                        modifier = Modifier.padding(11.dp).size(26.dp)
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("SPYGUARD BD", color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 19.sp)
                                    Text("স্মার্ট স্পেস ইনস্পেকশন", color = Muted, fontSize = 12.sp)
                                }
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = Color(0xFF1C493C)
                                ) {
                                    Text("প্রস্তুত", color = Mint, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp))
                                }
                            }
                            Text("আপনার আশপাশ পরীক্ষা করুন", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(
                                "ক্যামেরা পরিদর্শন ও দৃশ্যমান বেতার ডিভাইসের পর্যবেক্ষণ এক জায়গায়।",
                                color = Muted, fontSize = 13.sp
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                DashboardMetric("Wi-Fi", wifiNetworks.size.toString(), Modifier.weight(1f))
                                DashboardMetric("Bluetooth", bleDevices.size.toString(), Modifier.weight(1f))
                                DashboardMetric("পরিদর্শন", "লাইভ", Modifier.weight(1f))
                            }
                            HorizontalDivider(color = Color.White.copy(alpha = 0.10f))
                            Text("ডিভাইস: ${Build.MANUFACTURER} ${Build.MODEL} • Android ${Build.VERSION.RELEASE}",
                                color = Muted, fontSize = 11.sp)
                            Text(permissionState, color = Muted, fontSize = 12.sp)
                            OutlinedButton(
                                onClick = { permissionLauncher.launch(requiredPermissions) },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("অনুমতি ও অ্যাক্সেস যাচাই")
                            }
                        }
                    }
                }
                item {
                    AutoScanSummaryCard(
                        wifiState = wifiState,
                        wifiCount = wifiNetworks.size,
                        bluetoothState = bleState,
                        bluetoothCount = bleDevices.size,
                        opticalInspection = opticalInspection,
                        opticalInspectionCompleted = opticalInspectionCompleted
                    )
                }
                item { CameraInspectionScreen(
                    nearbyWifiNetworks = wifiNetworks,
                    nearbyBluetoothDevices = bleDevices.values.map { observation ->
                        "${observation.displayName} • ${observation.rssiDbm} dBm • ${bleFreshnessLabel(classifyBleFreshness(observation.lastSeenAtMillis, System.currentTimeMillis()))} • ${formatBleLastSeen(observation.lastSeenAtMillis)}"
                    },
                    observations = opticalInspection,
                    onObservationsChange = { opticalInspection = it },
                    inspectionCompleted = opticalInspectionCompleted,
                    onInspectionCompletedChange = { opticalInspectionCompleted = it }
                ) }
                item { SensorDiagnosticsScreen() }
                item {
                    SectionCard(
                        title = "Wi-Fi নেটওয়ার্ক স্ক্যান",
                        subtitle = "আশপাশে সম্প্রচারিত Wi-Fi নেটওয়ার্কের নাম, ফ্রিকোয়েন্সি ও সিগন্যাল শক্তি দেখুন। এগুলো ক্যামেরা বলে প্রমাণ করে না।",
                        icon = { Icon(Icons.Default.Wifi, null, tint = Mint) }
                    ) {
                        Button(onClick = {
                            if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) {
                                permissionLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION))
                            } else if (!wifi.isWifiEnabled) {
                                wifiState = "Wi-Fi বন্ধ — ফোনের Wi-Fi চালু করে আবার চেষ্টা করুন"
                            } else if (!isLocationServiceEnabled(context)) {
                                wifiState = "Android-এর Location বন্ধ — Location চালু করে আবার চেষ্টা করুন"
                            } else {
                                try {
                                    val accepted = wifi.startScan()
                                    wifiState = wifiScanStartStatus(
                                        hasLocationPermission = true,
                                        wifiEnabled = true,
                                        locationEnabled = true,
                                        requestAccepted = accepted
                                    )
                                } catch (_: SecurityException) {
                                    wifiState = "অনুমতি প্রত্যাখ্যাত — অ্যাপের Location permission যাচাই করুন"
                                } catch (_: Exception) {
                                    wifiState = "Android Wi-Fi scan শুরু করতে পারেনি — কিছুক্ষণ পরে আবার চেষ্টা করুন"
                                }
                            }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Wi-Fi স্ক্যান শুরু") }
                        Text(wifiState, color = Muted, fontSize = 13.sp)
                        wifiNetworks.forEachIndexed { index, network ->
                            ObservationRow(
                                title = network.substringBefore(" • "),
                                detail = network.substringAfter(" • ", "নেটওয়ার্ক তথ্য"),
                                marker = "Wi-Fi ${index + 1}"
                            )
                        }
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
                                    val scanPermissions = if (Build.VERSION.SDK_INT >= 31) {
                                        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
                                    } else {
                                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
                                    }
                                    val missingScanPermissions = scanPermissions.filterNot(::hasPermission)
                                    if (missingScanPermissions.isNotEmpty()) {
                                        permissionLauncher.launch(missingScanPermissions.toTypedArray())
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
                        bleDevices.values.sortedByDescending { it.rssiDbm }.take(30).forEach { observation ->
                            ObservationRow(
                                title = observation.displayName,
                                detail = "${observation.rssiDbm} dBm • সর্বশেষ দেখা: ${formatBleLastSeen(observation.lastSeenAtMillis)}",
                                marker = bleFreshnessLabel(classifyBleFreshness(observation.lastSeenAtMillis, System.currentTimeMillis())),
                                footnote = "ঠিকানা: ${maskBluetoothAddress(observation.address)} • নাম/সিগনাল মডেল বা ক্যামেরার পরিচয় নিশ্চিত করে না"
                            )
                        }
                    }
                }
                item {
                    SectionCard(
                        title = "Android Privacy Dashboard",
                        subtitle = "Android-এর সমর্থিত সংস্করণে সাম্প্রতিক Camera ও Microphone অ্যাক্সেসের ইতিহাস নিজে যাচাই করুন। এটি SpyGuard-এর লাইভ শনাক্তকরণ নয়।",
                        icon = { Icon(Icons.Default.Security, null, tint = Mint) }
                    ) {
                        Text(
                            "Privacy Dashboard-এ কোন অ্যাপ কখন ক্যামেরা বা মাইক্রোফোন ব্যবহার করেছে তা দেখা যেতে পারে। Android সংস্করণ ও নির্মাতাভেদে মেনুর নাম/সুবিধা আলাদা হতে পারে।",
                            color = Muted,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                val privacyIntent = Intent(Settings.ACTION_PRIVACY_SETTINGS)
                                try {
                                    context.startActivity(privacyIntent)
                                } catch (_: ActivityNotFoundException) {
                                    val appIntent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    try {
                                        context.startActivity(appIntent)
                                    } catch (_: Exception) {
                                    }
                                } catch (_: Exception) {
                                    val appIntent = Intent(
                                        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    try {
                                        context.startActivity(appIntent)
                                    } catch (_: Exception) {
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Privacy Settings খুলুন") }
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
private fun ObservationRow(
    title: String,
    detail: String,
    marker: String,
    footnote: String? = null
) {
    Surface(
        color = Color.White.copy(alpha = 0.045f),
        shape = RoundedCornerShape(13.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    Text(detail, color = Muted, fontSize = 11.sp)
                }
                Surface(color = Mint.copy(alpha = 0.12f), shape = RoundedCornerShape(50)) {
                    Text(marker, color = Mint, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp))
                }
            }
            if (!footnote.isNullOrBlank()) Text(footnote, color = Muted, fontSize = 10.sp)
        }
    }
}

@Composable
private fun DashboardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = Color.White.copy(alpha = 0.055f),
        shape = RoundedCornerShape(15.dp)
    ) {
        Column(
            Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Text(label, color = Muted, fontSize = 10.sp, maxLines = 1)
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
