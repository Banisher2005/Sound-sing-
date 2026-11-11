package com.soundsing.app.audio

/**
 * High-level status of the Sound Sing re-routing engine.
 */
enum class EngineStatus {
    IDLE,
    INITIALIZING,
    RUNNING,
    SILENCE_OR_BLOCKED,
    ERROR
}

/**
 * Diagnostics and real-time audio metrics.
 */
data class AudioMetrics(
    val rmsLevel: Float = 0f,            // 0.0 to 1.0 normalized
    val peakAmplitude: Int = 0,          // 0 to 32767
    val framesProcessed: Long = 0L,
    val silenceSeconds: Float = 0f,
    val isSilenceDetected: Boolean = false,
    val sampleRate: Int = 48000,
    val currentDelayMs: Int = 0,
    val volume: Float = 1.0f
)

/**
 * Specific failure reasons for the audio engine.
 */
sealed class EngineError(val message: String) {
    data class InitFailed(val reason: String) : EngineError("Failed to initialize audio subsystem: $reason")
    data class DeviceDisconnected(val deviceName: String) : EngineError("Output device disconnected: $deviceName")
    object ProjectionRevoked : EngineError("MediaProjection capture permission was revoked by the user or system.")
    data class CaptureFailed(val code: Int) : EngineError("AudioRecord capture failed with error code $code")
    data class TrackError(val reason: String) : EngineError("AudioTrack playback error: $reason")
}
