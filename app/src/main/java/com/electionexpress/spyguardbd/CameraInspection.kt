package com.electionexpress.spyguardbd

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val CameraPanel = Color(0xFF12201C)
private val CameraMint = Color(0xFF53E0A0)
private val CameraMuted = Color(0xFFB8C9C2)

/** User-controlled optical inspection preview; it does not claim to identify cameras. */
@Composable
fun CameraInspectionScreen(
    nearbyWifiNetworks: List<String> = emptyList(),
    nearbyBluetoothDevices: List<String> = emptyList(),
    observations: OpticalInspection,
    onObservationsChange: (OpticalInspection) -> Unit,
    inspectionCompleted: Boolean,
    onInspectionCompletedChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var provider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var previewUseCase by remember { mutableStateOf<Preview?>(null) }
    var cameraStatus by remember { mutableStateOf("ক্যামেরা ব্যবহারের অনুমতি প্রয়োজন") }
    var torchEnabled by remember { mutableStateOf(false) }
    var zoomRatio by remember { mutableStateOf(1f) }
    var roomLabel by remember { mutableStateOf("") }
    var reportNotes by remember { mutableStateOf("") }
    var pendingReportText by remember { mutableStateOf("") }
    var reportStatus by remember { mutableStateOf("") }
    val reportStore = remember(context) { InspectionReportStore(context.filesDir) }
    var savedReports by remember { mutableStateOf(reportStore.list()) }

    val saveReportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        if (uri == null) {
            reportStatus = "রিপোর্ট সংরক্ষণ বাতিল করা হয়েছে"
        } else {
            try {
                val output = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("ফাইল খোলা যায়নি")
                output.use { it.write(pendingReportText.toByteArray(Charsets.UTF_8)) }
                reportStatus = "রিপোর্ট ফাইল সংরক্ষিত হয়েছে"
            } catch (error: Exception) {
                reportStatus = "রিপোর্ট সংরক্ষণ করা যায়নি: ${error.localizedMessage ?: "অজানা সমস্যা"}"
            }
        }
    }
    var screenActive by remember { mutableStateOf(true) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        permissionGranted = granted
        cameraStatus = if (granted) "ক্যামেরা প্রস্তুত করা হচ্ছে…" else "ক্যামেরা অনুমতি দেওয়া হয়নি"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CameraPanel),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("লাইভ ক্যামেরা পরিদর্শন", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "সন্দেহজনক বস্তু, ছোট লেন্সের মতো উপাদান এবং অস্বাভাবিক আলোর প্রতিফলন নিজে পর্যবেক্ষণ করুন। প্রতিফলন একা ক্যামেরার প্রমাণ নয়।",
                color = CameraMuted,
                style = MaterialTheme.typography.bodySmall
            )

            if (permissionGranted) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth().height(250.dp),
                    factory = { viewContext ->
                        PreviewView(viewContext).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            val previewView = this
                            val providerFuture = ProcessCameraProvider.getInstance(viewContext)
                            providerFuture.addListener({
                                try {
                                    if (!screenActive || ContextCompat.checkSelfPermission(viewContext, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return@addListener
                                    val cameraProvider = providerFuture.get()
                                    provider = cameraProvider
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    previewUseCase?.let { previousPreview -> cameraProvider.unbind(previousPreview) }
                                    previewUseCase = preview
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
                                    zoomRatio = camera?.cameraInfo?.zoomState?.value?.zoomRatio ?: 1f
                                    cameraStatus = "লাইভ প্রিভিউ চালু"
                                } catch (error: Exception) {
                                    cameraStatus = "ক্যামেরা চালু করা যায়নি: ${error.localizedMessage ?: "অজানা সমস্যা"}"
                                }
                            }, ContextCompat.getMainExecutor(viewContext))
                        }
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val activeCamera = camera
                            if (activeCamera?.cameraInfo?.hasFlashUnit() == true) {
                                torchEnabled = !torchEnabled
                                activeCamera.cameraControl.enableTorch(torchEnabled)
                            } else {
                                cameraStatus = "এই ডিভাইসে ফ্ল্যাশ পাওয়া যায়নি"
                            }
                        },
                        enabled = camera?.cameraInfo?.hasFlashUnit() == true
                    ) { Text(if (torchEnabled) "ফ্ল্যাশ বন্ধ" else "ফ্ল্যাশ চালু") }
                    OutlinedButton(onClick = {
                        camera?.cameraControl?.enableTorch(false)
                        torchEnabled = false
                        cameraStatus = "পরিদর্শন চলমান"
                    }) { Text("ফ্ল্যাশ রিসেট") }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("জুম", color = Color.White, style = MaterialTheme.typography.labelLarge)
                    OutlinedButton(
                        onClick = {
                            val activeCamera = camera ?: return@OutlinedButton
                            val state = activeCamera.cameraInfo.zoomState.value ?: return@OutlinedButton
                            val next = (zoomRatio - 0.5f).coerceAtLeast(state.minZoomRatio)
                            activeCamera.cameraControl.setZoomRatio(next)
                            zoomRatio = next
                        },
                        enabled = camera != null && zoomRatio > (camera?.cameraInfo?.zoomState?.value?.minZoomRatio ?: 1f)
                    ) { Text("−") }
                    Text(String.format("%.1f×", zoomRatio), color = CameraMint)
                    OutlinedButton(
                        onClick = {
                            val activeCamera = camera ?: return@OutlinedButton
                            val state = activeCamera.cameraInfo.zoomState.value ?: return@OutlinedButton
                            val next = (zoomRatio + 0.5f).coerceAtMost(state.maxZoomRatio)
                            activeCamera.cameraControl.setZoomRatio(next)
                            zoomRatio = next
                        },
                        enabled = camera != null && zoomRatio < (camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 1f)
                    ) { Text("+") }
                }
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("ক্যামেরার অনুমতি দিন")
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1B332B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("●", color = CameraMint, style = MaterialTheme.typography.titleMedium)
                    Text(cameraStatus, color = Color.White, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(Modifier.height(2.dp))

            Text("01  •  অপটিক্যাল পর্যবেক্ষণ", color = CameraMint, style = MaterialTheme.typography.titleMedium)
            Text(
                "নিজে দেখে পাওয়া বিষয়গুলো টিক দিন। এগুলো পর্যবেক্ষণের নোট—স্বয়ংক্রিয় ক্যামেরা শনাক্তকরণ বা ঝুঁকির স্কোর নয়।",
                color = CameraMuted,
                style = MaterialTheme.typography.bodySmall
            )
            ObservationToggle("লেন্সের মতো গোল/কাচের উপাদান দেখা গেছে", observations.lensLikeElement) {
                onObservationsChange(observations.copy(lensLikeElement = it))
            }
            ObservationToggle("একাধিক কোণ থেকে দেখেও একই স্থানে ঝলক পুনরায় দেখা গেছে", observations.repeatableGlintFromMultipleAngles) {
                onObservationsChange(observations.copy(repeatableGlintFromMultipleAngles = it))
            }
            ObservationToggle("বস্তুতে বিদ্যুৎ বা ডেটা সংযোগের চিহ্ন দেখা গেছে", observations.visiblePowerOrDataConnection) {
                onObservationsChange(observations.copy(visiblePowerOrDataConnection = it))
            }
            ObservationToggle("অন্য আলো/কোণ বা দ্বিতীয়বার দেখে বিষয়টি পুনরায় যাচাই করেছি", observations.independentlyRechecked) {
                onObservationsChange(observations.copy(independentlyRechecked = it))
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = inspectionCompleted,
                    onCheckedChange = onInspectionCompletedChange
                )
                Text("আমি অপটিক্যাল পরীক্ষা সম্পন্ন করেছি", color = Color.White, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "${observations.recordedObservationCount}/4টি পর্যবেক্ষণ নথিভুক্ত",
                color = CameraMint,
                style = MaterialTheme.typography.labelLarge
            )
            Text(observations.guidance(), color = Color(0xFFFFD28A), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { onObservationsChange(OpticalInspection()); onInspectionCompletedChange(false) },
                modifier = Modifier.fillMaxWidth()
            ) { Text("পর্যবেক্ষণ তালিকা রিসেট") }

            Text(
                "সর্বশেষ স্ক্যানের ফল: Wi-Fi ${nearbyWifiNetworks.size}টি • Bluetooth LE ${nearbyBluetoothDevices.size}টি",
                color = CameraMint,
                style = MaterialTheme.typography.bodySmall
            )
            Text("02  •  পরিদর্শন রিপোর্ট", color = CameraMint, style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(
                value = roomLabel,
                onValueChange = { roomLabel = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("স্থান / রুমের নাম") },
                singleLine = true
            )
            OutlinedTextField(
                value = reportNotes,
                onValueChange = { reportNotes = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("অতিরিক্ত পর্যবেক্ষণ নোট") },
                minLines = 2
            )
            Button(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    val report = InspectionReport(
                        roomLabel = roomLabel,
                        observedAt = timestamp,
                        observations = observations,
                        notes = reportNotes,
                        nearbyWifiNetworks = nearbyWifiNetworks,
                        nearbyBluetoothDevices = nearbyBluetoothDevices
                    )
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, report.toPlainText())
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "পরিদর্শন রিপোর্ট শেয়ার করুন"))
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("রিপোর্ট তৈরি ও শেয়ার করুন") }
            OutlinedButton(
                onClick = {
                    val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                    pendingReportText = InspectionReport(
                        roomLabel = roomLabel,
                        observedAt = timestamp,
                        observations = observations,
                        notes = reportNotes,
                        nearbyWifiNetworks = nearbyWifiNetworks,
                        nearbyBluetoothDevices = nearbyBluetoothDevices
                    ).toPlainText()
                    val safeName = roomLabel.trim()
                        .replace(Regex("[^A-Za-z0-9_-]+"), "_")
                        .trim('_')
                        .take(30)
                        .ifBlank { "inspection" }
                    saveReportLauncher.launch("SpyGuard_BD_${safeName}.txt")
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("TXT ফাইল হিসেবে সংরক্ষণ করুন") }
            OutlinedButton(
                onClick = {
                    try {
                        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                        val report = InspectionReport(
                            roomLabel = roomLabel,
                            observedAt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()),
                            observations = observations,
                            notes = reportNotes,
                        nearbyWifiNetworks = nearbyWifiNetworks,
                        nearbyBluetoothDevices = nearbyBluetoothDevices
                        )
                        reportStore.save(report, timestamp)
                        savedReports = reportStore.list()
                        reportStatus = "রিপোর্টটি এই ডিভাইসের ব্যক্তিগত ইতিহাসে সংরক্ষিত হয়েছে"
                    } catch (error: Exception) {
                        reportStatus = "ইতিহাসে সংরক্ষণ করা যায়নি: ${error.localizedMessage ?: "অজানা সমস্যা"}"
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("ডিভাইসে রিপোর্ট ইতিহাসে সংরক্ষণ করুন") }
            Text("03  •  সংরক্ষিত রিপোর্ট (${savedReports.size})", color = CameraMint, style = MaterialTheme.typography.titleSmall)
            if (savedReports.isEmpty()) {
                Text("এখনো কোনো রিপোর্ট ইতিহাসে সংরক্ষণ করা হয়নি।", color = CameraMuted, style = MaterialTheme.typography.bodySmall)
            } else {
                savedReports.forEach { saved ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3029)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(saved.fileName, color = Color.White, style = MaterialTheme.typography.labelLarge)
                            Text(saved.content.lineSequence().take(3).joinToString("\n"), color = CameraMuted, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, saved.content)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "সংরক্ষিত রিপোর্ট শেয়ার করুন"))
                                }) { Text("শেয়ার") }
                                OutlinedButton(onClick = {
                                    if (reportStore.delete(saved.fileName)) {
                                        savedReports = reportStore.list()
                                        reportStatus = "রিপোর্ট ইতিহাস থেকে মুছে ফেলা হয়েছে"
                                    } else {
                                        reportStatus = "রিপোর্টটি মুছে ফেলা যায়নি"
                                    }
                                }) { Text("মুছুন") }
                            }
                        }
                    }
                }
            }
            if (reportStatus.isNotBlank()) {
                Text(reportStatus, color = CameraMint, style = MaterialTheme.typography.bodySmall)
            }
            Text(
                "রিপোর্টটি কেবল আপনার দেওয়া নোট ও হাতে টিক দেওয়া পর্যবেক্ষণ থেকে তৈরি হবে। এটি স্বয়ংক্রিয় শনাক্তকরণ নয়; শেয়ার করার আগে প্রাপক যাচাই করুন।",
                color = CameraMuted,
                style = MaterialTheme.typography.bodySmall
            )

            Text(
                "পরিদর্শনের টিপস: ফোন ধীরে ধীরে ঘোরান, বিভিন্ন কোণ থেকে বস্তু দেখুন এবং একই বিন্দুতে আলোর প্রতিফলন পুনরায় যাচাই করুন। অচেনা বৈদ্যুতিক বস্তু খুলবেন না বা স্পর্শ করবেন না।",
                color = CameraMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "সীমাবদ্ধতা: এই সংস্করণে লাইভ প্রিভিউ, ফ্ল্যাশ নিয়ন্ত্রণ ও হাতে নথিভুক্ত পর্যবেক্ষণ আছে; স্বয়ংক্রিয় লেন্স শনাক্তকরণ বা AI শ্রেণিবিন্যাস নেই।",
                color = Color(0xFFFFD28A),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        screenActive = true
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> {
                    camera?.cameraControl?.enableTorch(false)
                    torchEnabled = false
                }
                Lifecycle.Event.ON_RESUME -> {
                    val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                    permissionGranted = granted
                    if (!granted) {
                        camera?.cameraControl?.enableTorch(false)
                        provider?.let { cameraProvider -> previewUseCase?.let { useCase: Preview -> cameraProvider.unbind(useCase) } }
                        previewUseCase = null
                        camera = null
                        provider = null
                        torchEnabled = false
                        cameraStatus = "ক্যামেরা অনুমতি আর সক্রিয় নেই"
                    }
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(lifecycleObserver)
        onDispose {
            screenActive = false
            lifecycleOwner.lifecycle.removeObserver(lifecycleObserver)
            camera?.cameraControl?.enableTorch(false)
            provider?.let { cameraProvider -> previewUseCase?.let { useCase: Preview -> cameraProvider.unbind(useCase) } }
            previewUseCase = null
            camera = null
            provider = null
            torchEnabled = false
        }
    }
}

@Composable
private fun ObservationToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A3029)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = checked, onCheckedChange = onCheckedChange)
            Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
        }
    }
}
