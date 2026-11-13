package com.electionexpress.spyguardbd

import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.PermissionChecker

private val CameraPanel = Color(0xFF12201C)
private val CameraMint = Color(0xFF53E0A0)
private val CameraMuted = Color(0xFFB8C9C2)

/**
 * User-controlled optical inspection preview. The preview does not claim to
 * identify a camera; it helps the user visually inspect suspicious objects.
 */
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
    var cameraStatus by remember { mutableStateOf("ক্যামেরা ব্যবহারের অনুমতি প্রয়োজন") }
    var torchEnabled by remember { mutableStateOf(false) }

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
                "সন্দেহজনক বস্তু, ছোট লেন্সের মতো প্রতিফলন এবং অস্বাভাবিক আলো নিজে পর্যবেক্ষণ করুন। প্রতিফলন একা ক্যামেরার প্রমাণ নয়।",
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
                        }
                    },
                    update = { previewView ->
                        val providerFuture = ProcessCameraProvider.getInstance(context)
                        providerFuture.addListener({
                            try {
                                val provider = providerFuture.get()
                                val preview = Preview.Builder().build().also {
                                    it.setSurfaceProvider(previewView.surfaceProvider)
                                }
                                provider.unbindAll()
                                camera = provider.bindToLifecycle(
                                    lifecycleOwner,
                                    CameraSelector.DEFAULT_BACK_CAMERA,
                                    preview
                                )
                                cameraStatus = "লাইভ প্রিভিউ চালু"
                            } catch (error: Exception) {
                                cameraStatus = "ক্যামেরা চালু করা যায়নি: ${error.localizedMessage ?: "অজানা সমস্যা"}"
                            }
                        }, ContextCompat.getMainExecutor(context))
                    }
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val target = camera
                            if (target?.cameraInfo?.hasFlashUnit() == true) {
                                torchEnabled = !torchEnabled
                                target.cameraControl.enableTorch(torchEnabled)
                            } else {
                                cameraStatus = "এই ডিভাইসে ফ্ল্যাশ পাওয়া যায়নি"
                            }
                        },
                        enabled = camera?.cameraInfo?.hasFlashUnit() == true
                    ) {
                        Text(if (torchEnabled) "ফ্ল্যাশ বন্ধ" else "ফ্ল্যাশ চালু")
                    }
                    OutlinedButton(onClick = {
                        camera?.cameraControl?.enableTorch(false)
                        torchEnabled = false
                        cameraStatus = "পরিদর্শন চলমান"
                    }) { Text("রিসেট") }
                }
            } else {
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Text("ক্যামেরার অনুমতি দিন")
                }
            }

            Text(cameraStatus, color = CameraMint, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(2.dp))
            Text(
                "পরিদর্শনের টিপস: ফোন ধীরে ধীরে ঘোরান, বিভিন্ন কোণ থেকে বস্তু দেখুন এবং একই বিন্দুতে আলোর প্রতিফলন পুনরায় যাচাই করুন। কোনো সন্দেহজনক বস্তু পেলে কাছ থেকে ও দূর থেকে দেখুন।",
                color = CameraMuted,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                "সীমাবদ্ধতা: এই সংস্করণে লাইভ প্রিভিউ ও ফ্ল্যাশ নিয়ন্ত্রণ আছে; স্বয়ংক্রিয় লেন্স শনাক্তকরণ বা AI শ্রেণিবিন্যাস এখনো যুক্ত হয়নি।",
                color = Color(0xFFFFD28A),
                style = MaterialTheme.typography.bodySmall
            )
        }
    }

    DisposableEffect(lifecycleOwner) {
        onDispose {
            camera?.cameraControl?.enableTorch(false)
            camera = null
        }
    }
}
