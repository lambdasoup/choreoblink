package com.lambdasoup.choreoblink

import android.Manifest
import android.arch.lifecycle.LiveData
import android.arch.lifecycle.Observer
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.support.v4.content.LocalBroadcastManager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.Button
import android.widget.ProgressBar
import android.widget.TextView
import java.util.*

private const val PERMISSION: String = Manifest.permission.ACCESS_FINE_LOCATION

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

    override fun onChanged(nullableState: TimeSyncState?) {
        val state = requireNotNull(nullableState)

        return when (state) {
            TimeSyncState.NeedsPermission -> {
                progress.visibility = INVISIBLE
                text.visibility = INVISIBLE
                button.visibility = VISIBLE
                button.setOnClickListener { listener?.requestPermission(PERMISSION) }
            }
            TimeSyncState.Syncing, TimeSyncState.Idle -> {
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

class GpsRepository(context: Context) {

    val state = TimeSyncLiveData(context)

}

class TimeSyncLiveData(private val context: Context) : LiveData<TimeSyncState>() {

    init {
        value = TimeSyncState.Idle
    }

    private val locationManager: LocationManager =
            context.getSystemService(AppCompatActivity.LOCATION_SERVICE) as LocationManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            update()
        }
    }

    private val listener = object : LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
        override fun onProviderEnabled(provider: String?) {}
        override fun onProviderDisabled(provider: String?) {}
        override fun onLocationChanged(location: Location?) {
            if (location != null) {
                val delta = System.currentTimeMillis() - location.time
                postValue(TimeSyncState.Synced(delta))
                return
            }
        }
    }

    private fun update() {
        if (context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            postValue(TimeSyncState.NeedsPermission)
            return
        }

        if (value == TimeSyncState.Idle) {
            postValue(TimeSyncState.Syncing)
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0f, listener)
        }
    }

    override fun onActive() {
        super.onActive()

        LocalBroadcastManager.getInstance(context).registerReceiver(receiver,
                IntentFilter("${context.packageName}.CHECK_PERMISSIONS"))

        update()
    }

    override fun onInactive() {
        LocalBroadcastManager.getInstance(context).unregisterReceiver(receiver)
        locationManager.removeUpdates(listener)
        postValue(TimeSyncState.Idle)
        super.onInactive()
    }

}

sealed class TimeSyncState {
    object NeedsPermission : TimeSyncState()
    object Idle : TimeSyncState()
    object Syncing : TimeSyncState()
    data class Synced(val delta: Long) : TimeSyncState()
}