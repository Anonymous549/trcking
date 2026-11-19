package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ExerciseTemplateEntity
import com.example.data.local.entity.FinanceCategoryEntity
import com.example.data.local.entity.GoalMilestoneEntity
import com.example.data.local.entity.HealthMetricEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.WorkoutExerciseEntity
import com.example.data.local.entity.WorkoutLogEntity
import com.example.data.preferences.AnalyticsWeights
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DecryptedTransaction
import com.example.data.repository.LifeOsRepository
import com.example.domain.analytics.AnalyticsEngine
import com.example.domain.analytics.DailyProductivityBreakdown
import com.example.domain.analytics.MonthlyFinanceSummary
import com.example.domain.analytics.PeriodComparison
import com.example.domain.util.DateUtils
import com.example.ui.components.BarChartPoint
import com.example.ui.components.HeatmapDay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ComparisonMode { WEEK, MONTH }

data class AnalyticsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val comparisonMode: ComparisonMode = ComparisonMode.WEEK,
    val todayBreakdown: DailyProductivityBreakdown = DailyProductivityBreakdown(0, 0, 0, 0, 0, 0, 0),
    val comparison: PeriodComparison? = null,
    val trendScores: List<Float> = emptyList(),
    val trendLabels: List<String> = emptyList(),
    val studyBars: List<BarChartPoint> = emptyList(),
    val heatmapDays: List<HeatmapDay> = emptyList(),
    val activeGoalsCount: Int = 0,
    val completedGoalsCount: Int = 0,
)

private data class AnalyticsPackA(
    val prefs: UserPreferences,
    val habits: List<com.example.data.local.entity.HabitEntity>,
    val logs: List<com.example.data.local.entity.HabitLogEntity>,
    val tasks: List<com.example.data.local.entity.TaskEntity>,
    val sessions: List<com.example.data.local.entity.StudySessionEntity>,
)

private data class AnalyticsPackB(
    val workouts: List<WorkoutLogEntity>,
    val transactions: List<DecryptedTransaction>,
    val goals: List<LifeGoalEntity>,
    val screenLogs: List<com.example.data.local.entity.ScreenTimeLogEntity>,
)

