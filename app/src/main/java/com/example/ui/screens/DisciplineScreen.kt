package com.example.ui.screens

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.util.DateUtils
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.PrivacyBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.viewmodel.DisciplineViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisciplineScreen(
    viewModel: DisciplineViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showLogDialog by remember { mutableStateOf(false) }

    // Re-check UsageStats permission when returning from Android Settings
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshUsagePermissionAndSync()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val today = state.todayLog
    val ratio = (today.totalMinutes.toFloat() / today.dailyGoalMinutes.coerceAtLeast(1)).coerceIn(0f, 1.5f)

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Digital Discipline") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "usage_stats_permission") {
                PrivacyBanner(
                    title = if (state.hasUsageStatsPermission) "Device Usage Insights Active" else "Optional UsageStats Access",
                    body = if (state.hasUsageStatsPermission) {
                        "Usage statistics are read locally on your device only. Nothing is ever collected, stored externally, or transmitted."
                    } else {
                        "Optionally grant Android Usage Access to automatically read today's screen time and top distracting apps. Everything stays 100% on-device and is never transmitted."
                    },
                    icon = Icons.Default.Shield,
                    actionLabel = if (state.hasUsageStatsPermission) "Sync Today's Device Usage" else "Grant Usage Access",
                    onAction = {
                        if (state.hasUsageStatsPermission) {
                            viewModel.importFromDeviceUsage()
                        } else {
                            context.startActivity(viewModel.buildUsageSettingsIntent())
                        }
                    },
                )
            }

            item(key = "today_discipline") {
                LifeOsCard {
                    SectionHeader(
                        title = "Today's Digital Budget",
                        subtitle = "Goal: ${DateUtils.formatMinutesReadable(today.dailyGoalMinutes)} · Source: ${today.source}",
                        actionLabel = "Log",
                        actionTag = "edit_screen_time_button",
                        onActionClick = { showLogDialog = true },
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        MetricTile(
                            label = "Screen Time",
                            value = DateUtils.formatMinutesReadable(today.totalMinutes),
                            modifier = Modifier.weight(1f),
                            accentColor = if (today.totalMinutes > today.dailyGoalMinutes) LifeOsThemeTokens.colors.accent else LifeOsThemeTokens.colors.positive,
                        )
                        MetricTile(
                            label = "Social Media",
                            value = DateUtils.formatMinutesReadable(today.socialMediaMinutes),
                            modifier = Modifier.weight(1f),
                        )
                        MetricTile(
                            label = "Pickups / Distractions",
                            value = "${today.distractionCount}",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { ratio.coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = if (ratio > 1f) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    if (today.topAppsSummary.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Top apps today: ${today.topAppsSummary}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showLogDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = null)
                            Text(" Manual Log")
                        }
                        if (state.hasUsageStatsPermission) {
                            OutlinedButton(onClick = { viewModel.importFromDeviceUsage() }) {
                                Icon(Icons.Default.Sync, contentDescription = null)
                                Text(" Refresh")
                            }
                        }
                    }
                }
            }

            item(key = "history_header") {
                SectionHeader(title = "Recent Screen-Time History")
            }
            items(state.logs, key = { it.epochDay }) { log ->
                LifeOsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column {
                            Text(DateUtils.formatShortDate(log.epochDay), style = MaterialTheme.typography.titleSmall)
                            Text(
                                "Social: ${DateUtils.formatMinutesReadable(log.socialMediaMinutes)} · Distractions: ${log.distractionCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        StatusBadge(
                            text = "${DateUtils.formatMinutesReadable(log.totalMinutes)} / ${DateUtils.formatMinutesReadable(log.dailyGoalMinutes)}",
                            icon = Icons.Default.PhoneAndroid,
                        )
                    }
                }
            }
        }
    }

    if (showLogDialog) {
        var total by remember { mutableStateOf(today.totalMinutes.toString()) }
        var social by remember { mutableStateOf(today.socialMediaMinutes.toString()) }
        var distractions by remember { mutableStateOf(today.distractionCount.toString()) }
        var goal by remember { mutableStateOf(today.dailyGoalMinutes.toString()) }
        AlertDialog(
            onDismissRequest = { showLogDialog = false },
            title = { Text("Log Screen Time") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = total, onValueChange = { total = it }, label = { Text("Total Screen Time (min)") }, singleLine = true)
                    OutlinedTextField(value = social, onValueChange = { social = it }, label = { Text("Social Media (min)") }, singleLine = true)
                    OutlinedTextField(value = distractions, onValueChange = { distractions = it }, label = { Text("Distractions / Pickups") }, singleLine = true)
                    OutlinedTextField(value = goal, onValueChange = { goal = it }, label = { Text("Daily Goal (min)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveManualLog(
                        total.toIntOrNull() ?: 0,
                        social.toIntOrNull() ?: 0,
                        distractions.toIntOrNull() ?: 0,
                        goal.toIntOrNull() ?: 150,
                    )
                    showLogDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showLogDialog = false }) { Text("Cancel") } },
        )
    }
}
