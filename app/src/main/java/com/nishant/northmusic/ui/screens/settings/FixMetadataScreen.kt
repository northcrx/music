package com.nishant.northmusic.ui.screens.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nishant.northmusic.data.network.MetadataConfidence
import com.nishant.northmusic.data.network.MetadataIssueType
import com.nishant.northmusic.data.network.MetadataSuggestion
import com.nishant.northmusic.data.network.MetadataSuggestionService
import com.nishant.northmusic.data.network.ScanProgress
import com.nishant.northmusic.data.repository.MusicRepository
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.AsyncSongArtwork
import com.nishant.northmusic.ui.theme.PillShape
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FixMetadataScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val musicRepo = MusicRepository.getInstance(context)
    val songs by musicRepo.songs.collectAsState()

    var isScanning by remember { mutableStateOf(false) }
    var isApplyingAll by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf<ScanProgress?>(null) }
    val suggestions = remember { mutableStateListOf<MetadataSuggestion>() }
    val cancelFlag = remember { AtomicBoolean(false) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    fun stopScan() {
        cancelFlag.set(true)
        scanJob?.cancel()
        isScanning = false
    }

    fun startScan() {
        stopScan()
        cancelFlag.set(false)
        isScanning = true
        suggestions.clear()
        scanProgress = null

        scanJob = scope.launch(Dispatchers.IO) {
            MetadataSuggestionService.analyzeAndFetchSuggestionsStreaming(
                context = context,
                songs = if (songs.isNotEmpty()) songs else musicRepo.songs.value,
                onProgress = { progress ->
                    scanProgress = progress
                },
                onSuggestionFound = { suggestion ->
                    // Add suggestion live to the list
                    if (suggestions.none { it.song.id == suggestion.song.id }) {
                        suggestions.add(suggestion)
                    }
                },
                isCancelled = { cancelFlag.get() }
            )
            isScanning = false
        }
    }

    LaunchedEffect(Unit) {
        startScan()
    }

    val activeSuggestions = suggestions.filter { !it.isApplied && !it.isSkipped }
    val highConfidenceCount = activeSuggestions.count { it.confidence == MetadataConfidence.HIGH }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        TopAppBar(
            windowInsets = WindowInsets(0.dp),
            title = {
                Text(
                    text = "Fix metadata & artwork",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            navigationIcon = {
                IconButton(
                    onClick = {
                        stopScan()
                        onNavigateBack()
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            actions = {
                IconButton(
                    onClick = { startScan() },
                    enabled = !isScanning && !isApplyingAll
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Rescan",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
        )

        when {
            // Initial scanning with 0 items found yet
            isScanning && suggestions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 3.dp,
                                modifier = Modifier.size(44.dp)
                            )
                            val progress = scanProgress
                            if (progress != null && progress.total > 0) {
                                Text(
                                    text = "Analyzing candidate songs (${progress.current}/${progress.total})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = progress.currentSongTitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                LinearProgressIndicator(
                                    progress = { progress.current.toFloat() / progress.total.toFloat() },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp)),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
                                )
                            } else {
                                Text(
                                    text = "Scanning library for missing tags & artwork...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            OutlinedButton(
                                onClick = { stopScan() },
                                shape = PillShape
                            ) {
                                Icon(Icons.Rounded.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop scanning")
                            }
                        }
                    }
                }
            }

            // Finished scanning and no issues found
            !isScanning && activeSuggestions.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = "No metadata issues detected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "All analyzed songs have verified titles, artists, albums, and artwork tags.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { startScan() },
                            shape = PillShape
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Again")
                        }
                    }
                }
            }

            // Suggestions found (or scanning in background while streaming)
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomListScrollPadding),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Live scan status card if scanning continues in background
                    if (isScanning) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        val progress = scanProgress
                                        val statusText = if (progress != null && progress.total > 0) {
                                            "Scanning (${progress.current}/${progress.total}) • ${suggestions.size} found"
                                        } else {
                                            "Scanning library • ${suggestions.size} found"
                                        }
                                        Text(
                                            text = statusText,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        if (progress != null) {
                                            Text(
                                                text = progress.currentSongTitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    TextButton(onClick = { stopScan() }) {
                                        Text("Stop", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }

                    // Header Card with responsive, non-crushed Action Button
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "${activeSuggestions.size} issues found",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "$highConfidenceCount high confidence • Writes directly to file tags",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                if (highConfidenceCount > 0) {
                                    Button(
                                        onClick = {
                                            isApplyingAll = true
                                            scope.launch(Dispatchers.IO) {
                                                val highItems = suggestions.filter {
                                                    !it.isApplied && !it.isSkipped && it.confidence == MetadataConfidence.HIGH
                                                }
                                                for (item in highItems) {
                                                    MetadataSuggestionService.applySuggestion(context, item)
                                                    val idx = suggestions.indexOfFirst { it.song.id == item.song.id }
                                                    if (idx != -1) {
                                                        suggestions[idx] = suggestions[idx].copy(isApplied = true)
                                                    }
                                                }
                                                isApplyingAll = false
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        enabled = !isApplyingAll,
                                        shape = PillShape,
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                                    ) {
                                        if (isApplyingAll) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(16.dp),
                                                strokeWidth = 2.dp,
                                                color = MaterialTheme.colorScheme.onPrimary
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Applying tags to files...",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        } else {
                                            Icon(
                                                imageVector = Icons.Rounded.AutoFixHigh,
                                                contentDescription = null,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Apply all high confidence ($highConfidenceCount)",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // List of suggestion items
                    items(suggestions.filter { !it.isSkipped }, key = { it.song.id }) { item ->
                        SuggestionCard(
                            suggestion = item,
                            onApply = {
                                scope.launch(Dispatchers.IO) {
                                    MetadataSuggestionService.applySuggestion(context, item)
                                    val idx = suggestions.indexOfFirst { it.song.id == item.song.id }
                                    if (idx != -1) {
                                        suggestions[idx] = suggestions[idx].copy(isApplied = true)
                                    }
                                }
                            },
                            onSkip = {
                                val idx = suggestions.indexOfFirst { it.song.id == item.song.id }
                                if (idx != -1) {
                                    suggestions[idx] = suggestions[idx].copy(isSkipped = true)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SuggestionCard(
    suggestion: MetadataSuggestion,
    onApply: () -> Unit,
    onSkip: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Issue badge + Confidence badge row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                ) {
                    Text(
                        text = suggestion.issueType.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.Medium
                    )
                }

                val confidenceColor = when (suggestion.confidence) {
                    MetadataConfidence.HIGH -> MaterialTheme.colorScheme.primary
                    MetadataConfidence.MEDIUM -> Color(0xFFEA580C)
                    MetadataConfidence.LOW -> MaterialTheme.colorScheme.onSurfaceVariant
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = confidenceColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = suggestion.confidence.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = confidenceColor,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Current state row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AsyncSongArtwork(
                    albumArtUri = suggestion.song.albumArtUri,
                    colorSeed = suggestion.song.colorSeed,
                    size = 46.dp,
                    shape = RoundedCornerShape(10.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current: ${suggestion.song.title}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${suggestion.song.artist} • ${suggestion.song.album}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Proposed Suggestion box
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (!suggestion.suggestedArtworkUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = suggestion.suggestedArtworkUrl,
                            contentDescription = "Suggested artwork",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Suggested: ${suggestion.suggestedTitle}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${suggestion.suggestedArtist} • ${suggestion.suggestedAlbum}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Action row
            if (suggestion.isApplied) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Embedded into file tags & artwork updated",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onSkip) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = onApply,
                        shape = PillShape
                    ) {
                        Text("Apply to file")
                    }
                }
            }
        }
    }
}
