package com.electionexpress.spyguardbd

import android.Manifest
import android.content.pm.PackageManager
import android.view.ViewGroup
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.Executor

private data class CameraEvidence(
    val fileName: String,
    val createdAt: String,
    val classification: String
)

/**
 * Camera-assisted inspection. Captures remain in this app's private files directory.
 * A captured image is evidence for manual review, not automatic proof of a camera.
 */
@Composable
fun CameraInspectionPanel() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor: Executor = remember { ContextCompat.getMainExecutor(context) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasPermission = granted }

    var camera by remember { mutableStateOf<Camera?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewStatus by remember { mutableStateOf("ক্যামেরা চালু করতে অনুমতি দিন") }
    var torchOn by remember { mutableStateOf(false) }
    var lastCapture by remember { mutableStateOf("এখনো কোনো ছবি সংরক্ষণ করা হয়নি") }
    val evidence = remember { mutableStateListOf<CameraEvidence>() }

    val previewView = remember {
        PreviewView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = PreviewView.ScaleType.FILL_CENTER
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    val providerFuture = remember { ProcessCameraProvider.getInstance(context) }

    DisposableEffect(hasPermission, lifecycleOwner) {
        if (hasPermission) {
            providerFuture.addListener({
                try {
                    val provider = providerFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.surfaceProvider = previewView.surfaceProvider
                    }
                    val capture = ImageCapture.Builder()
                        .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                        .build()
                    provider.unbindAll()
                    val boundCamera = provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        capture
                    )
                    camera = boundCamera
                    imageCapture = capture
                    torchOn = false
                    previewStatus = "লাইভ প্রিভিউ প্রস্তুত — বস্তুটি নিজে পর্যবেক্ষণ করুন"
                } catch (error: Exception) {
                    previewStatus = "ক্যামেরা চালু করা যায়নি: ${error.localizedMessage ?: "অজানা ত্রুটি"}"
                }
            }, mainExecutor)
        } else {
            previewStatus = "ক্যামেরা অনুমতি দেওয়া হয়নি"
        }
        onDispose {
            try {
                if (providerFuture.isDone) providerFuture.get().unbindAll()
            } catch (_: Exception) { }
            camera = null
            imageCapture = null
            torchOn = false
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("CAMERA INSPECTION", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "লাইভ ক্যামেরা দিয়ে সন্দেহজনক বস্তু দেখুন, প্রয়োজনে ছবি তুলে স্থানীয়ভাবে প্রমাণ সংরক্ষণ করুন। এটি AI-ভিত্তিক স্বয়ংক্রিয় ক্যামেরা শনাক্তকরণ নয়।",
                color = Color(0xFFB8C9C2), fontSize = 12.sp
            )

            if (hasPermission) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxWidth().height(230.dp)
                )
            } else {
                OutlinedButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("ক্যামেরা অনুমতি দিন") }
            }

            Text(previewStatus, color = Color(0xFFB8C9C2), fontSize = 12.sp)
            if (hasPermission) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        enabled = camera?.cameraInfo?.hasFlashUnit() == true,
                        onClick = {
                            val next = !torchOn
                            try {
                                camera?.cameraControl?.enableTorch(next)
                                torchOn = next
                            } catch (_: Exception) {
                                previewStatus = "এই ডিভাইসে Torch নিয়ন্ত্রণ পাওয়া যাচ্ছে না"
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (torchOn) "Torch বন্ধ" else "Torch চালু") }

                    Button(
                        enabled = imageCapture != null,
                        onClick = {
                            val capture = imageCapture ?: return@Button
                            val directory = File(context.filesDir, "camera-evidence")
                            if (!directory.exists()) directory.mkdirs()
                            val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                            val file = File(directory, "inspection-$stamp.jpg")
                            val options = ImageCapture.OutputFileOptions.Builder(file).build()
                            capture.takePicture(options, mainExecutor, object : ImageCapture.OnImageSavedCallback {
                                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                                    val time = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
                                    evidence.add(0, CameraEvidence(file.name, time, "Visual Candidate — manual review required"))
                                    lastCapture = "ছবি অ্যাপের ব্যক্তিগত স্টোরেজে সংরক্ষিত: ${file.name}"
                                    previewStatus = "প্রমাণ সংরক্ষিত; ছবিটি ক্যামেরার উপস্থিতি নিশ্চিত করে না"
                                }

                                override fun onError(exception: ImageCaptureException) {
                                    lastCapture = "ছবি সংরক্ষণ ব্যর্থ: ${exception.message ?: "অজানা ত্রুটি"}"
                                }
                            })
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("প্রমাণের ছবি তুলুন") }
                }
            }

            Text(lastCapture, color = Color(0xFF53E0A0), fontSize = 12.sp)
            Text("Evidence log • ${evidence.size}", color = Color.White, style = MaterialTheme.typography.titleMedium)
            if (evidence.isEmpty()) {
                Text("এখনো কোনো প্রমাণ নেই।", color = Color(0xFFB8C9C2), fontSize = 12.sp)
            } else {
                evidence.take(5).forEach { item ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(item.fileName, color = Color.White, fontSize = 12.sp)
                        Text("${item.createdAt} • ${item.classification}", color = Color(0xFFB8C9C2), fontSize = 11.sp)
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            Text(
                "যাচাই নীতি: ছবিতে লেন্সের মতো প্রতিফলন বা ছিদ্র দেখা গেলে কেবল Visual Candidate লিখুন। AI/লেন্স-ভিত্তিক যাচাই, IR পরীক্ষা বা নেটওয়ার্ক প্রমাণ সম্পন্ন না হলে Verified/Active Surveillance দেখানো যাবে না।",
                color = Color(0xFFFFD18A), fontSize = 11.sp
            )
        }
    }
}
