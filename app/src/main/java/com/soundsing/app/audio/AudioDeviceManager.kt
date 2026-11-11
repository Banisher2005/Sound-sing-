package com.soundsing.app.audio

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.os.Build
import android.util.Log
import com.soundsing.app.model.AudioOutputDevice
import com.soundsing.app.model.CompatibilityReport
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Manages audio output device enumeration, hotplug events (connect/disconnect),
 * Bluetooth adapter state, and hardware compatibility checks.
 */
class AudioDeviceManager(
    private val context: Context
) {
    companion object {
        private const val TAG = "AudioDeviceManager"
    }

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = bluetoothManager?.adapter

    private val _availableDevices = MutableStateFlow<List<AudioOutputDevice>>(emptyList())
    val availableDevices: StateFlow<List<AudioOutputDevice>> = _availableDevices.asStateFlow()

    private val _deviceDisconnectEvents = MutableSharedFlow<AudioOutputDevice>()
    val deviceDisconnectEvents: SharedFlow<AudioOutputDevice> = _deviceDisconnectEvents.asSharedFlow()

    private val _bluetoothStateFlow = MutableStateFlow(bluetoothAdapter?.isEnabled ?: false)
    val bluetoothStateFlow: StateFlow<Boolean> = _bluetoothStateFlow.asStateFlow()

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices added: ${addedDevices?.size}")
            refreshDevices()
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>?) {
            Log.d(TAG, "Audio devices removed: ${removedDevices?.size}")
            removedDevices?.forEach { info ->
                val device = AudioOutputDevice.fromAudioDeviceInfo(info)
                _deviceDisconnectEvents.tryEmit(device)
            }
            refreshDevices()
        }
    }

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                val isEnabled = state == BluetoothAdapter.STATE_ON
                _bluetoothStateFlow.value = isEnabled
                refreshDevices()
            }
        }
    }

    fun startListening() {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(bluetoothReceiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(bluetoothReceiver, filter)
        }
        refreshDevices()
    }

    fun stopListening() {
        try {
            audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering AudioDeviceCallback", e)
        }
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            Log.w(TAG, "Error unregistering Bluetooth receiver", e)
        }
    }

    /**
     * Enumerate all active audio output devices.
     */
    fun refreshDevices() {
        val rawDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        val deviceList = rawDevices.map { info ->
            AudioOutputDevice.fromAudioDeviceInfo(info)
        }
        _availableDevices.value = deviceList
    }

    /**
     * Resolve raw AudioDeviceInfo from our model device id.
     */
    fun getRawDeviceInfo(deviceId: Int): AudioDeviceInfo? {
        val rawDevices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return rawDevices.find { it.id == deviceId }
    }

    /**
     * Generate an in-depth hardware and OS compatibility report.
     */
    @SuppressLint("MissingPermission")
    fun getCompatibilityReport(): CompatibilityReport {
        val isPlaybackCapture = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        var leAudioSupported = false
        var auracastSourceSupported = false
        var auracastAssistantSupported = false

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && bluetoothAdapter != null) {
            try {
                leAudioSupported = bluetoothAdapter.isLeAudioSupported == BluetoothStatusCodes.FEATURE_SUPPORTED
                auracastSourceSupported = bluetoothAdapter.isLeAudioBroadcastSourceSupported == BluetoothStatusCodes.FEATURE_SUPPORTED
                auracastAssistantSupported = bluetoothAdapter.isLeAudioBroadcastAssistantSupported == BluetoothStatusCodes.FEATURE_SUPPORTED
            } catch (e: SecurityException) {
                Log.w(TAG, "Bluetooth permissions not granted during LE check: ${e.message}")
            } catch (e: Throwable) {
                Log.w(TAG, "LE Audio query not supported by chipset: ${e.message}")
            }
        }

        val manufacturer = Build.MANUFACTURER.orEmpty()
        val isSamsung = manufacturer.equals("samsung", ignoreCase = true)
        val samsungNote = if (isSamsung) {
            "Samsung devices feature native 'Dual Audio' in Bluetooth Media Output settings for 2 Bluetooth devices. Sound Sing extends this capability across mixed outputs (e.g. Phone Speaker + Bluetooth, or 3.5mm/USB + Bluetooth)."
        } else {
            "Standard Android devices generally route audio exclusively to the last connected device. Sound Sing uses AudioPlaybackCapture and AudioTrack routing to bridge audio to a second output simultaneously."
        }

        return CompatibilityReport(
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            apiLevel = Build.VERSION.SDK_INT,
            manufacturer = manufacturer.replaceFirstChar { it.uppercase() },
            model = Build.MODEL.orEmpty(),
            isPlaybackCaptureSupported = isPlaybackCapture,
            isLeAudioSupported = leAudioSupported,
            isLeAudioBroadcastSourceSupported = auracastSourceSupported,
            isLeAudioBroadcastAssistantSupported = auracastAssistantSupported,
            isSamsungDevice = isSamsung,
            samsungDualAudioNote = samsungNote,
            bluetoothEnabled = bluetoothAdapter?.isEnabled ?: false
        )
    }
}

private object BluetoothStatusCodes {
    const val FEATURE_SUPPORTED = 10
}
