package com.nishant.northmusic.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.ui.platform.LocalContext
import com.nishant.northmusic.data.model.DeviceCategory
import com.nishant.northmusic.data.model.DeviceSoundProfiles
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Nightlight
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import java.net.URLEncoder
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.SpatialAudio
import androidx.compose.material.icons.rounded.SurroundSound
import androidx.compose.material3.Card
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import com.nishant.northmusic.ui.components.AppSlider
import com.nishant.northmusic.ui.theme.PillShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.AudioOutputDevice
import com.nishant.northmusic.data.model.EqualizerPreset
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.viewmodels.EqualizerViewModel
import kotlin.math.roundToInt

private val BandLabels = listOf("60Hz", "230Hz", "910Hz", "3.6kHz", "14kHz")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerScreen(
    equalizerViewModel: EqualizerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by equalizerViewModel.equalizerState.collectAsState()
    val activeProfile = state.activeProfile
    val isEnabled = state.isMasterEnabled
    val isManualControlsEnabled = isEnabled && !activeProfile.isAutoMode
    val controlsAlpha = if (!isEnabled) 0.38f else if (activeProfile.isAutoMode) 0.75f else 1f
    val context = LocalContext.current
    var showProfileDialog by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Top App Bar matching Settings sub-screens style (no icon chip box)
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Equalizer",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomListScrollPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Master On/Off Switch Card in neutral surfaceContainer
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Equalizer",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Enable audio processing and effects",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { equalizerViewModel.toggleMaster(it) },
                            thumbContent = if (isEnabled) {
                                {
                                    Icon(
                                        imageVector = Icons.Rounded.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(SwitchDefaults.IconSize)
                                    )
                                }
                            } else null
                        )
                    }
                }
            }

            // Output Device Profile Selector (Bluetooth, Wired, Phone speaker)
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f)
                ) {
                    Text(
                        text = "OUTPUT DEVICE PROFILE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
                    )

                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        AudioOutputDevice.entries.forEachIndexed { index, device ->
                            val selected = state.currentDevice == device
                            val icon = when (device) {
                                AudioOutputDevice.BLUETOOTH -> Icons.Rounded.Headphones
                                AudioOutputDevice.WIRED -> Icons.Rounded.Headset
                                AudioOutputDevice.SPEAKER -> Icons.AutoMirrored.Rounded.VolumeUp
                            }
                            val shortLabel = when (device) {
                                AudioOutputDevice.BLUETOOTH -> "Bluetooth"
                                AudioOutputDevice.WIRED -> "Wired"
                                AudioOutputDevice.SPEAKER -> "Speaker"
                            }
                            SegmentedButton(
                                selected = selected,
                                onClick = {
                                    if (isEnabled) {
                                        equalizerViewModel.selectOutputDevice(device)
                                    }
                                },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = AudioOutputDevice.entries.size
                                ),
                                enabled = isEnabled,
                                modifier = Modifier.height(44.dp),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    activeContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                                    inactiveContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                ),
                                icon = {}
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = device.label,
                                        modifier = Modifier.size(17.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = shortLabel,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hardware Acoustic Calibration Card (Soundbar / Neckband profiling)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (state.currentDevice == AudioOutputDevice.BLUETOOTH) Icons.Rounded.Headphones else Icons.Rounded.Speaker,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Acoustic Calibration",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val deviceDisplay = state.connectedDeviceName?.takeIf { it.isNotBlank() }
                                        ?: if (state.currentDevice == AudioOutputDevice.BLUETOOTH) "Bluetooth Audio" else state.currentDevice.label
                                    Text(
                                        text = deviceDisplay,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            if (state.matchedProfile != null) {
                                Surface(
                                    shape = PillShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Calibrated",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }

                        if (state.matchedProfile != null) {
                            val profile = state.matchedProfile!!
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = profile.brand,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "•",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = profile.category.label,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = profile.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = profile.description,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (isEnabled) showProfileDialog = true },
                                    enabled = isEnabled,
                                    shape = PillShape,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Change profile",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        } else {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "Standard Profile Active",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "No dedicated calibration profile matched for this model. You can manually choose a soundbar or neckband curve, or submit your device on GitHub to be added in the next update.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { if (isEnabled) showProfileDialog = true },
                                    enabled = isEnabled,
                                    shape = PillShape,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Tune,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Browse profiles",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }

                                Button(
                                    onClick = {
                                        val deviceNameForUrl = state.connectedDeviceName?.takeIf { it.isNotBlank() } ?: "Unknown Audio Device"
                                        val encodedTitle = URLEncoder.encode("Sound Profile Request: " + deviceNameForUrl, "UTF-8")
                                        val encodedBody = URLEncoder.encode(
                                            "### Device Sound Profile Request\n- **Device Name**: " + deviceNameForUrl + "\n- **Device Type**: " + state.currentDevice.label + "\n\n### Acoustic Feedback\nPlease add a tuned sound profile for this soundbar / neckband / headphones in NorthMusic.",
                                            "UTF-8"
                                        )
                                        val githubUrl = "https://github.com/northcrx/music/issues/new?title=" + encodedTitle + "&body=" + encodedBody
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(githubUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (_: Throwable) {}
                                    },
                                    shape = PillShape,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Submit on GitHub",
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Model-Specific Audio & Home Theater Control Deck
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        val currentMatched = state.matchedProfile
                        val deckTitle = when {
                            currentMatched != null && currentMatched.category == DeviceCategory.HOME_THEATER ->
                                "${currentMatched.brand} Soundbar Control Deck"
                            currentMatched != null ->
                                "${currentMatched.brand} Audio Control Deck"
                            state.currentDevice == AudioOutputDevice.BLUETOOTH ->
                                "Bluetooth Audio Control Deck"
                            else ->
                                "Audio Control Deck"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SurroundSound,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = deckTitle,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Remote-style subwoofer, spatial & acoustic modes",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // Subwoofer / Bass Level Slider (-6 dB to +6 dB)
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (state.matchedProfile?.category == DeviceCategory.HOME_THEATER) "Subwoofer Level (Woofer)" else "Deep Bass Level",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    val wooferDb = activeProfile.wooferLevelDb
                                    val formattedWoofer = if (wooferDb > 0) "+%.1f dB".format(wooferDb) else if (wooferDb < 0) "%.1f dB".format(wooferDb) else "0.0 dB (Flat)"
                                    Text(
                                        text = formattedWoofer,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = if (wooferDb != 0f) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                AppSlider(
                                    value = (activeProfile.wooferLevelDb + 6f) / 12f,
                                    onValueChange = { norm ->
                                        val db = (norm * 12f - 6f).roundToInt().toFloat()
                                        equalizerViewModel.setWooferLevel(db)
                                    },
                                    enabled = isEnabled,
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "-6 dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "0 dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(text = "+6 dB", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Sound Mode Chips (Surround Sound, Bass Boost, Voice Enhance, Night Mode)
                        Text(
                            text = "SOUND MODES",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = activeProfile.isSurroundExpanded,
                                onClick = {
                                    if (isEnabled) equalizerViewModel.toggleSurroundExpanded(!activeProfile.isSurroundExpanded)
                                },
                                label = { Text("Surround Sound") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.SpatialAudio,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = PillShape,
                                enabled = isEnabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )

                            FilterChip(
                                selected = activeProfile.isBassBoostMode,
                                onClick = {
                                    if (isEnabled) equalizerViewModel.toggleBassBoostMode(!activeProfile.isBassBoostMode)
                                },
                                label = { Text("Bass Boost") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Bolt,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = PillShape,
                                enabled = isEnabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )

                            FilterChip(
                                selected = activeProfile.isVoiceEnhanced,
                                onClick = {
                                    if (isEnabled) equalizerViewModel.toggleVoiceEnhanced(!activeProfile.isVoiceEnhanced)
                                },
                                label = { Text("Voice Enhance") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.RecordVoiceOver,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = PillShape,
                                enabled = isEnabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )

                            FilterChip(
                                selected = activeProfile.isNightMode,
                                onClick = {
                                    if (isEnabled) equalizerViewModel.toggleNightMode(!activeProfile.isNightMode)
                                },
                                label = { Text("Night Mode") },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Nightlight,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                },
                                shape = PillShape,
                                enabled = isEnabled,
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            )
                        }
                    }
                }
            }

            // AUTO Mode Switch & Intelligent Acoustic Card in neutral surfaceContainer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "Auto mode",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Intelligent acoustic adjustment",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Switch(
                                checked = activeProfile.isAutoMode,
                                onCheckedChange = { equalizerViewModel.toggleAutoMode(it) },
                                enabled = isEnabled,
                                thumbContent = if (activeProfile.isAutoMode) {
                                    {
                                        Icon(
                                            imageVector = Icons.Rounded.Check,
                                            contentDescription = null,
                                            modifier = Modifier.size(SwitchDefaults.IconSize)
                                        )
                                    }
                                } else null
                            )
                        }

                        // When ON, show the Auto information card
                        AnimatedVisibility(
                            visible = activeProfile.isAutoMode,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 14.dp)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(16.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        // Line 1: Output device and genre (e.g. "Phone speaker · Pop")
                                        Text(
                                            text = "${state.currentDevice.label} · ${state.detectedGenre}",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        // Line 2: Sparkle icon + Suggested preset as plain text (no chip, never wraps vertically)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoAwesome,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Suggested: ${state.suggestedPresetTitle}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.primary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }

                                        // Collapsible "Why?" text button
                                        var isWhyExpanded by remember { mutableStateOf(false) }

                                        TextButton(
                                            onClick = { isWhyExpanded = !isWhyExpanded },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.height(28.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.AutoMirrored.Rounded.HelpOutline,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = if (isWhyExpanded) "Hide details" else "Why?",
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                            }
                                        }

                                        AnimatedVisibility(visible = isWhyExpanded) {
                                            Text(
                                                text = "Based on your output device and the song's genre. Auto mode calibrates acoustic response automatically without manual clipping.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Auto mode banner
            if (isEnabled && activeProfile.isAutoMode) {
                item {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Auto is controlling the equalizer",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                            Button(
                                onClick = { equalizerViewModel.toggleAutoMode(false) },
                                shape = PillShape,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "Switch to manual",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            // 5-Band Vertical Equalizer Section (36dp wide rounded tracks, accent fill, white thumb)
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "5-BAND EQUALIZER",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold
                            )
                            if (activeProfile.isAutoMode) {
                                Surface(
                                    shape = PillShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Lock,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(
                                            text = "Auto Calibrated (Locked)",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            } else if (state.matchedProfile != null) {
                                TextButton(
                                    onClick = { equalizerViewModel.resetToCalibratedProfile() },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp),
                                    modifier = Modifier.height(28.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.RestartAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = "Reset curve",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Vertical sliders row with dB markings
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(210.dp)
                                .alpha(controlsAlpha),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            activeProfile.bandGains.forEachIndexed { index, gainDb ->
                                VerticalBandSlider(
                                    frequency = BandLabels.getOrElse(index) { "${index}k" },
                                    gainDb = gainDb,
                                    enabled = isEnabled && !activeProfile.isAutoMode,
                                    onGainChanged = { newGain ->
                                        equalizerViewModel.setBandGain(index, newGain)
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        if (activeProfile.isAutoMode) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = "Gains auto-adjusted for this device",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TextButton(
                                        onClick = { equalizerViewModel.toggleAutoMode(false) },
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = "Unlock to edit",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Presets Row below sliders
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp)
                    ) {
                        Text(
                            text = "PRESETS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .alpha(controlsAlpha),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            EqualizerPreset.entries.forEach { preset ->
                                val selected = activeProfile.selectedPreset == preset
                                FilterChip(
                                    selected = selected,
                                    onClick = {
                                        if (activeProfile.isAutoMode) {
                                            equalizerViewModel.toggleAutoMode(false)
                                        }
                                        if (selected) {
                                            equalizerViewModel.selectPreset(null)
                                        } else {
                                            equalizerViewModel.selectPreset(preset)
                                        }
                                    },
                                    enabled = isEnabled,
                                    label = { Text(preset.label) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // Effects: Bass Boost & Virtualizer
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .alpha(if (isEnabled) 1f else 0.38f),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "AUDIO EFFECTS",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )

                        // Bass Boost
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SurroundSound,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Bass boost",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${(activeProfile.bassBoostPercent * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AppSlider(
                                value = activeProfile.bassBoostPercent,
                                onValueChange = { equalizerViewModel.setBassBoost(it) },
                                enabled = isEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Virtualizer (3D Surround)
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.SpatialAudio,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Text(
                                        text = "Virtualizer (3D surround)",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                Text(
                                    text = "${(activeProfile.virtualizerPercent * 100).roundToInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            AppSlider(
                                value = activeProfile.virtualizerPercent,
                                onValueChange = { equalizerViewModel.setVirtualizer(it) },
                                enabled = isEnabled,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }

    if (showProfileDialog) {
        var profileSearchQuery by remember { mutableStateOf("") }
        val filteredProfiles = remember(profileSearchQuery) {
            if (profileSearchQuery.isBlank()) DeviceSoundProfiles.ALL_PROFILES
            else {
                val q = profileSearchQuery.trim().lowercase()
                DeviceSoundProfiles.ALL_PROFILES.filter { profile ->
                    profile.name.lowercase().contains(q) ||
                    profile.brand.lowercase().contains(q) ||
                    profile.description.lowercase().contains(q) ||
                    profile.matchKeywords.any { it.contains(q) }
                }
            }
        }

        AlertDialog(
            onDismissRequest = { showProfileDialog = false },
            title = {
                Text(
                    text = "Select Sound Profile",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = profileSearchQuery,
                        onValueChange = { profileSearchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Search model (e.g. B67, Nord, HT-S20)...", style = MaterialTheme.typography.bodySmall) },
                        leadingIcon = {
                            Icon(imageVector = Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                        },
                        trailingIcon = {
                            if (profileSearchQuery.isNotBlank()) {
                                IconButton(onClick = { profileSearchQuery = "" }) {
                                    Icon(imageVector = Icons.Rounded.Clear, contentDescription = "Clear", modifier = Modifier.size(18.dp))
                                }
                            }
                        },
                        singleLine = true,
                        shape = PillShape,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                    )

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 380.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (filteredProfiles.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "No profile matching '$profileSearchQuery'",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            items(filteredProfiles.size) { index ->
                                val profile = filteredProfiles[index]
                                val isSelected = state.matchedProfile?.id == profile.id
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            equalizerViewModel.selectSoundProfile(profile)
                                            showProfileDialog = false
                                        }
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        RadioButton(
                                            selected = isSelected,
                                            onClick = {
                                                equalizerViewModel.selectSoundProfile(profile)
                                                showProfileDialog = false
                                            }
                                        )
                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = profile.brand,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = "•",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                                Text(
                                                    text = profile.category.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Text(
                                                text = profile.name,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = profile.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showProfileDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

/**
 * 5-Band Vertical Slider Component with:
 * - 36dp wide rounded track
 * - Accent-colored filled part
 * - White thumb
 * - dB label on top
 * - Hz label below
 * - Disabled & dimmed when Auto is ON
 */
@Composable
fun VerticalBandSlider(
    frequency: String,
    gainDb: Float,
    enabled: Boolean,
    onGainChanged: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val density = LocalDensity.current

    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // dB display label on top
        val formattedDb = if (gainDb > 0) "+%.1fdB".format(gainDb) else "%.1fdB".format(gainDb)
        Text(
            text = formattedDb,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = if (gainDb != 0f && enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = if (gainDb != 0f) FontWeight.Bold else FontWeight.Normal
        )

        // Vertical Track & White Thumb (36dp wide rounded track)
        BoxWithConstraints(
            modifier = Modifier
                .height(140.dp)
                .width(36.dp)
                .pointerInput(enabled) {
                    if (enabled) {
                        detectVerticalDragGestures { change, dragAmount ->
                            change.consume()
                            val totalHeight = size.height.toFloat()
                            val deltaFraction = -dragAmount / totalHeight
                            val deltaDb = deltaFraction * 20f // 20dB range (-10 to +10)
                            onGainChanged((gainDb + deltaDb).coerceIn(-10f, 10f))
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            val totalTrackHeight = maxHeight
            val trackHeightPx = constraints.maxHeight.toFloat()
            // Fraction: -10dB -> 0.0 (bottom), +10dB -> 1.0 (top)
            val fraction = ((gainDb + 10f) / 20f).coerceIn(0f, 1f)
            val thumbSize = 28.dp
            val thumbSizePx = with(density) { thumbSize.toPx() }

            // Inactive Background Track: 36dp wide rounded pill
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            ) {
                // Accent-colored filled part from bottom up to current level
                val fillHeightDp = totalTrackHeight * fraction
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(fillHeightDp)
                        .align(Alignment.BottomCenter)
                        .background(
                            if (enabled) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                        )
                )

                // Center 0dB horizontal tick
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .align(Alignment.Center)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                )
            }

            // White thumb with drop shadow positioned along the track
            val availableTravel = (trackHeightPx - thumbSizePx).coerceAtLeast(0f)
            val thumbYOffset = ((1f - fraction) * availableTravel).roundToInt()

            Box(
                modifier = Modifier
                    .offset {
                        IntOffset(
                            x = 0,
                            y = thumbYOffset - (constraints.maxHeight / 2) + (thumbSizePx / 2).roundToInt()
                        )
                    }
                    .size(thumbSize)
                    .shadow(3.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }

        // Hz Frequency Label below
        Text(
            text = frequency,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}
