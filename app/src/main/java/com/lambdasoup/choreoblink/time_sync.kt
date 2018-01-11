package com.lambdasoup.choreoblink

import android.Manifest
import android.app.Application
import android.arch.lifecycle.AndroidViewModel
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*


class TimeSyncView @JvmOverloads constructor(context: Context,
                                             attrs: AttributeSet? = null,
                                             defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr), Observer<TimeSyncState> {

    private val button: Button
    private val text: TextView
    private val progress: ProgressBar

    var listener: Listener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.card_timesync, this)
        button = findViewById(R.id.button)
        progress = findViewById(R.id.progress)
        text = findViewById(R.id.text)
    }

    override fun onChanged(state: TimeSyncState?) {
        when (state) {
            is TimeSyncState.NeedsPermission -> {
                progress.visibility = INVISIBLE
                text.visibility = INVISIBLE
                button.visibility = VISIBLE
                button.setOnClickListener { listener?.requestPermission(state.permission) }
            }
            is TimeSyncState.Syncing -> {
                button.visibility = INVISIBLE
                progress.visibility = VISIBLE
                text.visibility = INVISIBLE
            }
            is TimeSyncState.Synced -> {
                button.visibility = INVISIBLE
                text.visibility = VISIBLE
                text.text = String.format(Locale.ENGLISH, "synced (%dms)", state.delta)
                progress.visibility = INVISIBLE
            }
        }
    }

    interface Listener {
        fun requestPermission(permission: String)
    }
}

class TimeSyncViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = GpsRepository(application)

    val state: LiveData<TimeSyncState> = repository.state

    fun update(location: Location? = null) = repository.update(location)

}

class GpsRepository(private val context: Context) {

    private var delta: Long = 0

    val state = MutableLiveData<TimeSyncState>()

    fun update(location: Location?) {
        if (location != null) {
            delta = System.currentTimeMillis() - location.time
            state.postValue(TimeSyncState.Synced(delta))
            return
        }

        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            state.postValue(TimeSyncState.Syncing)
        } else {
            state.postValue(TimeSyncState.NeedsPermission)
        }

    }

}

sealed class TimeSyncState {
    object NeedsPermission : TimeSyncState() {
        val permission: String = Manifest.permission.ACCESS_FINE_LOCATION
    }

    object Syncing : TimeSyncState()
    data class Synced(val delta: Long) : TimeSyncState()
}