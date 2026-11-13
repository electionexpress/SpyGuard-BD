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
import kotlin.math.abs

private data class SensorSpec(val type: Int, val title: String, val unit: String)

private val inspectedSensors = listOf(
    SensorSpec(Sensor.TYPE_ACCELEROMETER, "Accelerometer", "m/s²"),
    SensorSpec(Sensor.TYPE_GYROSCOPE, "Gyroscope", "rad/s"),
    SensorSpec(Sensor.TYPE_MAGNETIC_FIELD, "Magnetometer", "µT"),
    SensorSpec(Sensor.TYPE_LIGHT, "Ambient light", "lux"),
    SensorSpec(Sensor.TYPE_PROXIMITY, "Proximity", "cm"),
    SensorSpec(Sensor.TYPE_PRESSURE, "Air pressure", "hPa")
)

/**
 * Shows only sensors physically exposed by Android and their live measurements.
 * Sensor readings are environmental observations, never proof of a nearby device/camera.
 */
@Composable
fun SensorDiagnosticsScreen() {
    val context = LocalContext.current
    val manager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    val latestValues = remember { mutableStateMapOf<Int, String>() }
    val available = remember(manager) {
        inspectedSensors.mapNotNull { spec ->
            manager.getDefaultSensor(spec.type)?.let { spec to it }
        }
    }

    DisposableEffect(manager) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val values = event.values.joinToString(" · ") { value ->
                    String.format(Locale.getDefault(), "%.2f", value)
                }
                latestValues[event.sensor.type] = values
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        available.forEach { (_, sensor) ->
            manager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        onDispose { manager.unregisterListener(listener) }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF10242B)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("স্বতন্ত্র সেন্সর পরীক্ষা", color = Color.White, style = MaterialTheme.typography.titleLarge)
            Text(
                "এই পরীক্ষা Wi-Fi ও Bluetooth স্ক্যানের ওপর নির্ভর করে না। ফোনে থাকা সেন্সরগুলোর লাইভ রিডিং দেখায়।",
                color = Color(0xFFAAC2C5), style = MaterialTheme.typography.bodySmall
            )
            Text(
                "পাওয়া গেছে: ${available.size}/${inspectedSensors.size}টি সেন্সর",
                color = Color(0xFF55E6C1), style = MaterialTheme.typography.titleMedium
            )
            inspectedSensors.forEach { spec ->
                val sensor = available.firstOrNull { it.first.type == spec.type }?.second
                val reading = latestValues[spec.type]
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(spec.title, color = Color.White)
                        Text(
                            when {
                                sensor == null -> "এই ফোনে সেন্সরটি পাওয়া যায়নি"
                                reading == null -> "সেন্সর আছে • রিডিং অপেক্ষমাণ"
                                else -> "লাইভ: $reading ${spec.unit}"
                            },
                            color = if (sensor == null) Color(0xFFFFC27A) else Color(0xFFAAC2C5),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Text(
                        if (sensor == null) "অনুপস্থিত" else "উপলব্ধ",
                        color = if (sensor == null) Color(0xFFFFC27A) else Color(0xFF55E6C1),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
            Text(
                "সীমাবদ্ধতা: চৌম্বক ক্ষেত্র, আলো বা কম্পনের পরিবর্তন থেকে একা কোনো গোপন ক্যামেরা বা অন্য ফোন শনাক্ত করা যায় না। এই রিডিং কেবল পর্যবেক্ষণ; নিশ্চিত ডিভাইস শনাক্তকরণ নয়।",
                color = Color(0xFFAAC2C5), style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
