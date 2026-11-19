package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.DashboardWidget
import com.example.domain.util.DateUtils
import com.example.ui.components.CompletionRingCanvas
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.viewmodel.DashboardUiState
import com.example.ui.viewmodel.DashboardViewModel

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onNavigateHabits: () -> Unit,
    onNavigateFocus: () -> Unit,
    onNavigateFitness: () -> Unit,
    onNavigateFinance: () -> Unit,
    onNavigateGoals: () -> Unit,
    onNavigateJournal: () -> Unit,
    onNavigateAnalytics: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showCustomizeDialog by remember { mutableStateOf(false) }
    var showQuickHabitDialog by remember { mutableStateOf(false) }
    var showQuickTaskDialog by remember { mutableStateOf(false) }
    var showQuickExpenseDialog by remember { mutableStateOf(false) }
    var showQuickJournalDialog by remember { mutableStateOf(false) }
    var showQuickSetupDialog by remember { mutableStateOf(false) }

    val visibleWidgets = remember(state.prefs.dashboardWidgetOrder, state.prefs.hiddenDashboardWidgets) {
        state.prefs.dashboardWidgetOrder
            .mapNotNull { id -> DashboardWidget.entries.firstOrNull { it.id == id } }
            .filter { it.id !in state.prefs.hiddenDashboardWidgets }
    }

    val remainingWidgets = remember(visibleWidgets) {
        visibleWidgets.filter { it != DashboardWidget.DAILY_PROGRESS }
    }
    val showDailyProgress = DashboardWidget.DAILY_PROGRESS in visibleWidgets

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp)
            .testTag("dashboard_list"),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Welcome Header & Date
        item(key = "header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = DateUtils.formatFullDate(state.todayEpochDay),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.testTag("dashboard_date"),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Welcome back, ${state.prefs.userName}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.testTag("dashboard_welcome"),
                    )
                }
                IconButton(
                    onClick = { showCustomizeDialog = true },
                    modifier = Modifier.testTag("customize_dashboard_button"),
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Customize Dashboard",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        // Primary Summary Widget (Daily Completion Percentage & Streaks)
        if (showDailyProgress) {
            item(key = DashboardWidget.DAILY_PROGRESS.id) {
                DailyProgressWidget(state, onNavigateAnalytics)
            }
        }

        // Quick-Add Grid Layout (2x2 on phones, 4-column on medium/expanded screens)
        item(key = "quick_add_grid") {
            QuickAddGridSection(
                onAddHabit = { showQuickHabitDialog = true },
                onAddTask = { showQuickTaskDialog = true },
                onAddExpense = { showQuickExpenseDialog = true },
                onAddJournal = { showQuickJournalDialog = true },
            )
        }

        // Quick-setup / Removable Demo Data Banner
        if (!state.prefs.quickSetupDismissed) {
            item(key = "quick_setup_banner") {
                AnimatedVisibility(visible = true) {
                    LifeOsCard(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = if (state.prefs.demoDataActive) "Sample Data Preloaded" else "Quick Setup",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                            IconButton(
                                onClick = { viewModel.dismissQuickSetup() },
                                modifier = Modifier.testTag("dismiss_quick_setup_button"),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Dismiss setup banner")
                            }
                        }
                        Text(
                            text = "Your offline vault starts with realistic example habits, 8:00 AM routine, and sample logs. Personalize your name or clear demo data anytime.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showQuickSetupDialog = true },
                                modifier = Modifier.testTag("open_quick_setup_button"),
                            ) {
                                Text("Personalize")
                            }
                            if (state.prefs.demoDataActive) {
                                OutlinedButton(
                                    onClick = { viewModel.clearDemoData() },
                                    modifier = Modifier.testTag("clear_demo_data_button"),
                                ) {
                                    Text("Clear Sample Data")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dynamic Customizable Widgets
        items(remainingWidgets, key = { it.id }) { widget ->
            when (widget) {
                DashboardWidget.DAILY_PROGRESS -> Unit
                DashboardWidget.TOP_PRIORITIES -> TopPrioritiesWidget(state, viewModel::toggleTaskComplete, onNavigateFocus)
                DashboardWidget.HABIT_CHECKLIST -> HabitChecklistWidget(state, viewModel::toggleHabitToday, onNavigateHabits)
                DashboardWidget.UPCOMING_TASKS -> UpcomingTasksAndRoutineWidget(state, viewModel::toggleRoutineBlockToday, onNavigateFocus)
                DashboardWidget.STUDY_AND_FITNESS -> StudyAndFitnessWidget(state, onNavigateFocus, onNavigateFitness)
                DashboardWidget.FINANCIAL_SUMMARY -> FinancialSummaryWidget(state, onNavigateFinance)
                DashboardWidget.GOAL_PROGRESS -> GoalProgressWidget(state, onNavigateGoals)
                DashboardWidget.WEEKLY_CHART -> WeeklyChartWidget(state, onNavigateAnalytics)
            }
        }
    }

    if (showCustomizeDialog) {
        CustomizeDashboardDialog(
            order = state.prefs.dashboardWidgetOrder,
            hidden = state.prefs.hiddenDashboardWidgets,
            onDismiss = { showCustomizeDialog = false },
            onSave = { newOrder, newHidden ->
                viewModel.updateWidgetCustomization(newOrder, newHidden)
                showCustomizeDialog = false
            },
        )
    }

    if (showQuickSetupDialog) {
        var nameInput by remember { mutableStateOf(state.prefs.userName) }
        AlertDialog(
            onDismissRequest = { showQuickSetupDialog = false },
            title = { Text("Quick Profile Setup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        "Everything stays 100% offline on your device.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("quick_setup_name_input"),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateQuickProfile(nameInput, dismissBanner = true)
                    showQuickSetupDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showQuickSetupDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showQuickHabitDialog) {
        QuickHabitDialog(
            onDismiss = { showQuickHabitDialog = false },
            onSave = { name, cat ->
                viewModel.quickAddHabit(name, cat)
                showQuickHabitDialog = false
            },
        )
    }

    if (showQuickTaskDialog) {
        QuickTaskDialog(
            onDismiss = { showQuickTaskDialog = false },
            onSave = { title, priority, top3 ->
                viewModel.quickAddTask(title, priority, top3)
                showQuickTaskDialog = false
            },
        )
    }

    if (showQuickExpenseDialog) {
        QuickExpenseDialog(
            currencySymbol = state.prefs.currencySymbol,
            onDismiss = { showQuickExpenseDialog = false },
            onSave = { title, amount, isIncome ->
                // Default category id 1 (income) or 3 (expense) safely fallback
                viewModel.quickAddExpense(title, amount, if (isIncome) 1L else 3L, if (isIncome) "INCOME" else "EXPENSE")
                showQuickExpenseDialog = false
            },
        )
    }

    if (showQuickJournalDialog) {
        QuickJournalDialog(
            onDismiss = { showQuickJournalDialog = false },
            onSave = { title, mood, body ->
                viewModel.quickAddJournal(title, mood, body)
                showQuickJournalDialog = false
            },
        )
    }
}

private data class QuickAddAction(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val testTag: String,
    val onClick: () -> Unit,
)

@Composable
private fun QuickAddGridSection(
    onAddHabit: () -> Unit,
    onAddTask: () -> Unit,
    onAddExpense: () -> Unit,
    onAddJournal: () -> Unit,
) {
    val actions = listOf(
        QuickAddAction("Add Habit", "Daily discipline", Icons.Default.Add, "quick_add_habit", onAddHabit),
        QuickAddAction("Add Task", "Top priority", Icons.Default.TaskAlt, "quick_add_task", onAddTask),
        QuickAddAction("Log Expense", "Local ledger", Icons.Default.Payments, "quick_add_expense", onAddExpense),
        QuickAddAction("Quick Note", "Private journal", Icons.Default.MenuBook, "quick_add_journal", onAddJournal),
    )

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val columns = if (maxWidth >= 560.dp) 4 else 2
            val rows = actions.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                rows.forEach { rowItems ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        rowItems.forEach { action ->
                            QuickAddGridButton(
                                action = action,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddGridButton(
    action: QuickAddAction,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = action.onClick,
        modifier = modifier
            .defaultMinSize(minHeight = 64.dp)
            .testTag(action.testTag),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, LifeOsThemeTokens.colors.border),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon,
                    contentDescription = action.title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = action.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = action.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun DailyProgressWidget(state: DashboardUiState, onClick: () -> Unit) {
    LifeOsCard(onClick = onClick) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            CompletionRingCanvas(
                progress = state.dailyCompletionPercent / 100f,
                centerLabel = "${state.dailyCompletionPercent}%",
                size = 86.dp,
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Today's Rhythm",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "${state.completedHabitsToday} of ${state.totalHabitsToday} habits completed",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                ) {
                    state.topStreaks.take(3).forEach { (habit, streak) ->
                        StatusBadge(
                            text = "${habit.name.take(12)} · ${streak}d",
                            icon = Icons.Default.LocalFireDepartment,
                            containerColor = LifeOsThemeTokens.colors.warningContainer,
                            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopPrioritiesWidget(
    state: DashboardUiState,
    onToggleTask: (com.example.data.local.entity.TaskEntity) -> Unit,
    onOpenFocus: () -> Unit,
) {
    LifeOsCard {
        SectionHeader(
            title = "Top 3 Priorities",
            actionLabel = "All Tasks",
            onActionClick = onOpenFocus,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.topThreeTasks.isEmpty()) {
            Text(
                text = "No active priorities today. Tap 'All Tasks' or '+ Task' to add focus items.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.topThreeTasks.forEach { task ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleTask(task) }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (task.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                        contentDescription = "Toggle task",
                        tint = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None,
                        ),
                        modifier = Modifier.weight(1f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val pLabel = when (task.priority) {
                        1 -> "P1"
                        2 -> "P2"
                        else -> "P3"
                    }
                    StatusBadge(text = pLabel)
                }
            }
        }
    }
}

@Composable
private fun HabitChecklistWidget(
    state: DashboardUiState,
    onToggleHabit: (Long) -> Unit,
    onOpenHabits: () -> Unit,
) {
    LifeOsCard {
        SectionHeader(
            title = "Daily Habits",
            subtitle = "${state.completedHabitsToday}/${state.totalHabitsToday} checked",
            actionLabel = "Tracker",
            onActionClick = onOpenHabits,
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (state.habits.isEmpty()) {
            Text(
                text = "Create your first habit to build daily momentum.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.habits.take(5).forEach { habit ->
                val done = state.todayHabitStatuses[habit.id] == "COMPLETED"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onToggleHabit(habit.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = if (done) Icons.Default.CheckCircle else Icons.Outlined.CheckCircle,
                        contentDescription = "Toggle ${habit.name}",
                        tint = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = habit.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun UpcomingTasksAndRoutineWidget(
    state: DashboardUiState,
    onToggleRoutine: (com.example.data.local.entity.RoutineBlockEntity) -> Unit,
    onOpenFocus: () -> Unit,
) {
    LifeOsCard {
        SectionHeader(
            title = "Routine & Upcoming",
            actionLabel = "Schedule",
            onActionClick = onOpenFocus,
        )
        Spacer(modifier = Modifier.height(8.dp))
        state.nextRoutineBlock?.let { block ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleRoutine(block) }
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Icon(Icons.Default.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${DateUtils.formatMinuteOfDay(block.startMinuteOfDay)} — ${block.title}",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "Tap to mark today's routine block complete",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        state.upcomingTasks.take(3).forEach { task ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "• ${task.title}",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                task.dueEpochDay?.let { day ->
                    Text(
                        text = DateUtils.formatShortDate(day),
                        style = MonoMetricSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun StudyAndFitnessWidget(
    state: DashboardUiState,
    onOpenStudy: () -> Unit,
    onOpenFitness: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        LifeOsCard(
            modifier = Modifier.weight(1f),
            onClick = onOpenStudy,
        ) {
            Icon(Icons.Default.School, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Study Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = DateUtils.formatMinutesReadable(state.studyMinutesWeek),
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = "${DateUtils.formatMinutesReadable(state.studyMinutesToday)} today",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        LifeOsCard(
            modifier = Modifier.weight(1f),
            onClick = onOpenFitness,
        ) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = LifeOsThemeTokens.colors.accent)
            Spacer(modifier = Modifier.height(8.dp))
            Text("Workout Activity", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${state.workoutsThisWeek} sessions",
                style = MaterialTheme.typography.titleLarge,
            )
            Text(
                text = state.latestWorkout?.title ?: "Log a workout",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun FinancialSummaryWidget(state: DashboardUiState, onOpenFinance: () -> Unit) {
    val summary = state.financeSummary ?: return
    val sym = state.prefs.currencySymbol
    val code = state.prefs.currencyCode
    LifeOsCard(onClick = onOpenFinance) {
        SectionHeader(title = "Monthly Finance", actionLabel = "Open", onActionClick = onOpenFinance)
        Spacer(modifier = Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MetricTile(
                label = "Income",
                value = DateUtils.formatCurrency(summary.totalIncome, sym, code),
                modifier = Modifier.weight(1f),
                accentColor = LifeOsThemeTokens.colors.positive,
            )
            MetricTile(
                label = "Spent",
                value = DateUtils.formatCurrency(summary.totalExpense, sym, code),
                modifier = Modifier.weight(1f),
                accentColor = LifeOsThemeTokens.colors.accent,
            )
            MetricTile(
                label = "Savings",
                value = "${summary.savingsRatePercent}%",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun GoalProgressWidget(state: DashboardUiState, onOpenGoals: () -> Unit) {
    LifeOsCard(onClick = onOpenGoals) {
        SectionHeader(title = "Active Goals", actionLabel = "View All", onActionClick = onOpenGoals)
        Spacer(modifier = Modifier.height(8.dp))
        if (state.activeGoals.isEmpty()) {
            Text(
                text = "Set short-term or long-term goals to track milestones here.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            state.activeGoals.forEach { goal ->
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${goal.progressPercent}%",
                            style = MonoMetricSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { goal.progressPercent / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun WeeklyChartWidget(state: DashboardUiState, onOpenAnalytics: () -> Unit) {
    LifeOsCard(onClick = onOpenAnalytics) {
        SectionHeader(
            title = "Weekly Productivity",
            subtitle = "7-day life balance score",
            actionLabel = "Analytics",
            onActionClick = onOpenAnalytics,
        )
        Spacer(modifier = Modifier.height(12.dp))
        WeeklyBarChart(points = state.weeklyProductivityBars)
    }
}

@Composable
private fun CustomizeDashboardDialog(
    order: List<String>,
    hidden: Set<String>,
    onDismiss: () -> Unit,
    onSave: (List<String>, Set<String>) -> Unit,
) {
    var workingOrder by remember { mutableStateOf(order) }
    var workingHidden by remember { mutableStateOf(hidden) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Dashboard") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    "Toggle visibility or reorder sections.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                workingOrder.forEachIndexed { index, id ->
                    val widget = DashboardWidget.entries.firstOrNull { it.id == id } ?: return@forEachIndexed
                    val visible = id !in workingHidden
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Switch(
                            checked = visible,
                            onCheckedChange = { checked ->
                                workingHidden = if (checked) workingHidden - id else workingHidden + id
                            },
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = widget.label,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = {
                                if (index > 0) {
                                    val mutable = workingOrder.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index - 1, item)
                                    workingOrder = mutable
                                }
                            },
                            enabled = index > 0,
                        ) {
                            Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up")
                        }
                        IconButton(
                            onClick = {
                                if (index < workingOrder.lastIndex) {
                                    val mutable = workingOrder.toMutableList()
                                    val item = mutable.removeAt(index)
                                    mutable.add(index + 1, item)
                                    workingOrder = mutable
                                }
                            },
                            enabled = index < workingOrder.lastIndex,
                        ) {
                            Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down")
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(workingOrder, workingHidden) }) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@Composable
private fun QuickHabitDialog(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Discipline") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Habit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Habit Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(name, category) }, enabled = name.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuickTaskDialog(onDismiss: () -> Unit, onSave: (String, Int, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var priority by remember { mutableIntStateOf(1) }
    var topThree by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Add Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1 to "High", 2 to "Medium", 3 to "Low").forEach { (p, lbl) ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(lbl) },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = topThree, onCheckedChange = { topThree = it })
                    Text("Pin to Today's Top 3")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, priority, topThree) }, enabled = title.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuickExpenseDialog(currencySymbol: String, onDismiss: () -> Unit, onSave: (String, Double, Boolean) -> Unit) {
    var title by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(false) }
    val amount = amountText.toDoubleOrNull() ?: 0.0
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Finance Entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = !isIncome, onClick = { isIncome = false }, label = { Text("Expense") })
                    FilterChip(selected = isIncome, onClick = { isIncome = true }, label = { Text("Income") })
                }
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Description") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount ($currencySymbol)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, amount, isIncome) }, enabled = title.isNotBlank() && amount > 0.0) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun QuickJournalDialog(onDismiss: () -> Unit, onSave: (String, Int, String) -> Unit) {
    var title by remember { mutableStateOf("") }
    var reflection by remember { mutableStateOf("") }
    var mood by remember { mutableIntStateOf(4) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Quick Private Note") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Headline") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { m ->
                        FilterChip(selected = mood == m, onClick = { mood = m }, label = { Text("$m/5") })
                    }
                }
                OutlinedTextField(
                    value = reflection,
                    onValueChange = { reflection = it },
                    label = { Text("Reflection (encrypted locally)") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(title, mood, reflection) }, enabled = title.isNotBlank() || reflection.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
