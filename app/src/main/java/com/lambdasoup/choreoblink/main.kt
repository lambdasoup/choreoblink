package com.lambdasoup.choreoblink

import android.Manifest
import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.view.WindowManager

class MainActivity : AppCompatActivity(), TimeSyncView.Listener {

    private lateinit var timeSyncView: TimeSyncView
    private lateinit var timeSyncViewModel: TimeSyncViewModel

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

        val intent = Intent("$packageName.CHECK_PERMISSIONS")
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent)
    }

    override fun requestPermission(permission: String) {
        requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 0)
    }

}

