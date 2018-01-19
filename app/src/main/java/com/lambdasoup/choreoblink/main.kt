package com.lambdasoup.choreoblink

import android.Manifest
import android.arch.lifecycle.Lifecycle
import android.arch.lifecycle.LifecycleObserver
import android.arch.lifecycle.OnLifecycleEvent
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager

class MainActivity : AppCompatActivity(), TimeSyncView.Listener {

    private lateinit var timeSyncView: TimeSyncView
    private lateinit var timeSyncViewModel: TimeSyncViewModel
    private lateinit var timestampProvider: TimestampProvider

    private lateinit var cameraView: CameraView
    private lateinit var cameraViewModel: CameraViewModel

    private lateinit var choreoView: ChoreoView
    private lateinit var choreoViewModel: ChoreoViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // bind time stuff
        timeSyncView = findViewById(R.id.time_sync)
        timeSyncView.listener = this
        timeSyncViewModel = ViewModelProviders.of(this)
                .get(TimeSyncViewModel::class.java)
        timeSyncViewModel.state.observe(this, timeSyncView)
        timestampProvider = TimestampProvider(this, timeSyncViewModel::update)
        lifecycle.addObserver(timestampProvider)

        // bind camera stuff
        cameraView = findViewById(R.id.camera)
        cameraViewModel = ViewModelProviders.of(this)
                .get(CameraViewModel::class.java)
        cameraViewModel.device.observe(this, cameraView)

        // bind choreo stuff
        choreoView = findViewById(R.id.choreos)
        choreoViewModel = ViewModelProviders.of(this).get(ChoreoViewModel::class.java)
        choreoViewModel.choreos.observe(this, choreoView)
    }

    override fun onResume() {
        super.onResume()

        timeSyncViewModel.update()
    }

    override fun requestPermission(permission: String) {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
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
