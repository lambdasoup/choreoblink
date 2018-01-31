package com.lambdasoup.choreoblink

import android.arch.lifecycle.*
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

class TorchManager(context: Context) : LifecycleObserver {

    private val scheduler: ScheduledThreadPoolExecutor = ScheduledThreadPoolExecutor(1)
    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val state = TorchLiveData()

    var torch = false
    var future: ScheduledFuture<*>? = null

    init {
        try {
            val ids = manager.cameraIdList
            state.value = TorchState(Device(ids[0], 15, 15, false), null, 0L)
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun startScheduler() {
        future = scheduler.scheduleWithFixedDelay(ticker, 0, 1, TimeUnit.MILLISECONDS)
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun stopScheduler() {
        future!!.cancel(true)
        scheduler.execute(turnOff)
    }

    private val ticker = Runnable {
        val state = state.value ?: return@Runnable

        val choreo = state.choreo ?: return@Runnable
        val delta = state.delta ?: return@Runnable
        val device = state.device ?: return@Runnable

        val time = System.currentTimeMillis() + state.delta

        val on: Boolean
        on = time % 2000 > 1000

        if (on && !torch) {
            manager.setTorchMode(device.id, true)
            Log.d("torch", "turning on")
            torch = true
            this.state.postValue(state.copy(device = device.copy(flash = true)))
        }

        if (!on && torch) {
            manager.setTorchMode(device.id, false)
            Log.d("torch", "turning off")
            torch = false
            this.state.postValue(state.copy(device = device.copy(flash = false)))
        }
    }

    private val turnOff = Runnable {
        val state = state.value ?: return@Runnable
        val device = state.device ?: return@Runnable
        manager.setTorchMode(device.id, false)
    }

    fun updateTimeDelta(delta: Long) {
        state.delta.value = delta
    }

    fun setChoreo(choreo: Choreo) {
        state.choreo.value = choreo
    }

}

data class TorchState(val device: Device?, val choreo: Choreo?, val delta: Long?)

data class Device(val id: String, val onDelay: Long, val offDelay: Long, val flash: Boolean)


class TorchLiveData : MediatorLiveData<TorchState>() {

    val delta = MutableLiveData<Long>()
    val choreo = MutableLiveData<Choreo>()

    init {
        addSource(delta) { value = value?.copy(delta = it) }
        addSource(choreo) { value = value?.copy(choreo = it) }
    }

}

class CameraView @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0) :
    CardView(context, attrs, defStyleAttr), Observer<TorchState?> {

    override fun onChanged(torchState: TorchState?) {
        val state = torchState ?: return
        val device = state.device ?: return

        if (device == null) {
            delay.text = "no camera device available"
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

class Torch {

    private val executor = ScheduledThreadPoolExecutor(1)

    var on = false

    internal fun on() {
        if (on) {
            return
        }
        on = true
        executor.execute {
            //            try {
//                cameraManager!!.setTorchMode(cameraId!!, true)
//            } catch (e: CameraAccessException) {
//                e.printStackTrace()
//            }
        }
    }

    internal fun off() {
        if (!on) {
            return
        }
        on = false
//        executor.execute {
//            try {
//                cameraManager!!.setTorchMode(cameraId!!, false)
//            } catch (e: CameraAccessException) {
//                e.printStackTrace()
//            }
//        }
    }

}