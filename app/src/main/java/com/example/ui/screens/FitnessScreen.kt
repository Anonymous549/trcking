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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
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
import com.example.data.local.entity.WorkoutExerciseEntity
import com.example.domain.util.DateUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.PrivacyBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.TrendLineChart
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.viewmodel.FitnessViewModel

private enum class FitnessTab(val label: String) {
    WORKOUTS("Workouts"),
    METRICS("Health & Body"),
    LIBRARY("Exercises"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FitnessScreen(
    viewModel: FitnessViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(FitnessTab.WORKOUTS) }
    var showWorkoutDialog by remember { mutableStateOf(false) }
    var showMetricsDialog by remember { mutableStateOf(false) }
    var showExerciseDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Fitness & Health") },
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
                onClick = {
                    when (activeTab) {
                        FitnessTab.WORKOUTS -> showWorkoutDialog = true
                        FitnessTab.METRICS -> showMetricsDialog = true
                        FitnessTab.LIBRARY -> showExerciseDialog = true
                    }
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add fitness entry")
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
            item(key = "fitness_tabs") {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    FitnessTab.entries.forEachIndexed { idx, tab ->
                        SegmentedButton(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(idx, FitnessTab.entries.size),
                        ) {
                            Text(tab.label, maxLines = 1)
                        }
                    }
                }
            }

            item(key = "disclaimer") {
                PrivacyBanner(
                    title = "Personal Wellness Log",
                    body = "For personal tracking only. LIFE OS 365 does not provide medical advice, diagnoses, or health claims.",
                    icon = Icons.Default.Info,
                )
            }

            when (activeTab) {
                FitnessTab.WORKOUTS -> {
                    if (state.workouts.isEmpty()) {
                        item(key = "empty_workouts") {
                            EmptyStateCard(
                                icon = Icons.Default.FitnessCenter,
                                title = "No workouts logged yet",
                                message = "Log sets, reps, weights, and duration to track physical progress.",
                                actionLabel = "Log Workout",
                                onActionClick = { showWorkoutDialog = true },
                            )
                        }
                    } else {
                        items(state.workouts, key = { it.id }) { workout ->
                            val exercises = state.workoutExercises[workout.id].orEmpty()
                            LifeOsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(workout.title, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${DateUtils.formatShortDate(workout.epochDay)} · ${workout.durationMinutes} min${if (workout.routineName.isNotBlank()) " · ${workout.routineName}" else ""}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteWorkout(workout.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete workout")
                                    }
                                }
                                if (exercises.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    exercises.forEach { ex ->
                                        val weightStr = if (ex.weightKg > 0f) " @ ${ex.weightKg}kg" else ""
                                        Text(
                                            text = "• ${ex.exerciseName}: ${ex.sets} × ${ex.reps}$weightStr",
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                FitnessTab.METRICS -> {
                    item(key = "today_health") {
                        val m = state.todayMetric
                        LifeOsCard {
                            SectionHeader(
                                title = "Today's Health Snapshot",
                                actionLabel = "Edit",
                                onActionClick = { showMetricsDialog = true },
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricTile("Steps", "${m.steps}", Modifier.weight(1f))
                                MetricTile("Water", "${m.waterMl} ml", Modifier.weight(1f), accentColor = MaterialTheme.colorScheme.primary)
                                MetricTile("Sleep", "${m.sleepHours} hrs", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                MetricTile("Weight", m.weightKg?.let { "$it kg" } ?: "—", Modifier.weight(1f))
                                MetricTile("Calories", "${m.calories} kcal", Modifier.weight(1f))
                                MetricTile("Protein", "${m.proteinG} g", Modifier.weight(1f))
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.addWater(250) }) {
                                    Icon(Icons.Default.WaterDrop, contentDescription = null)
                                    Text(" +250 ml Water")
                                }
                                OutlinedButton(onClick = { showMetricsDialog = true }) {
                                    Text("Update Metrics")
                                }
                            }
                        }
                    }

                    if (state.weightTrend.size >= 2) {
                        item(key = "weight_chart") {
                            LifeOsCard {
                                SectionHeader(title = "Weight Trend (kg)")
                                Spacer(modifier = Modifier.height(10.dp))
                                TrendLineChart(
                                    values = state.weightTrend,
                                    labels = listOf(state.weightLabels.first(), state.weightLabels.last()),
                                    lineColor = LifeOsThemeTokens.colors.accent,
                                )
                            }
                        }
                    }
                }

                FitnessTab.LIBRARY -> {
                    items(state.templates, key = { it.id }) { tpl ->
                        LifeOsCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column {
                                    Text(tpl.name, style = MaterialTheme.typography.titleSmall)
                                    Text(
                                        "${tpl.muscleGroup} · ${tpl.equipment}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (tpl.isCustom) {
                                    StatusBadge(text = "Custom")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showWorkoutDialog) {
        LogWorkoutDialog(
            templates = state.templates.map { it.name },
            onDismiss = { showWorkoutDialog = false },
            onSave = { title, routine, duration, notes, exercises ->
                viewModel.logWorkout(title, routine, duration, notes, exercises)
                showWorkoutDialog = false
            },
        )
    }

    if (showMetricsDialog) {
        val cur = state.todayMetric
        var steps by remember { mutableStateOf(cur.steps.toString()) }
        var weight by remember { mutableStateOf(cur.weightKg?.toString() ?: "") }
        var waist by remember { mutableStateOf(cur.waistCm?.toString() ?: "") }
        var chest by remember { mutableStateOf(cur.chestCm?.toString() ?: "") }
        var calories by remember { mutableStateOf(cur.calories.toString()) }
        var protein by remember { mutableStateOf(cur.proteinG.toString()) }
        var sleep by remember { mutableStateOf(cur.sleepHours.toString()) }
        AlertDialog(
            onDismissRequest = { showMetricsDialog = false },
            title = { Text("Today's Health & Nutrition") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = steps, onValueChange = { steps = it }, label = { Text("Steps") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = sleep, onValueChange = { sleep = it }, label = { Text("Sleep (h)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight kg") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = waist, onValueChange = { waist = it }, label = { Text("Waist cm") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = calories, onValueChange = { calories = it }, label = { Text("Calories") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = protein, onValueChange = { protein = it }, label = { Text("Protein g") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = chest, onValueChange = { chest = it }, label = { Text("Chest cm (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateTodayMetrics(
                        steps = steps.toIntOrNull() ?: 0,
                        weightKg = weight.toFloatOrNull(),
                        waistCm = waist.toFloatOrNull(),
                        chestCm = chest.toFloatOrNull(),
                        calories = calories.toIntOrNull() ?: 0,
                        proteinG = protein.toIntOrNull() ?: 0,
                        carbsG = cur.carbsG,
                        fatsG = cur.fatsG,
                        sleepHours = sleep.toFloatOrNull() ?: 0f,
                        nutritionNote = cur.nutritionNote,
                    )
                    showMetricsDialog = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showMetricsDialog = false }) { Text("Cancel") } },
        )
    }

    if (showExerciseDialog) {
        var name by remember { mutableStateOf("") }
        var group by remember { mutableStateOf("Full Body") }
        var equip by remember { mutableStateOf("Bodyweight") }
        AlertDialog(
            onDismissRequest = { showExerciseDialog = false },
            title = { Text("Add Custom Exercise") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Exercise Name") }, singleLine = true)
                    OutlinedTextField(value = group, onValueChange = { group = it }, label = { Text("Muscle Group") }, singleLine = true)
                    OutlinedTextField(value = equip, onValueChange = { equip = it }, label = { Text("Equipment") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.addCustomExercise(name, group, equip)
                    showExerciseDialog = false
                }, enabled = name.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showExerciseDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun LogWorkoutDialog(
    templates: List<String>,
    onDismiss: () -> Unit,
    onSave: (String, String, Int, String, List<WorkoutExerciseEntity>) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var routine by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("45") }
    var exName by remember { mutableStateOf(templates.firstOrNull() ?: "Push-ups") }
    var sets by remember { mutableStateOf("3") }
    var reps by remember { mutableStateOf("12") }
    var weight by remember { mutableStateOf("0") }
    var addedExercises by remember { mutableStateOf<List<WorkoutExerciseEntity>>(emptyList()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log Workout") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Workout Title") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = routine, onValueChange = { routine = it }, label = { Text("Routine") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = duration, onValueChange = { duration = it }, label = { Text("Minutes") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                Text("Add Exercise Row", style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = exName, onValueChange = { exName = it }, label = { Text("Exercise") }, singleLine = true)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = sets, onValueChange = { sets = it }, label = { Text("Sets") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = reps, onValueChange = { reps = it }, label = { Text("Reps") }, modifier = Modifier.weight(1f), singleLine = true)
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("kg") }, modifier = Modifier.weight(1f), singleLine = true)
                }
                TextButton(onClick = {
                    if (exName.isNotBlank()) {
                        addedExercises = addedExercises + WorkoutExerciseEntity(
                            workoutId = 0,
                            exerciseName = exName.trim(),
                            sets = sets.toIntOrNull() ?: 3,
                            reps = reps.toIntOrNull() ?: 10,
                            weightKg = weight.toFloatOrNull() ?: 0f,
                        )
                    }
                }) {
                    Text("+ Append Exercise (${addedExercises.size} added)")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val list = if (addedExercises.isEmpty() && exName.isNotBlank()) {
                        listOf(
                            WorkoutExerciseEntity(
                                workoutId = 0,
                                exerciseName = exName.trim(),
                                sets = sets.toIntOrNull() ?: 3,
                                reps = reps.toIntOrNull() ?: 10,
                                weightKg = weight.toFloatOrNull() ?: 0f,
                            ),
                        )
                    } else addedExercises
                    onSave(title, routine, duration.toIntOrNull() ?: 30, "", list)
                },
                enabled = title.isNotBlank() && (duration.toIntOrNull() ?: 0) > 0,
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
