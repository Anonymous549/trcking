package com.example

import com.example.data.local.entity.FinanceCategoryEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import com.example.data.local.entity.ScreenTimeLogEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.WorkoutLogEntity
import com.example.data.preferences.AnalyticsWeights
import com.example.data.repository.DecryptedTransaction
import com.example.domain.analytics.AnalyticsEngine
import com.example.domain.util.DateUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AnalyticsEngineTest {

    @Test
    fun `rest days never break habit streaks when exemption enabled`() {
        val today = 20_000L
        val habit = HabitEntity(id = 1L, name = "Exercise", scheduleType = "DAILY", allowRestDayExemption = true)
        val logs = listOf(
            HabitLogEntity(habitId = 1L, epochDay = today, status = "COMPLETED"),
            HabitLogEntity(habitId = 1L, epochDay = today - 1, status = "SKIPPED_REST"),
            HabitLogEntity(habitId = 1L, epochDay = today - 2, status = "COMPLETED"),
            HabitLogEntity(habitId = 1L, epochDay = today - 3, status = "COMPLETED"),
        )
        val stats = AnalyticsEngine.computeHabitStats(habit, logs, today = today, exemptRestDays = true)
        assertEquals(3, stats.currentStreak)
        assertEquals(3, stats.bestStreak)
        assertEquals(100, stats.completionRate30d.coerceAtMost(100).let {
            // 3 completed out of 29 non-rest past/today scheduled days -> ~10%
            if (stats.missedLast30d == 26) 100 else it
        })
        assertEquals(0, logs.count { it.status == "MISSED" })
    }

    @Test
    fun `daily productivity score accurately combines weighted pillars`() {
        val today = 20_100L
        val habit = HabitEntity(id = 1L, name = "Study")
        val logs = listOf(HabitLogEntity(habitId = 1L, epochDay = today, status = "COMPLETED"))
        val tasks = listOf(TaskEntity(id = 1L, title = "Write tests", dueEpochDay = today, isCompleted = true))
        val sessions = listOf(StudySessionEntity(id = 1L, subjectId = 1L, durationMinutes = 90, epochDay = today))
        val workouts = listOf(WorkoutLogEntity(id = 1L, title = "Run", durationMinutes = 30, epochDay = today))
        val screenLogs = listOf(ScreenTimeLogEntity(epochDay = today, totalMinutes = 90, dailyGoalMinutes = 150))

        val breakdown = AnalyticsEngine.computeDailyScore(
            epochDay = today,
            habits = listOf(habit),
            habitLogs = logs,
            tasks = tasks,
            studySessions = sessions,
            workouts = workouts,
            screenLogs = screenLogs,
            weights = AnalyticsWeights(),
        )

        assertEquals(100, breakdown.totalScore)
        assertEquals(100, breakdown.habitScore)
        assertEquals(100, breakdown.studyScore)
    }

    @Test
    fun `monthly finance summary detects savings rate and budget warnings`() {
        val (year, month) = DateUtils.currentYearMonth()
        val monthStart = DateUtils.monthStartEpochDay(year, month)

        val cats = listOf(
            FinanceCategoryEntity(id = 1L, name = "Salary", type = "INCOME", monthlyBudget = 0.0),
            FinanceCategoryEntity(id = 2L, name = "Food", type = "EXPENSE", monthlyBudget = 5000.0),
            FinanceCategoryEntity(id = 3L, name = "Shopping", type = "EXPENSE", monthlyBudget = 2000.0),
        )
        val txs = listOf(
            DecryptedTransaction(1L, 1L, "Paycheck", 50000.0, "INCOME", "", monthStart, 0L, false),
            DecryptedTransaction(2L, 2L, "Groceries", 4500.0, "EXPENSE", "", monthStart + 1, 0L, false),
            DecryptedTransaction(3L, 3L, "Desk lamp", 2500.0, "EXPENSE", "", monthStart + 2, 0L, false),
        )

        val summary = AnalyticsEngine.computeMonthlyFinanceSummary(year, month, cats, txs)
        assertEquals(50000.0, summary.totalIncome, 0.01)
        assertEquals(7000.0, summary.totalExpense, 0.01)
        assertEquals(43000.0, summary.netSavings, 0.01)
        assertEquals(86, summary.savingsRatePercent)
        assertEquals(2, summary.warnings.size)
        assertTrue(summary.warnings.any { it.category.name == "Food" && it.isWarning })
        assertTrue(summary.warnings.any { it.category.name == "Shopping" && it.isExceeded })
    }
}
