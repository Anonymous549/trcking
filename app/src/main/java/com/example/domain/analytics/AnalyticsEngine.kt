package com.example.domain.analytics

import com.example.data.local.entity.FinanceCategoryEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.ScreenTimeLogEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.WorkoutLogEntity
import com.example.data.preferences.AnalyticsWeights
import com.example.data.repository.DecryptedTransaction
import com.example.domain.util.DateUtils
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

data class HabitStats(
    val habitId: Long,
    val currentStreak: Int,
    val bestStreak: Int,
    val completionRate30d: Int,
    val missedLast30d: Int,
    val completedThisWeek: Int,
    val completedThisMonth: Int,
)

data class DailyProductivityBreakdown(
    val epochDay: Long,
    val totalScore: Int,
    val habitScore: Int,
    val taskScore: Int,
    val studyScore: Int,
    val fitnessScore: Int,
    val disciplineScore: Int,
)

data class CategorySpendSummary(
    val category: FinanceCategoryEntity,
    val spent: Double,
    val budgetRatio: Float,
    val isWarning: Boolean,
    val isExceeded: Boolean,
)

data class MonthlyFinanceSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val netSavings: Double,
    val savingsRatePercent: Int,
    val categoryBreakdown: List<CategorySpendSummary>,
    val warnings: List<CategorySpendSummary>,
)

data class PeriodMetrics(
    val avgProductivityScore: Int,
    val habitConsistencyPercent: Int,
    val totalStudyMinutes: Int,
    val totalWorkouts: Int,
    val completedGoals: Int,
    val netSavings: Double,
)

data class PeriodComparison(
    val current: PeriodMetrics,
    val previous: PeriodMetrics,
)

object AnalyticsEngine {

    fun isHabitScheduledOnDay(habit: HabitEntity, epochDay: Long): Boolean {
        if (habit.scheduleType == "DAILY") return true
        val isoDay = DateUtils.isoDayOfWeek(epochDay)
        val days = habit.targetDaysCsv.split(",").mapNotNull { it.trim().toIntOrNull() }
        return if (days.isEmpty()) true else isoDay in days
    }

    fun computeHabitStats(
        habit: HabitEntity,
        logs: List<HabitLogEntity>,
        today: Long = DateUtils.todayEpochDay(),
        exemptRestDays: Boolean = true,
    ): HabitStats {
        val logsByDay = logs.filter { it.habitId == habit.id }.associateBy { it.epochDay }

        // Current streak (start from today if logged, otherwise yesterday)
        var streak = 0
        var cursor = if (logsByDay[today]?.status == "COMPLETED") today else today - 1
        for (i in 0 until 365) {
            val day = cursor - i
            val log = logsByDay[day]
            val scheduled = isHabitScheduledOnDay(habit, day)
            when {
                log?.status == "COMPLETED" -> streak++
                log?.status == "SKIPPED_REST" && (exemptRestDays || habit.allowRestDayExemption) -> {
                    // Rest days never break streaks
                }
                !scheduled -> {
                    // Unscheduled day does not break streak
                }
                else -> break
            }
        }

        // Best streak over past 365 days
        var best = streak
        var running = 0
        for (day in (today - 365)..today) {
            val log = logsByDay[day]
            val scheduled = isHabitScheduledOnDay(habit, day)
            when {
                log?.status == "COMPLETED" -> {
                    running++
                    if (running > best) best = running
                }
                log?.status == "SKIPPED_REST" && (exemptRestDays || habit.allowRestDayExemption) -> {
                    // preserved
                }
                !scheduled -> {
                    // preserved
                }
                else -> running = 0
            }
        }

        // 30-day consistency & missed count
        var scheduledDays = 0
        var completedDays = 0
        var missedDays = 0
        for (offset in 0 until 30) {
            val day = today - offset
            val log = logsByDay[day]
            if (log?.status == "SKIPPED_REST" && (exemptRestDays || habit.allowRestDayExemption)) {
                continue
            }
            if (isHabitScheduledOnDay(habit, day)) {
                scheduledDays++
                if (log?.status == "COMPLETED") {
                    completedDays++
                } else if (offset > 0) {
                    // Today is still in progress; only count past days as missed
                    missedDays++
                }
            }
        }
        val rate = if (scheduledDays == 0) 100 else ((completedDays * 100f) / scheduledDays).roundToInt().coerceIn(0, 100)

        val completedWeek = (0 until 7).count { logsByDay[today - it]?.status == "COMPLETED" }
        val completedMonth = (0 until 30).count { logsByDay[today - it]?.status == "COMPLETED" }

        return HabitStats(
            habitId = habit.id,
            currentStreak = streak,
            bestStreak = best,
            completionRate30d = rate,
            missedLast30d = missedDays,
            completedThisWeek = completedWeek,
            completedThisMonth = completedMonth,
        )
    }

