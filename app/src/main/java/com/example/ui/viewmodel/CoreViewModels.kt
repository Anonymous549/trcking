package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CareerItemEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.RoutineBlockEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudySubjectEntity
import com.example.data.local.entity.StudyTopicEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.WorkoutLogEntity
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DecryptedJournalEntry
import com.example.data.repository.DecryptedTransaction
import com.example.data.repository.LifeOsRepository
import com.example.domain.analytics.AnalyticsEngine
import com.example.domain.analytics.HabitStats
import com.example.domain.analytics.MonthlyFinanceSummary
import com.example.domain.util.DateUtils
import com.example.ui.components.BarChartPoint
import com.example.ui.components.HeatmapDay
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AppViewModel(
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    val preferences: StateFlow<UserPreferences> = prefsRepo.preferences.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        UserPreferences(),
    )

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    private val _lockInitialized = MutableStateFlow(false)
    val lockInitialized: StateFlow<Boolean> = _lockInitialized.asStateFlow()

    init {
        viewModelScope.launch {
            val prefs = prefsRepo.preferences.first()
            _isUnlocked.value = !prefs.biometricLockEnabled
            _lockInitialized.value = true
        }
    }

    fun markUnlocked() {
        _isUnlocked.value = true
    }

    fun lockNow() {
        _isUnlocked.value = false
    }
}

data class DashboardUiState(
    val prefs: UserPreferences = UserPreferences(),
    val todayEpochDay: Long = DateUtils.todayEpochDay(),
    val dailyCompletionPercent: Int = 0,
    val completedHabitsToday: Int = 0,
    val totalHabitsToday: Int = 0,
    val habits: List<HabitEntity> = emptyList(),
    val todayHabitStatuses: Map<Long, String> = emptyMap(),
    val topThreeTasks: List<TaskEntity> = emptyList(),
    val upcomingTasks: List<TaskEntity> = emptyList(),
    val nextRoutineBlock: RoutineBlockEntity? = null,
    val studyMinutesToday: Int = 0,
    val studyMinutesWeek: Int = 0,
    val workoutsThisWeek: Int = 0,
    val latestWorkout: WorkoutLogEntity? = null,
    val financeSummary: MonthlyFinanceSummary? = null,
    val activeGoals: List<LifeGoalEntity> = emptyList(),
    val topStreaks: List<Pair<HabitEntity, Int>> = emptyList(),
    val weeklyProductivityBars: List<BarChartPoint> = emptyList(),
)

private data class DashboardCoreData(
    val prefs: UserPreferences,
    val habits: List<HabitEntity>,
    val logs: List<HabitLogEntity>,
    val tasks: List<TaskEntity>,
    val routine: List<RoutineBlockEntity>,
)

private data class DashboardSecondaryData(
    val sessions: List<StudySessionEntity>,
    val workouts: List<WorkoutLogEntity>,
    val goals: List<LifeGoalEntity>,
    val transactions: List<DecryptedTransaction>,
    val categories: List<com.example.data.local.entity.FinanceCategoryEntity>,
)

