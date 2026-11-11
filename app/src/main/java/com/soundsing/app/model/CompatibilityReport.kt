package com.soundsing.app.model

/**
 * Audit information regarding the host device's OS version, hardware features,
 * and audio subsystem capabilities.
 */
data class CompatibilityReport(
    val androidVersion: String,
    val apiLevel: Int,
    val manufacturer: String,
    val model: String,
    val isPlaybackCaptureSupported: Boolean,
    val isLeAudioSupported: Boolean,
    val isLeAudioBroadcastSourceSupported: Boolean,
    val isLeAudioBroadcastAssistantSupported: Boolean,
    val isSamsungDevice: Boolean,
    val samsungDualAudioNote: String,
    val bluetoothEnabled: Boolean
)