    fun computeDailyScore(
        epochDay: Long,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        tasks: List<TaskEntity>,
        studySessions: List<StudySessionEntity>,
        workouts: List<WorkoutLogEntity>,
        screenLogs: List<ScreenTimeLogEntity>,
        weights: AnalyticsWeights = AnalyticsWeights(),
    ): DailyProductivityBreakdown {
        val activeHabits = habits.filter { !it.isArchived && isHabitScheduledOnDay(it, epochDay) }
        val dayLogs = habitLogs.filter { it.epochDay == epochDay }.associateBy { it.habitId }

        val nonRestHabits = activeHabits.filter { habit ->
            val log = dayLogs[habit.id]
            !(log?.status == "SKIPPED_REST" && (weights.exemptRestDays || habit.allowRestDayExemption))
        }
        val habitRatio = if (nonRestHabits.isEmpty()) {
            1f
        } else {
            nonRestHabits.count { dayLogs[it.id]?.status == "COMPLETED" }.toFloat() / nonRestHabits.size
        }

        val tasksDueOrCompletedToday = tasks.filter { task ->
            task.dueEpochDay == epochDay ||
                (task.completedAt != null && DateUtils.millisToDays(task.completedAt) == epochDay)
        }
        val taskRatio = if (tasksDueOrCompletedToday.isEmpty()) {
            if (tasks.any { it.isCompleted }) 1f else 0.7f
        } else {
            tasksDueOrCompletedToday.count { it.isCompleted }.toFloat() / tasksDueOrCompletedToday.size
        }

        val studyMinutesToday = studySessions.filter { it.epochDay == epochDay }.sumOf { it.durationMinutes }
        val studyRatio = min(1f, studyMinutesToday / 90f)

        val workedOutToday = workouts.any { it.epochDay == epochDay }
        // Compassionate fitness scoring: never heavily penalize rest days when exemptRestDays is true
        val fitnessRatio = when {
            workedOutToday -> 1f
            weights.exemptRestDays -> 0.65f
            else -> 0.25f
        }

        val screenLog = screenLogs.firstOrNull { it.epochDay == epochDay }
        val disciplineRatio = when {
            screenLog == null -> 0.8f
            screenLog.totalMinutes <= screenLog.dailyGoalMinutes -> 1f
            else -> {
                val over = (screenLog.totalMinutes - screenLog.dailyGoalMinutes).toFloat()
                max(0.2f, 1f - (over / max(60f, screenLog.dailyGoalMinutes.toFloat())))
            }
        }

        val totalWeight = max(
            1,
            weights.habitWeight + weights.taskWeight + weights.studyWeight + weights.fitnessWeight + weights.disciplineWeight,
        )
        val weighted = (
            habitRatio * weights.habitWeight +
                taskRatio * weights.taskWeight +
                studyRatio * weights.studyWeight +
                fitnessRatio * weights.fitnessWeight +
                disciplineRatio * weights.disciplineWeight
            ) / totalWeight

        return DailyProductivityBreakdown(
            epochDay = epochDay,
            totalScore = (weighted * 100f).roundToInt().coerceIn(0, 100),
            habitScore = (habitRatio * 100f).roundToInt(),
            taskScore = (taskRatio * 100f).roundToInt(),
            studyScore = (studyRatio * 100f).roundToInt(),
            fitnessScore = (fitnessRatio * 100f).roundToInt(),
            disciplineScore = (disciplineRatio * 100f).roundToInt(),
        )
    }

