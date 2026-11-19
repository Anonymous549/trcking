package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.BookItemEntity
import com.example.data.local.entity.CareerItemEntity
import com.example.data.local.entity.ExerciseTemplateEntity
import com.example.data.local.entity.FinanceCategoryEntity
import com.example.data.local.entity.FinanceTransactionEntity
import com.example.data.local.entity.GoalMilestoneEntity
import com.example.data.local.entity.HabitEntity
import com.example.data.local.entity.HabitLogEntity
import com.example.data.local.entity.HealthMetricEntity
import com.example.data.local.entity.JournalEntryEntity
import com.example.data.local.entity.LearningResourceEntity
import com.example.data.local.entity.LifeGoalEntity
import com.example.data.local.entity.RoutineBlockEntity
import com.example.data.local.entity.SavingsGoalEntity
import com.example.data.local.entity.ScreenTimeLogEntity
import com.example.data.local.entity.StudySessionEntity
import com.example.data.local.entity.StudySubjectEntity
import com.example.data.local.entity.StudyTopicEntity
import com.example.data.local.entity.TaskEntity
import com.example.data.local.entity.WorkoutExerciseEntity
import com.example.data.local.entity.WorkoutLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt ASC")
    fun observeActiveHabits(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY createdAt ASC")
    suspend fun getAllHabitsOnce(): List<HabitEntity>

    @Query("SELECT * FROM habit_logs ORDER BY epochDay DESC")
    fun observeAllLogs(): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs ORDER BY epochDay DESC")
    suspend fun getAllLogsOnce(): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun deleteHabitById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHabitLog(log: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteLogForDay(habitId: Long, epochDay: Long)

    @Query("DELETE FROM habits WHERE isSampleData = 1")
    suspend fun deleteSampleHabits()
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, priority ASC, dueEpochDay ASC")
    fun observeTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks")
    suspend fun getAllTasksOnce(): List<TaskEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskEntity): Long

    @Update
    suspend fun updateTask(task: TaskEntity)

    @Query("DELETE FROM tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("DELETE FROM tasks WHERE isSampleData = 1")
    suspend fun deleteSampleTasks()
}

@Dao
interface RoutineDao {
    @Query("SELECT * FROM routine_blocks ORDER BY startMinuteOfDay ASC, sortOrder ASC")
    fun observeRoutineBlocks(): Flow<List<RoutineBlockEntity>>

    @Query("SELECT * FROM routine_blocks ORDER BY startMinuteOfDay ASC")
    suspend fun getAllRoutineBlocksOnce(): List<RoutineBlockEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlock(block: RoutineBlockEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlocks(blocks: List<RoutineBlockEntity>)

    @Update
    suspend fun updateBlock(block: RoutineBlockEntity)

    @Query("DELETE FROM routine_blocks WHERE id = :id")
    suspend fun deleteBlockById(id: Long)

    @Query("DELETE FROM routine_blocks")
    suspend fun deleteAllBlocks()

    @Query("DELETE FROM routine_blocks WHERE isSampleData = 1")
    suspend fun deleteSampleRoutine()
}

@Dao
interface StudyDao {
    @Query("SELECT * FROM study_subjects ORDER BY createdAt ASC")
    fun observeSubjects(): Flow<List<StudySubjectEntity>>

    @Query("SELECT * FROM study_subjects")
    suspend fun getAllSubjectsOnce(): List<StudySubjectEntity>

    @Query("SELECT * FROM study_topics ORDER BY id ASC")
    fun observeTopics(): Flow<List<StudyTopicEntity>>

    @Query("SELECT * FROM study_topics")
    suspend fun getAllTopicsOnce(): List<StudyTopicEntity>

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    fun observeSessions(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions ORDER BY timestamp DESC")
    suspend fun getAllSessionsOnce(): List<StudySessionEntity>

    @Query("SELECT * FROM career_items ORDER BY isCompleted ASC, id DESC")
    fun observeCareerItems(): Flow<List<CareerItemEntity>>

    @Query("SELECT * FROM career_items")
    suspend fun getAllCareerItemsOnce(): List<CareerItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubject(subject: StudySubjectEntity): Long

    @Update
    suspend fun updateSubject(subject: StudySubjectEntity)

    @Query("DELETE FROM study_subjects WHERE id = :id")
    suspend fun deleteSubjectById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTopic(topic: StudyTopicEntity): Long

    @Update
    suspend fun updateTopic(topic: StudyTopicEntity)

    @Query("DELETE FROM study_topics WHERE id = :id")
    suspend fun deleteTopicById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: StudySessionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCareerItem(item: CareerItemEntity): Long

    @Update
    suspend fun updateCareerItem(item: CareerItemEntity)

    @Query("DELETE FROM career_items WHERE id = :id")
    suspend fun deleteCareerItemById(id: Long)

    @Query("DELETE FROM study_subjects WHERE isSampleData = 1")
    suspend fun deleteSampleSubjects()

    @Query("DELETE FROM career_items WHERE isSampleData = 1")
    suspend fun deleteSampleCareerItems()
}

@Dao
interface FitnessDao {
    @Query("SELECT * FROM exercise_templates ORDER BY isCustom DESC, muscleGroup ASC, name ASC")
    fun observeExerciseTemplates(): Flow<List<ExerciseTemplateEntity>>

    @Query("SELECT * FROM exercise_templates")
    suspend fun getAllTemplatesOnce(): List<ExerciseTemplateEntity>

    @Query("SELECT * FROM workout_logs ORDER BY epochDay DESC, timestamp DESC")
    fun observeWorkoutLogs(): Flow<List<WorkoutLogEntity>>

    @Query("SELECT * FROM workout_logs ORDER BY epochDay DESC")
    suspend fun getAllWorkoutLogsOnce(): List<WorkoutLogEntity>

    @Query("SELECT * FROM workout_exercises")
    fun observeWorkoutExercises(): Flow<List<WorkoutExerciseEntity>>

    @Query("SELECT * FROM workout_exercises")
    suspend fun getAllWorkoutExercisesOnce(): List<WorkoutExerciseEntity>

    @Query("SELECT * FROM health_metrics ORDER BY epochDay DESC")
    fun observeHealthMetrics(): Flow<List<HealthMetricEntity>>

    @Query("SELECT * FROM health_metrics ORDER BY epochDay DESC")
    suspend fun getAllHealthMetricsOnce(): List<HealthMetricEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: ExerciseTemplateEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplates(templates: List<ExerciseTemplateEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutLog(log: WorkoutLogEntity): Long

    @Query("DELETE FROM workout_logs WHERE id = :id")
    suspend fun deleteWorkoutById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertHealthMetric(metric: HealthMetricEntity)

    @Query("DELETE FROM workout_logs WHERE isSampleData = 1")
    suspend fun deleteSampleWorkouts()

    @Query("DELETE FROM health_metrics WHERE isSampleData = 1")
    suspend fun deleteSampleHealthMetrics()
}

@Dao
interface FinanceDao {
    @Query("SELECT * FROM finance_categories ORDER BY type ASC, name ASC")
    fun observeCategories(): Flow<List<FinanceCategoryEntity>>

    @Query("SELECT * FROM finance_categories")
    suspend fun getAllCategoriesOnce(): List<FinanceCategoryEntity>

    @Query("SELECT * FROM finance_transactions ORDER BY epochDay DESC, timestamp DESC")
    fun observeTransactions(): Flow<List<FinanceTransactionEntity>>

    @Query("SELECT * FROM finance_transactions ORDER BY epochDay DESC")
    suspend fun getAllTransactionsOnce(): List<FinanceTransactionEntity>

    @Query("SELECT * FROM savings_goals ORDER BY id ASC")
    fun observeSavingsGoals(): Flow<List<SavingsGoalEntity>>

    @Query("SELECT * FROM savings_goals")
    suspend fun getAllSavingsGoalsOnce(): List<SavingsGoalEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: FinanceCategoryEntity): Long

    @Update
    suspend fun updateCategory(category: FinanceCategoryEntity)

    @Query("DELETE FROM finance_categories WHERE id = :id")
    suspend fun deleteCategoryById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: FinanceTransactionEntity): Long

    @Query("DELETE FROM finance_transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoalEntity)

    @Query("DELETE FROM savings_goals WHERE id = :id")
    suspend fun deleteSavingsGoalById(id: Long)

    @Query("DELETE FROM finance_transactions WHERE isSampleData = 1")
    suspend fun deleteSampleTransactions()

    @Query("DELETE FROM savings_goals WHERE isSampleData = 1")
    suspend fun deleteSampleSavingsGoals()
}

@Dao
interface GoalsDao {
    @Query("SELECT * FROM life_goals ORDER BY isCompleted ASC, priority ASC, id DESC")
    fun observeGoals(): Flow<List<LifeGoalEntity>>

    @Query("SELECT * FROM life_goals")
    suspend fun getAllGoalsOnce(): List<LifeGoalEntity>

    @Query("SELECT * FROM goal_milestones ORDER BY id ASC")
    fun observeMilestones(): Flow<List<GoalMilestoneEntity>>

    @Query("SELECT * FROM goal_milestones")
    suspend fun getAllMilestonesOnce(): List<GoalMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: LifeGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: LifeGoalEntity)

    @Query("DELETE FROM life_goals WHERE id = :id")
    suspend fun deleteGoalById(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestone(milestone: GoalMilestoneEntity): Long

    @Update
    suspend fun updateMilestone(milestone: GoalMilestoneEntity)

    @Query("DELETE FROM goal_milestones WHERE id = :id")
    suspend fun deleteMilestoneById(id: Long)

    @Query("DELETE FROM life_goals WHERE isSampleData = 1")
    suspend fun deleteSampleGoals()
}

@Dao
interface JournalReadingDisciplineDao {
    @Query("SELECT * FROM journal_entries ORDER BY epochDay DESC, timestamp DESC")
    fun observeJournalEntries(): Flow<List<JournalEntryEntity>>

    @Query("SELECT * FROM journal_entries ORDER BY epochDay DESC")
    suspend fun getAllJournalEntriesOnce(): List<JournalEntryEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntryEntity): Long

    @Update
    suspend fun updateJournalEntry(entry: JournalEntryEntity)

    @Query("DELETE FROM journal_entries WHERE id = :id")
    suspend fun deleteJournalEntryById(id: Long)

    @Query("SELECT * FROM books ORDER BY updatedAt DESC")
    fun observeBooks(): Flow<List<BookItemEntity>>

    @Query("SELECT * FROM books")
    suspend fun getAllBooksOnce(): List<BookItemEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBook(book: BookItemEntity): Long

    @Update
    suspend fun updateBook(book: BookItemEntity)

    @Query("DELETE FROM books WHERE id = :id")
    suspend fun deleteBookById(id: Long)

    @Query("SELECT * FROM learning_resources ORDER BY createdAt DESC")
    fun observeResources(): Flow<List<LearningResourceEntity>>

    @Query("SELECT * FROM learning_resources")
    suspend fun getAllResourcesOnce(): List<LearningResourceEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResource(resource: LearningResourceEntity): Long

    @Update
    suspend fun updateResource(resource: LearningResourceEntity)

    @Query("DELETE FROM learning_resources WHERE id = :id")
    suspend fun deleteResourceById(id: Long)

    @Query("SELECT * FROM screen_time_logs ORDER BY epochDay DESC")
    fun observeScreenTimeLogs(): Flow<List<ScreenTimeLogEntity>>

    @Query("SELECT * FROM screen_time_logs ORDER BY epochDay DESC")
    suspend fun getAllScreenTimeLogsOnce(): List<ScreenTimeLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertScreenTimeLog(log: ScreenTimeLogEntity)

    @Query("DELETE FROM journal_entries WHERE isSampleData = 1")
    suspend fun deleteSampleJournal()

    @Query("DELETE FROM books WHERE isSampleData = 1")
    suspend fun deleteSampleBooks()

    @Query("DELETE FROM learning_resources WHERE isSampleData = 1")
    suspend fun deleteSampleResources()

    @Query("DELETE FROM screen_time_logs WHERE isSampleData = 1")
    suspend fun deleteSampleScreenTime()
}
