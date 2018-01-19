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

    private lateinit var timeView: TimeSyncView
    private lateinit var torchView: CameraView
    private lateinit var choreoView: ChoreoView

    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        timeView = findViewById(R.id.time)
        torchView = findViewById(R.id.torch)
        choreoView = findViewById(R.id.choreo)

        viewModel = ViewModelProviders.of(this)
                .get(MainViewModel::class.java)
        viewModel.time.observe(this, timeView)
        viewModel.choreo.observe(this, choreoView)
        viewModel.device.observe(this, torchView)
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

class MainViewModel(app: Application) : AndroidViewModel(app) {

    private val torchManager = app.getService(TorchManager::class.java)
    private val choreoRepository = app.getService(ChoreoRepository::class.java)
    private val timeSource = app.getService(TimeSource::class.java)

    val choreo = choreoRepository.choreos
    val device = Transformations.map(torchManager.devices, List<Device>::firstOrNull)!!
    val time: LiveData<TimeSyncState> = timeSource.state

    init {
        time.observeForever { timeSyncState ->
            if (timeSyncState is TimeSyncState.Synced) {
                torchManager.updateTimeDelta(timeSyncState.delta)
            }
        }
    }

}

