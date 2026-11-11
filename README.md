# Sound Sing 🎧🔀🔊

> **Simultaneous Dual Audio Output Engine for Android**  
> *Built with Kotlin, Jetpack Compose (Material 3), minSdk 29 (Android 10), targetSdk 35 (Android 15).*

Sound Sing is an open-source, 100% on-device Android utility inspired in spirit by **VoiceMeeter** on Windows. It captures system audio from apps (Chrome, YouTube Music, podcasts, video games, emulators) using Android's official `AudioPlaybackCapture` API and re-routes a duplicate, low-latency copy to a user-chosen secondary output device (Bluetooth A2DP, Bluetooth LE Audio, 3.5mm wired headphones, or USB-C DAC/speaker) while the phone's primary output continues playing the original audio stream.

---

## 📸 Core Features

1. **System Audio Capture & Re-Route**
   - Uses `MediaProjectionManager` + `AudioPlaybackCaptureConfiguration` to capture `USAGE_MEDIA`, `USAGE_GAME`, and `USAGE_UNKNOWN`.
   - **Feedback Loop Prevention**: Explicitly excludes the app's own process UID via `excludeUid(Process.myUid())`.
   - **Excludes Phone Calls**: Phone calls and voice communications (`USAGE_VOICE_COMMUNICATION`) are never captured or routed by OS design.
   - Runs in an active Foreground Service (`foregroundServiceType = "mediaProjection"`) with a persistent system notification and 1-tap **Stop** action.
   - Low-latency playback via `AudioTrack` configured with `PERFORMANCE_MODE_LOW_LATENCY`, small chunks (512 stereo frames / ~10.6ms), and 48 kHz 16-bit PCM stereo.

2. **Output Device Picker**
   - Automatically detects and enumerates output hardware via `AudioManager.getDevices(GET_DEVICES_OUTPUTS)`:
     - Bluetooth A2DP & Bluetooth LE Audio (LC3 / Auracast)
     - 3.5mm Wired Headsets & Headphones
     - USB-C Audio DACs & Accessories
     - Phone Internal Speaker
   - Dynamic hotplug detection using `AudioDeviceCallback` and `BluetoothAdapter` broadcast receivers.

3. **Per-Output Controls & Delay Compensation**
   - **Volume Slider**: Real-time linear software & hardware gain scaling (0% to 100%).
   - **Ring Buffer Delay Slider (0 to 500 ms)**: High-performance circular buffer (`AudioRingBuffer`) allows adding microsecond-accurate delay to the duplicate copy to help line up the secondary output with the phone's primary output.

4. **Unsupported App & Silence Detection**
   - Continuous real-time RMS (Root Mean Square) signal level and peak amplitude monitoring.
   - When silence or DRM capture refusal is detected for >4 seconds, displays an alert:  
     *"This app blocks audio capture (common with Netflix, Prime Video, Spotify, Disney+). Android apps can opt out via allowAudioPlaybackCapture=false or DRM copy-protection."*

5. **Hardware Compatibility Checker**
   - Real-time audit of Android OS version, Brand/Model, and Bluetooth chipset features:
     - `isPlaybackCaptureSupported` (Android 10+ / API 29+)
     - `isLeAudioSupported` (Bluetooth LE Audio / LC3 codec)
     - `isLeAudioBroadcastSourceSupported` & `isLeAudioBroadcastAssistantSupported` (Auracast)
     - Explanatory note on Samsung OneUI's native **Dual Audio** feature.

6. **Interactive Guide & Tips**
   - Step-by-step onboarding walkthrough.
   - Latency reduction best practices (Battery Unrestricted, Bluetooth low-latency codecs).
   - Hardware fallback recommendation: External 2-in-1 Bluetooth transmitter for 0ms dual-listener sharing.

---

## 🔒 Privacy & Permissions

- **NO INTERNET PERMISSION**: `android.permission.INTERNET` is **NOT** present in `AndroidManifest.xml`.
- **Zero Data Collection**: No telemetry, analytics, ad libraries, or external network requests. Everything operates strictly in local memory.
- **Required Permissions**:
  - `android.permission.RECORD_AUDIO`: Required to instantiate `AudioRecord` for `AudioPlaybackCapture`.
  - `android.permission.BLUETOOTH_CONNECT` (Android 12+): Required to query connected Bluetooth devices and capabilities.
  - `android.permission.FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_MEDIA_PROJECTION` (Android 14+): Required to keep the audio engine alive in the background.
  - `android.permission.POST_NOTIFICATIONS` (Android 13+): Required for foreground service notification and quick-stop control.
  - **MediaProjection Consent**: User accepts the standard Android OS capture prompt ("Sound Sing will start capturing...").

