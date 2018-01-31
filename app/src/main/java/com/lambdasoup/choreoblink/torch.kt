package com.lambdasoup.choreoblink

import android.arch.lifecycle.MediatorLiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.graphics.Color
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
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

}

data class TorchState(val device: Device?, val choreo: Choreo?, val delta: Long?)

data class Device(val id: String, val onDelay: Long, val offDelay: Long, val flash: Boolean)


class TorchLiveData(context: Context) : MediatorLiveData<TorchState>() {

    private val scheduler: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val delta = MutableLiveData<Long>()
    val choreo = MutableLiveData<Choreo>()

    init {
        try {
            val ids = manager.cameraIdList
            value = TorchState(Device(ids[0], 20, 20, false), null, 0L)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }

        addSource(delta) { value = value?.copy(delta = it) }
        addSource(choreo) { value = value?.copy(choreo = it) }
    }

    var torch = false
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

        val on = choreo.on(time, device.onDelay, device.offDelay)

        if (on && !torch) {
            manager.setTorchMode(device.id, true)
            Log.d("torch", "turning on")
            torch = true
            postValue(state.copy(device = device.copy(flash = true)))
        }

        if (!on && torch) {
            manager.setTorchMode(device.id, false)
            Log.d("torch", "turning off")
            torch = false
            postValue(state.copy(device = device.copy(flash = false)))
        }
    }

    private val turnOff = Runnable {
        val state = value ?: return@Runnable
        val device = state.device ?: return@Runnable
        manager.setTorchMode(device.id, false)
    }

}

class CameraView @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0) :
    CardView(context, attrs, defStyleAttr), Observer<TorchState?> {

    override fun onChanged(torchState: TorchState?) {
        val state = torchState ?: return

        val device = state.device
        if (device == null) {
            delay.text = "no camera device available"
            return
        } else {
            delay.text =
                    "set delay: ON - ${device.onDelay}ms; OFF - ${device.onDelay}ms"
        }

        if (device.flash) {
            indicator.setBackgroundColor(Color.RED)
        } else {
            indicator.setBackgroundColor(Color.WHITE)
        }
    }

    private val delay: TextView
    private val indicator: View

    init {
        LayoutInflater.from(context).inflate(R.layout.card_torch, this)
        delay = findViewById(R.id.delay)
        indicator = findViewById(R.id.indicator)
    }

}
