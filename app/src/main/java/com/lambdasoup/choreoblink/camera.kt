package com.lambdasoup.choreoblink

import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.Observer
import android.content.Context
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraManager
import android.support.v7.widget.CardView
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.TextView
import java.util.concurrent.ScheduledThreadPoolExecutor

class CameraRepository(context: Context) {

    private val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager

    val devices = MutableLiveData<List<Device>>()

    init {
        try {
            val ids = manager.cameraIdList
            devices.postValue(ids.map { id ->
                val characteristics = manager.getCameraCharacteristics(id)
                Device(id, 15, 15)
            })
        } catch (e: CameraAccessException) {
            throw RuntimeException(e)
        }
    }

}

data class Device(val id: String, val onDelay: Long, val offDelay: Long)

class CameraView @JvmOverloads constructor(context: Context,
                                           attrs: AttributeSet? = null,
                                           defStyleAttr: Int = 0)
    : CardView(context, attrs, defStyleAttr), Observer<Device?> {

    override fun onChanged(device: Device?) {
        if (device == null) {
            delay.text = "no camera device available"
        } else {
            delay.text = "set delay: ON - ${device.onDelay}ms; OFF - ${device.onDelay}ms"
        }
    }

    private val delay: TextView

    init {
        LayoutInflater.from(context).inflate(R.layout.card_camera, this)
        delay = findViewById(R.id.delay)
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