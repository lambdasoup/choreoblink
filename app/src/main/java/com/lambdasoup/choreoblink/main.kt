package com.lambdasoup.choreoblink

import android.Manifest
import android.app.Application
import android.arch.lifecycle.*
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

        viewModel.state.observe(this,
            Observer<MainViewModel.State> { t ->
                timeView.onChanged(t?.timeSyncState)
                torchView.onChanged(t?.torchState)
                choreoView.onChanged(t?.choreos)
            })

        choreoView.listener = object : ChoreoView.Listener {
            override fun onChoreoSelected(choreo: Choreo) {
                viewModel.selectChoreo(choreo)
            }
        }

        torchView.listener = object : CameraView.Listener {
            override fun onOnDelayChanged(value: Long) {
                viewModel.changeOnDelayTo(value)
            }

            override fun onOffDelayChanged(value: Long) {
                viewModel.changeOffDelayTo(value)
            }
        }

        timeView.listener = this
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

    val state: LiveData<State> = object : MediatorLiveData<State>() {

        init {
            value = MainViewModel.State(null, null, null)
            addSource(choreoRepository.choreos, { value = value!!.copy(choreos = it) })
            addSource(torchManager.state, { value = value!!.copy(torchState = it) })
            addSource(timeSource.state, { value = value!!.copy(timeSyncState = it) })
        }

        override fun onActive() {
            super.onActive()
            timeSource.state.observeForever(timeObserver)
        }

        override fun onInactive() {
            timeSource.state.removeObserver(timeObserver)
            super.onInactive()
        }
    }

    private val timeObserver =
        Observer<TimeSyncState> { timeSyncState ->
            if (timeSyncState is TimeSyncState.Synced) {
                torchManager.updateTimeDelta(timeSyncState.delta)
            }
        }

    fun selectChoreo(choreo: Choreo) {
        torchManager.setChoreo(choreo)
    }

    data class State(
        val choreos: List<Choreo>?,
        val torchState: TorchState?,
        val timeSyncState: TimeSyncState?
    )

    fun changeOnDelayTo(value: Long) {
        torchManager.setOnDelay(value)
    }

    fun changeOffDelayTo(value: Long) {
        torchManager.setOffDelay(value)
    }
}

