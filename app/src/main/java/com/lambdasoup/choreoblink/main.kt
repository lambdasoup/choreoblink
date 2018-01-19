package com.lambdasoup.choreoblink

import android.Manifest
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Transformations
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager

class MainActivity : AppCompatActivity(), TimeSyncView.Listener {

    private lateinit var timeSyncView: TimeSyncView
    private lateinit var cameraView: CameraView
    private lateinit var choreoView: ChoreoView

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timeSyncView = findViewById(R.id.time_sync)
        cameraView = findViewById(R.id.camera)
        choreoView = findViewById(R.id.choreos)

        viewModel = ViewModelProviders.of(this)
                .get(MainViewModel::class.java)
        viewModel.state.observe(this, timeSyncView)
        viewModel.choreos.observe(this, choreoView)
        viewModel.device.observe(this, cameraView)
    }

    override fun onResume() {
        super.onResume()

        val intent = Intent("$packageName.CHECK_PERMISSIONS")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun requestPermission(permission: String) {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
    }

}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraRepository = CameraRepository(application)
    private val choreoRepository = ChoreoRepository()
    private val gpsRepository = GpsRepository(application)

    val choreos = choreoRepository.choreos
    val device = Transformations.map(cameraRepository.devices, List<Device>::firstOrNull)!!
    val state: LiveData<TimeSyncState> = gpsRepository.state

}

