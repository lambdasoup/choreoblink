package com.lambdasoup.choreoblink

import android.app.Application

class ChoreoBlink : Application() {

    private val torchManager: TorchManager by lazy { TorchManager(this) }
    private val choreoRepository: ChoreoRepository by lazy { ChoreoRepository() }
    private val timeSource: TimeSource by lazy { TimeSource(this) }

    fun <T> getService(t: Class<T>): T {
        val service: Any = when (t) {

            TorchManager::class.java -> torchManager
            TimeSource::class.java -> timeSource
            ChoreoRepository::class.java -> choreoRepository

            else -> throw IllegalArgumentException("no service with type $t")
        }
        @Suppress("UNCHECKED_CAST")
        return service as T
    }
}

fun <T> Application.getService(t: Class<T>): T = (this as ChoreoBlink).getService(t)

