package com.soundsing.app.viewmodel

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.soundsing.app.audio.AudioDeviceManager
import com.soundsing.app.audio.AudioMetrics
import com.soundsing.app.audio.EngineStatus
import com.soundsing.app.model.AudioOutputDevice
import com.soundsing.app.model.CompatibilityReport
import com.soundsing.app.service.AudioRouteService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SoundSingUiState(
    val engineStatus: EngineStatus = EngineStatus.IDLE,
    val metrics: AudioMetrics = AudioMetrics(),
    val availableDevices: List<AudioOutputDevice> = emptyList(),
    val selectedOutputDevice: AudioOutputDevice? = null,
    val volume: Float = 1.0f,
    val delayMs: Int = 0,
    val compatibilityReport: CompatibilityReport? = null,
    val isServiceRunning: Boolean = false,
    val errorMessage: String? = null
)

class SoundSingViewModel(application: Application) : AndroidViewModel(application) {

    private val audioDeviceManager = AudioDeviceManager(application)
    private var boundService: AudioRouteService? = null
    private var isBound = false

    private val _uiState = MutableStateFlow(SoundSingUiState())
    val uiState: StateFlow<SoundSingUiState> = _uiState.asStateFlow()

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as? AudioRouteService.LocalBinder
            val s = binder?.getService()
            boundService = s
            isBound = true

            _uiState.update { it.copy(isServiceRunning = true) }

            val engine = binder?.getAudioEngine()
            if (engine != null) {
                viewModelScope.launch {
                    engine.engineStatus.collectLatest { status ->
                        _uiState.update { it.copy(engineStatus = status) }
                    }
                }
                viewModelScope.launch {
                    engine.metrics.collectLatest { metrics ->
                        _uiState.update { it.copy(metrics = metrics) }
                    }
                }
                viewModelScope.launch {
                    engine.lastError.collectLatest { error ->
                        if (error != null) {
                            _uiState.update { it.copy(errorMessage = error.message) }
                        }
                    }
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundService = null
            isBound = false
            _uiState.update {
                it.copy(
                    isServiceRunning = false,
                    engineStatus = EngineStatus.IDLE
                )
            }
        }
    }

    init {
        audioDeviceManager.startListening()
        loadCompatibilityReport()

        viewModelScope.launch {
            audioDeviceManager.availableDevices.collectLatest { devices ->
                _uiState.update { state ->
                    val updatedSelection = if (state.selectedOutputDevice != null) {
                        devices.find { it.id == state.selectedOutputDevice.id }
                    } else {
                        devices.find { it.category == AudioOutputDevice.DeviceCategory.BLUETOOTH }
                            ?: devices.find { it.category == AudioOutputDevice.DeviceCategory.USB }
                            ?: devices.find { it.category == AudioOutputDevice.DeviceCategory.WIRED }
                            ?: devices.firstOrNull()
                    }
                    state.copy(
                        availableDevices = devices,
                        selectedOutputDevice = updatedSelection
                    )
                }
            }
        }

        viewModelScope.launch {
            audioDeviceManager.deviceDisconnectEvents.collectLatest { disconnectedDevice ->
                if (_uiState.value.selectedOutputDevice?.id == disconnectedDevice.id) {
                    _uiState.update {
                        it.copy(
                            errorMessage = "Active output device '${disconnectedDevice.name}' disconnected."
                        )
                    }
                }
            }
        }
    }

    fun bindService() {
        if (!isBound) {
            val intent = Intent(getApplication(), AudioRouteService::class.java)
            getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService() {
        if (isBound) {
            try {
                getApplication<Application>().unbindService(serviceConnection)
            } catch (e: Exception) {
                // Ignore if already unbound
            }
            isBound = false
            boundService = null
        }
    }

    fun selectDevice(device: AudioOutputDevice) {
        _uiState.update { it.copy(selectedOutputDevice = device) }
        val rawInfo = audioDeviceManager.getRawDeviceInfo(device.id)
        boundService?.updateTargetDevice(rawInfo)
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1f)
        _uiState.update { it.copy(volume = clamped) }
        boundService?.setVolume(clamped)
    }

    fun setDelayMs(delayMs: Int) {
        val clamped = delayMs.coerceIn(0, 500)
        _uiState.update { it.copy(delayMs = clamped) }
        boundService?.setDelayMs(clamped)
    }

    fun refreshDevices() {
        audioDeviceManager.refreshDevices()
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun loadCompatibilityReport() {
        val report = audioDeviceManager.getCompatibilityReport()
        _uiState.update { it.copy(compatibilityReport = report) }
    }

    fun stopRouting() {
        val intent = Intent(getApplication(), AudioRouteService::class.java).apply {
            action = AudioRouteService.ACTION_STOP
        }
        getApplication<Application>().startService(intent)
        _uiState.update {
            it.copy(
                isServiceRunning = false,
                engineStatus = EngineStatus.IDLE
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        audioDeviceManager.stopListening()
        unbindService()
    }
}
