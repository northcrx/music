package com.nishant.northmusic.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nishant.northmusic.data.model.CategoryRule
import com.nishant.northmusic.data.repository.CategoryRulesRepository
import com.nishant.northmusic.navigation.BottomListScrollPadding
import com.nishant.northmusic.ui.components.AppDialog
import com.nishant.northmusic.ui.theme.PillShape

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryRulesScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    remember { CategoryRulesRepository.init(context) }
    val rules by CategoryRulesRepository.rulesFlow.collectAsState()

    var showAddDialog by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<CategoryRule?>(null) }
    var showResetDialog by remember { mutableStateOf(false) }

    // Dialog state for add/edit
    var inputTag by remember { mutableStateOf("") }
    var inputCategory by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                windowInsets = WindowInsets(0.dp),
                title = {
                    Text(
                        text = "Category rules",
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
                    IconButton(onClick = { showResetDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset to defaults",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    inputTag = ""
                    inputCategory = "Hip-Hop"
                    showAddDialog = true
                },
                shape = PillShape,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text("Add rule", style = MaterialTheme.typography.labelLarge)
                }
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = BottomListScrollPadding),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Rules map source genre and tags into folder names. Matching is case-insensitive and checks every tag.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp, end = 4.dp)
                )
            }

            items(rules, key = { it.tag }) { rule ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh
                            ) {
                                Text(
                                    text = rule.tag,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(16.dp)
                            )

                            Text(
                                text = rule.category,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = {
                                    ruleToEdit = rule
                                    inputTag = rule.tag
                                    inputCategory = rule.category
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = "Edit rule",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            IconButton(
                                onClick = {
                                    CategoryRulesRepository.deleteRule(rule.tag)
                                },
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete rule",
                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Add Rule Dialog
    if (showAddDialog) {
        AppDialog(
            onDismissRequest = { showAddDialog = false },
            title = "Add category rule",
            subtitle = "Map a genre tag to a download folder name",
            confirmButtonText = "Add",
            isConfirmEnabled = inputTag.isNotBlank() && inputCategory.isNotBlank(),
            onConfirm = {
                CategoryRulesRepository.addRule(inputTag, inputCategory)
                showAddDialog = false
            },
            dismissButtonText = "Cancel",
            onDismiss = { showAddDialog = false }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inputTag,
                    onValueChange = { inputTag = it },
                    label = { Text("Tag / Genre") },
                    placeholder = { Text("e.g. trap, punjabi, synthwave") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inputCategory,
                    onValueChange = { inputCategory = it },
                    label = { Text("Folder / Category Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryRulesRepository.PRESET_CATEGORIES.forEach { preset ->
                        FilterChip(
                            selected = inputCategory.equals(preset, ignoreCase = true),
                            onClick = { inputCategory = preset },
                            label = { Text(preset, fontSize = 12.sp) },
                            shape = PillShape
                        )
                    }
                }
            }
        }
    }

    // Edit Rule Dialog
    if (ruleToEdit != null) {
        val targetRule = ruleToEdit!!
        AppDialog(
            onDismissRequest = { ruleToEdit = null },
            title = "Edit category rule",
            confirmButtonText = "Save",
            isConfirmEnabled = inputTag.isNotBlank() && inputCategory.isNotBlank(),
            onConfirm = {
                CategoryRulesRepository.updateRule(targetRule.tag, inputTag, inputCategory)
                ruleToEdit = null
            },
            dismissButtonText = "Cancel",
            onDismiss = { ruleToEdit = null }
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = inputTag,
                    onValueChange = { inputTag = it },
                    label = { Text("Tag / Genre") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = inputCategory,
                    onValueChange = { inputCategory = it },
                    label = { Text("Folder / Category Name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Suggestions:",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryRulesRepository.PRESET_CATEGORIES.forEach { preset ->
                        FilterChip(
                            selected = inputCategory.equals(preset, ignoreCase = true),
                            onClick = { inputCategory = preset },
                            label = { Text(preset, fontSize = 12.sp) },
                            shape = PillShape
                        )
                    }
                }
            }
        }
    }

    // Reset Confirmation Dialog
    if (showResetDialog) {
        AppDialog(
            onDismissRequest = { showResetDialog = false },
            title = "Reset category rules?",
            subtitle = "This will restore the standard mapping rules for Hip-Hop, Pop, Bollywood, English, Rock, Electronic, Classical, Jazz, and Chill.",
            confirmButtonText = "Reset",
            onConfirm = {
                CategoryRulesRepository.resetToDefaults()
                showResetDialog = false
            },
            dismissButtonText = "Cancel",
            onDismiss = { showResetDialog = false }
        ) {
            // No extra content needed
        }
    }
}
