package com.soundsing.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.soundsing.app.model.AudioOutputDevice
import com.soundsing.app.ui.components.DeviceCard
import com.soundsing.app.viewmodel.SoundSingUiState

@Composable
fun DevicesScreen(
    uiState: SoundSingUiState,
    onSelectDevice: (AudioOutputDevice) -> Unit,
    onRefreshDevices: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bluetoothDevices = uiState.availableDevices.filter {
        it.category == AudioOutputDevice.DeviceCategory.BLUETOOTH
    }
    val physicalDevices = uiState.availableDevices.filter {
        it.category == AudioOutputDevice.DeviceCategory.WIRED || it.category == AudioOutputDevice.DeviceCategory.USB
    }
    val builtInDevices = uiState.availableDevices.filter {
        it.category == AudioOutputDevice.DeviceCategory.BUILTIN_SPEAKER || it.category == AudioOutputDevice.DeviceCategory.OTHER
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Audio Output Picker",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Select your target secondary output",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = onRefreshDevices,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Refresh Devices",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Dual Output Architecture",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "The phone's active default output (e.g. your primary headset or speaker) continues playing the system's original audio stream.\n\nSound Sing intercepts a duplicate audio feed using Android's AudioPlaybackCapture and routes it directly to this chosen second output.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Bluetooth Outputs (A2DP & LE Audio)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (bluetoothDevices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No Bluetooth audio devices currently connected.\nTurn on Bluetooth and pair your wireless earbuds, headphones, or Bluetooth speaker.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(bluetoothDevices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    isSelected = uiState.selectedOutputDevice?.id == device.id,
                    onSelect = { onSelectDevice(device) }
                )
            }
        }

        item {
            Text(
                text = "Wired & USB Outputs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        if (physicalDevices.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = "No 3.5mm headphones or USB-C audio DACs connected.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            items(physicalDevices, key = { it.id }) { device ->
                DeviceCard(
                    device = device,
                    isSelected = uiState.selectedOutputDevice?.id == device.id,
                    onSelect = { onSelectDevice(device) }
                )
            }
        }

        item {
            Text(
                text = "Phone Internal Outputs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        items(builtInDevices, key = { it.id }) { device ->
            DeviceCard(
                device = device,
                isSelected = uiState.selectedOutputDevice?.id == device.id,
                onSelect = { onSelectDevice(device) }
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