class AnalyticsViewModel(
    private val repo: LifeOsRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    private val modeFlow = MutableStateFlow(ComparisonMode.WEEK)

    private val packAFlow = combine(
        prefsRepo.preferences,
        repo.habits,
        repo.habitLogs,
        repo.tasks,
        repo.studySessions,
    ) { prefs, habits, logs, tasks, sessions ->
        AnalyticsPackA(prefs, habits, logs, tasks, sessions)
    }

    private val packBFlow = combine(
        repo.workoutLogs,
        repo.financeTransactions,
        repo.goals,
        repo.screenTimeLogs,
    ) { workouts, tx, goals, screen ->
        AnalyticsPackB(workouts, tx, goals, screen)
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        packAFlow,
        packBFlow,
        modeFlow,
    ) { a, b, mode ->
        val today = DateUtils.todayEpochDay()
        val weights = a.prefs.analyticsWeights
        val breakdown = AnalyticsEngine.computeDailyScore(
            today, a.habits, a.logs, a.tasks, a.sessions, b.workouts, b.screenLogs, weights,
        )

        val span = if (mode == ComparisonMode.WEEK) 7L else 30L
        val currentMetrics = AnalyticsEngine.computePeriodMetrics(
            today - span + 1, today,
            a.habits, a.logs, a.tasks, a.sessions, b.workouts, b.transactions, b.goals, b.screenLogs, weights,
        )
        val prevMetrics = AnalyticsEngine.computePeriodMetrics(
            today - span * 2 + 1, today - span,
            a.habits, a.logs, a.tasks, a.sessions, b.workouts, b.transactions, b.goals, b.screenLogs, weights,
        )

        val trendDays = (13 downTo 0).map { today - it }
        val trendScores = trendDays.map { d ->
            AnalyticsEngine.computeDailyScore(d, a.habits, a.logs, a.tasks, a.sessions, b.workouts, b.screenLogs, weights).totalScore.toFloat()
        }
        val trendLabels = listOf(
            DateUtils.formatShortDate(trendDays.first()),
            DateUtils.formatShortDate(trendDays[trendDays.size / 2]),
            "Today",
        )

        val studyBars = (6 downTo 0).map { offset ->
            val d = today - offset
            val hours = a.sessions.filter { it.epochDay == d }.sumOf { it.durationMinutes } / 60f
            BarChartPoint(DateUtils.formatDayOfWeekShort(d), hours, offset == 0)
        }

        val heatmap = (83 downTo 0).map { offset ->
            val d = today - offset
            val score = AnalyticsEngine.computeDailyScore(d, a.habits, a.logs, a.tasks, a.sessions, b.workouts, b.screenLogs, weights).totalScore
            HeatmapDay(d, score / 100f, "${DateUtils.formatShortDate(d)}: $score%")
        }

        AnalyticsUiState(
            prefs = a.prefs,
            comparisonMode = mode,
            todayBreakdown = breakdown,
            comparison = PeriodComparison(currentMetrics, prevMetrics),
            trendScores = trendScores,
            trendLabels = trendLabels,
            studyBars = studyBars,
            heatmapDays = heatmap,
            activeGoalsCount = b.goals.count { !it.isCompleted },
            completedGoalsCount = b.goals.count { it.isCompleted },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AnalyticsUiState())

    fun setComparisonMode(mode: ComparisonMode) {
        modeFlow.value = mode
    }

    fun updateAnalyticsWeights(weights: AnalyticsWeights) {
        viewModelScope.launch { prefsRepo.updateAnalyticsWeights(weights) }
    }
}

data class FitnessUiState(
    val workouts: List<WorkoutLogEntity> = emptyList(),
    val workoutExercises: Map<Long, List<WorkoutExerciseEntity>> = emptyMap(),
    val templates: List<ExerciseTemplateEntity> = emptyList(),
    val todayMetric: HealthMetricEntity = HealthMetricEntity(DateUtils.todayEpochDay()),
    val recentMetrics: List<HealthMetricEntity> = emptyList(),
    val weightTrend: List<Float> = emptyList(),
    val weightLabels: List<String> = emptyList(),
)

class FitnessViewModel(
    private val repo: LifeOsRepository,
) : ViewModel() {
    val uiState: StateFlow<FitnessUiState> = combine(
        repo.workoutLogs,
        repo.workoutExercises,
        repo.exerciseTemplates,
        repo.healthMetrics,
    ) { workouts, exercises, templates, metrics ->
        val today = DateUtils.todayEpochDay()
        val todayMetric = metrics.firstOrNull { it.epochDay == today } ?: HealthMetricEntity(today)
        val weightEntries = metrics.filter { it.weightKg != null }.sortedBy { it.epochDay }.takeLast(10)
        FitnessUiState(
            workouts = workouts,
            workoutExercises = exercises.groupBy { it.workoutId },
            templates = templates,
            todayMetric = todayMetric,
            recentMetrics = metrics.take(7),
            weightTrend = weightEntries.mapNotNull { it.weightKg },
            weightLabels = weightEntries.map { DateUtils.formatShortDate(it.epochDay) },
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FitnessUiState())

    fun addWater(deltaMl: Int) {
        viewModelScope.launch {
            val current = uiState.value.todayMetric
            repo.upsertHealthMetric(current.copy(waterMl = (current.waterMl + deltaMl).coerceAtLeast(0), isSampleData = false))
        }
    }

    fun updateTodayMetrics(
        steps: Int,
        weightKg: Float?,
        waistCm: Float?,
        chestCm: Float?,
        calories: Int,
        proteinG: Int,
        carbsG: Int,
        fatsG: Int,
        sleepHours: Float,
        nutritionNote: String,
    ) {
        viewModelScope.launch {
            val current = uiState.value.todayMetric
            repo.upsertHealthMetric(
                current.copy(
                    steps = steps.coerceAtLeast(0),
                    weightKg = weightKg,
                    waistCm = waistCm,
                    chestCm = chestCm,
                    calories = calories.coerceAtLeast(0),
                    proteinG = proteinG.coerceAtLeast(0),
                    carbsG = carbsG.coerceAtLeast(0),
                    fatsG = fatsG.coerceAtLeast(0),
                    sleepHours = sleepHours.coerceIn(0f, 24f),
                    nutritionNote = nutritionNote.trim(),
                    isSampleData = false,
                ),
            )
        }
    }

    fun logWorkout(title: String, routineName: String, durationMinutes: Int, notes: String, exercises: List<WorkoutExerciseEntity>) {
        if (title.isBlank() || durationMinutes <= 0) return
        viewModelScope.launch {
            repo.logWorkoutWithExercises(
                WorkoutLogEntity(
                    title = title.trim(),
                    routineName = routineName.trim(),
                    durationMinutes = durationMinutes,
                    epochDay = DateUtils.todayEpochDay(),
                    notes = notes.trim(),
                ),
                exercises,
            )
        }
    }

    fun deleteWorkout(id: Long) {
        viewModelScope.launch { repo.deleteWorkout(id) }
    }

    fun addCustomExercise(name: String, muscleGroup: String, equipment: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveExerciseTemplate(
                ExerciseTemplateEntity(
                    name = name.trim(),
                    muscleGroup = muscleGroup.trim().ifBlank { "Full Body" },
                    equipment = equipment.trim().ifBlank { "Bodyweight" },
                    isCustom = true,
                ),
            )
        }
    }
}

data class FinanceUiState(
    val prefs: UserPreferences = UserPreferences(),
    val categories: List<FinanceCategoryEntity> = emptyList(),
    val transactions: List<DecryptedTransaction> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val summary: MonthlyFinanceSummary? = null,
)

class FinanceViewModel(
    private val repo: LifeOsRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<FinanceUiState> = combine(
        prefsRepo.preferences,
        repo.financeCategories,
        repo.financeTransactions,
        repo.savingsGoals,
    ) { prefs, categories, txs, savings ->
        val (year, month) = DateUtils.currentYearMonth()
        val summary = AnalyticsEngine.computeMonthlyFinanceSummary(year, month, categories, txs)
        FinanceUiState(
            prefs = prefs,
            categories = categories,
            transactions = txs,
            savingsGoals = savings,
            summary = summary,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FinanceUiState())

    fun addTransaction(categoryId: Long, title: String, amount: Double, type: String, note: String) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            repo.saveTransaction(categoryId, title.trim(), amount, type, note.trim(), DateUtils.todayEpochDay())
        }
    }

    fun deleteTransaction(id: Long) {
        viewModelScope.launch { repo.deleteTransaction(id) }
    }

    fun saveCategory(name: String, type: String, monthlyBudget: Double) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveFinanceCategory(
                FinanceCategoryEntity(
                    name = name.trim(),
                    type = type,
                    monthlyBudget = monthlyBudget.coerceAtLeast(0.0),
                    isCustom = true,
                ),
            )
        }
    }

    fun updateCategoryBudget(category: FinanceCategoryEntity, newBudget: Double) {
        viewModelScope.launch {
            repo.saveFinanceCategory(category.copy(monthlyBudget = newBudget.coerceAtLeast(0.0)))
        }
    }

    fun saveSavingsGoal(goal: SavingsGoalEntity) {
        if (goal.title.isBlank() || goal.targetAmount <= 0.0) return
        viewModelScope.launch { repo.saveSavingsGoal(goal) }
    }

    fun contributeToSavings(goal: SavingsGoalEntity, delta: Double) {
        viewModelScope.launch {
            repo.saveSavingsGoal(goal.copy(currentAmount = (goal.currentAmount + delta).coerceAtLeast(0.0)))
        }
    }

    fun deleteSavingsGoal(id: Long) {
        viewModelScope.launch { repo.deleteSavingsGoal(id) }
    }
}

