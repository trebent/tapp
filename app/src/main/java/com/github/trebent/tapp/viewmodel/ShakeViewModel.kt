/**
 * This file implements the shake detection.
 */
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

/**
 * Shake view model, providing composables a way of subscribing to shake events.
 *
 * @property application
 * @constructor Create empty Shake view model
 */
class ShakeViewModel(private val application: Application) : AndroidViewModel(application) {
    private val _i = MutableStateFlow(false)
    val initialised = _i.asStateFlow()

    // Subscribable to react to shake events.
    private val _shakeEvents = MutableSharedFlow<Unit>()
    val shakeEvents = _shakeEvents.asSharedFlow()

    private val sensorManager = application.getSystemService(SensorManager::class.java)
    private val shakeDetector = ShakeDetector(sensorManager, {
        Log.i("ShakeDetector", "detected a shake!")
        viewModelScope.launch { _shakeEvents.emit(Unit) }
    })

    /**
     * Init logic here mostly to align with the other two view models, nothing to see here really.
     */
    init {
        Log.i("ShakeViewModel", "initialising the view model...")
        _i.value = true
    }

    /**
     * Start listening with the underlying shake detector.
     *
     */
    fun startListening() {
        shakeDetector.start()
    }

    /**
     * Stop listening with the underlying shake detector.
     *
     */
    fun stopListening() {
        shakeDetector.stop()
    }
}

/**
 * Shake detector, extends SensorEventListener to detect accelerometer events.
 *
 * @property sensorManager
 * @property onShake what happens when you shake the device
 * @constructor Create empty Shake detector
 */
private class ShakeDetector(
    private val sensorManager: SensorManager,
    private val onShake: () -> Unit
) : SensorEventListener {

    private var lastUpdate: Long = 0

    // Current count to compare to the shake threshold.
    private var shakeCount = 0
    private val shakeThreshold = 15f
    private val shakeResetTime = 500  // Maximum time between movements to consider it a shake.

    // Number of spikes to consider a shake, important for a "back and forth" movement to actually
    // trigger it, rather than just a sudden movement forward/backward.
    private val requiredShakes = 2

    // Time of last shake, to account for the reset time.
    private var lastShakeTime: Long = 0

    /**
     * Start the detector. Call Stop() to clean up resources associated with starting it.
     *
     */
    fun start() {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI)
    }

    /**
     * Stop the detector.
     *
     */
    fun stop() {
        sensorManager.unregisterListener(this)
    }

    /**
     * On sensor changed is called per sensor movement detected.
     *
     * @param event
     */
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

            // NOTE: the gforce section was written with AI assistance.
            val gForce = sqrt(gX * gX + gY * gY + gZ * gZ)

            if (gForce > shakeThreshold / 10f) { // normalize threshold
                val now = System.currentTimeMillis()
                if (lastShakeTime + shakeResetTime < now) {
                    shakeCount = 0 // reset if too much time passed
                }

                lastShakeTime = now
                shakeCount++

                if (shakeCount >= requiredShakes) {
                    shakeCount = 0
                    onShake()
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
}
