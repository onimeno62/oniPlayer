package com.example.ui.library.hero

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalContext

@Composable
fun rememberDeviceTilt(reduceMotion: Boolean): State<Offset> {
    val context = LocalContext.current
    val tiltState = remember { mutableStateOf(Offset.Zero) }

    if (reduceMotion) {
        return remember { derivedStateOf { Offset.Zero } }
    }

    DisposableEffect(context) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        if (sensorManager == null || accelerometer == null) {
            return@DisposableEffect onDispose {}
        }

        var xSmoothed = 0f
        var ySmoothed = 0f
        val alpha = 0.10f // Smooth drift feel

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                if (event == null) return
                // xRaw: tilt left/right (negative maps to tilt right, positive tilt left)
                // yRaw: tilt forward/backward
                val xRaw = event.values[0]
                val yRaw = event.values[1]

                // Map ~-5 to 5 range to -1 to 1
                val xTarget = (-xRaw / 5f).coerceIn(-1f, 1f)
                val yTarget = (yRaw / 5f).coerceIn(-1f, 1f)

                xSmoothed = xSmoothed * (1f - alpha) + xTarget * alpha
                ySmoothed = ySmoothed * (1f - alpha) + yTarget * alpha

                tiltState.value = Offset(xSmoothed, ySmoothed)
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_UI
        )

        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    return tiltState
}