data class GoalsUiState(
    val goals: List<LifeGoalEntity> = emptyList(),
    val milestonesByGoal: Map<Long, List<GoalMilestoneEntity>> = emptyMap(),
    val personalVision: String = "",
    val selectedHorizon: String = "ALL",
)

class GoalsViewModel(
    private val repo: LifeOsRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    private val selectedHorizon = MutableStateFlow("ALL")

    val uiState: StateFlow<GoalsUiState> = combine(
        repo.goals,
        repo.milestones,
        prefsRepo.preferences,
        selectedHorizon,
    ) { goals, milestones, prefs, horizon ->
        GoalsUiState(
            goals = goals,
            milestonesByGoal = milestones.groupBy { it.goalId },
            personalVision = prefs.personalVision,
            selectedHorizon = horizon,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), GoalsUiState())

    fun selectHorizon(horizon: String) {
        selectedHorizon.value = horizon
    }

    fun saveVision(vision: String) {
        viewModelScope.launch { prefsRepo.updatePersonalVision(vision.trim()) }
    }

    fun saveGoal(goal: LifeGoalEntity) {
        if (goal.title.isBlank()) return
        viewModelScope.launch { repo.saveGoal(goal) }
    }

    fun updateGoalProgress(goal: LifeGoalEntity, progress: Int) {
        val clamped = progress.coerceIn(0, 100)
        viewModelScope.launch {
            repo.saveGoal(
                goal.copy(
                    progressPercent = clamped,
                    isCompleted = clamped == 100,
                    completedAt = if (clamped == 100) System.currentTimeMillis() else null,
                ),
            )
        }
    }

    fun deleteGoal(id: Long) {
        viewModelScope.launch { repo.deleteGoal(id) }
    }

    fun addMilestone(goalId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch { repo.saveMilestone(GoalMilestoneEntity(goalId = goalId, title = title.trim())) }
    }

    fun toggleMilestone(milestone: GoalMilestoneEntity, goal: LifeGoalEntity) {
        viewModelScope.launch {
            val list = uiState.value.milestonesByGoal[goal.id].orEmpty()
            repo.toggleMilestoneAndSyncGoal(milestone, list, goal)
        }
    }
}
