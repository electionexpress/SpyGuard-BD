package com.electionexpress.spyguardbd

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import java.util.Locale

private data class SensorSpec(val type: Int, val title: String, val unit: String)
private val inspectedSensors = listOf(
    SensorSpec(Sensor.TYPE_ACCELEROMETER, "Accelerometer", "m/s²"),
    SensorSpec(Sensor.TYPE_GYROSCOPE, "Gyroscope", "rad/s"),
    SensorSpec(Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer", "µT"),
    SensorSpec(Sensor.TYPE_LIGHT, "Ambient light", "lux"),
    SensorSpec(Sensor.TYPE_PROXIMITY, "Proximity", "cm"),
    SensorSpec(Sensor.TYPE_PRESSURE, "Air pressure", "hPa")
)

@Composable
fun SensorDiagnosticsScreen() {
    val context = LocalContext.current
    val manager = remember { context.getSystemService(Context.SENSOR_SERVICE) as SensorManager }
    val latestValues = remember { mutableStateMapOf<Int, String>() }
    val available = remember(manager) {
        inspectedSensors.mapNotNull { spec -> manager.getDefaultSensor(spec.type)?.let { spec to it } }
    }
    DisposableEffect(manager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                latestValues[event.sensor.type] = event.values.joinToString(" · ") {
                    String.format(Locale.getDefault(), "%.2f", it)
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        available.forEach { (_, sensor) -> manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI) }
        onDispose { manager.unregisterListener(listener) }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10242B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("ফোনের সেন্সর পর্যবেক্ষণ", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text("Wi-Fi/Bluetooth বন্ধ থাকলেও নিজের ফোনের সেন্সর রিডিং দেখা যায়; এগুলো অন্য ডিভাইস বা ক্যামেরার প্রমাণ নয়।",
                color = Color(0xFFAAC2C5), style = MaterialTheme.typography.bodySmall)
            Text("উপলব্ধ: ${available.size}/${inspectedSensors.size}টি", color = Color(0xFF55E6C1))
            inspectedSensors.forEach { spec ->
                val sensor = available.firstOrNull { it.first.type == spec.type }?.second
                val reading = latestValues[spec.type]
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column(Modifier.weight(1f)) {
                        Text(spec.title, color = Color.White)
                        Text(when {
                            sensor == null -> "এই ফোনে নেই"
                            reading == null -> "রিডিং অপেক্ষমাণ"
                            else -> "$reading ${spec.unit}"
                        }, color = Color(0xFFAAC2C5), style = MaterialTheme.typography.bodySmall)
                    }
                    Text(if (sensor == null) "অনুপস্থিত" else "সক্রিয়", color = if (sensor == null) Color(0xFFFFC27A) else Color(0xFF55E6C1))
                }
            }
        }
    }
}
