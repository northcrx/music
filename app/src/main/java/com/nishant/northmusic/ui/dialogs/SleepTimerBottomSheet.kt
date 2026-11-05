package com.nishant.northmusic.ui.dialogs

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.HourglassEmpty
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.ui.components.AppBottomSheet
import com.nishant.northmusic.ui.components.SettingsSwitchRow
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.PlayerViewModel
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerBottomSheet(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sleepTimerState by playerViewModel.sleepTimerState.collectAsState()

    var selectedMinutes by remember(sleepTimerState.totalDurationMinutes) {
        val initial = if (sleepTimerState.totalDurationMinutes in 5..180) {
            sleepTimerState.totalDurationMinutes.toFloat()
        } else {
            30f
        }
        mutableFloatStateOf(initial)
    }
    var finishLastSong by remember(sleepTimerState.finishLastSong) {
        mutableStateOf(sleepTimerState.finishLastSong)
    }
    var fadeOut by remember(sleepTimerState.fadeOut) {
        mutableStateOf(sleepTimerState.fadeOut)
    }

    val chipOptions = listOf(
        15 to "15 min",
        45 to "45 min",
        60 to "1 hour",
        120 to "2 hours"
    )

    AppBottomSheet(
        onDismissRequest = onDismiss,
        title = "Sleep timer",
        subtitle = if (sleepTimerState.isActive) {
            "Timer active · ${sleepTimerState.formattedRemaining} remaining"
        } else {
            "Rotate the clock dial or pick a preset"
        },
        icon = Icons.Rounded.Bedtime,
        buttonsContent = {
            if (sleepTimerState.isActive) {
                TextButton(
                    onClick = onDismiss,
                    shape = PillShape
                ) {
                    Text("Close")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        playerViewModel.cancelSleepTimer()
                        onDismiss()
                    },
                    shape = PillShape,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) {
                    Text("Cancel timer")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        playerViewModel.startSleepTimer(
                            minutes = selectedMinutes.roundToInt(),
                            finishLastSong = finishLastSong,
                            fadeOut = fadeOut
                        )
                        onDismiss()
                    },
                    shape = PillShape
                ) {
                    Text("Update timer")
                }
            } else {
                TextButton(
                    onClick = onDismiss,
                    shape = PillShape
                ) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        playerViewModel.startSleepTimer(
                            minutes = selectedMinutes.roundToInt(),
                            finishLastSong = finishLastSong,
                            fadeOut = fadeOut
                        )
                        onDismiss()
                    },
                    shape = PillShape
                ) {
                    Text("Set timer")
                }
            }
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Active timer banner if running
            if (sleepTimerState.isActive) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.HourglassEmpty,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Remaining: ${sleepTimerState.formattedRemaining}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (sleepTimerState.finishLastSong) "Will finish current song" else "Pauses promptly",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }

            // Quick presets row of 4 equal-width FilterChips
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                chipOptions.forEach { (mins, label) ->
                    val isSelected = selectedMinutes.roundToInt() == mins
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMinutes = mins.toFloat() },
                        label = {
                            Text(
                                text = label,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Interactive Circular Clock Dial
            SleepTimerClockDial(
                totalMinutes = selectedMinutes.roundToInt(),
                onMinutesChanged = { selectedMinutes = it.toFloat() }
            )

            // Switch options: Finish last song and Music fades out using SettingsSwitchRow
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    SettingsSwitchRow(
                        title = "Finish last song",
                        subtitle = "Wait for current track to end before stopping",
                        checked = finishLastSong,
                        onCheckedChange = { finishLastSong = it }
                    )
                    SettingsSwitchRow(
                        title = "Music fades out",
                        subtitle = "Smoothly lower volume over the last 10 seconds",
                        checked = fadeOut,
                        onCheckedChange = { fadeOut = it }
                    )
                }
            }
        }
    }
}

