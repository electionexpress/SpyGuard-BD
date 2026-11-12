package com.electionexpress.spyguardbd

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SpyGuardApp() }
    }
}

@Composable
private fun SpyGuardApp() {
    val context = LocalContext.current
    val cameraManager = remember {
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    var cameraPermission by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var flashMessage by remember { mutableStateOf("ফ্ল্যাশ পরীক্ষা এখনো করা হয়নি") }
    var flashOn by remember { mutableStateOf(false) }
    var cameraCount by remember { mutableStateOf(cameraManager.cameraIdList.size) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermission = granted
        if (!granted) flashMessage = "ক্যামেরা অনুমতি দেওয়া হয়নি"
    }

    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            primary = Color(0xFF53E0A0),
            background = Color(0xFF07110F),
            surface = Color(0xFF12201C)
        )
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF07110F)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = Color(0xFF53E0A0))
                    Text("  SPYGUARD BD", fontSize = 23.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
                Text("ব্যক্তিগত নিরাপত্তা স্ক্যানার", color = Color(0xFFB8C9C2))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C))
                ) {
                    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Icon(Icons.Default.Radar, contentDescription = null, tint = Color(0xFF53E0A0))
                        Text("ডিভাইস প্রস্তুতি", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text("ক্যামেরা পাওয়া গেছে: $cameraCount", color = Color(0xFFD8E7E0))
                        Text("ক্যামেরা অনুমতি: " + if (cameraPermission) "অনুমোদিত" else "প্রয়োজন", color = Color(0xFFD8E7E0))
                    }
                }
                FeatureCard("ক্যামেরা ও ফ্ল্যাশ পরীক্ষা", "ফোনের ক্যামেরা/ফ্ল্যাশ সচল কি না যাচাই", Icons.Default.CameraAlt)
                Button(
                    onClick = {
                        if (!cameraPermission) {
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        } else {
                            val flashId = cameraManager.cameraIdList.firstOrNull { id ->
                                cameraManager.getCameraCharacteristics(id)
                                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                            }
                            if (flashId == null) {
                                flashMessage = "এই ক্যামেরায় LED ফ্ল্যাশ পাওয়া যায়নি"
                            } else {
                                try {
                                    flashOn = !flashOn
                                    cameraManager.setTorchMode(flashId, flashOn)
                                    flashMessage = if (flashOn) "ফ্ল্যাশ চালু হয়েছে — আবার চাপলে বন্ধ হবে" else "ফ্ল্যাশ বন্ধ হয়েছে"
                                } catch (_: Exception) {
                                    flashMessage = "ফ্ল্যাশ চালু করা যায়নি"
                                    flashOn = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.FlashlightOn, contentDescription = null)
                    Text(if (cameraPermission) "  ফ্ল্যাশ পরীক্ষা" else "  ক্যামেরা অনুমতি দিন")
                }
                Text(flashMessage, color = Color(0xFFB8C9C2))
                FeatureCard("Wi-Fi ও Bluetooth", "পরবর্তী ধাপে আশেপাশের দৃশ্যমান ডিভাইস স্ক্যান যুক্ত হবে", Icons.Default.Wifi)
                FeatureCard("গোপন ক্যামেরা অনুসন্ধান", "এটি এখনো পরীক্ষামূলক; ফোন একা সব গোপন ক্যামেরা শনাক্ত করতে পারে না", Icons.Default.Security)
                Spacer(Modifier.weight(1f))
                Text("সংস্করণ 0.1.0 • প্রাথমিক পরীক্ষামূলক বিল্ড", color = Color(0xFF789087), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, detail: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF12201C))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = Color(0xFF53E0A0))
            Column(Modifier.padding(start = 12.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(detail, color = Color(0xFFB8C9C2), fontSize = 12.sp)
            }
        }
    }
}
