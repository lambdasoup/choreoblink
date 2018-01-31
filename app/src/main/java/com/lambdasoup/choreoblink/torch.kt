package com.lambdasoup.choreoblink

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.preference.PreferenceManager
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.SeekBar
import android.widget.TextView
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit

class TorchManager(context: Context) {

    val state = TorchLiveData(context)

    fun updateTimeDelta(delta: Long) {
        state.delta.value = delta
    }

    fun setChoreo(choreo: Choreo) {
        state.choreo.value = choreo
    }

    fun setOnDelay(value: Long) {
        state.setOnDelay(value)
    }

    fun setOffDelay(value: Long) {
        state.setOffDelay(value)
    }
}

data class TorchState(val device: Device?, val choreo: Choreo?, val delta: Long?)

data class Device(val id: String, val onDelay: Long, val offDelay: Long, val flash: Boolean)

private const val PREF_KEY_ON_DELAY = "on-delay"
private const val PREF_KEY_OFF_DELAY = "off-delay"

class TorchLiveData(context: Context) : MediatorLiveData<TorchState>() {

    private val scheduler: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    private val prefs = PreferenceManager.getDefaultSharedPreferences(context)

    val delta = MutableLiveData<Long>()
    val choreo = MutableLiveData<Choreo>()

    private val prefsListener =
        SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            val current: TorchState = value ?: return@OnSharedPreferenceChangeListener
            val device = current.device ?: return@OnSharedPreferenceChangeListener

            if (key == PREF_KEY_ON_DELAY) {
                value = current.copy(
                    device = device.copy(
                        onDelay = prefs.getLong(PREF_KEY_ON_DELAY, 0)
                    )
                )
            }
            if (key == PREF_KEY_OFF_DELAY) {
                value = current.copy(
                    device = device.copy(
                        offDelay = prefs.getLong(PREF_KEY_OFF_DELAY, 0)
                    )
                )
            }
        }

    init {
        try {
            val ids = manager.cameraIdList
            val onDelay = prefs.getLong(PREF_KEY_ON_DELAY, 0)
            val offDelay = prefs.getLong(PREF_KEY_OFF_DELAY, 0)
            value = TorchState(Device(ids[0], onDelay, offDelay, false), null, 0L)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }

        prefs.registerOnSharedPreferenceChangeListener(prefsListener)

        addSource(delta) { value = value?.copy(delta = it) }
        addSource(choreo) { value = value?.copy(choreo = it) }
    }

    var torch = false
    var undelayedTorch = false
    var future: ScheduledFuture<*>? = null


    override fun onActive() {
        super.onActive()
        future = scheduler.scheduleWithFixedDelay(ticker, 0, 1, TimeUnit.MILLISECONDS)
    }

    override fun onInactive() {
        future!!.cancel(true)
        scheduler.execute(turnOff)
        super.onInactive()
    }

    private val ticker = Runnable {
        val state = value ?: return@Runnable

        val choreo = state.choreo ?: return@Runnable
        val delta = state.delta ?: return@Runnable
        val device = state.device ?: return@Runnable

        val time = System.currentTimeMillis() + delta

        val undelayedOn = choreo.on(time, 0, 0)
        if (undelayedOn && !undelayedTorch) {
            undelayedTorch = true
            postValue(state.copy(device = device.copy(flash = true)))
        }
        if (!undelayedOn && undelayedTorch) {
            undelayedTorch = false
            postValue(state.copy(device = device.copy(flash = false)))
        }

        val on = choreo.on(time, device.onDelay, device.offDelay)
        if (on && !torch) {
            manager.setTorchMode(device.id, true)
            Log.d("torch", "turning on")
            torch = true
        }
        if (!on && torch) {
            manager.setTorchMode(device.id, false)
            Log.d("torch", "turning off")
            torch = false
        }
    }

    private val turnOff = Runnable {
        val state = value ?: return@Runnable
        val device = state.device ?: return@Runnable
        manager.setTorchMode(device.id, false)
    }

    fun setOnDelay(value: Long) {
        prefs.edit().putLong(PREF_KEY_ON_DELAY, value).apply()
    }

    fun setOffDelay(value: Long) {
        prefs.edit().putLong(PREF_KEY_OFF_DELAY, value).apply()
    }
}

class CameraView @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0) :
    CardView(context, attrs, defStyleAttr), Observer<TorchState?>, SeekBar.OnSeekBarChangeListener {

    private val delay: TextView
    private val indicator: View
    private val onDelay: SeekBar
    private val offDelay: SeekBar

    var listener: Listener? = null

    init {
        LayoutInflater.from(context).inflate(R.layout.card_torch, this)
        delay = findViewById(R.id.delay)
        indicator = findViewById(R.id.indicator)
        onDelay = findViewById(R.id.on_delay)
        offDelay = findViewById(R.id.off_delay)
        onDelay.setOnSeekBarChangeListener(this)
        offDelay.setOnSeekBarChangeListener(this)
    }

    override fun onChanged(torchState: TorchState?) {
        val state = torchState ?: return

        val device = state.device
        if (device == null) {
            delay.text = "no camera device available"
            return
        } else {
            delay.text =
                    "set delay: ON - ${device.onDelay}ms; OFF - ${device.offDelay}ms"
        }

        if (device.flash) {
            indicator.setBackgroundColor(Color.RED)
        } else {
            indicator.setBackgroundColor(Color.WHITE)
        }

        onDelay.progress = device.onDelay.toInt()
        offDelay.progress = device.offDelay.toInt()
    }

    override fun onProgressChanged(seekBar: SeekBar, value: Int, fromUser: Boolean) {
        if (!fromUser) {
            return
        }

        when (seekBar.id) {
            R.id.on_delay -> listener?.onOnDelayChanged(value.toLong())
            R.id.off_delay -> listener?.onOffDelayChanged(value.toLong())
        }
    }

    override fun onStartTrackingTouch(seekBar: SeekBar?) {}

    override fun onStopTrackingTouch(seekBar: SeekBar?) {}

    interface Listener {
        fun onOnDelayChanged(value: Long)
        fun onOffDelayChanged(value: Long)
    }
}