---

## 📁 Project Folder Structure

```
Sound-sing-/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/soundsing/app/
│   │   │   │   ├── audio/
│   │   │   │   │   ├── AudioDeviceManager.kt   # Output enumeration, hotplug callbacks, compat audit
│   │   │   │   │   ├── AudioEngine.kt          # PlaybackCapture, AudioRecord & AudioTrack low-latency engine
│   │   │   │   │   ├── AudioRingBuffer.kt      # Thread-safe circular buffer for 0-500ms delay
│   │   │   │   │   └── AudioState.kt           # EngineStatus, AudioMetrics, EngineError
│   │   │   │   ├── model/
│   │   │   │   │   ├── AudioOutputDevice.kt    # Device category & model
│   │   │   │   │   └── CompatibilityReport.kt  # Hardware compatibility report
│   │   │   │   ├── service/
│   │   │   │   │   └── AudioRouteService.kt    # mediaProjection ForegroundService & notification
│   │   │   │   ├── ui/
│   │   │   │   │   ├── components/
│   │   │   │   │   │   ├── AudioMeter.kt       # Real-time multi-band visualizer & RMS bar
│   │   │   │   │   │   ├── DeviceCard.kt       # Device selection card with status badge
│   │   │   │   │   │   └── PermissionHandler.kt# Multi-permission flow & rationale dialogs
│   │   │   │   │   ├── navigation/
│   │   │   │   │   │   ├── NavGraph.kt         # Jetpack Compose Navigation graph
│   │   │   │   │   │   └── Screen.kt           # Screen routes & icons
│   │   │   │   │   ├── screens/
│   │   │   │   │   │   ├── CompatibilityScreen.kt # Hardware & LE Audio diagnostics
│   │   │   │   │   │   ├── DevicesScreen.kt    # Output picker grouped by type
│   │   │   │   │   │   ├── GuideScreen.kt      # Setup instructions, latency tips, fallbacks
│   │   │   │   │   │   └── HomeScreen.kt       # Live status, start/stop, volume, delay sliders
│   │   │   │   │   └── theme/
│   │   │   │   │       ├── Color.kt            # Material 3 neon/dark & light color palette
│   │   │   │   │       ├── Theme.kt            # Edge-to-edge Material 3 theme
│   │   │   │   │       └── Type.kt             # Typography definitions
│   │   │   │   ├── viewmodel/
│   │   │   │   │   └── SoundSingViewModel.kt   # StateFlow view model coordinating service & UI 
│   │   │   │   ├── MainActivity.kt             # Compose activity & MediaProjection launcher
│   │   │   │   └── SoundSingApp.kt             # Application class & notification channel
│   │   │   ├── res/
│   │   │   │   ├── drawable/                   # Vector icons (ic_stat_audio, ic_launcher_foreground)
│   │   │   │   ├── mipmap-anydpi-v26/          # Adaptive launcher icons
│   │   │   │   └── values/                     # strings.xml, colors.xml, themes.xml
│   │   │   └── AndroidManifest.xml             # Foreground service & permissions
│   │   └── test/
│   ├── build.gradle.kts                        # App-level build script (minSdk 29, targetSdk 35)
│   └── proguard-rules.pro                      # Proguard obfuscation rules
├── gradle/
│   ├── libs.versions.toml                      # Gradle version catalog
│   └── wrapper/
│       └── gradle-wrapper.properties           # Gradle 8.11.1 distribution
├── build.gradle.kts                            # Root build script
├── gradle.properties                           # JVM & AndroidX settings
├── settings.gradle.kts                         # Dependency & plugin repositories
├── .gitignore                                  # Git exclusion rules
└── README.md                                   # Comprehensive documentation
```

---

## 🛠️ Build and Run Instructions

### Prerequisites
- **Android Studio Ladybug (2024.2+)** or newer.
- **JDK 17** (configured in Android Studio under `Settings -> Build, Execution, Deployment -> Build Tools -> Gradle -> Gradle JDK`).
- **Physical Android Device** running **Android 10 (API 29)** or higher.  
  *(Note: Real devices are required to test Bluetooth audio routing; Android Studio emulators do not emulate dual Bluetooth audio sinks).*

