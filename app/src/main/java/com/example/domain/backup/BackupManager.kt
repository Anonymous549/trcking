package com.example.domain.backup

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
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.security.CryptoManager
import com.example.domain.util.DateUtils
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class LifeOsBackupPayload(
    val preferences: UserPreferences,
    val habits: List<HabitEntity> = emptyList(),
    val habitLogs: List<HabitLogEntity> = emptyList(),
    val tasks: List<TaskEntity> = emptyList(),
    val routineBlocks: List<RoutineBlockEntity> = emptyList(),
    val studySubjects: List<StudySubjectEntity> = emptyList(),
    val studyTopics: List<StudyTopicEntity> = emptyList(),
    val studySessions: List<StudySessionEntity> = emptyList(),
    val careerItems: List<CareerItemEntity> = emptyList(),
    val exerciseTemplates: List<ExerciseTemplateEntity> = emptyList(),
    val workoutLogs: List<WorkoutLogEntity> = emptyList(),
    val workoutExercises: List<WorkoutExerciseEntity> = emptyList(),
    val healthMetrics: List<HealthMetricEntity> = emptyList(),
    val financeCategories: List<FinanceCategoryEntity> = emptyList(),
    val financeTransactions: List<FinanceTransactionEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalEntity> = emptyList(),
    val lifeGoals: List<LifeGoalEntity> = emptyList(),
    val goalMilestones: List<GoalMilestoneEntity> = emptyList(),
    val journalEntries: List<JournalEntryEntity> = emptyList(),
    val books: List<BookItemEntity> = emptyList(),
    val learningResources: List<LearningResourceEntity> = emptyList(),
    val screenTimeLogs: List<ScreenTimeLogEntity> = emptyList(),
)

@Serializable
data class LifeOsBackupEnvelope(
    val schemaVersion: Int = 1,
    val app: String = "LIFE OS 365",
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val encrypted: Boolean = false,
    val sha256Checksum: String = "",
    val payloadJson: String = "",
)

data class BackupValidationResult(
    val payload: LifeOsBackupPayload,
    val totalRecords: Int,
    val duplicateCount: Int,
)

enum class RestoreMode {
    MERGE_SKIP_DUPLICATES,
    REPLACE_ALL,
}

