package com.soundsing.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioDeviceInfo
import android.media.projection.MediaProjection
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.soundsing.app.MainActivity
import com.soundsing.app.R
import com.soundsing.app.audio.AudioDeviceManager
import com.soundsing.app.audio.AudioEngine
import com.soundsing.app.audio.EngineStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Foreground Service hosting the AudioEngine and MediaProjection capture session.
 * Marked with foregroundServiceType = mediaProjection.
 */
class AudioRouteService : Service() {

    companion object {
        private const val TAG = "AudioRouteService"
        private const val NOTIFICATION_ID = 4040
        const val ACTION_START = "com.soundsing.app.action.START"
        const val ACTION_STOP = "com.soundsing.app.action.STOP"
        const val EXTRA_TARGET_DEVICE_ID = "extra_target_device_id"

        @Volatile
        var activeMediaProjection: MediaProjection? = null
    }

    private val binder = LocalBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private var audioEngine: AudioEngine? = null
    private var audioDeviceManager: AudioDeviceManager? = null

    private var targetDeviceId: Int = -1
    private var currentTargetDevice: AudioDeviceInfo? = null

    inner class LocalBinder : Binder() {
        fun getService(): AudioRouteService = this@AudioRouteService
        fun getAudioEngine(): AudioEngine? = audioEngine
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        audioDeviceManager = AudioDeviceManager(this)
        audioDeviceManager?.startListening()

        createNotificationChannel()

        serviceScope.launch {
            audioDeviceManager?.deviceDisconnectEvents?.collectLatest { disconnectedDevice ->
                if (disconnectedDevice.id == targetDeviceId) {
                    Log.w(TAG, "Active routed device disconnected: ${disconnectedDevice.name}")
                    audioEngine?.stop()
                    updateNotification(
                        title = "Audio Output Disconnected",
                        content = "${disconnectedDevice.name} was disconnected. Routing stopped."
                    )
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action

        if (action == ACTION_STOP) {
            Log.d(TAG, "Received ACTION_STOP intent. Stopping service.")
            stopAudioRouting()
            stopSelf()
            return START_NOT_STICKY
        }

        if (action == ACTION_START) {
            targetDeviceId = intent.getIntExtra(EXTRA_TARGET_DEVICE_ID, -1)
            currentTargetDevice = if (targetDeviceId != -1) {
                audioDeviceManager?.getRawDeviceInfo(targetDeviceId)
            } else null

            startForegroundServiceWithNotification()
            startAudioEngine()
        }

        return START_NOT_STICKY
    }

    private fun startForegroundServiceWithNotification() {
        val notification = buildNotification(
            title = getString(R.string.notification_title_active),
            content = currentTargetDevice?.let { "Routing audio to ${it.productName}" }
                ?: getString(R.string.notification_desc_active)
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun startAudioEngine() {
        val projection = activeMediaProjection
        if (projection == null) {
            Log.e(TAG, "MediaProjection is null. Cannot start capture.")
            stopSelf()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            projection.registerCallback(object : MediaProjection.Callback() {
                override fun onStop() {
                    Log.w(TAG, "MediaProjection session stopped / revoked by system.")
                    stopAudioRouting()
                    stopSelf()
                }
            }, null)

            val engine = AudioEngine(projection)
            audioEngine = engine

            serviceScope.launch {
                engine.engineStatus.collectLatest { status ->
                    when (status) {
                        EngineStatus.RUNNING -> {
                            updateNotification(
                                title = getString(R.string.notification_title_active),
                                content = currentTargetDevice?.let { "Streaming audio to ${it.productName}" }
                                    ?: "Routing media audio to second output"
                            )
                        }
                        EngineStatus.SILENCE_OR_BLOCKED -> {
                            updateNotification(
                                title = "Audio Silent or Capture Blocked",
                                content = "Media may be paused or current app blocked capture."
                            )
                        }
                        EngineStatus.ERROR -> {
                            updateNotification(
                                title = "Routing Error",
                                content = "Encountered an audio route error."
                            )
                        }
                        else -> Unit
                    }
                }
            }

            val started = engine.start(currentTargetDevice)
            if (!started) {
                Log.e(TAG, "Failed to start AudioEngine.")
            }
        }
    }

    fun updateTargetDevice(device: AudioDeviceInfo?) {
        currentTargetDevice = device
        targetDeviceId = device?.id ?: -1
        audioEngine?.setTargetDevice(device)
        updateNotification(
            title = getString(R.string.notification_title_active),
            content = device?.let { "Routing audio to ${it.productName}" }
                ?: getString(R.string.notification_desc_active)
        )
    }

    fun setVolume(volume: Float) {
        audioEngine?.setVolume(volume)
    }

    fun setDelayMs(delayMs: Int) {
        audioEngine?.setDelayMs(delayMs)
    }

    private fun stopAudioRouting() {
        audioEngine?.stop()
        audioEngine = null
        try {
            activeMediaProjection?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping activeMediaProjection", e)
        }
        activeMediaProjection = null
    }

    private fun buildNotification(title: String, content: String): Notification {
        val channelId = getString(R.string.notification_channel_id)

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, AudioRouteService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_stat_audio)
            .setContentTitle(title)
            .setContentText(content)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .addAction(
                android.R.drawable.ic_media_pause,
                getString(R.string.action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun updateNotification(title: String, content: String) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = buildNotification(title, content)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.notification_channel_id),
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.notification_channel_desc)
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopAudioRouting()
        audioDeviceManager?.stopListening()
        serviceScope.cancel()
        Log.d(TAG, "AudioRouteService destroyed.")
    }
}
