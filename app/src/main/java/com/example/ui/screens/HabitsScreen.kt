package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.HabitEntity
import com.example.ui.components.CalendarHeatmapGrid
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.viewmodel.HabitsViewModel

@Composable
fun HabitsScreen(viewModel: HabitsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedHeatmapHint by remember { mutableStateOf<String?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_habit_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Habit")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item(key = "habits_header") {
                SectionHeader(
                    title = "Habits & Discipline",
                    subtitle = "One-tap completion, streaks & rest-day protection",
                )
            }

            item(key = "habit_metrics") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile(
                        label = "Today",
                        value = "${state.todayCompleted}/${state.todayTotal}",
                        modifier = Modifier.weight(1f),
                    )
                    MetricTile(
                        label = "30d Rate",
                        value = "${state.overallRate30d}%",
                        modifier = Modifier.weight(1f),
                        accentColor = LifeOsThemeTokens.colors.positive,
                    )
                    MetricTile(
                        label = "Missed (7d)",
                        value = "${state.missedThisWeek}",
                        modifier = Modifier.weight(1f),
                        accentColor = LifeOsThemeTokens.colors.accent,
                    )
                }
            }

            item(key = "heatmap") {
                LifeOsCard {
                    SectionHeader(
                        title = "12-Week Consistency Heatmap",
                        subtitle = selectedHeatmapHint ?: "Tap any cell to inspect daily completion",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    CalendarHeatmapGrid(
                        days = state.heatmapDays,
                        onDaySelected = { selectedHeatmapHint = it.label },
                    )
                }
            }

            item(key = "category_chips") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.categories.forEach { cat ->
                        FilterChip(
                            selected = state.selectedCategory == cat,
                            onClick = { viewModel.selectCategory(cat) },
                            label = { Text(cat) },
                        )
                    }
                }
            }

            if (state.items.isEmpty()) {
                item(key = "empty_habits") {
                    EmptyStateCard(
                        icon = Icons.Default.Repeat,
                        title = "No habits yet",
                        message = "Create daily, weekly, or custom-schedule habits to build consistency.",
                        actionLabel = "Add Habit",
                        actionTag = "empty_add_habit_button",
                        onActionClick = { showCreateDialog = true },
                    )
                }
            } else {
                items(state.items, key = { it.habit.id }) { item ->
                    val completed = item.todayStatus == "COMPLETED"
                    val restSkipped = item.todayStatus == "SKIPPED_REST"
                    LifeOsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            IconButton(
                                onClick = { viewModel.toggleHabitToday(item.habit.id, item.todayStatus) },
                                modifier = Modifier.testTag("toggle_habit_${item.habit.id}"),
                            ) {
                                Icon(
                                    imageVector = if (completed) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                                    contentDescription = "Complete ${item.habit.name}",
                                    tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(30.dp),
                                )
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.habit.name,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = "${item.habit.category} · ${item.habit.scheduleType} · ${item.stats.completionRate30d}% (30d)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusBadge(
                                        text = "${item.stats.currentStreak}d streak",
                                        icon = Icons.Default.LocalFireDepartment,
                                        containerColor = LifeOsThemeTokens.colors.warningContainer,
                                        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                    )
                                    StatusBadge(
                                        text = "Best ${item.stats.bestStreak}d",
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    if (restSkipped) {
                                        StatusBadge(text = "Rest Day")
                                    }
                                }
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                if (item.habit.allowRestDayExemption && !completed && !restSkipped) {
                                    IconButton(onClick = { viewModel.markRestDay(item.habit.id) }) {
                                        Icon(Icons.Default.Bedtime, contentDescription = "Take rest day")
                                    }
                                }
                                IconButton(onClick = { viewModel.deleteHabit(item.habit.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete habit")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateHabitDialog(
            onDismiss = { showCreateDialog = false },
            onSave = { habit ->
                viewModel.saveHabit(habit)
                showCreateDialog = false
            },
        )
    }
}

@Composable
private fun CreateHabitDialog(
    onDismiss: () -> Unit,
    onSave: (HabitEntity) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Discipline") }
    var scheduleType by remember { mutableStateOf("DAILY") }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5)) }
    var allowRest by remember { mutableStateOf(true) }

    val dayLabels = listOf(1 to "M", 2 to "T", 3 to "W", 4 to "T", 5 to "F", 6 to "S", 7 to "S")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("habit_name_input"),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("DAILY", "WEEKLY", "CUSTOM").forEach { sched ->
                        FilterChip(
                            selected = scheduleType == sched,
                            onClick = { scheduleType = sched },
                            label = { Text(sched) },
                        )
                    }
                }
                if (scheduleType != "DAILY") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        dayLabels.forEach { (iso, lbl) ->
                            val picked = iso in selectedDays
                            FilterChip(
                                selected = picked,
                                onClick = {
                                    selectedDays = if (picked) selectedDays - iso else selectedDays + iso
                                },
                                label = { Text(lbl) },
                            )
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = allowRest, onCheckedChange = { allowRest = it })
                    Text("Protect streak on rest days", style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val daysCsv = if (scheduleType == "DAILY") {
                        "1,2,3,4,5,6,7"
                    } else {
                        selectedDays.sorted().joinToString(",").ifEmpty { "1,2,3,4,5,6,7" }
                    }
                    onSave(
                        HabitEntity(
                            name = name.trim(),
                            category = category.trim().ifBlank { "Discipline" },
                            scheduleType = scheduleType,
                            targetDaysCsv = daysCsv,
                            allowRestDayExemption = allowRest,
                        ),
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.testTag("save_habit_button"),
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
