package com.lambdasoup.choreoblink

import android.arch.core.executor.testing.InstantTaskExecutorRule
import android.arch.lifecycle.MutableLiveData
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.reset
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestRule


@Suppress("IllegalIdentifier")
class MainViewModelTest {

    @Rule
    @JvmField
    var rule: TestRule = InstantTaskExecutorRule()

    private val torchManager: TorchManager = mock()
    private val choreoRepository: ChoreoRepository = mock()
    private val timeSource: TimeSource = mock()
    private val app: ChoreoBlink = mock()

    @Before
    fun setup() {
        reset(app, torchManager, choreoRepository, timeSource)

        whenever(app.getService(TorchManager::class.java)).thenReturn(torchManager)
        whenever(app.getService(ChoreoRepository::class.java)).thenReturn(choreoRepository)
        whenever(app.getService(TimeSource::class.java)).thenReturn(timeSource)
    }

    @Test
    fun `should update torch and model with real time`() {
        val timeSyncState = MutableLiveData<TimeSyncState>()
        whenever(timeSource.state).thenReturn(timeSyncState)
        val vm = MainViewModel(app)
        val time = TimeSyncState.Synced(123L)

        timeSyncState.postValue(time)

        verify(torchManager).updateTimeDelta(time.delta)
        assertEquals(time, vm.time.value)
    }

}