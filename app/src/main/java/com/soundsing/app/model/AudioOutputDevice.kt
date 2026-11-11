package com.soundsing.app.model

import android.media.AudioDeviceInfo
import android.os.Build

/**
 * Representation of an audio output device available on the system.
 */
data class AudioOutputDevice(
    val id: Int,
    val name: String,
    val type: Int,
    val typeName: String,
    val category: DeviceCategory,
    val address: String = "",
    val isDefault: Boolean = false
) {
    enum class DeviceCategory {
        BLUETOOTH,
        WIRED,
        USB,
        BUILTIN_SPEAKER,
        OTHER
    }

    companion object {
        fun fromAudioDeviceInfo(info: AudioDeviceInfo, isDefault: Boolean = false): AudioOutputDevice {
            val (name, category, typeName) = parseDeviceInfo(info)
            return AudioOutputDevice(
                id = info.id,
                name = name,
                type = info.type,
                typeName = typeName,
                category = category,
                address = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) info.address else "",
                isDefault = isDefault
            )
        }

        private fun parseDeviceInfo(info: AudioDeviceInfo): Triple<String, DeviceCategory, String> {
            val productName = info.productName.toString().trim()
            val type = info.type

            return when (type) {
                AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "Bluetooth Audio (A2DP)",
                        DeviceCategory.BLUETOOTH,
                        "Bluetooth A2DP"
                    )
                }
                AudioDeviceInfo.TYPE_BLE_HEADSET -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "BLE Headset",
                        DeviceCategory.BLUETOOTH,
                        "Bluetooth LE Audio Headset"
                    )
                }
                AudioDeviceInfo.TYPE_BLE_SPEAKER -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "BLE Speaker",
                        DeviceCategory.BLUETOOTH,
                        "Bluetooth LE Audio Speaker"
                    )
                }
                AudioDeviceInfo.TYPE_BLE_BROADCAST -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "BLE Broadcast / Auracast",
                        DeviceCategory.BLUETOOTH,
                        "Auracast Broadcast"
                    )
                }
                AudioDeviceInfo.TYPE_HEARING_AID -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "Hearing Aid",
                        DeviceCategory.BLUETOOTH,
                        "Hearing Aid (ASHA/BLE)"
                    )
                }
                AudioDeviceInfo.TYPE_WIRED_HEADPHONES -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "Wired Headphones",
                        DeviceCategory.WIRED,
                        "3.5mm Wired Headphones"
                    )
                }
                AudioDeviceInfo.TYPE_WIRED_HEADSET -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "Wired Headset",
                        DeviceCategory.WIRED,
                        "3.5mm Wired Headset"
                    )
                }
                AudioDeviceInfo.TYPE_USB_DEVICE,
                AudioDeviceInfo.TYPE_USB_HEADSET,
                AudioDeviceInfo.TYPE_USB_ACCESSORY -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "USB Audio Device",
                        DeviceCategory.USB,
                        "USB-C Audio / DAC"
                    )
                }
                AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> {
                    Triple(
                        "Phone Built-in Speaker",
                        DeviceCategory.BUILTIN_SPEAKER,
                        "Internal Speaker"
                    )
                }
                AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> {
                    Triple(
                        "Phone Earpiece",
                        DeviceCategory.BUILTIN_SPEAKER,
                        "Internal Earpiece"
                    )
                }
                else -> {
                    Triple(
                        if (productName.isNotEmpty()) productName else "Audio Output ($type)",
                        DeviceCategory.OTHER,
                        "Output Type $type"
                    )
                }
            }
        }
    }
}
