package com.example.data.repository

import androidx.room.withTransaction
import com.example.data.local.LifeOsDatabase
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
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.sample.SampleDataSeeder
import com.example.data.security.CryptoManager
import com.example.domain.util.DateUtils
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

data class DecryptedJournalEntry(
    val id: Long,
    val epochDay: Long,
    val title: String,
    val moodScore: Int,
    val reflection: String,
    val gratitude: String,
    val achievements: String,
    val challenges: String,
    val lessons: String,
    val notes: String,
    val tags: String,
    val timestamp: Long,
    val isSampleData: Boolean,
)

data class DecryptedTransaction(
    val id: Long,
    val categoryId: Long,
    val title: String,
    val amount: Double,
    val type: String,
    val note: String,
    val epochDay: Long,
    val timestamp: Long,
    val isSampleData: Boolean,
)

class LifeOsRepository(
    private val db: LifeOsDatabase,
    private val prefsRepo: UserPreferencesRepository,
) {
    // Habits
    val habits: Flow<List<HabitEntity>> = db.habitDao().observeActiveHabits()
    val habitLogs: Flow<List<HabitLogEntity>> = db.habitDao().observeAllLogs()

    suspend fun saveHabit(habit: HabitEntity): Long = db.habitDao().insertHabit(habit)
    suspend fun deleteHabit(id: Long) = db.habitDao().deleteHabitById(id)

    suspend fun setHabitStatus(habitId: Long, epochDay: Long, status: String?) {
        if (status == null) {
            db.habitDao().deleteLogForDay(habitId, epochDay)
        } else {
            db.habitDao().upsertHabitLog(
                HabitLogEntity(habitId = habitId, epochDay = epochDay, status = status),
            )
        }
    }

    // Tasks
    val tasks: Flow<List<TaskEntity>> = db.taskDao().observeTasks()
    suspend fun saveTask(task: TaskEntity): Long = db.taskDao().insertTask(task)
    suspend fun updateTask(task: TaskEntity) = db.taskDao().updateTask(task)
    suspend fun deleteTask(id: Long) = db.taskDao().deleteTaskById(id)

    suspend fun toggleTaskComplete(task: TaskEntity) {
        val nowCompleted = !task.isCompleted
        db.taskDao().updateTask(
            task.copy(
                isCompleted = nowCompleted,
                completedAt = if (nowCompleted) System.currentTimeMillis() else null,
            ),
        )
        // Spawn next occurrence automatically when completing a recurring task
        if (nowCompleted && task.recurrence != "NONE") {
            val baseDay = task.dueEpochDay ?: DateUtils.todayEpochDay()
            val nextDay = when (task.recurrence) {
                "DAILY" -> baseDay + 1
                "WEEKLY" -> baseDay + 7
                "MONTHLY" -> baseDay + 30
                else -> baseDay + 1
            }
            db.taskDao().insertTask(
                task.copy(
                    id = 0,
                    dueEpochDay = nextDay,
                    isCompleted = false,
                    completedAt = null,
                    isSampleData = false,
                    createdAt = System.currentTimeMillis(),
                ),
            )
        }
    }

    // Routine
    val routineBlocks: Flow<List<RoutineBlockEntity>> = db.routineDao().observeRoutineBlocks()
    suspend fun saveRoutineBlock(block: RoutineBlockEntity): Long = db.routineDao().insertBlock(block)
    suspend fun deleteRoutineBlock(id: Long) = db.routineDao().deleteBlockById(id)

    suspend fun toggleRoutineBlockToday(block: RoutineBlockEntity, today: Long = DateUtils.todayEpochDay()) {
        val nextDay = if (block.lastCompletedEpochDay == today) null else today
        db.routineDao().updateBlock(block.copy(lastCompletedEpochDay = nextDay))
    }

    suspend fun resetStarterRoutine() {
        db.withTransaction {
            db.routineDao().deleteAllBlocks()
            db.routineDao().insertBlocks(SampleDataSeeder.starterRoutineBlocks(markAsSample = false))
        }
    }

    // Study & Career
    val subjects: Flow<List<StudySubjectEntity>> = db.studyDao().observeSubjects()
    val topics: Flow<List<StudyTopicEntity>> = db.studyDao().observeTopics()
    val studySessions: Flow<List<StudySessionEntity>> = db.studyDao().observeSessions()
    val careerItems: Flow<List<CareerItemEntity>> = db.studyDao().observeCareerItems()

    suspend fun saveSubject(subject: StudySubjectEntity): Long = db.studyDao().insertSubject(subject)
    suspend fun deleteSubject(id: Long) = db.studyDao().deleteSubjectById(id)
    suspend fun saveTopic(topic: StudyTopicEntity): Long = db.studyDao().insertTopic(topic)
    suspend fun updateTopic(topic: StudyTopicEntity) = db.studyDao().updateTopic(topic)
    suspend fun deleteTopic(id: Long) = db.studyDao().deleteTopicById(id)
    suspend fun logStudySession(session: StudySessionEntity): Long = db.studyDao().insertSession(session)
    suspend fun saveCareerItem(item: CareerItemEntity): Long = db.studyDao().insertCareerItem(item)
    suspend fun deleteCareerItem(id: Long) = db.studyDao().deleteCareerItemById(id)

    // Fitness & Health
    val exerciseTemplates: Flow<List<ExerciseTemplateEntity>> = db.fitnessDao().observeExerciseTemplates()
    val workoutLogs: Flow<List<WorkoutLogEntity>> = db.fitnessDao().observeWorkoutLogs()
    val workoutExercises: Flow<List<WorkoutExerciseEntity>> = db.fitnessDao().observeWorkoutExercises()
    val healthMetrics: Flow<List<HealthMetricEntity>> = db.fitnessDao().observeHealthMetrics()

    suspend fun saveExerciseTemplate(template: ExerciseTemplateEntity): Long =
        db.fitnessDao().insertTemplate(template)

    suspend fun logWorkoutWithExercises(
        log: WorkoutLogEntity,
        exercises: List<WorkoutExerciseEntity>,
    ) {
        db.withTransaction {
            val workoutId = db.fitnessDao().insertWorkoutLog(log)
            if (exercises.isNotEmpty()) {
                db.fitnessDao().insertWorkoutExercises(exercises.map { it.copy(workoutId = workoutId) })
            }
        }
    }

    suspend fun deleteWorkout(id: Long) = db.fitnessDao().deleteWorkoutById(id)
    suspend fun upsertHealthMetric(metric: HealthMetricEntity) = db.fitnessDao().upsertHealthMetric(metric)

    // Finance (decrypted note exposure)
    val financeCategories: Flow<List<FinanceCategoryEntity>> = db.financeDao().observeCategories()
    val financeTransactions: Flow<List<DecryptedTransaction>> =
        db.financeDao().observeTransactions().map { list ->
            list.map { tx ->
                DecryptedTransaction(
                    id = tx.id,
                    categoryId = tx.categoryId,
                    title = tx.title,
                    amount = tx.amount,
                    type = tx.type,
                    note = CryptoManager.decryptField(tx.encryptedNote),
                    epochDay = tx.epochDay,
                    timestamp = tx.timestamp,
                    isSampleData = tx.isSampleData,
                )
            }
        }
    val savingsGoals: Flow<List<SavingsGoalEntity>> = db.financeDao().observeSavingsGoals()

    suspend fun saveFinanceCategory(category: FinanceCategoryEntity): Long =
        db.financeDao().insertCategory(category)

    suspend fun deleteFinanceCategory(id: Long) = db.financeDao().deleteCategoryById(id)

    suspend fun saveTransaction(
        categoryId: Long,
        title: String,
        amount: Double,
        type: String,
        plainNote: String,
        epochDay: Long,
    ): Long {
        return db.financeDao().insertTransaction(
            FinanceTransactionEntity(
                categoryId = categoryId,
                title = title,
                amount = amount,
                type = type,
                encryptedNote = CryptoManager.encryptField(plainNote),
                epochDay = epochDay,
            ),
        )
    }

    suspend fun deleteTransaction(id: Long) = db.financeDao().deleteTransactionById(id)
    suspend fun saveSavingsGoal(goal: SavingsGoalEntity): Long = db.financeDao().insertSavingsGoal(goal)
    suspend fun deleteSavingsGoal(id: Long) = db.financeDao().deleteSavingsGoalById(id)

    // Goals & Milestones
    val goals: Flow<List<LifeGoalEntity>> = db.goalsDao().observeGoals()
    val milestones: Flow<List<GoalMilestoneEntity>> = db.goalsDao().observeMilestones()

    suspend fun saveGoal(goal: LifeGoalEntity): Long = db.goalsDao().insertGoal(goal)
    suspend fun deleteGoal(id: Long) = db.goalsDao().deleteGoalById(id)
    suspend fun saveMilestone(milestone: GoalMilestoneEntity): Long = db.goalsDao().insertMilestone(milestone)
    suspend fun deleteMilestone(id: Long) = db.goalsDao().deleteMilestoneById(id)

    suspend fun toggleMilestoneAndSyncGoal(
        milestone: GoalMilestoneEntity,
        allMilestonesForGoal: List<GoalMilestoneEntity>,
        goal: LifeGoalEntity,
    ) {
        val updated = milestone.copy(isDone = !milestone.isDone)
        db.goalsDao().updateMilestone(updated)
        val merged = allMilestonesForGoal.map { if (it.id == updated.id) updated else it }
        if (merged.isNotEmpty()) {
            val doneCount = merged.count { it.isDone }
            val pct = ((doneCount * 100f) / merged.size).toInt().coerceIn(0, 100)
            db.goalsDao().updateGoal(
                goal.copy(
                    progressPercent = pct,
                    isCompleted = pct == 100,
                    completedAt = if (pct == 100) System.currentTimeMillis() else null,
                ),
            )
        }
    }

    // Journal (decrypted field exposure)
    val journalEntries: Flow<List<DecryptedJournalEntry>> =
        db.journalReadingDisciplineDao().observeJournalEntries().map { list ->
            list.map { e ->
                DecryptedJournalEntry(
                    id = e.id,
                    epochDay = e.epochDay,
                    title = e.title,
                    moodScore = e.moodScore,
                    reflection = CryptoManager.decryptField(e.encryptedReflection),
                    gratitude = CryptoManager.decryptField(e.encryptedGratitude),
                    achievements = CryptoManager.decryptField(e.encryptedAchievements),
                    challenges = CryptoManager.decryptField(e.encryptedChallenges),
                    lessons = CryptoManager.decryptField(e.encryptedLessons),
                    notes = CryptoManager.decryptField(e.encryptedNotes),
                    tags = e.tags,
                    timestamp = e.timestamp,
                    isSampleData = e.isSampleData,
                )
            }
        }

    suspend fun saveJournalEntry(entry: DecryptedJournalEntry): Long {
        return db.journalReadingDisciplineDao().insertJournalEntry(
            JournalEntryEntity(
                id = entry.id,
                epochDay = entry.epochDay,
                title = entry.title,
                moodScore = entry.moodScore.coerceIn(1, 5),
                encryptedReflection = CryptoManager.encryptField(entry.reflection),
                encryptedGratitude = CryptoManager.encryptField(entry.gratitude),
                encryptedAchievements = CryptoManager.encryptField(entry.achievements),
                encryptedChallenges = CryptoManager.encryptField(entry.challenges),
                encryptedLessons = CryptoManager.encryptField(entry.lessons),
                encryptedNotes = CryptoManager.encryptField(entry.notes),
                tags = entry.tags,
                timestamp = if (entry.id == 0L) System.currentTimeMillis() else entry.timestamp,
                isSampleData = false,
            ),
        )
    }

    suspend fun deleteJournalEntry(id: Long) =
        db.journalReadingDisciplineDao().deleteJournalEntryById(id)

    // Reading & Knowledge
    val books: Flow<List<BookItemEntity>> = db.journalReadingDisciplineDao().observeBooks()
    val learningResources: Flow<List<LearningResourceEntity>> = db.journalReadingDisciplineDao().observeResources()

    suspend fun saveBook(book: BookItemEntity): Long = db.journalReadingDisciplineDao().insertBook(book)
    suspend fun deleteBook(id: Long) = db.journalReadingDisciplineDao().deleteBookById(id)
    suspend fun saveLearningResource(resource: LearningResourceEntity): Long =
        db.journalReadingDisciplineDao().insertResource(resource)
    suspend fun deleteLearningResource(id: Long) =
        db.journalReadingDisciplineDao().deleteResourceById(id)

    // Digital Discipline
    val screenTimeLogs: Flow<List<ScreenTimeLogEntity>> = db.journalReadingDisciplineDao().observeScreenTimeLogs()
    suspend fun upsertScreenTimeLog(log: ScreenTimeLogEntity) =
        db.journalReadingDisciplineDao().upsertScreenTimeLog(log)

    // Sample data & destructive clear
    suspend fun clearSampleData() = SampleDataSeeder.clearSampleData(db, prefsRepo)

    suspend fun clearAllUserData() {
        db.clearAllTables()
        db.fitnessDao().insertTemplates(SampleDataSeeder.defaultExerciseTemplates())
        SampleDataSeeder.defaultFinanceCategories().forEach { db.financeDao().insertCategory(it) }
        prefsRepo.setSeededState(seeded = true, demoActive = false)
    }
}
