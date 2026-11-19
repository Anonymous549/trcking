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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.LifeGoalEntity
import com.example.domain.util.DateUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.viewmodel.GoalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    viewModel: GoalsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showVisionDialog by remember { mutableStateOf(false) }
    var showGoalDialog by remember { mutableStateOf(false) }
    var milestoneGoalTarget by remember { mutableStateOf<LifeGoalEntity?>(null) }

    val horizons = listOf("ALL", "DAILY", "WEEKLY", "MONTHLY", "YEARLY", "SHORT_TERM", "LONG_TERM", "COMPLETED")
    val filtered = when (state.selectedHorizon) {
        "ALL" -> state.goals.filter { !it.isCompleted }
        "COMPLETED" -> state.goals.filter { it.isCompleted }
        else -> state.goals.filter { !it.isCompleted && it.horizon == state.selectedHorizon }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Goals & Personal Vision") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showGoalDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Goal")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "vision_card") {
                LifeOsCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Personal Long-Term Vision", style = MaterialTheme.typography.titleMedium)
                        IconButton(onClick = { showVisionDialog = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Vision")
                        }
                    }
                    Text(
                        text = state.personalVision,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            item(key = "horizon_filters") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    horizons.forEach { h ->
                        FilterChip(
                            selected = state.selectedHorizon == h,
                            onClick = { viewModel.selectHorizon(h) },
                            label = { Text(h.replace("_", " ")) },
                        )
                    }
                }
            }

            if (filtered.isEmpty()) {
                item(key = "empty_goals") {
                    EmptyStateCard(
                        icon = Icons.Default.Flag,
                        title = "No goals in this view",
                        message = "Create daily, weekly, monthly, yearly, or long-term goals with milestones.",
                        actionLabel = "Add Goal",
                        onActionClick = { showGoalDialog = true },
                    )
                }
            } else {
                items(filtered, key = { it.id }) { goal ->
                    val milestones = state.milestonesByGoal[goal.id].orEmpty()
                    LifeOsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    StatusBadge(text = goal.horizon.replace("_", " "))
                                    StatusBadge(text = goal.category, containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(goal.title, style = MaterialTheme.typography.titleMedium)
                                goal.targetEpochDay?.let {
                                    Text("Target: ${DateUtils.formatShortDate(it)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            Text("${goal.progressPercent}%", style = MonoMetricSmall, color = MaterialTheme.colorScheme.primary)
                            IconButton(onClick = { viewModel.deleteGoal(goal.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete goal")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(progress = { goal.progressPercent / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp))
                        if (milestones.isEmpty()) {
                            Slider(
                                value = goal.progressPercent.toFloat(),
                                onValueChange = { viewModel.updateGoalProgress(goal, it.toInt()) },
                                valueRange = 0f..100f,
                            )
                        } else {
                            Spacer(modifier = Modifier.height(6.dp))
                            milestones.forEach { m ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.toggleMilestone(m, goal) }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Icon(
                                        imageVector = if (m.isDone) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                        contentDescription = "Toggle milestone",
                                        tint = if (m.isDone) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Text(m.title, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                        TextButton(onClick = { milestoneGoalTarget = goal }) {
                            Text("+ Add Milestone")
                        }
                    }
                }
            }
        }
    }

    if (showVisionDialog) {
        var text by remember { mutableStateOf(state.personalVision) }
        AlertDialog(
            onDismissRequest = { showVisionDialog = false },
            title = { Text("Personal Vision Statement") },
            text = {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Long-term ambitions") },
                    minLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveVision(text)
                    showVisionDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showVisionDialog = false }) { Text("Cancel") } },
        )
    }

    if (showGoalDialog) {
        var title by remember { mutableStateOf("") }
        var horizon by remember { mutableStateOf("MONTHLY") }
        var category by remember { mutableStateOf("Personal") }
        var priority by remember { mutableIntStateOf(2) }
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("New Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Title") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("DAILY", "WEEKLY", "MONTHLY", "YEARLY", "SHORT_TERM", "LONG_TERM").forEach { h ->
                            FilterChip(selected = horizon == h, onClick = { horizon = h }, label = { Text(h.replace("_", " ")) })
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "High", 2 to "Med", 3 to "Low").forEach { (p, l) ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(l) })
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveGoal(
                        LifeGoalEntity(
                            title = title.trim(),
                            horizon = horizon,
                            category = category.trim().ifBlank { "Personal" },
                            priority = priority,
                            targetEpochDay = DateUtils.todayEpochDay() + 30,
                        ),
                    )
                    showGoalDialog = false
                }, enabled = title.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showGoalDialog = false }) { Text("Cancel") } },
        )
    }

    milestoneGoalTarget?.let { g ->
        var mTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { milestoneGoalTarget = null },
            title = { Text("Add Milestone") },
            text = { OutlinedTextField(value = mTitle, onValueChange = { mTitle = it }, label = { Text("Milestone") }, singleLine = true) },
            confirmButton = {
                Button(onClick = {
                    viewModel.addMilestone(g.id, mTitle)
                    milestoneGoalTarget = null
                }, enabled = mTitle.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { milestoneGoalTarget = null }) { Text("Cancel") } },
        )
    }
}