class BackupManager(
    private val db: LifeOsDatabase,
    private val prefsRepo: UserPreferencesRepository,
) {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun exportToJson(password: String? = null): String {
        val prefs = prefsRepo.preferences.first()
        val payload = LifeOsBackupPayload(
            preferences = prefs,
            habits = db.habitDao().getAllHabitsOnce(),
            habitLogs = db.habitDao().getAllLogsOnce(),
            tasks = db.taskDao().getAllTasksOnce(),
            routineBlocks = db.routineDao().getAllRoutineBlocksOnce(),
            studySubjects = db.studyDao().getAllSubjectsOnce(),
            studyTopics = db.studyDao().getAllTopicsOnce(),
            studySessions = db.studyDao().getAllSessionsOnce(),
            careerItems = db.studyDao().getAllCareerItemsOnce(),
            exerciseTemplates = db.fitnessDao().getAllTemplatesOnce(),
            workoutLogs = db.fitnessDao().getAllWorkoutLogsOnce(),
            workoutExercises = db.fitnessDao().getAllWorkoutExercisesOnce(),
            healthMetrics = db.fitnessDao().getAllHealthMetricsOnce(),
            financeCategories = db.financeDao().getAllCategoriesOnce(),
            financeTransactions = db.financeDao().getAllTransactionsOnce().map {
                it.copy(encryptedNote = CryptoManager.decryptField(it.encryptedNote))
            },
            savingsGoals = db.financeDao().getAllSavingsGoalsOnce(),
            lifeGoals = db.goalsDao().getAllGoalsOnce(),
            goalMilestones = db.goalsDao().getAllMilestonesOnce(),
            journalEntries = db.journalReadingDisciplineDao().getAllJournalEntriesOnce().map {
                it.copy(
                    encryptedReflection = CryptoManager.decryptField(it.encryptedReflection),
                    encryptedGratitude = CryptoManager.decryptField(it.encryptedGratitude),
                    encryptedAchievements = CryptoManager.decryptField(it.encryptedAchievements),
                    encryptedChallenges = CryptoManager.decryptField(it.encryptedChallenges),
                    encryptedLessons = CryptoManager.decryptField(it.encryptedLessons),
                    encryptedNotes = CryptoManager.decryptField(it.encryptedNotes),
                )
            },
            books = db.journalReadingDisciplineDao().getAllBooksOnce(),
            learningResources = db.journalReadingDisciplineDao().getAllResourcesOnce(),
            screenTimeLogs = db.journalReadingDisciplineDao().getAllScreenTimeLogsOnce(),
        )
        val rawPayload = json.encodeToString(payload)
        val checksum = CryptoManager.sha256Hex(rawPayload)
        val isEncrypted = !password.isNullOrBlank()
        val storedPayload = if (isEncrypted) {
            CryptoManager.encryptWithPassword(rawPayload, password!!)
        } else {
            rawPayload
        }
        val envelope = LifeOsBackupEnvelope(
            schemaVersion = 1,
            exportedAtMillis = System.currentTimeMillis(),
            encrypted = isEncrypted,
            sha256Checksum = checksum,
            payloadJson = storedPayload,
        )
        return json.encodeToString(envelope)
    }

    fun isBackupPasswordProtected(rawJson: String): Boolean {
        val envelope = json.decodeFromString<LifeOsBackupEnvelope>(rawJson)
        return envelope.encrypted
    }

    suspend fun validateAndInspectBackup(rawJson: String, password: String? = null): BackupValidationResult {
        val envelope = json.decodeFromString<LifeOsBackupEnvelope>(rawJson)
        require(envelope.app == "LIFE OS 365") { "Unrecognized backup file" }
        require(envelope.schemaVersion in 1..1) { "Unsupported backup schema version ${envelope.schemaVersion}" }

        val decryptedJson = if (envelope.encrypted) {
            require(!password.isNullOrBlank()) { "Password required for encrypted backup" }
            CryptoManager.decryptWithPassword(envelope.payloadJson, password)
        } else {
            envelope.payloadJson
        }

        if (envelope.sha256Checksum.isNotBlank()) {
            val actual = CryptoManager.sha256Hex(decryptedJson)
            require(actual == envelope.sha256Checksum) { "Backup integrity check failed (checksum mismatch)" }
        }

        val payload = json.decodeFromString<LifeOsBackupPayload>(decryptedJson)

        // Duplicate detection against existing database records
        val existingHabitNames = db.habitDao().getAllHabitsOnce().map { it.name.lowercase() }.toSet()
        val existingTaskKeys = db.taskDao().getAllTasksOnce().map { "${it.title.lowercase()}_${it.dueEpochDay}" }.toSet()
        val existingGoalTitles = db.goalsDao().getAllGoalsOnce().map { it.title.lowercase() }.toSet()
        val existingBookTitles = db.journalReadingDisciplineDao().getAllBooksOnce().map { it.title.lowercase() }.toSet()
        val existingJournalKeys = db.journalReadingDisciplineDao().getAllJournalEntriesOnce().map { "${it.epochDay}_${it.title.lowercase()}" }.toSet()

        var duplicates = 0
        duplicates += payload.habits.count { it.name.lowercase() in existingHabitNames }
        duplicates += payload.tasks.count { "${it.title.lowercase()}_${it.dueEpochDay}" in existingTaskKeys }
        duplicates += payload.lifeGoals.count { it.title.lowercase() in existingGoalTitles }
        duplicates += payload.books.count { it.title.lowercase() in existingBookTitles }
        duplicates += payload.journalEntries.count { "${it.epochDay}_${it.title.lowercase()}" in existingJournalKeys }

        val total = payload.habits.size + payload.tasks.size + payload.studySubjects.size +
            payload.workoutLogs.size + payload.financeTransactions.size + payload.lifeGoals.size +
            payload.journalEntries.size + payload.books.size

        return BackupValidationResult(payload = payload, totalRecords = total, duplicateCount = duplicates)
    }

    suspend fun restoreBackup(payload: LifeOsBackupPayload, mode: RestoreMode) {
        db.withTransaction {
            if (mode == RestoreMode.REPLACE_ALL) {
                db.clearAllTables()
            }

            val existingHabits = db.habitDao().getAllHabitsOnce().associateBy { it.name.lowercase() }
            val habitIdMap = mutableMapOf<Long, Long>()
            payload.habits.forEach { habit ->
                val existing = existingHabits[habit.name.lowercase()]
                val newId = if (mode == RestoreMode.MERGE_SKIP_DUPLICATES && existing != null) {
                    existing.id
                } else {
                    db.habitDao().insertHabit(habit.copy(id = 0))
                }
                habitIdMap[habit.id] = newId
            }
            payload.habitLogs.forEach { log ->
                val mappedHabitId = habitIdMap[log.habitId] ?: return@forEach
                db.habitDao().upsertHabitLog(log.copy(id = 0, habitId = mappedHabitId))
            }

            val existingTaskKeys = db.taskDao().getAllTasksOnce().map { "${it.title.lowercase()}_${it.dueEpochDay}" }.toSet()
            payload.tasks.forEach { task ->
                val key = "${task.title.lowercase()}_${task.dueEpochDay}"
                if (mode == RestoreMode.REPLACE_ALL || key !in existingTaskKeys) {
                    db.taskDao().insertTask(task.copy(id = 0))
                }
            }

            val existingRoutineKeys = db.routineDao().getAllRoutineBlocksOnce().map { "${it.startMinuteOfDay}_${it.title.lowercase()}" }.toSet()
            payload.routineBlocks.forEach { block ->
                val key = "${block.startMinuteOfDay}_${block.title.lowercase()}"
                if (mode == RestoreMode.REPLACE_ALL || key !in existingRoutineKeys) {
                    db.routineDao().insertBlock(block.copy(id = 0))
                }
            }

            val existingSubjects = db.studyDao().getAllSubjectsOnce().associateBy { it.name.lowercase() }
            val subjectIdMap = mutableMapOf<Long, Long>()
            payload.studySubjects.forEach { subject ->
                val existing = existingSubjects[subject.name.lowercase()]
                val newId = if (mode == RestoreMode.MERGE_SKIP_DUPLICATES && existing != null) {
                    existing.id
                } else {
                    db.studyDao().insertSubject(subject.copy(id = 0))
                }
                subjectIdMap[subject.id] = newId
            }
            payload.studyTopics.forEach { topic ->
                val mappedId = subjectIdMap[topic.subjectId] ?: return@forEach
                db.studyDao().insertTopic(topic.copy(id = 0, subjectId = mappedId))
            }
            payload.studySessions.forEach { session ->
                val mappedId = subjectIdMap[session.subjectId] ?: return@forEach
                db.studyDao().insertSession(session.copy(id = 0, subjectId = mappedId))
            }
            payload.careerItems.forEach { item ->
                db.studyDao().insertCareerItem(item.copy(id = 0))
            }

            val existingTemplates = db.fitnessDao().getAllTemplatesOnce().map { it.name.lowercase() }.toSet()
            payload.exerciseTemplates.forEach { t ->
                if (mode == RestoreMode.REPLACE_ALL || t.name.lowercase() !in existingTemplates) {
                    db.fitnessDao().insertTemplate(t.copy(id = 0))
                }
            }
            val workoutIdMap = mutableMapOf<Long, Long>()
            payload.workoutLogs.forEach { log ->
                val newId = db.fitnessDao().insertWorkoutLog(log.copy(id = 0))
                workoutIdMap[log.id] = newId
            }
            val mappedExercises = payload.workoutExercises.mapNotNull { ex ->
                val newWorkoutId = workoutIdMap[ex.workoutId] ?: return@mapNotNull null
                ex.copy(id = 0, workoutId = newWorkoutId)
            }
            if (mappedExercises.isNotEmpty()) {
                db.fitnessDao().insertWorkoutExercises(mappedExercises)
            }
            payload.healthMetrics.forEach { metric ->
                db.fitnessDao().upsertHealthMetric(metric)
            }

            val existingCategories = db.financeDao().getAllCategoriesOnce().associateBy { "${it.type}_${it.name.lowercase()}" }
            val categoryIdMap = mutableMapOf<Long, Long>()
            payload.financeCategories.forEach { cat ->
                val existing = existingCategories["${cat.type}_${cat.name.lowercase()}"]
                val newId = if (mode == RestoreMode.MERGE_SKIP_DUPLICATES && existing != null) {
                    existing.id
                } else {
                    db.financeDao().insertCategory(cat.copy(id = 0))
                }
                categoryIdMap[cat.id] = newId
            }
            payload.financeTransactions.forEach { tx ->
                val mappedCat = categoryIdMap[tx.categoryId] ?: return@forEach
                db.financeDao().insertTransaction(
                    tx.copy(
                        id = 0,
                        categoryId = mappedCat,
                        encryptedNote = CryptoManager.encryptField(tx.encryptedNote),
                    ),
                )
            }
            payload.savingsGoals.forEach { goal ->
                db.financeDao().insertSavingsGoal(goal.copy(id = 0))
            }

            val existingGoals = db.goalsDao().getAllGoalsOnce().associateBy { it.title.lowercase() }
            val goalIdMap = mutableMapOf<Long, Long>()
            payload.lifeGoals.forEach { goal ->
                val existing = existingGoals[goal.title.lowercase()]
                val newId = if (mode == RestoreMode.MERGE_SKIP_DUPLICATES && existing != null) {
                    existing.id
                } else {
                    db.goalsDao().insertGoal(goal.copy(id = 0))
                }
                goalIdMap[goal.id] = newId
            }
            payload.goalMilestones.forEach { m ->
                val mappedId = goalIdMap[m.goalId] ?: return@forEach
                db.goalsDao().insertMilestone(m.copy(id = 0, goalId = mappedId))
            }

            val existingJournalKeys = db.journalReadingDisciplineDao().getAllJournalEntriesOnce()
                .map { "${it.epochDay}_${it.title.lowercase()}" }.toSet()
            payload.journalEntries.forEach { entry ->
                val key = "${entry.epochDay}_${entry.title.lowercase()}"
                if (mode == RestoreMode.REPLACE_ALL || key !in existingJournalKeys) {
                    db.journalReadingDisciplineDao().insertJournalEntry(
                        entry.copy(
                            id = 0,
                            encryptedReflection = CryptoManager.encryptField(entry.encryptedReflection),
                            encryptedGratitude = CryptoManager.encryptField(entry.encryptedGratitude),
                            encryptedAchievements = CryptoManager.encryptField(entry.encryptedAchievements),
                            encryptedChallenges = CryptoManager.encryptField(entry.encryptedChallenges),
                            encryptedLessons = CryptoManager.encryptField(entry.encryptedLessons),
                            encryptedNotes = CryptoManager.encryptField(entry.encryptedNotes),
                        ),
                    )
                }
            }

            val existingBookTitles = db.journalReadingDisciplineDao().getAllBooksOnce().map { it.title.lowercase() }.toSet()
            payload.books.forEach { book ->
                if (mode == RestoreMode.REPLACE_ALL || book.title.lowercase() !in existingBookTitles) {
                    db.journalReadingDisciplineDao().insertBook(book.copy(id = 0))
                }
            }
            payload.learningResources.forEach { res ->
                db.journalReadingDisciplineDao().insertResource(res.copy(id = 0))
            }
            payload.screenTimeLogs.forEach { log ->
                db.journalReadingDisciplineDao().upsertScreenTimeLog(log)
            }
        }
        prefsRepo.restoreFromPreferences(payload.preferences)
    }

    suspend fun exportFinanceCsv(): String {
        val categories = db.financeDao().getAllCategoriesOnce().associateBy { it.id }
        val txs = db.financeDao().getAllTransactionsOnce()
        val sb = StringBuilder()
        sb.appendLine("Date,Title,Type,Category,Amount,Note")
        txs.forEach { tx ->
            val date = DateUtils.formatShortDate(tx.epochDay)
            val cat = categories[tx.categoryId]?.name ?: "Uncategorized"
            val note = CryptoManager.decryptField(tx.encryptedNote)
            sb.appendLine(
                listOf(date, tx.title, tx.type, cat, tx.amount.toString(), note)
                    .joinToString(",") { csvEscape(it) },
            )
        }
        return sb.toString()
    }

    suspend fun exportHabitsAndStudyCsv(): String {
        val habits = db.habitDao().getAllHabitsOnce().associateBy { it.id }
        val habitLogs = db.habitDao().getAllLogsOnce()
        val subjects = db.studyDao().getAllSubjectsOnce().associateBy { it.id }
        val sessions = db.studyDao().getAllSessionsOnce()
        val sb = StringBuilder()
        sb.appendLine("RecordType,Date,NameOrSubject,CategoryOrMode,StatusOrDurationMinutes,Notes")
        habitLogs.forEach { log ->
            val habit = habits[log.habitId] ?: return@forEach
            sb.appendLine(
                listOf("HABIT", DateUtils.formatShortDate(log.epochDay), habit.name, habit.category, log.status, log.note)
                    .joinToString(",") { csvEscape(it) },
            )
        }
        sessions.forEach { s ->
            val subject = subjects[s.subjectId]?.name ?: "Study"
            sb.appendLine(
                listOf("STUDY", DateUtils.formatShortDate(s.epochDay), subject, s.mode, s.durationMinutes.toString(), s.notes)
                    .joinToString(",") { csvEscape(it) },
            )
        }
        return sb.toString()
    }

    private fun csvEscape(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }
}
