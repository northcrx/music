package com.nishant.northmusic.ui.screens.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.nishant.northmusic.ui.theme.PillShape
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel
import com.nishant.northmusic.util.PermissionUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsSetupScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    onRunSetupAgain: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val state by settingsViewModel.settingsState.collectAsState()

    var hasAudio by remember { mutableStateOf(PermissionUtils.hasAudioPermission(context)) }
    var hasNotif by remember { mutableStateOf(PermissionUtils.hasNotificationPermission(context)) }
    var hasBluetooth by remember { mutableStateOf(PermissionUtils.hasBluetoothPermission(context)) }
    var isBatteryUnrestricted by remember { mutableStateOf(PermissionUtils.isBatteryOptimizationIgnored(context)) }

    // Recheck permissions on resume
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasAudio = PermissionUtils.hasAudioPermission(context)
                hasNotif = PermissionUtils.hasNotificationPermission(context)
                hasBluetooth = PermissionUtils.hasBluetoothPermission(context)
                isBatteryUnrestricted = PermissionUtils.isBatteryOptimizationIgnored(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val audioLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasAudio = granted
    }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotif = granted
    }

    val bluetoothLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasBluetooth = granted
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Permissions & setup",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Group 1: Essential Permissions
            item {
                Text(
                    text = "DEVICE PERMISSIONS",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 1. Music Access
                        PermissionRow(
                            title = "Music access",
                            description = "Required to index, load cover art, and play local audio files.",
                            icon = Icons.Rounded.AudioFile,
                            statusText = if (hasAudio) "Granted" else "Missing",
                            isGranted = hasAudio,
                            onActionClick = {
                                if (hasAudio) {
                                    PermissionUtils.openAppSettings(context)
                                } else {
                                    audioLauncher.launch(PermissionUtils.getRequiredAudioPermission())
                                }
                            },
                            actionLabel = if (hasAudio) "Settings" else "Fix"
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )

                        // 2. Notifications
                        PermissionRow(
                            title = "Notifications",
                            description = "Playback controls and track info on lock screen and status bar.",
                            icon = Icons.Rounded.Notifications,
                            statusText = if (hasNotif) "Allowed" else "Blocked",
                            isGranted = hasNotif,
                            onActionClick = {
                                if (hasNotif) {
                                    PermissionUtils.openAppSettings(context)
                                } else {
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    } else {
                                        PermissionUtils.openAppSettings(context)
                                    }
                                }
                            },
                            actionLabel = if (hasNotif) "Settings" else "Allow"
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )

                        // 3. Bluetooth
                        PermissionRow(
                            title = "Bluetooth",
                            description = "Used to detect wireless headphones and handle auto-pause on disconnect.",
                            icon = Icons.Rounded.Bluetooth,
                            statusText = if (hasBluetooth) "Allowed" else "Not granted",
                            isGranted = hasBluetooth,
                            onActionClick = {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                    if (!hasBluetooth) {
                                        bluetoothLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                                    } else {
                                        PermissionUtils.openAppSettings(context)
                                    }
                                } else {
                                    PermissionUtils.openAppSettings(context)
                                }
                            },
                            actionLabel = if (hasBluetooth) "Settings" else "Allow"
                        )
                    }
                }
            }

            // Group 2: System Optimization & Network
            item {
                Text(
                    text = "SYSTEM & NETWORK",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // 4. Background playback
                        PermissionRow(
                            title = "Background playback",
                            description = "Prevent the system from stopping music when the screen is turned off.",
                            icon = Icons.Rounded.BatteryChargingFull,
                            statusText = if (isBatteryUnrestricted) "Unrestricted" else "Optimized",
                            isGranted = isBatteryUnrestricted,
                            onActionClick = {
                                PermissionUtils.openBatterySettings(context)
                            },
                            actionLabel = "Battery settings"
                        )

                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                            modifier = Modifier.padding(start = 68.dp)
                        )

                        // 5. Online features switch
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.CloudDownload,
                                        contentDescription = null,
                                        tint = if (state.enableOnlineFeatures) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Online features",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = if (state.enableOnlineFeatures) "Enabled (streaming & online search)" else "Disabled (pure offline mode)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (state.enableOnlineFeatures) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            Switch(
                                checked = state.enableOnlineFeatures,
                                onCheckedChange = { settingsViewModel.setEnableOnlineFeatures(it) }
                            )
                        }
                    }
                }
            }

            // Group 3: Setup Wizard Re-run
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primaryContainer,
                                modifier = Modifier.size(40.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }

                            Column {
                                Text(
                                    text = "Setup Wizard",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Reconfigure welcome preferences from scratch",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Text(
                            text = "Run the 9-step initial setup wizard again to customize look, audio permissions, scan folders, and download folder rules.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 18.sp
                        )

                        FilledTonalButton(
                            onClick = onRunSetupAgain,
                            shape = PillShape,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Run setup again")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    icon: ImageVector,
    statusText: String,
    isGranted: Boolean,
    onActionClick: () -> Unit,
    actionLabel: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "• $statusText",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                )
            }
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 16.sp
            )
        }

        if (isGranted) {
            OutlinedButton(
                onClick = onActionClick,
                shape = PillShape,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        } else {
            Button(
                onClick = onActionClick,
                shape = PillShape,
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(actionLabel, style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}