class DashboardViewModel(
    private val repo: LifeOsRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {

    private val coreFlow = combine(
        prefsRepo.preferences,
        repo.habits,
        repo.habitLogs,
        repo.tasks,
        repo.routineBlocks,
    ) { prefs, habits, logs, tasks, routine ->
        DashboardCoreData(prefs, habits, logs, tasks, routine)
    }

    private val secondaryFlow = combine(
        repo.studySessions,
        repo.workoutLogs,
        repo.goals,
        repo.financeTransactions,
        repo.financeCategories,
    ) { sessions, workouts, goals, transactions, categories ->
        DashboardSecondaryData(sessions, workouts, goals, transactions, categories)
    }

    val uiState: StateFlow<DashboardUiState> = combine(
        coreFlow,
        secondaryFlow,
        repo.screenTimeLogs,
    ) { core, sec, screenLogs ->
        val today = DateUtils.todayEpochDay()
        val dueHabits = core.habits.filter { AnalyticsEngine.isHabitScheduledOnDay(it, today) }
        val todayLogs = core.logs.filter { it.epochDay == today }.associate { it.habitId to it.status }
        val completedHabitsCount = dueHabits.count { todayLogs[it.id] == "COMPLETED" }

        val top3 = core.tasks.filter { !it.isCompleted && it.isTopThree }.take(3)
            .ifEmpty { core.tasks.filter { !it.isCompleted }.take(3) }
        val upcoming = core.tasks.filter { !it.isCompleted && it !in top3 }.take(4)

        val nowMin = DateUtils.currentMinuteOfDay()
        val nextRoutine = core.routine.firstOrNull { it.lastCompletedEpochDay != today && it.startMinuteOfDay >= nowMin - 30 }
            ?: core.routine.firstOrNull { it.lastCompletedEpochDay != today }

        val studyToday = sec.sessions.filter { it.epochDay == today }.sumOf { it.durationMinutes }
        val studyWeek = sec.sessions.filter { it.epochDay in (today - 6)..today }.sumOf { it.durationMinutes }
        val workoutsWeek = sec.workouts.count { it.epochDay in (today - 6)..today }

        val (year, month) = DateUtils.currentYearMonth(today)
        val financeSummary = AnalyticsEngine.computeMonthlyFinanceSummary(year, month, sec.categories, sec.transactions)

        val streaks = core.habits.map { h ->
            h to AnalyticsEngine.computeHabitStats(h, core.logs, today, core.prefs.exemptRestDays).currentStreak
        }.sortedByDescending { it.second }.take(4)

        val weeklyBars = (6 downTo 0).map { offset ->
            val day = today - offset
            val score = AnalyticsEngine.computeDailyScore(
                epochDay = day,
                habits = core.habits,
                habitLogs = core.logs,
                tasks = core.tasks,
                studySessions = sec.sessions,
                workouts = sec.workouts,
                screenLogs = screenLogs,
                weights = core.prefs.analyticsWeights,
            ).totalScore
            BarChartPoint(
                label = DateUtils.formatDayOfWeekShort(day),
                value = score.toFloat(),
                isHighlighted = offset == 0,
            )
        }

        val todayScore = weeklyBars.lastOrNull()?.value?.toInt() ?: 0

        DashboardUiState(
            prefs = core.prefs,
            todayEpochDay = today,
            dailyCompletionPercent = todayScore,
            completedHabitsToday = completedHabitsCount,
            totalHabitsToday = dueHabits.size,
            habits = dueHabits,
            todayHabitStatuses = todayLogs,
            topThreeTasks = top3,
            upcomingTasks = upcoming,
            nextRoutineBlock = nextRoutine,
            studyMinutesToday = studyToday,
            studyMinutesWeek = studyWeek,
            workoutsThisWeek = workoutsWeek,
            latestWorkout = sec.workouts.firstOrNull(),
            financeSummary = financeSummary,
            activeGoals = sec.goals.filter { !it.isCompleted }.take(3),
            topStreaks = streaks,
            weeklyProductivityBars = weeklyBars,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    fun toggleHabitToday(habitId: Long) {
        viewModelScope.launch {
            val today = DateUtils.todayEpochDay()
            val current = uiState.value.todayHabitStatuses[habitId]
            val next = if (current == "COMPLETED") null else "COMPLETED"
            repo.setHabitStatus(habitId, today, next)
        }
    }

    fun toggleTaskComplete(task: TaskEntity) {
        viewModelScope.launch { repo.toggleTaskComplete(task) }
    }

    fun toggleRoutineBlockToday(block: RoutineBlockEntity) {
        viewModelScope.launch { repo.toggleRoutineBlockToday(block) }
    }

    fun quickAddHabit(name: String, category: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            repo.saveHabit(HabitEntity(name = name.trim(), category = category))
        }
    }

    fun quickAddTask(title: String, priority: Int, isTopThree: Boolean) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.saveTask(
                TaskEntity(
                    title = title.trim(),
                    priority = priority,
                    dueEpochDay = DateUtils.todayEpochDay(),
                    isTopThree = isTopThree,
                ),
            )
        }
    }

    fun quickAddExpense(title: String, amount: Double, categoryId: Long, type: String) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            repo.saveTransaction(
                categoryId = categoryId,
                title = title.trim(),
                amount = amount,
                type = type,
                plainNote = "",
                epochDay = DateUtils.todayEpochDay(),
            )
        }
    }

    fun quickAddJournal(title: String, mood: Int, reflection: String) {
        if (title.isBlank() && reflection.isBlank()) return
        viewModelScope.launch {
            repo.saveJournalEntry(
                DecryptedJournalEntry(
                    id = 0,
                    epochDay = DateUtils.todayEpochDay(),
                    title = title.trim().ifBlank { "Quick Note" },
                    moodScore = mood,
                    reflection = reflection.trim(),
                    gratitude = "",
                    achievements = "",
                    challenges = "",
                    lessons = "",
                    notes = "",
                    tags = "quick",
                    timestamp = System.currentTimeMillis(),
                    isSampleData = false,
                ),
            )
        }
    }

    fun updateWidgetCustomization(order: List<String>, hidden: Set<String>) {
        viewModelScope.launch { prefsRepo.updateDashboardCustomization(order, hidden) }
    }

    fun updateQuickProfile(name: String, dismissBanner: Boolean) {
        viewModelScope.launch {
            if (name.isNotBlank()) prefsRepo.updateProfileName(name)
            if (dismissBanner) prefsRepo.dismissQuickSetup()
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            repo.clearSampleData()
            prefsRepo.dismissQuickSetup()
        }
    }

    fun dismissQuickSetup() {
        viewModelScope.launch { prefsRepo.dismissQuickSetup() }
    }
}

