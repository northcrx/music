package com.nishant.northmusic.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Unified AppSlider used consistently across:
 * - Player seek bar
 * - Sleep timer
 * - Equalizer bass boost & virtualizer
 *
 * Characteristics:
 * - Consistent 8dp track height
 * - 18dp round thumb in primary (accent) color
 * - Primary accent active track
 * - surfaceContainerHighest inactive track
 * - No tick dots on track
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    onValueChangeFinished: (() -> Unit)? = null,
    trackHeight: Dp = 8.dp,
    thumbSize: Dp = 18.dp
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = SliderDefaults.colors(
            activeTrackColor = MaterialTheme.colorScheme.primary,
            inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f),
            disabledThumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
            activeTickColor = Color.Transparent,
            inactiveTickColor = Color.Transparent,
            disabledActiveTickColor = Color.Transparent,
            disabledInactiveTickColor = Color.Transparent
        ),
        thumb = {
            Box(
                modifier = Modifier
                    .size(thumbSize)
                    .clip(CircleShape)
                    .background(
                        if (enabled) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    )
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(trackHeight),
                colors = SliderDefaults.colors(
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    disabledActiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f),
                    disabledInactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.38f)
                ),
                drawStopIndicator = null
            )
        }
    )
}
