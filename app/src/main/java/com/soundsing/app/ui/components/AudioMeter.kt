package com.soundsing.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.soundsing.app.audio.EngineStatus
import com.soundsing.app.ui.theme.DarkPrimary
import com.soundsing.app.ui.theme.DarkSecondary
import com.soundsing.app.ui.theme.StatusActive
import com.soundsing.app.ui.theme.StatusWarning

@Composable
fun AudioMeter(
    rmsLevel: Float,
    status: EngineStatus,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Audio Signal Level",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val statusText = when (status) {
                EngineStatus.IDLE -> "OFFLINE"
                EngineStatus.INITIALIZING -> "CONNECTING"
                EngineStatus.RUNNING -> "LIVE STREAMING"
                EngineStatus.SILENCE_OR_BLOCKED -> "SILENCE / BLOCKED"
                EngineStatus.ERROR -> "FAULT"
            }

            val statusColor = when (status) {
                EngineStatus.RUNNING -> StatusActive
                EngineStatus.SILENCE_OR_BLOCKED -> StatusWarning
                EngineStatus.ERROR -> MaterialTheme.colorScheme.error
                else -> Color.Gray
            }

            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(statusColor.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        val barCount = 18
        val isRunning = status == EngineStatus.RUNNING
        val gradient = Brush.verticalGradient(
            colors = listOf(DarkSecondary, DarkPrimary)
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            for (i in 0 until barCount) {
                val multiplier = 0.3f + (0.7f * ((i % 5) + 1) / 5f)
                val targetHeightRatio = if (isRunning) {
                    ((rmsLevel * 6f) * multiplier).coerceIn(0.08f, 1.0f)
                } else if (status == EngineStatus.INITIALIZING) {
                    pulseAlpha * 0.2f
                } else {
                    0.06f
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 1.5.dp)
                        .height((48 * targetHeightRatio).dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (isRunning) gradient else Brush.verticalGradient(listOf(Color.DarkGray, Color.Gray)))
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        val clampedRms = (rmsLevel * 3.5f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(MaterialTheme.colorScheme.surface)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clampedRms)
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .background(if (status == EngineStatus.RUNNING) DarkPrimary else Color.Transparent)
            )
        }
    }
}