@Composable
fun SleepTimerClockDial(
    totalMinutes: Int,
    onMinutesChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHighest
    val onSurfaceColor = MaterialTheme.colorScheme.onSurface
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

    // Hand angle: 0 to 360 degrees (based on minutes % 60)
    val minutesInHour = if (totalMinutes == 0) 0 else ((totalMinutes - 1) % 60) + 1
    val angleDegrees = (minutesInHour / 60f) * 360f

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        // - 5m micro-adjustment button
        FilledTonalIconButton(
            onClick = {
                val next = (totalMinutes - 5).coerceAtLeast(5)
                onMinutesChanged(next)
            },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Rounded.Remove, contentDescription = "Decrease 5 minutes")
        }

        Spacer(modifier = Modifier.width(10.dp))

        // Clock Dial Canvas
        Box(
            modifier = Modifier
                .size(200.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val touch = change.position
                        val dx = touch.x - center.x
                        val dy = touch.y - center.y
                        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (deg < 0f) deg += 360f
                        var mins = ((deg / 360f) * 60f).roundToInt().coerceIn(1, 60)
                        mins = ((mins + 2) / 5) * 5
                        if (mins == 0) mins = 5
                        val currentHours = totalMinutes / 60
                        val newTotal = (currentHours * 60) + mins
                        onMinutesChanged(newTotal.coerceIn(5, 180))
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = offset.x - center.x
                        val dy = offset.y - center.y
                        var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat() + 90f
                        if (deg < 0f) deg += 360f
                        var mins = ((deg / 360f) * 60f).roundToInt().coerceIn(1, 60)
                        mins = ((mins + 2) / 5) * 5
                        if (mins == 0) mins = 5
                        val currentHours = totalMinutes / 60
                        val newTotal = (currentHours * 60) + mins
                        onMinutesChanged(newTotal.coerceIn(5, 180))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val strokeWidth = 8.dp.toPx()
                val radius = (size.minDimension / 2f) - 16.dp.toPx()

                // Background dial track
                drawCircle(
                    color = trackColor,
                    radius = radius,
                    center = center,
                    style = Stroke(width = strokeWidth)
                )

                // Colored progress arc from top (270 deg)
                val sweep = (minutesInHour / 60f) * 360f
                drawArc(
                    color = primaryColor,
                    startAngle = -90f,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2, radius * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )

                // 12 clock tick marks around the dial
                for (i in 0 until 12) {
                    val tickAngle = (i * 30f) - 90f
                    val tickRad = Math.toRadians(tickAngle.toDouble())
                    val isQuarter = i % 3 == 0
                    val tickLen = if (isQuarter) 9.dp.toPx() else 4.dp.toPx()
                    val tickStroke = if (isQuarter) 2.5.dp.toPx() else 1.5.dp.toPx()
                    val startR = radius - 8.dp.toPx()
                    val endR = startR - tickLen

                    val startX = center.x + (startR * cos(tickRad)).toFloat()
                    val startY = center.y + (startR * sin(tickRad)).toFloat()
                    val endX = center.x + (endR * cos(tickRad)).toFloat()
                    val endY = center.y + (endR * sin(tickRad)).toFloat()

                    drawLine(
                        color = if (isQuarter) onSurfaceColor.copy(alpha = 0.5f) else onSurfaceVariant.copy(alpha = 0.2f),
                        start = Offset(startX, startY),
                        end = Offset(endX, endY),
                        strokeWidth = tickStroke,
                        cap = StrokeCap.Round
                    )
                }

                // Clock hand from center to knob
                val rad = Math.toRadians((angleDegrees - 90f).toDouble())
                val knobX = center.x + (radius * cos(rad)).toFloat()
                val knobY = center.y + (radius * sin(rad)).toFloat()

                drawLine(
                    color = primaryColor.copy(alpha = 0.5f),
                    start = center,
                    end = Offset(knobX, knobY),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                // Center hub
                drawCircle(
                    color = primaryColor,
                    radius = 5.dp.toPx(),
                    center = center
                )

                // Knob at perimeter
                drawCircle(
                    color = primaryColor,
                    radius = 11.dp.toPx(),
                    center = Offset(knobX, knobY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 4.dp.toPx(),
                    center = Offset(knobX, knobY)
                )
            }

            // Digital readout in the center of the clock
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val formattedTime = if (totalMinutes >= 60) {
                    val hrs = totalMinutes / 60
                    val mins = totalMinutes % 60
                    if (mins > 0) "${hrs}h ${mins}m" else "${hrs} hr"
                } else {
                    "$totalMinutes min"
                }

                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Sleep timer",
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        // + 5m micro-adjustment button
        FilledTonalIconButton(
            onClick = {
                val next = (totalMinutes + 5).coerceAtMost(180)
                onMinutesChanged(next)
            },
            modifier = Modifier.size(42.dp)
        ) {
            Icon(Icons.Rounded.Add, contentDescription = "Increase 5 minutes")
        }
    }
}
