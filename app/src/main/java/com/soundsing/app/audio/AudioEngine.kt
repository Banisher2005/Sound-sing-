package com.soundsing.app.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioPlaybackCaptureConfiguration
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.projection.MediaProjection
import android.os.Build
import android.os.Process
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Sound Sing Low-Latency Capture and Re-routing Engine.
 *
 * Captures USAGE_MEDIA, USAGE_GAME, and USAGE_UNKNOWN via AudioPlaybackCaptureConfiguration.
 * Excludes own UID to prevent feedback loops.
 * Routes audio to a target AudioDeviceInfo using AudioTrack with PERFORMANCE_MODE_LOW_LATENCY.
 */
@RequiresApi(Build.VERSION_CODES.Q)
class AudioEngine(
    private val mediaProjection: MediaProjection
) {
    companion object {
        private const val TAG = "SoundSingEngine"
        const val SAMPLE_RATE = 48000
        const val CHANNEL_CONFIG_IN = AudioFormat.CHANNEL_IN_STEREO
        const val CHANNEL_CONFIG_OUT = AudioFormat.CHANNEL_OUT_STEREO
        const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private const val SILENCE_THRESHOLD_RMS = 0.0015f
        private const val SILENCE_TIMEOUT_MS = 4000L
    }

    private val ringBuffer = AudioRingBuffer(
        sampleRate = SAMPLE_RATE,
        channelCount = 2,
        maxDelayMs = 500
    )

    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null

    private var audioRecord: AudioRecord? = null
    private var audioTrack: AudioTrack? = null

    private val _engineStatus = MutableStateFlow(EngineStatus.IDLE)
    val engineStatus: StateFlow<EngineStatus> = _engineStatus.asStateFlow()

    private val _metrics = MutableStateFlow(AudioMetrics())
    val metrics: StateFlow<AudioMetrics> = _metrics.asStateFlow()

    private val _lastError = MutableStateFlow<EngineError?>(null)
    val lastError: StateFlow<EngineError?> = _lastError.asStateFlow()

    @Volatile
    private var targetOutputDevice: AudioDeviceInfo? = null

    @Volatile
    private var volumeMultiplier: Float = 1.0f

    fun setTargetDevice(device: AudioDeviceInfo?) {
        targetOutputDevice = device
        audioTrack?.let { track ->
            if (device != null) {
                val success = track.setPreferredDevice(device)
                Log.d(TAG, "setPreferredDevice ${device.productName}: $success")
            } else {
                track.setPreferredDevice(null)
            }
        }
    }

    fun setVolume(volume: Float) {
        val clamped = volume.coerceIn(0f, 1.0f)
        volumeMultiplier = clamped
        audioTrack?.setVolume(clamped)
        _metrics.update { it.copy(volume = clamped) }
    }

    fun setDelayMs(delayMs: Int) {
        ringBuffer.setDelayMs(delayMs)
        _metrics.update { it.copy(currentDelayMs = ringBuffer.getDelayMs()) }
    }

    @SuppressLint("MissingPermission")
    fun start(outputDevice: AudioDeviceInfo?): Boolean {
        if (isRunning.get()) {
            Log.w(TAG, "Audio engine already running.")
            return true
        }

        _engineStatus.value = EngineStatus.INITIALIZING
        _lastError.value = null
        targetOutputDevice = outputDevice

        try {
            // 1. Build AudioPlaybackCaptureConfiguration
            val captureConfig = AudioPlaybackCaptureConfiguration.Builder(mediaProjection)
                .addMatchingUsage(AudioAttributes.USAGE_MEDIA)
                .addMatchingUsage(AudioAttributes.USAGE_GAME)
                .addMatchingUsage(AudioAttributes.USAGE_UNKNOWN)
                .excludeUid(Process.myUid()) // CRITICAL: Exclude own UID to prevent feedback loops
                .build()

            // 2. Determine low-latency buffer sizes
            val minRecordBufferSize = AudioRecord.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_IN,
                AUDIO_FORMAT
            )
            val recordBufferSizeInBytes = max(minRecordBufferSize, 2048 * 4)

            val minTrackBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                CHANNEL_CONFIG_OUT,
                AUDIO_FORMAT
            )
            val trackBufferSizeInBytes = max(minTrackBufferSize, 2048 * 4)

            val formatIn = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG_IN)
                .build()

            // 3. Initialize AudioRecord with Playback Capture Config
            val record = AudioRecord.Builder()
                .setAudioFormat(formatIn)
                .setBufferSizeInBytes(recordBufferSizeInBytes)
                .setAudioPlaybackCaptureConfig(captureConfig)
                .build()

            if (record.state != AudioRecord.STATE_INITIALIZED) {
                record.release()
                val err = EngineError.InitFailed("AudioRecord initialization failed (state != INITIALIZED)")
                _lastError.value = err
                _engineStatus.value = EngineStatus.ERROR
                return false
            }

            // 4. Initialize low-latency AudioTrack
            val trackAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()

            val formatOut = AudioFormat.Builder()
                .setEncoding(AUDIO_FORMAT)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL_CONFIG_OUT)
                .build()

            val trackBuilder = AudioTrack.Builder()
                .setAudioAttributes(trackAttributes)
                .setAudioFormat(formatOut)
                .setBufferSizeInBytes(trackBufferSizeInBytes)
                .setTransferMode(AudioTrack.MODE_STREAM)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                trackBuilder.setPerformanceMode(AudioTrack.PERFORMANCE_MODE_LOW_LATENCY)
            }

            val track = trackBuilder.build()

            if (track.state != AudioTrack.STATE_INITIALIZED) {
                record.release()
                track.release()
                val err = EngineError.InitFailed("AudioTrack initialization failed (state != INITIALIZED)")
                _lastError.value = err
                _engineStatus.value = EngineStatus.ERROR
                return false
            }

            // 5. Assign preferred output device if provided
            outputDevice?.let { dev ->
                val ok = track.setPreferredDevice(dev)
                Log.d(TAG, "Initialized track with preferred device: ${dev.productName}, success=$ok")
            }

            track.setVolume(volumeMultiplier)

            this.audioRecord = record
            this.audioTrack = track
            ringBuffer.reset()

            isRunning.set(true)
            _engineStatus.value = EngineStatus.RUNNING

            // 6. Launch high-priority audio processing thread
            workerThread = Thread({
                runAudioProcessingLoop(record, track)
            }, "SoundSing-EngineThread").apply {
                priority = Thread.MAX_PRIORITY
                start()
            }

            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting AudioEngine", e)
            _lastError.value = EngineError.InitFailed(e.message ?: "Unknown error")
            _engineStatus.value = EngineStatus.ERROR
            stop()
            return false
        }
    }

    private fun runAudioProcessingLoop(record: AudioRecord, track: AudioTrack) {
        try {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
        } catch (e: Exception) {
            Log.w(TAG, "Could not set urgent audio thread priority", e)
        }

        try {
            record.startRecording()
            track.play()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start record or track", e)
            _lastError.value = EngineError.InitFailed("Failed to start audio streams: ${e.message}")
            _engineStatus.value = EngineStatus.ERROR
            return
        }

        val chunkShorts = 1024
        val inputBuffer = ShortArray(chunkShorts)
        val delayedBuffer = ShortArray(chunkShorts)

        var totalFrames: Long = 0L
        var silenceStartTimeMs: Long = 0L
        var isSilenceReported = false

        while (isRunning.get()) {
            val shortsRead = record.read(inputBuffer, 0, chunkShorts)

            if (shortsRead > 0) {
                var sumSquares = 0.0
                var peak = 0

                for (i in 0 until shortsRead) {
                    val sample = inputBuffer[i].toInt()
                    sumSquares += sample * sample
                    val absSample = abs(sample)
                    if (absSample > peak) {
                        peak = absSample
                    }
                }

                val meanSquare = sumSquares / shortsRead
                val rms = (sqrt(meanSquare) / 32768.0).toFloat()

                val now = System.currentTimeMillis()
                if (rms < SILENCE_THRESHOLD_RMS) {
                    if (silenceStartTimeMs == 0L) {
                        silenceStartTimeMs = now
                    } else if (now - silenceStartTimeMs > SILENCE_TIMEOUT_MS) {
                        if (!isSilenceReported) {
                            isSilenceReported = true
                            _engineStatus.value = EngineStatus.SILENCE_OR_BLOCKED
                        }
                    }
                } else {
                    silenceStartTimeMs = 0L
                    if (isSilenceReported) {
                        isSilenceReported = false
                        _engineStatus.value = EngineStatus.RUNNING
                    }
                }

                val processedShorts = ringBuffer.process(inputBuffer, delayedBuffer, shortsRead)

                val currentVol = volumeMultiplier
                if (currentVol < 0.99f) {
                    for (i in 0 until processedShorts) {
                        delayedBuffer[i] = (delayedBuffer[i] * currentVol).toInt().toShort()
                    }
                }

                var writtenShorts = 0
                while (writtenShorts < processedShorts && isRunning.get()) {
                    val written = track.write(
                        delayedBuffer,
                        writtenShorts,
                        processedShorts - writtenShorts,
                        AudioTrack.WRITE_BLOCKING
                    )
                    if (written < 0) {
                        Log.e(TAG, "AudioTrack.write error code: $written")
                        break
                    }
                    writtenShorts += written
                }

                totalFrames += (shortsRead / 2)

                if (totalFrames % 2400 == 0L) {
                    val silenceDurationSec = if (silenceStartTimeMs > 0) {
                        (now - silenceStartTimeMs) / 1000f
                    } else 0f

                    _metrics.update {
                        it.copy(
                            rmsLevel = rms.coerceIn(0f, 1f),
                            peakAmplitude = peak,
                            framesProcessed = totalFrames,
                            silenceSeconds = silenceDurationSec,
                            isSilenceDetected = isSilenceReported
                        )
                    }
                }
            } else if (shortsRead < 0) {
                Log.e(TAG, "AudioRecord.read error: $shortsRead")
                if (isRunning.get()) {
                    _lastError.value = EngineError.CaptureFailed(shortsRead)
                }
                break
            }
        }

        try {
            record.stop()
            track.stop()
        } catch (e: Exception) {
            Log.w(TAG, "Error stopping record or track", e)
        }
    }

    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }

        try {
            workerThread?.interrupt()
            workerThread?.join(500)
        } catch (e: Exception) {
            Log.w(TAG, "Interrupted while joining worker thread", e)
        } finally {
            workerThread = null
        }

        try {
            audioRecord?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioRecord", e)
        } finally {
            audioRecord = null
        }

        try {
            audioTrack?.release()
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing AudioTrack", e)
        } finally {
            audioTrack = null
        }

        ringBuffer.reset()
        _engineStatus.value = EngineStatus.IDLE
        _metrics.update { AudioMetrics() }
        Log.d(TAG, "AudioEngine cleanly stopped.")
    }
}