data class HabitItemUi(
    val habit: HabitEntity,
    val todayStatus: String?,
    val stats: HabitStats,
)

data class HabitsUiState(
    val items: List<HabitItemUi> = emptyList(),
    val categories: List<String> = listOf("All"),
    val selectedCategory: String = "All",
    val heatmapDays: List<HeatmapDay> = emptyList(),
    val overallRate30d: Int = 0,
    val missedThisWeek: Int = 0,
    val todayCompleted: Int = 0,
    val todayTotal: Int = 0,
)

class HabitsViewModel(
    private val repo: LifeOsRepository,
    private val prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    private val selectedCategory = MutableStateFlow("All")

    val uiState: StateFlow<HabitsUiState> = combine(
        repo.habits,
        repo.habitLogs,
        prefsRepo.preferences,
        selectedCategory,
    ) { habits, logs, prefs, category ->
        val today = DateUtils.todayEpochDay()
        val itemList = habits.map { habit ->
            val todayLog = logs.firstOrNull { it.habitId == habit.id && it.epochDay == today }
            HabitItemUi(
                habit = habit,
                todayStatus = todayLog?.status,
                stats = AnalyticsEngine.computeHabitStats(habit, logs, today, prefs.exemptRestDays),
            )
        }
        val filtered = if (category == "All") itemList else itemList.filter { it.habit.category == category }
        val cats = listOf("All") + habits.map { it.category }.distinct().sorted()

        // 84-day (12-week) calendar heatmap across all active habits
        val heatmap = (83 downTo 0).map { offset ->
            val day = today - offset
            val due = habits.filter { AnalyticsEngine.isHabitScheduledOnDay(it, day) }
            val completed = due.count { h -> logs.any { it.habitId == h.id && it.epochDay == day && it.status == "COMPLETED" } }
            val ratio = if (due.isEmpty()) 0f else completed.toFloat() / due.size
            HeatmapDay(
                epochDay = day,
                intensity = ratio,
                label = "${DateUtils.formatShortDate(day)}: $completed/${due.size}",
            )
        }

        val avgRate = if (itemList.isEmpty()) 0 else itemList.map { it.stats.completionRate30d }.average().toInt()
        val missedWeek = (1..6).sumOf { offset ->
            val day = today - offset
            habits.count { h ->
                AnalyticsEngine.isHabitScheduledOnDay(h, day) &&
                    logs.none { it.habitId == h.id && it.epochDay == day && (it.status == "COMPLETED" || it.status == "SKIPPED_REST") }
            }
        }
        val dueToday = itemList.filter { AnalyticsEngine.isHabitScheduledOnDay(it.habit, today) }

        HabitsUiState(
            items = filtered,
            categories = cats,
            selectedCategory = category,
            heatmapDays = heatmap,
            overallRate30d = avgRate,
            missedThisWeek = missedWeek,
            todayCompleted = dueToday.count { it.todayStatus == "COMPLETED" },
            todayTotal = dueToday.size,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HabitsUiState())

    fun selectCategory(category: String) {
        selectedCategory.value = category
    }

    fun toggleHabitToday(habitId: Long, currentStatus: String?) {
        viewModelScope.launch {
            val next = if (currentStatus == "COMPLETED") null else "COMPLETED"
            repo.setHabitStatus(habitId, DateUtils.todayEpochDay(), next)
        }
    }

    fun markRestDay(habitId: Long) {
        viewModelScope.launch {
            repo.setHabitStatus(habitId, DateUtils.todayEpochDay(), "SKIPPED_REST")
        }
    }

    fun saveHabit(habit: HabitEntity) {
        if (habit.name.isBlank()) return
        viewModelScope.launch { repo.saveHabit(habit) }
    }

    fun deleteHabit(id: Long) {
        viewModelScope.launch { repo.deleteHabit(id) }
    }
}

data class PomodoroState(
    val isRunning: Boolean = false,
    val isBreak: Boolean = false,
    val focusMinutes: Int = 25,
    val breakMinutes: Int = 5,
    val remainingSeconds: Int = 25 * 60,
    val selectedSubjectId: Long? = null,
    val completedPomodorosToday: Int = 0,
)

data class FocusUiState(
    val tasks: List<TaskEntity> = emptyList(),
    val routineBlocks: List<RoutineBlockEntity> = emptyList(),
    val subjects: List<StudySubjectEntity> = emptyList(),
    val topics: List<StudyTopicEntity> = emptyList(),
    val sessions: List<StudySessionEntity> = emptyList(),
    val careerItems: List<CareerItemEntity> = emptyList(),
    val pomodoro: PomodoroState = PomodoroState(),
    val weeklyStudyBars: List<BarChartPoint> = emptyList(),
    val weeklyStudyMinutes: Int = 0,
    val weeklyStudySessionCount: Int = 0,
    val todayEpochDay: Long = DateUtils.todayEpochDay(),
)

private data class StudyAggregate(
    val subjects: List<StudySubjectEntity>,
    val topics: List<StudyTopicEntity>,
    val sessions: List<StudySessionEntity>,
    val career: List<CareerItemEntity>,
)

class FocusViewModel(
    private val repo: LifeOsRepository,
) : ViewModel() {
    private val _pomodoro = MutableStateFlow(PomodoroState())
    private var timerJob: Job? = null

    private val studyAggregateFlow = combine(
        repo.subjects,
        repo.topics,
        repo.studySessions,
        repo.careerItems,
    ) { subjects, topics, sessions, career ->
        StudyAggregate(subjects, topics, sessions, career)
    }

    val uiState: StateFlow<FocusUiState> = combine(
        repo.tasks,
        repo.routineBlocks,
        studyAggregateFlow,
        _pomodoro,
    ) { tasks, routine, study, pomo ->
        val today = DateUtils.todayEpochDay()
        val weekSessions = study.sessions.filter { it.epochDay in (today - 6)..today }
        val bars = (6 downTo 0).map { offset ->
            val day = today - offset
            val mins = study.sessions.filter { it.epochDay == day }.sumOf { it.durationMinutes }
            BarChartPoint(
                label = DateUtils.formatDayOfWeekShort(day),
                value = mins / 60f,
                isHighlighted = offset == 0,
            )
        }
        val activeSubjectId = pomo.selectedSubjectId ?: study.subjects.firstOrNull()?.id
        FocusUiState(
            tasks = tasks,
            routineBlocks = routine,
            subjects = study.subjects,
            topics = study.topics,
            sessions = study.sessions,
            careerItems = study.career,
            pomodoro = pomo.copy(
                selectedSubjectId = activeSubjectId,
                completedPomodorosToday = study.sessions.count { it.epochDay == today && it.mode == "POMODORO" },
            ),
            weeklyStudyBars = bars,
            weeklyStudyMinutes = weekSessions.sumOf { it.durationMinutes },
            weeklyStudySessionCount = weekSessions.size,
            todayEpochDay = today,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusUiState())

    fun saveTask(task: TaskEntity) {
        if (task.title.isBlank()) return
        viewModelScope.launch { repo.saveTask(task) }
    }

    fun toggleTask(task: TaskEntity) {
        viewModelScope.launch { repo.toggleTaskComplete(task) }
    }

    fun toggleTopThree(task: TaskEntity) {
        viewModelScope.launch { repo.updateTask(task.copy(isTopThree = !task.isTopThree)) }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch { repo.deleteTask(id) }
    }

    fun saveRoutineBlock(block: RoutineBlockEntity) {
        if (block.title.isBlank()) return
        viewModelScope.launch { repo.saveRoutineBlock(block) }
    }

    fun toggleRoutineToday(block: RoutineBlockEntity) {
        viewModelScope.launch { repo.toggleRoutineBlockToday(block) }
    }

    fun deleteRoutineBlock(id: Long) {
        viewModelScope.launch { repo.deleteRoutineBlock(id) }
    }

    fun resetStarterRoutine() {
        viewModelScope.launch { repo.resetStarterRoutine() }
    }

    fun saveSubject(subject: StudySubjectEntity) {
        if (subject.name.isBlank()) return
        viewModelScope.launch { repo.saveSubject(subject) }
    }

    fun deleteSubject(id: Long) {
        viewModelScope.launch { repo.deleteSubject(id) }
    }

    fun saveTopic(subjectId: Long, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            repo.saveTopic(
                StudyTopicEntity(
                    subjectId = subjectId,
                    title = title.trim(),
                    nextRevisionEpochDay = DateUtils.todayEpochDay() + 1,
                ),
            )
        }
    }

    fun toggleTopicMastered(topic: StudyTopicEntity) {
        viewModelScope.launch {
            val nextRev = if (!topic.isMastered) DateUtils.todayEpochDay() + 7 else DateUtils.todayEpochDay() + 1
            repo.updateTopic(
                topic.copy(
                    isMastered = !topic.isMastered,
                    revisionCount = topic.revisionCount + 1,
                    nextRevisionEpochDay = nextRev,
                ),
            )
        }
    }

    fun deleteTopic(id: Long) {
        viewModelScope.launch { repo.deleteTopic(id) }
    }

    fun logStudySession(subjectId: Long, minutes: Int, mode: String, notes: String) {
        if (minutes <= 0) return
        viewModelScope.launch {
            repo.logStudySession(
                StudySessionEntity(
                    subjectId = subjectId,
                    durationMinutes = minutes,
                    mode = mode,
                    notes = notes.trim(),
                    epochDay = DateUtils.todayEpochDay(),
                ),
            )
        }
    }

    fun saveCareerItem(item: CareerItemEntity) {
        if (item.title.isBlank()) return
        viewModelScope.launch { repo.saveCareerItem(item) }
    }

    fun deleteCareerItem(id: Long) {
        viewModelScope.launch { repo.deleteCareerItem(id) }
    }

    // Pomodoro Controls
    fun selectPomodoroSubject(subjectId: Long) {
        _pomodoro.value = _pomodoro.value.copy(selectedSubjectId = subjectId)
    }

    fun setFocusDuration(minutes: Int) {
        val clamped = minutes.coerceIn(5, 120)
        timerJob?.cancel()
        _pomodoro.value = _pomodoro.value.copy(
            isRunning = false,
            isBreak = false,
            focusMinutes = clamped,
            remainingSeconds = clamped * 60,
        )
    }

    fun toggleTimer() {
        val current = _pomodoro.value
        if (current.isRunning) {
            timerJob?.cancel()
            _pomodoro.value = current.copy(isRunning = false)
        } else {
            _pomodoro.value = current.copy(isRunning = true)
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_pomodoro.value.isRunning && _pomodoro.value.remainingSeconds > 0) {
                    delay(1000L)
                    val s = _pomodoro.value
                    if (s.remainingSeconds <= 1) {
                        onTimerFinished()
                    } else {
                        _pomodoro.value = s.copy(remainingSeconds = s.remainingSeconds - 1)
                    }
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        val s = _pomodoro.value
        _pomodoro.value = s.copy(
            isRunning = false,
            remainingSeconds = (if (s.isBreak) s.breakMinutes else s.focusMinutes) * 60,
        )
    }

    fun completeSessionEarly() {
        val s = _pomodoro.value
        val elapsedMinutes = ((s.focusMinutes * 60 - s.remainingSeconds) / 60).coerceAtLeast(1)
        val subjectId = uiState.value.subjects.firstOrNull { it.id == s.selectedSubjectId }?.id
            ?: uiState.value.subjects.firstOrNull()?.id
        if (!s.isBreak && subjectId != null) {
            logStudySession(subjectId, elapsedMinutes, "POMODORO", "Pomodoro focus session")
        }
        resetTimer()
    }

    private suspend fun onTimerFinished() {
        val s = _pomodoro.value
        val subjectId = uiState.value.subjects.firstOrNull { it.id == s.selectedSubjectId }?.id
            ?: uiState.value.subjects.firstOrNull()?.id
        if (!s.isBreak && subjectId != null) {
            repo.logStudySession(
                StudySessionEntity(
                    subjectId = subjectId,
                    durationMinutes = s.focusMinutes,
                    mode = "POMODORO",
                    notes = "Completed ${s.focusMinutes}m Pomodoro",
                    epochDay = DateUtils.todayEpochDay(),
                ),
            )
            _pomodoro.value = s.copy(
                isRunning = false,
                isBreak = true,
                remainingSeconds = s.breakMinutes * 60,
            )
        } else {
            _pomodoro.value = s.copy(
                isRunning = false,
                isBreak = false,
                remainingSeconds = s.focusMinutes * 60,
            )
        }
    }
}
