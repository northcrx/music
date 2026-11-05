package com.nishant.northmusic.ui.screens.settings

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.viewmodels.SettingsViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

data class HomeSectionInfo(
    val id: String,
    val title: String,
    val summary: String,
    val icon: ImageVector
)

val AllHomeSections = listOf(
    HomeSectionInfo("continue_listening", "Continue listening", "Resume your last played song quickly", Icons.Rounded.PlayCircle),
    HomeSectionInfo("for_you", "For you", "Personalized mix tiles and quick discovery", Icons.Rounded.AutoAwesome),
    HomeSectionInfo("recently_added", "Recently added", "Horizontal carousel of your newest tracks", Icons.Rounded.Schedule),
    HomeSectionInfo("downloaded", "Downloaded", "Saved offline tracks (hidden when empty)", Icons.Rounded.Download),
    HomeSectionInfo("categories", "Browse by category", "Category chips linking to filtered library", Icons.Rounded.Folder),
    HomeSectionInfo("artists", "Artists", "Horizontal circular artist photos", Icons.Rounded.Person)
)

val DefaultHomeSectionOrder = AllHomeSections.map { it.id }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeSectionsSettingsScreen(
    settingsViewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by settingsViewModel.settingsState.collectAsState()
    val haptic = LocalHapticFeedback.current

    // Normalize order list to ensure all known sections are present
    var currentOrder by remember(state.homeSectionOrder) {
        val list = state.homeSectionOrder.toMutableList()
        AllHomeSections.forEach { section ->
            if (!list.contains(section.id)) {
                list.add(section.id)
            }
        }
        mutableStateOf<List<String>>(list)
    }

    val lazyListState = rememberLazyListState()
    val reorderableLazyListState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val updated = currentOrder.toMutableList().apply {
            add(to.index, removeAt(from.index))
        }
        currentOrder = updated
        settingsViewModel.setHomeSectionOrder(updated)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Home sections",
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
            actions = {
                TextButton(
                    onClick = {
                        currentOrder = DefaultHomeSectionOrder
                        settingsViewModel.setHomeSectionOrder(DefaultHomeSectionOrder)
                        AllHomeSections.forEach { section ->
                            settingsViewModel.setHomeSectionEnabled(section.id, true)
                        }
                    }
                ) {
                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )

        LazyColumn(
            state = lazyListState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomListScrollPadding),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text(
                    text = "Reorder the sections displayed on the Home screen or toggle their visibility off.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }

            items(
                items = currentOrder,
                key = { it }
            ) { sectionId ->
                val sectionInfo = AllHomeSections.find { it.id == sectionId }
                if (sectionInfo != null) {
                    val isEnabled = !state.disabledHomeSections.contains(sectionId)

                    ReorderableItem(
                        state = reorderableLazyListState,
                        key = sectionId
                    ) { isDragging ->
                        val elevation by animateDpAsState(
                            targetValue = if (isDragging) 8.dp else 0.dp,
                            label = "elevation"
                        )

                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .shadow(elevation, shape = RoundedCornerShape(18.dp)),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isDragging) {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainer
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                // Drag Handle
                                IconButton(
                                    onClick = {},
                                    modifier = Modifier
                                        .draggableHandle(
                                            onDragStarted = {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        )
                                        .size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DragHandle,
                                        contentDescription = "Reorder",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                // Icon
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = sectionInfo.icon,
                                            contentDescription = null,
                                            tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }

                                // Text Title & Summary
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = sectionInfo.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = sectionInfo.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Switch
                                Switch(
                                    checked = isEnabled,
                                    onCheckedChange = { checked ->
                                        settingsViewModel.setHomeSectionEnabled(sectionId, checked)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
