package com.github.trebent.tapp.viewmodel

import android.app.Application
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class ShakeViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()
    private val _shakeEvents = MutableSharedFlow<Unit>()
    val shakeEvents = _shakeEvents.asSharedFlow()

    private val sensorManager = application.getSystemService(SensorManager::class.java)
    private val shakeDetector = ShakeDetector(sensorManager, {
        Log.i("ShakeDetector", "detected a shake!")
        viewModelScope.launch { _shakeEvents.emit(Unit) }
    })

    init {
        Log.i("ShakeViewModel", "initialising the view model...")
        _i.value = true
    }

    fun startListening() {
        shakeDetector.start()
    }

    fun stopListening() {
        shakeDetector.stop()
    }
}

private class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastUpdate: Long = 0
    private var shakeCount = 0
    private val SHAKE_THRESHOLD = 15f       // Higher threshold
    private val SHAKE_COUNT_RESET_TIME = 500 // ms
    private val REQUIRED_SHAKES = 2          // Number of spikes to consider a shake

    private var lastShakeTime: Long = 0

    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    fun stop() {
        sensorManager.unregisterListener(this)
    }


    override fun onSensorChanged(event: SensorEvent) {
        val curTime = System.currentTimeMillis()
        if ((curTime - lastUpdate) > 100) {
            lastUpdate = curTime

            val x = event.values[0]
            val y = event.values[1]
            val z = event.values[2]

            val gX = x / SensorManager.GRAVITY_EARTH
            val gY = y / SensorManager.GRAVITY_EARTH
            val gZ = z / SensorManager.GRAVITY_EARTH

            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > SHAKE_THRESHOLD / 10f) { // normalize threshold
                val now = System.currentTimeMillis()
                if (lastShakeTime + SHAKE_COUNT_RESET_TIME < now) {
                    shakeCount = 0 // reset if too much time passed
                }

                lastShakeTime = now
                shakeCount++

                if (shakeCount >= REQUIRED_SHAKES) {
                    shakeCount = 0
                    onShake()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
