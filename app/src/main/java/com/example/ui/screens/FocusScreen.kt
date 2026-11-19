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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.CareerItemEntity
import com.example.data.local.entity.RoutineBlockEntity
import com.example.data.local.entity.StudySubjectEntity
import com.example.data.local.entity.TaskEntity
import com.example.domain.util.DateUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.theme.MonoMetricLarge
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.viewmodel.FocusViewModel

private enum class FocusSubTab(val label: String) {
    TASKS("Tasks"),
    STUDY("Study & Career"),
    ROUTINE("Routine"),
}

@Composable
fun FocusScreen(viewModel: FocusViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var activeTab by remember { mutableStateOf(FocusSubTab.TASKS) }
    var taskFilter by remember { mutableStateOf("ACTIVE") }

    var showTaskDialog by remember { mutableStateOf(false) }
    var showSubjectDialog by remember { mutableStateOf(false) }
    var showCareerDialog by remember { mutableStateOf(false) }
    var showRoutineDialog by remember { mutableStateOf(false) }
    var topicTargetSubject by remember { mutableStateOf<StudySubjectEntity?>(null) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    when (activeTab) {
                        FocusSubTab.TASKS -> showTaskDialog = true
                        FocusSubTab.STUDY -> showSubjectDialog = true
                        FocusSubTab.ROUTINE -> showRoutineDialog = true
                    }
                },
                modifier = Modifier.testTag("focus_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add item")
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
            item(key = "focus_header") {
                SectionHeader(
                    title = "Focus, Study & Routine",
                    subtitle = "Tasks, Pomodoro timer, career growth & daily timeline",
                )
                Spacer(modifier = Modifier.height(10.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    FocusSubTab.entries.forEachIndexed { index, tab ->
                        SegmentedButton(
                            selected = activeTab == tab,
                            onClick = { activeTab = tab },
                            shape = SegmentedButtonDefaults.itemShape(index, FocusSubTab.entries.size),
                            modifier = Modifier.testTag("focus_tab_${tab.name.lowercase()}"),
                        ) {
                            Text(tab.label, maxLines = 1)
                        }
                    }
                }
            }

            when (activeTab) {
                FocusSubTab.TASKS -> {
                    item(key = "task_filters") {
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            listOf("ACTIVE" to "Active", "OVERDUE" to "Overdue", "COMPLETED" to "Completed").forEach { (k, lbl) ->
                                FilterChip(
                                    selected = taskFilter == k,
                                    onClick = { taskFilter = k },
                                    label = { Text(lbl) },
                                )
                            }
                        }
                    }

                    val filteredTasks = when (taskFilter) {
                        "OVERDUE" -> state.tasks.filter { !it.isCompleted && it.dueEpochDay != null && it.dueEpochDay < state.todayEpochDay }
                        "COMPLETED" -> state.tasks.filter { it.isCompleted }
                        else -> state.tasks.filter { !it.isCompleted }
                    }

                    if (filteredTasks.isEmpty()) {
                        item(key = "empty_tasks") {
                            EmptyStateCard(
                                icon = Icons.Default.TaskAlt,
                                title = "No ${taskFilter.lowercase()} tasks",
                                message = "Keep your to-do list intentional and focused.",
                                actionLabel = "Add Task",
                                onActionClick = { showTaskDialog = true },
                            )
                        }
                    } else {
                        items(filteredTasks, key = { it.id }) { task ->
                            LifeOsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    IconButton(onClick = { viewModel.toggleTask(task) }) {
                                        Icon(
                                            imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Toggle ${task.title}",
                                            tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = task.title,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                                            ),
                                        )
                                        val dueTxt = task.dueEpochDay?.let { DateUtils.formatShortDate(it) } ?: "No deadline"
                                        val recTxt = if (task.recurrence != "NONE") " · ${task.recurrence}" else ""
                                        Text(
                                            text = "$dueTxt$recTxt",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = if (!task.isCompleted && task.dueEpochDay != null && task.dueEpochDay < state.todayEpochDay) {
                                                MaterialTheme.colorScheme.error
                                            } else {
                                                MaterialTheme.colorScheme.onSurfaceVariant
                                            },
                                        )
                                    }
                                    IconButton(onClick = { viewModel.toggleTopThree(task) }) {
                                        Icon(
                                            Icons.Default.PushPin,
                                            contentDescription = "Pin Top 3",
                                            tint = if (task.isTopThree) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        )
                                    }
                                    IconButton(onClick = { viewModel.deleteTask(task.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete task")
                                    }
                                }
                            }
                        }
                    }
                }

                FocusSubTab.STUDY -> {
                    // Pomodoro Timer Card
                    item(key = "pomodoro_card") {
                        LifeOsCard(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = if (state.pomodoro.isBreak) "Break Timer" else "Pomodoro Focus Timer",
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                StatusBadge(text = "${state.pomodoro.completedPomodorosToday} today")
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = DateUtils.formatTimerSeconds(state.pomodoro.remainingSeconds),
                                style = MonoMetricLarge,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            if (state.subjects.isNotEmpty()) {
                                Row(
                                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    state.subjects.forEach { subj ->
                                        FilterChip(
                                            selected = state.pomodoro.selectedSubjectId == subj.id,
                                            onClick = { viewModel.selectPomodoroSubject(subj.id) },
                                            label = { Text(subj.name) },
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { viewModel.toggleTimer() },
                                    modifier = Modifier.testTag("pomodoro_toggle_button"),
                                ) {
                                    Icon(
                                        imageVector = if (state.pomodoro.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                                        contentDescription = null,
                                    )
                                    Text(if (state.pomodoro.isRunning) " Pause" else " Start")
                                }
                                OutlinedButton(onClick = { viewModel.completeSessionEarly() }) {
                                    Icon(Icons.Default.Stop, contentDescription = null)
                                    Text(" Log")
                                }
                                IconButton(onClick = { viewModel.resetTimer() }) {
                                    Icon(Icons.Default.Refresh, contentDescription = "Reset timer")
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf(15, 25, 45, 60).forEach { mins ->
                                    FilterChip(
                                        selected = state.pomodoro.focusMinutes == mins,
                                        onClick = { viewModel.setFocusDuration(mins) },
                                        label = { Text("${mins}m") },
                                    )
                                }
                            }
                        }
                    }

                    // Weekly Study Report
                    item(key = "study_weekly_report") {
                        LifeOsCard {
                            SectionHeader(
                                title = "Weekly Study Report",
                                subtitle = "${DateUtils.formatMinutesReadable(state.weeklyStudyMinutes)} across ${state.weeklyStudySessionCount} sessions",
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            WeeklyBarChart(points = state.weeklyStudyBars)
                        }
                    }

                    // Subjects, Topics & Exam Countdowns
                    item(key = "subjects_header") {
                        SectionHeader(
                            title = "Subjects & Exam Countdowns",
                            actionLabel = "+ Subject",
                            onActionClick = { showSubjectDialog = true },
                        )
                    }
                    if (state.subjects.isEmpty()) {
                        item(key = "empty_subjects") {
                            EmptyStateCard(
                                icon = Icons.Default.School,
                                title = "No study subjects yet",
                                message = "Add courses, topics for spaced revision, and exam dates.",
                                actionLabel = "Add Subject",
                                onActionClick = { showSubjectDialog = true },
                            )
                        }
                    } else {
                        items(state.subjects, key = { "subj_${it.id}" }) { subj ->
                            val subjTopics = state.topics.filter { it.subjectId == subj.id }
                            val masteredCount = subjTopics.count { it.isMastered }
                            val progress = if (subjTopics.isEmpty()) 0f else masteredCount.toFloat() / subjTopics.size
                            LifeOsCard {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(subj.name, style = MaterialTheme.typography.titleMedium)
                                        Text(
                                            text = "${subj.category} · Target ${subj.targetWeeklyHours.toInt()}h/wk",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    subj.examEpochDay?.let { examDay ->
                                        val left = DateUtils.daysBetween(state.todayEpochDay, examDay)
                                        if (left >= 0) {
                                            StatusBadge(
                                                text = "${left}d to exam",
                                                containerColor = LifeOsThemeTokens.colors.warningContainer,
                                                contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                                            )
                                        }
                                    }
                                    IconButton(onClick = { viewModel.deleteSubject(subj.id) }) {
                                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete subject")
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { progress },
                                    modifier = Modifier.fillMaxWidth().height(6.dp),
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                subjTopics.forEach { topic ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.toggleTopicMastered(topic) }
                                            .padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Icon(
                                            imageVector = if (topic.isMastered) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                            contentDescription = "Toggle topic",
                                            tint = if (topic.isMastered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(topic.title, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                                        topic.nextRevisionEpochDay?.let { rev ->
                                            Text(
                                                text = "Rev ${DateUtils.formatShortDate(rev)}",
                                                style = MonoMetricSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                                TextButton(onClick = { topicTargetSubject = subj }) {
                                    Text("+ Add Topic / Chapter")
                                }
                            }
                        }
                    }

                    // Career, Skills & Projects
                    item(key = "career_header") {
                        SectionHeader(
                            title = "Career, Skills & Projects",
                            actionLabel = "+ Add",
                            onActionClick = { showCareerDialog = true },
                        )
                    }
                    items(state.careerItems, key = { "career_${it.id}" }) { item ->
                        LifeOsCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    StatusBadge(text = item.itemType)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(item.title, style = MaterialTheme.typography.titleSmall)
                                }
                                Text("${item.progressPercent}%", style = MonoMetricSmall)
                                IconButton(onClick = { viewModel.deleteCareerItem(item.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete career item")
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { item.progressPercent / 100f },
                                modifier = Modifier.fillMaxWidth().height(6.dp),
                            )
                        }
                    }
                }

                FocusSubTab.ROUTINE -> {
                    item(key = "routine_header") {
                        LifeOsCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Daily Routine Timeline", style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        "8:00 AM starter schedule — tap to check off or customize",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                TextButton(onClick = { viewModel.resetStarterRoutine() }) {
                                    Icon(Icons.Default.RestartAlt, contentDescription = null)
                                    Text(" Reset 8 AM")
                                }
                            }
                        }
                    }
                    items(state.routineBlocks, key = { "routine_${it.id}" }) { block ->
                        val doneToday = block.lastCompletedEpochDay == state.todayEpochDay
                        LifeOsCard(onClick = { viewModel.toggleRoutineToday(block) }) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Text(
                                    text = DateUtils.formatMinuteOfDay(block.startMinuteOfDay),
                                    style = MonoMetricSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Icon(
                                    imageVector = if (doneToday) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                    contentDescription = "Toggle routine block",
                                    tint = if (doneToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = block.title,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            textDecoration = if (doneToday) TextDecoration.LineThrough else TextDecoration.None,
                                        ),
                                    )
                                    Text(
                                        text = block.category,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                IconButton(onClick = { viewModel.deleteRoutineBlock(block.id) }) {
                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Delete block")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTaskDialog) {
        var title by remember { mutableStateOf("") }
        var priority by remember { mutableIntStateOf(2) }
        var recurrence by remember { mutableStateOf("NONE") }
        var isTop3 by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showTaskDialog = false },
            title = { Text("New Task") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Task Title") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("task_title_input"),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(1 to "High", 2 to "Med", 3 to "Low").forEach { (p, lbl) ->
                            FilterChip(selected = priority == p, onClick = { priority = p }, label = { Text(lbl) })
                        }
                    }
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("NONE", "DAILY", "WEEKLY", "MONTHLY").forEach { r ->
                            FilterChip(selected = recurrence == r, onClick = { recurrence = r }, label = { Text(r) })
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isTop3, onCheckedChange = { isTop3 = it })
                        Text("Pin to Top 3")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTask(
                            TaskEntity(
                                title = title.trim(),
                                priority = priority,
                                dueEpochDay = state.todayEpochDay,
                                recurrence = recurrence,
                                isTopThree = isTop3,
                            ),
                        )
                        showTaskDialog = false
                    },
                    enabled = title.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTaskDialog = false }) { Text("Cancel") } },
        )
    }

    if (showSubjectDialog) {
        var name by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("Academic") }
        var examDays by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSubjectDialog = false },
            title = { Text("New Study Subject") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Subject / Course") }, singleLine = true)
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                    OutlinedTextField(value = examDays, onValueChange = { examDays = it }, label = { Text("Days until exam (optional)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val days = examDays.toLongOrNull()
                        viewModel.saveSubject(
                            StudySubjectEntity(
                                name = name.trim(),
                                category = category.trim().ifBlank { "Academic" },
                                examEpochDay = days?.let { state.todayEpochDay + it },
                            ),
                        )
                        showSubjectDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showSubjectDialog = false }) { Text("Cancel") } },
        )
    }

    topicTargetSubject?.let { subj ->
        var topicTitle by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { topicTargetSubject = null },
            title = { Text("Add Topic to ${subj.name}") },
            text = {
                OutlinedTextField(value = topicTitle, onValueChange = { topicTitle = it }, label = { Text("Topic / Chapter") }, singleLine = true)
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveTopic(subj.id, topicTitle)
                        topicTargetSubject = null
                    },
                    enabled = topicTitle.isNotBlank(),
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { topicTargetSubject = null }) { Text("Cancel") } },
        )
    }

    if (showCareerDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("SKILL") }
        var progress by remember { mutableStateOf("25") }
        AlertDialog(
            onDismissRequest = { showCareerDialog = false },
            title = { Text("Add Career / Skill Item") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        listOf("SKILL", "CAREER_GOAL", "PROJECT", "ASSIGNMENT").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = progress, onValueChange = { progress = it }, label = { Text("Progress % (0-100)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCareerItem(
                            CareerItemEntity(
                                title = title.trim(),
                                itemType = type,
                                progressPercent = (progress.toIntOrNull() ?: 0).coerceIn(0, 100),
                            ),
                        )
                        showCareerDialog = false
                    },
                    enabled = title.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCareerDialog = false }) { Text("Cancel") } },
        )
    }

    if (showRoutineDialog) {
        var title by remember { mutableStateOf("") }
        var hour by remember { mutableStateOf("9") }
        var minute by remember { mutableStateOf("0") }
        var category by remember { mutableStateOf("Routine") }
        AlertDialog(
            onDismissRequest = { showRoutineDialog = false },
            title = { Text("Add Routine Block") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Activity") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(value = hour, onValueChange = { hour = it }, label = { Text("Hour (0-23)") }, modifier = Modifier.weight(1f), singleLine = true)
                        OutlinedTextField(value = minute, onValueChange = { minute = it }, label = { Text("Min (0-59)") }, modifier = Modifier.weight(1f), singleLine = true)
                    }
                    OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val h = (hour.toIntOrNull() ?: 8).coerceIn(0, 23)
                        val m = (minute.toIntOrNull() ?: 0).coerceIn(0, 59)
                        viewModel.saveRoutineBlock(
                            RoutineBlockEntity(
                                startMinuteOfDay = h * 60 + m,
                                title = title.trim(),
                                category = category.trim().ifBlank { "Routine" },
                            ),
                        )
                        showRoutineDialog = false
                    },
                    enabled = title.isNotBlank(),
                ) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showRoutineDialog = false }) { Text("Cancel") } },
        )
    }
}