### Steps to Build
1. Clone or open the project folder in Android Studio:
   ```bash
   git clone https://github.com/Banisher2005/Sound-sing-.git
   ```
2. In Android Studio, select **File -> Open...** and select the `Sound-sing-` root directory.
3. Allow Gradle to sync dependencies defined in `gradle/libs.versions.toml`.
4. Connect your Android device via USB with **USB Debugging** enabled in Developer Options.
5. In the top toolbar, select `app` in the run configurations dropdown and choose your physical device.
6. Click **Run** (`Shift + F10`) to compile and deploy.

---

## ⚠️ Known Limitations & Truth in Engineering

Sound Sing operates strictly within official Android APIs without requiring root. As a senior developer, it is important to be completely transparent about platform limitations:

1. **DRM / Opt-Out Streaming Apps**
   - Streaming services (Netflix, Amazon Prime Video, Disney+, Spotify, Apple Music) flag their playback with DRM or `android:allowAudioPlaybackCapture="false"`. Android's kernel silences the capture buffer for these applications. Sound Sing automatically detects this and notifies the user via the in-app silence warning.
2. **Audio Latency & Delay Offset**
   - Android's audio capture pipeline introduces inherent buffer latency (~15ms to 40ms). Furthermore, Bluetooth A2DP adds 100ms - 250ms of transmission buffer latency.
   - **No Way to Delay Original Output**: Android provides no public API to intentionally delay the phone's primary speaker output. Therefore, the **Ring Buffer Delay** slider allows delaying the *secondary* copy, which works perfectly when the secondary device is faster (or when routing to wired/USB while primary is delayed), but cannot make a delayed Bluetooth output play *earlier* than the phone's speaker.
3. **Dual Bluetooth Connections Depend on Phone Hardware**
   - Not all phones can maintain two active high-bitrate A2DP Bluetooth audio streams simultaneously. Many smartphones only support one active A2DP connection at a time.
   - Phones with **Bluetooth 5.2+ and LE Audio (LC3)** or Samsung devices with native **Dual Audio** support dual simultaneous Bluetooth playback natively.
   - For all other phones, Sound Sing excels at routing between **Mixed Outputs**: e.g., Phone Speaker + Bluetooth, Wired 3.5mm + Bluetooth, or USB-C DAC + Bluetooth.

---

## 📋 Testing Checklist (Real Device)

| Test Case | Procedure | Expected Result |
| :--- | :--- | :--- |
| **1. Permission Flow** | Launch app on clean install, tap "Start Audio Re-routing". | Explanatory dialog appears; prompts for Audio, Bluetooth, and Notifications. |
| **2. MediaProjection Prompt** | Grant permissions and start routing. | Android OS system consent dialog appears ("Sound Sing will start capturing..."). |
| **3. Foreground Notification** | Accept capture prompt. | Notification appears with small audio icon, "Sound Sing Active", and "Stop Routing" button. |
| **4. Stop from Notification** | Tap "Stop Routing" in notification tray. | Engine immediately stops, service unbinds, notification dismisses cleanly. |
| **5. Device Picker** | Open "Outputs" tab with Bluetooth headphones connected. | Bluetooth headphones appear in the list with active selection indicator. |
| **6. Audio Duplication** | Play a video in Chrome or a track in YouTube Music. | Audio plays from primary phone output and duplicate copy plays through selected secondary device. |
| **7. Ring Buffer Delay** | Move Delay slider from 0ms to 250ms while playing rhythmic audio. | Delay on second output increases smoothly without audio distortion or crashes. |
| **8. Volume Slider** | Move Volume slider from 100% down to 0%. | Secondary output volume attenuates smoothly without affecting primary output volume. |
| **9. Silence / DRM Detection** | Open Netflix or pause all audio for 5 seconds. | Status transitions to "SILENCE_OR_BLOCKED" and alert banner appears. |
| **10. Bluetooth Disconnect** | Power off Bluetooth headphones while actively streaming. | AudioDeviceCallback detects disconnection, engine stops gracefully without crashing. |
| **11. Dark & Light Theme** | Toggle system dark mode. | App UI adapts immediately to Material 3 palette with appropriate contrast. |
| **12. Privacy Audit** | Inspect APK permissions with `aapt dump permissions`. | Verifies no `android.permission.INTERNET` is requested. |