    fun computeMonthlyFinanceSummary(
        year: Int,
        month1Based: Int,
        categories: List<FinanceCategoryEntity>,
        transactions: List<DecryptedTransaction>,
    ): MonthlyFinanceSummary {
        val start = DateUtils.monthStartEpochDay(year, month1Based)
        val len = DateUtils.monthLength(year, month1Based)
        val end = start + len - 1
        val monthTx = transactions.filter { it.epochDay in start..end }

        val income = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val expense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val net = income - expense
        val savingsRate = if (income > 0.0) (((income - expense) / income) * 100.0).roundToInt() else 0

        val expenseCategories = categories.filter { it.type == "EXPENSE" }
        val breakdown = expenseCategories.map { cat ->
            val spent = monthTx.filter { it.type == "EXPENSE" && it.categoryId == cat.id }.sumOf { it.amount }
            val ratio = if (cat.monthlyBudget > 0.0) (spent / cat.monthlyBudget).toFloat() else 0f
            CategorySpendSummary(
                category = cat,
                spent = spent,
                budgetRatio = ratio,
                isWarning = cat.monthlyBudget > 0.0 && ratio >= 0.8f && ratio <= 1.0f,
                isExceeded = cat.monthlyBudget > 0.0 && ratio > 1.0f,
            )
        }.sortedByDescending { it.spent }

        val warnings = breakdown.filter { it.isWarning || it.isExceeded }

        return MonthlyFinanceSummary(
            totalIncome = income,
            totalExpense = expense,
            netSavings = net,
            savingsRatePercent = savingsRate,
            categoryBreakdown = breakdown,
            warnings = warnings,
        )
    }

    fun computePeriodMetrics(
        startEpochDay: Long,
        endEpochDay: Long,
        habits: List<HabitEntity>,
        habitLogs: List<HabitLogEntity>,
        tasks: List<TaskEntity>,
        studySessions: List<StudySessionEntity>,
        workouts: List<WorkoutLogEntity>,
        transactions: List<DecryptedTransaction>,
        goals: List<LifeGoalEntity>,
        screenLogs: List<ScreenTimeLogEntity>,
        weights: AnalyticsWeights,
    ): PeriodMetrics {
        val days = (startEpochDay..endEpochDay).toList()
        val dailyScores = days.map { day ->
            computeDailyScore(day, habits, habitLogs, tasks, studySessions, workouts, screenLogs, weights).totalScore
        }
        val avgScore = if (dailyScores.isEmpty()) 0 else dailyScores.average().roundToInt()

        val activeHabits = habits.filter { !it.isArchived }
        var expected = 0
        var completed = 0
        days.forEach { day ->
            activeHabits.forEach { habit ->
                val log = habitLogs.firstOrNull { it.habitId == habit.id && it.epochDay == day }
                if (log?.status == "SKIPPED_REST" && (weights.exemptRestDays || habit.allowRestDayExemption)) {
                    return@forEach
                }
                if (isHabitScheduledOnDay(habit, day)) {
                    expected++
                    if (log?.status == "COMPLETED") completed++
                }
            }
        }
        val consistency = if (expected == 0) 0 else ((completed * 100f) / expected).roundToInt()
        val studyMins = studySessions.filter { it.epochDay in startEpochDay..endEpochDay }.sumOf { it.durationMinutes }
        val workoutCount = workouts.count { it.epochDay in startEpochDay..endEpochDay }
        val periodTx = transactions.filter { it.epochDay in startEpochDay..endEpochDay }
        val netSavings = periodTx.filter { it.type == "INCOME" }.sumOf { it.amount } -
            periodTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }

        val completedGoals = goals.count { goal ->
            goal.isCompleted && goal.completedAt != null &&
                DateUtils.millisToDays(goal.completedAt) in startEpochDay..endEpochDay
        }

        return PeriodMetrics(
            avgProductivityScore = avgScore,
            habitConsistencyPercent = consistency,
            totalStudyMinutes = studyMins,
            totalWorkouts = workoutCount,
            completedGoals = completedGoals,
            netSavings = netSavings,
        )
    }
}
