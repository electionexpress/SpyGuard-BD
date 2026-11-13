package com.electionexpress.spyguardbd

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.lifecycle.compose.LocalLifecycleOwner

private val CameraPanel = Color(0xFF12201C)
private val CameraMint = Color(0xFF53E0A0)
private val CameraMuted = Color(0xFFB8C9C2)

/** User-controlled optical inspection preview; it does not claim to identify cameras. */
@Composable
fun CameraInspectionScreen() {
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
    var cameraStatus by remember { mutableStateOf("ক্যামেরা ব্যবহারের অনুমতি প্রয়োজন") }
    var torchEnabled by remember { mutableStateOf(false) }
    var observations by remember { mutableStateOf(OpticalInspection()) }

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
                                    val cameraProvider = providerFuture.get()
                                    provider = cameraProvider
                                    val preview = Preview.Builder().build().also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                    cameraProvider.unbindAll()
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        CameraSelector.DEFAULT_BACK_CAMERA,
                                        preview
                                    )
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
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("ক্যামেরার অনুমতি দিন")
                }
            }

            Text(cameraStatus, color = CameraMint, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(2.dp))

            Text("অপটিক্যাল পর্যবেক্ষণ তালিকা", color = Color.White, style = MaterialTheme.typography.titleMedium)
            Text(
                "নিজে দেখে পাওয়া বিষয়গুলো টিক দিন। এগুলো পর্যবেক্ষণের নোট—স্বয়ংক্রিয় ক্যামেরা শনাক্তকরণ বা ঝুঁকির স্কোর নয়।",
                color = CameraMuted,
                style = MaterialTheme.typography.bodySmall
            )
            ObservationToggle("লেন্সের মতো গোল/কাচের উপাদান দেখা গেছে", observations.lensLikeElement) {
                observations = observations.copy(lensLikeElement = it)
            }
            ObservationToggle("একাধিক কোণ থেকে দেখেও একই স্থানে ঝলক পুনরায় দেখা গেছে", observations.repeatableGlintFromMultipleAngles) {
                observations = observations.copy(repeatableGlintFromMultipleAngles = it)
            }
            ObservationToggle("বস্তুতে বিদ্যুৎ বা ডেটা সংযোগের চিহ্ন দেখা গেছে", observations.visiblePowerOrDataConnection) {
                observations = observations.copy(visiblePowerOrDataConnection = it)
            }
            ObservationToggle("অন্য আলো/কোণ বা দ্বিতীয়বার দেখে বিষয়টি পুনরায় যাচাই করেছি", observations.independentlyRechecked) {
                observations = observations.copy(independentlyRechecked = it)
            }

            Text(
                "${observations.recordedObservationCount}/4টি পর্যবেক্ষণ নথিভুক্ত",
                color = CameraMint,
                style = MaterialTheme.typography.labelLarge
            )
            Text(observations.guidance(), color = Color(0xFFFFD28A), style = MaterialTheme.typography.bodySmall)
            OutlinedButton(
                onClick = { observations = OpticalInspection() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("পর্যবেক্ষণ তালিকা রিসেট") }

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
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            provider?.unbindAll()
            camera = null
            provider = null
        }
    }
}

@Composable
private fun ObservationToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = onCheckedChange)
        Text(label, color = Color.White, style = MaterialTheme.typography.bodySmall)
    }
}
