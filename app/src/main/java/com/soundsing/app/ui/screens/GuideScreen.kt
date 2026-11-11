package com.soundsing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DevicesOther
import androidx.compose.material.icons.filled.FormatListNumbered
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundsing.app.ui.theme.DarkPrimary
import com.soundsing.app.ui.theme.DarkSecondary
import com.soundsing.app.ui.theme.StatusWarning

@Composable
fun GuideScreen(
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column {
            Text(
                text = "Sound Sing Guide & Tips",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = "Setup steps, latency reduction, and hardware fallbacks",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.FormatListNumbered,
                        contentDescription = null,
                        tint = DarkPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Quick Setup Guide",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                StepItem(
                    stepNumber = "1",
                    title = "Grant Required Permissions",
                    description = "Grant Audio Recording, Bluetooth Connect, and Notification permissions when prompted."
                )

                StepItem(
                    stepNumber = "2",
                    title = "Connect Your Second Output",
                    description = "Pair your second Bluetooth earbuds/speaker, or connect a 3.5mm / USB-C audio accessory."
                )

                StepItem(
                    stepNumber = "3",
                    title = "Pick Output in Sound Sing",
                    description = "In the 'Outputs' tab, tap your desired device to designate it as the second output."
                )

                StepItem(
                    stepNumber = "4",
                    title = "Accept System Screen/Audio Capture",
                    description = "When tapping 'Start Audio Re-routing', Android displays a consent dialog ('Sound Sing will capture...'). This is Android's required permission for AudioPlaybackCapture."
                )

                StepItem(
                    stepNumber = "5",
                    title = "Play Media & Enjoy",
                    description = "Open YouTube Music, Chrome, games, or podcasts. Audio will play simultaneously on both outputs!"
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = DarkSecondary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Reducing Audio Delay / Echo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                BulletPoint(
                    title = "Use the Ring Buffer Delay Slider",
                    desc = "Because Bluetooth wireless protocols have buffer latency (typically 80ms - 250ms), adjust the delay slider on the Home tab to sync the two outputs."
                )

                BulletPoint(
                    title = "Exclude From Battery Optimizations",
                    desc = "Set Sound Sing's App Info -> Battery usage to 'Unrestricted' so Android doesn't throttle our real-time audio thread."
                )

                BulletPoint(
                    title = "Prefer Bluetooth LE Audio or Low-Latency Codecs",
                    desc = "Devices supporting Bluetooth LE Audio (LC3) or aptX Low Latency offer the tightest synchronization."
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = StatusWarning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Why Some Apps Don't Capture",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Android's AudioPlaybackCapture API strictly obeys app manifest flags:\n\n" +
                            "• If an app specifies 'android:allowAudioPlaybackCapture=\"false\"', the Android OS kernel will output complete silence to capture recorders.\n" +
                            "• Streaming services like Netflix, Amazon Prime Video, Disney+, and Spotify enforce Widevine DRM encryption and opt out of capture.\n" +
                            "• Apps that work great: Chrome, YouTube (non-DRM video), Soundcloud, video games, emulator apps, media players, and browser audio.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DevicesOther,
                        contentDescription = null,
                        tint = DarkPrimary
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Hardware Fallback: 2-in-1 Transmitters",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "If your phone's Bluetooth stack limits simultaneous connection or if you want perfectly imperceptible 0ms latency between two listeners (e.g. sharing a movie on a plane):\n\n" +
                            "Consider an external 3.5mm / USB-C Bluetooth Dual Transmitter (e.g. 1Mii, Avantree, or AirFly Pro). These hardware dongles physically pair with 2 sets of wireless headphones simultaneously with zero software overhead.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun StepItem(
    stepNumber: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(DarkPrimary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stepNumber,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BulletPoint(
    title: String,
    desc: String
) {
    Column(modifier = Modifier.padding(vertical = 6.dp)) {
        Text(
            text = "• $title",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = desc,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 12.dp, top = 2.dp)
        )
    }
}
