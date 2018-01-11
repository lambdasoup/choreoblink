package com.lambdasoup.choreoblink

import android.Manifest
import android.annotation.SuppressLint
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Switch
import android.widget.TextView
import java.util.*
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity(), TimeSyncView.Listener {

    private val torch = Torch()
    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            Log.d(TAG, "torch enabled: " + enabled)
            if (enabled) {
                //                onDelay = System.currentTimeMillis() - start;
            } else {
                //                offDelay = System.currentTimeMillis() - start;
            }
        }
    }
    private var executor: ScheduledExecutorService? = null
    private var cameraId: String? = null

    private val onDelay: Long = 0
    private val offDelay: Long = 0

    private lateinit var timeSyncView: TimeSyncView

    private lateinit var timeSyncViewModel: TimeSyncViewModel

    private lateinit var timestampProvider: TimestampProvider

    private var showChoreo = false
    private var cameraManager: CameraManager? = null
    private var dlay: TextView? = null
    private val updateTime = Runnable {
        runOnUiThread {
            dlay!!.text = String.format(Locale.ENGLISH, "on_delay: %dms", onDelay)
//            val real = System.currentTimeMillis() + delta
//            corr!!.text = formatTime(real)
//
//            val corr2 = real + onDelay
//            choreo((corr2 % 10000 / 1000).toInt())
        }
    }
    private var toggle: Switch? = null

    private fun choreo(t: Int) {
        if (!showChoreo) {
            return
        }

        val on = CHOREO[t]
        Log.d(TAG, "on: " + on.toString() + "   t: " + t)
        if (on) {
            torch.on()
        } else {
            torch.off()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        timeSyncView = findViewById(R.id.time_sync)
        timeSyncView.listener = this
        timeSyncViewModel = ViewModelProviders.of(this)
                .get(TimeSyncViewModel::class.java)
        timeSyncViewModel.state.observe(this, timeSyncView)

        timestampProvider = TimestampProvider(this, timeSyncViewModel::update)
        lifecycle.addObserver(timestampProvider)

        dlay = findViewById(R.id.dlay)
        toggle = findViewById(R.id.toggle)
        toggle!!.setOnCheckedChangeListener { buttonView, isChecked ->
            if (!isChecked) {
                torch.off()
            }

            showChoreo = isChecked
        }
        findViewById<View>(R.id.delay).setOnClickListener { measureTorchDelay() }

        try {
            val cameraIdList = cameraManager!!.cameraIdList
            Log.d(TAG, "list: " + cameraIdList)
            val characteristics = cameraManager!!.getCameraCharacteristics(cameraIdList[0])
            val available = characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)
            Log.d(TAG, "characteristics: " + characteristics)
            cameraId = cameraIdList[0]
        } catch (e: CameraAccessException) {
            e.printStackTrace()
        }

    }

    private fun measureTorchDelay() {
        if (torch.on) {
            torch.off()
        } else {
            torch.on()
        }
    }

    @SuppressLint("NewApi")
    override fun onResume() {
        super.onResume()

        timeSyncViewModel.update()

        cameraManager!!.registerTorchCallback(torchCallback, null)

        executor = ScheduledThreadPoolExecutor(1)
        executor!!.scheduleAtFixedRate(updateTime, 0, 10, TimeUnit.MILLISECONDS)
    }

    override fun onPause() {

        cameraManager!!.unregisterTorchCallback(torchCallback)

        executor!!.shutdown()

        torch.off()

        super.onPause()
    }

    inner class Torch {

        var on = false

        internal fun on() {
            if (on) {
                return
            }
            on = true
            executor!!.execute {
                try {
                    cameraManager!!.setTorchMode(cameraId!!, true)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }
        }

        internal fun off() {
            if (!on) {
                return
            }
            on = false
            executor!!.execute {
                try {
                    cameraManager!!.setTorchMode(cameraId!!, false)
                } catch (e: CameraAccessException) {
                    e.printStackTrace()
                }
            }
        }

    }

    override fun requestPermission(permission: String) {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
    }

    companion object {

        private val TAG = MainActivity::class.java.name
        private val CHOREO = booleanArrayOf(true, false, true, false, true, false, true, false, true, false)

        private fun formatTime(t: Long): String {
            val ms = t % 1000
            val s = t / 1000
            return String.format(Locale.ENGLISH, "%ds%dms", s, ms)
        }
    }

}

private class TimestampProvider(val context: Context, val callback: (Location) -> Unit)
    : LifecycleObserver, LocationListener {

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String?) {}

    override fun onProviderDisabled(provider: String?) {}

    override fun onLocationChanged(location: Location?) {
        location?.apply(callback)
    }

    private val locationManager: LocationManager =
            context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    fun start() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_DENIED) {
            return
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, this)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    fun stop() {
        locationManager.removeUpdates(this)
    }
}
