package com.example.data.sample

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
import com.example.data.security.CryptoManager
import com.example.domain.util.DateUtils
import kotlinx.coroutines.flow.first

object SampleDataSeeder {

    fun starterRoutineBlocks(markAsSample: Boolean = false): List<RoutineBlockEntity> = listOf(
        RoutineBlockEntity(startMinuteOfDay = 8 * 60, title = "Wake up", category = "Wellness", sortOrder = 0, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 8 * 60 + 30, title = "Morning routine & breakfast", category = "Wellness", sortOrder = 1, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 9 * 60, title = "Focused study", category = "Study", sortOrder = 2, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 11 * 60, title = "Short break", category = "Rest", sortOrder = 3, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 11 * 60 + 30, title = "Study session", category = "Study", sortOrder = 4, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 13 * 60, title = "Lunch", category = "Wellness", sortOrder = 5, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 14 * 60, title = "Classes or assignments", category = "Study", sortOrder = 6, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 16 * 60, title = "Skill development", category = "Career", sortOrder = 7, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 17 * 60, title = "Workout", category = "Fitness", sortOrder = 8, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 18 * 60, title = "Refresh and relax", category = "Rest", sortOrder = 9, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 19 * 60, title = "Revision", category = "Study", sortOrder = 10, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 20 * 60 + 30, title = "Dinner", category = "Wellness", sortOrder = 11, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 21 * 60 + 30, title = "Reading", category = "Knowledge", sortOrder = 12, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 22 * 60, title = "Daily progress review", category = "Discipline", sortOrder = 13, isSampleData = markAsSample),
        RoutineBlockEntity(startMinuteOfDay = 23 * 60 + 30, title = "Sleep", category = "Wellness", sortOrder = 14, isSampleData = markAsSample),
    )

    fun defaultExerciseTemplates(): List<ExerciseTemplateEntity> = listOf(
        ExerciseTemplateEntity(name = "Push-ups", muscleGroup = "Chest", equipment = "Bodyweight"),
        ExerciseTemplateEntity(name = "Barbell Squat", muscleGroup = "Legs", equipment = "Barbell"),
        ExerciseTemplateEntity(name = "Pull-ups", muscleGroup = "Back", equipment = "Pull-up Bar"),
        ExerciseTemplateEntity(name = "Dumbbell Shoulder Press", muscleGroup = "Shoulders", equipment = "Dumbbells"),
        ExerciseTemplateEntity(name = "Romanian Deadlift", muscleGroup = "Legs", equipment = "Barbell"),
        ExerciseTemplateEntity(name = "Plank Hold (sec)", muscleGroup = "Core", equipment = "Bodyweight"),
        ExerciseTemplateEntity(name = "Dumbbell Row", muscleGroup = "Back", equipment = "Dumbbells"),
        ExerciseTemplateEntity(name = "Brisk Walk / Run (min)", muscleGroup = "Cardio", equipment = "Outdoor"),
        ExerciseTemplateEntity(name = "Surya Namaskar", muscleGroup = "Mobility", equipment = "Mat"),
    )

    fun defaultFinanceCategories(): List<FinanceCategoryEntity> = listOf(
        FinanceCategoryEntity(name = "Salary / Stipend", type = "INCOME", monthlyBudget = 0.0),
        FinanceCategoryEntity(name = "Freelance / Bonus", type = "INCOME", monthlyBudget = 0.0),
        FinanceCategoryEntity(name = "Food & Groceries", type = "EXPENSE", monthlyBudget = 6000.0),
        FinanceCategoryEntity(name = "Rent & Utilities", type = "EXPENSE", monthlyBudget = 12000.0),
        FinanceCategoryEntity(name = "Books & Education", type = "EXPENSE", monthlyBudget = 2500.0),
        FinanceCategoryEntity(name = "Transport", type = "EXPENSE", monthlyBudget = 2000.0),
        FinanceCategoryEntity(name = "Fitness & Wellness", type = "EXPENSE", monthlyBudget = 2000.0),
        FinanceCategoryEntity(name = "Personal & Leisure", type = "EXPENSE", monthlyBudget = 3000.0),
    )

    suspend fun seedInitialIfNeeded(db: LifeOsDatabase, prefsRepo: UserPreferencesRepository) {
        val prefs = prefsRepo.preferences.first()
        if (prefs.hasSeededInitialData) return

        val today = DateUtils.todayEpochDay()

        // Base exercise library & finance categories (always helpful even after clearing sample logs)
        if (db.fitnessDao().getAllTemplatesOnce().isEmpty()) {
            db.fitnessDao().insertTemplates(defaultExerciseTemplates())
        }
        val catIds = if (db.financeDao().getAllCategoriesOnce().isEmpty()) {
            defaultFinanceCategories().map { db.financeDao().insertCategory(it) }
        } else {
            db.financeDao().getAllCategoriesOnce().map { it.id }
        }

        // 1. Starter Habits (the 8 example habits)
        val habitIds = listOf(
            HabitEntity(name = "Wake up on time", category = "Discipline", reminderHour = 8, reminderMinute = 0, isSampleData = true),
            HabitEntity(name = "Exercise", category = "Fitness", reminderHour = 17, reminderMinute = 0, isSampleData = true),
            HabitEntity(name = "Read", category = "Knowledge", reminderHour = 21, reminderMinute = 30, isSampleData = true),
            HabitEntity(name = "Study", category = "Study", reminderHour = 9, reminderMinute = 0, isSampleData = true),
            HabitEntity(name = "Drink water", category = "Wellness", isSampleData = true),
            HabitEntity(name = "Limit social media", category = "Digital", isSampleData = true),
            HabitEntity(name = "Journal", category = "Mindfulness", reminderHour = 22, reminderMinute = 0, isSampleData = true),
            HabitEntity(name = "Sleep on time", category = "Wellness", reminderHour = 23, reminderMinute = 15, isSampleData = true),
        ).map { db.habitDao().insertHabit(it) }

        // Seed 18 days of realistic habit logs (including rest-day skips so no punishment!)
        for (offset in 1..18) {
            val day = today - offset
            habitIds.forEachIndexed { idx, habitId ->
                val status = when {
                    offset % 7 == 0 && idx == 1 -> "SKIPPED_REST"
                    (offset + idx) % 6 == 0 -> "MISSED"
                    else -> "COMPLETED"
                }
                db.habitDao().upsertHabitLog(HabitLogEntity(habitId = habitId, epochDay = day, status = status))
            }
        }
        // Mark first 3 completed today so dashboard opens with lively progress
        habitIds.take(3).forEach { habitId ->
            db.habitDao().upsertHabitLog(HabitLogEntity(habitId = habitId, epochDay = today, status = "COMPLETED"))
        }

        // 2. Starter Routine (8:00 AM - 23:30)
        db.routineDao().insertBlocks(starterRoutineBlocks(markAsSample = true))

        // 3. Tasks & Top 3 Priorities
        val taskDao = db.taskDao()
        taskDao.insertTask(TaskEntity(title = "Complete Algorithms problem set", notes = "Dynamic programming & graphs", priority = 1, dueEpochDay = today, isTopThree = true, isSampleData = true))
        taskDao.insertTask(TaskEntity(title = "45-min evening strength workout", priority = 1, dueEpochDay = today, isTopThree = true, isSampleData = true))
        taskDao.insertTask(TaskEntity(title = "Review monthly budget & log expenses", priority = 2, dueEpochDay = today, isTopThree = true, isSampleData = true))
        taskDao.insertTask(TaskEntity(title = "Read 20 pages of Deep Work", priority = 2, dueEpochDay = today + 1, recurrence = "DAILY", isSampleData = true))
        taskDao.insertTask(TaskEntity(title = "Prepare portfolio project README", priority = 3, dueEpochDay = today + 3, isSampleData = true))

        // 4. Study Subjects, Topics, Sessions & Career
        val studyDao = db.studyDao()
        val s1 = studyDao.insertSubject(StudySubjectEntity(name = "System Design & Architecture", category = "Engineering", targetWeeklyHours = 8f, examEpochDay = today + 24, examTitle = "Technical Assessment", isSampleData = true))
        val s2 = studyDao.insertSubject(StudySubjectEntity(name = "Applied Statistics", category = "Academic", targetWeeklyHours = 5f, examEpochDay = today + 12, examTitle = "Midterm Exam", isSampleData = true))
        studyDao.insertTopic(StudyTopicEntity(subjectId = s1, title = "Offline-First Caching & Room", isMastered = true, revisionCount = 2, nextRevisionEpochDay = today + 5))
        studyDao.insertTopic(StudyTopicEntity(subjectId = s1, title = "Event-Driven Queues", isMastered = false, revisionCount = 1, nextRevisionEpochDay = today))
        studyDao.insertTopic(StudyTopicEntity(subjectId = s2, title = "Bayesian Inference", isMastered = false, revisionCount = 1, nextRevisionEpochDay = today + 1))

        for (offset in 0..5) {
            studyDao.insertSession(
                StudySessionEntity(
                    subjectId = if (offset % 2 == 0) s1 else s2,
                    durationMinutes = 50 + (offset % 3) * 25,
                    mode = if (offset % 2 == 0) "POMODORO" else "DEEP_WORK",
                    notes = "Focused study block",
                    epochDay = today - offset,
                    isSampleData = true,
                ),
            )
        }
        studyDao.insertCareerItem(CareerItemEntity(title = "Kotlin Multiplatform & Compose Mastery", itemType = "SKILL", progressPercent = 72, notes = "Building production offline-first apps", isSampleData = true))
        studyDao.insertCareerItem(CareerItemEntity(title = "Open-Source Portfolio Release", itemType = "PROJECT", progressPercent = 60, dueEpochDay = today + 15, isSampleData = true))
        studyDao.insertCareerItem(CareerItemEntity(title = "Statistics Lab Assignment #4", itemType = "ASSIGNMENT", progressPercent = 40, dueEpochDay = today + 4, isSampleData = true))

        // 5. Fitness & Health
        val fitnessDao = db.fitnessDao()
        val w1 = fitnessDao.insertWorkoutLog(WorkoutLogEntity(title = "Upper Body Strength", routineName = "Push/Pull", durationMinutes = 48, epochDay = today - 1, notes = "Controlled tempo", isSampleData = true))
        fitnessDao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(workoutId = w1, exerciseName = "Push-ups", sets = 4, reps = 15, weightKg = 0f),
                WorkoutExerciseEntity(workoutId = w1, exerciseName = "Dumbbell Row", sets = 4, reps = 12, weightKg = 14f),
                WorkoutExerciseEntity(workoutId = w1, exerciseName = "Dumbbell Shoulder Press", sets = 3, reps = 10, weightKg = 10f),
            ),
        )
        val w2 = fitnessDao.insertWorkoutLog(WorkoutLogEntity(title = "Lower Body & Mobility", routineName = "Legs", durationMinutes = 42, epochDay = today - 3, isSampleData = true))
        fitnessDao.insertWorkoutExercises(
            listOf(
                WorkoutExerciseEntity(workoutId = w2, exerciseName = "Barbell Squat", sets = 4, reps = 10, weightKg = 45f),
                WorkoutExerciseEntity(workoutId = w2, exerciseName = "Plank Hold (sec)", sets = 3, reps = 60, weightKg = 0f),
            ),
        )
        for (offset in 0..6) {
            fitnessDao.upsertHealthMetric(
                HealthMetricEntity(
                    epochDay = today - offset,
                    steps = 6800 + (offset % 4) * 950,
                    weightKg = 68.8f - (6 - offset) * 0.1f,
                    waistCm = 79.5f,
                    waterMl = 2000 + (offset % 3) * 250,
                    calories = 2150,
                    proteinG = 112,
                    carbsG = 240,
                    fatsG = 68,
                    sleepHours = 7.4f + (offset % 2) * 0.4f,
                    isSampleData = true,
                ),
            )
        }

        // 6. Finance (INR ₹ default)
        val financeDao = db.financeDao()
        if (catIds.size >= 8) {
            financeDao.insertTransaction(FinanceTransactionEntity(categoryId = catIds[0], title = "Monthly Stipend / Income", amount = 45000.0, type = "INCOME", encryptedNote = CryptoManager.encryptField("Primary monthly inflow"), epochDay = today - 10, isSampleData = true))
            financeDao.insertTransaction(FinanceTransactionEntity(categoryId = catIds[3], title = "Apartment Rent", amount = 11500.0, type = "EXPENSE", encryptedNote = CryptoManager.encryptField("Monthly rent"), epochDay = today - 8, isSampleData = true))
            financeDao.insertTransaction(FinanceTransactionEntity(categoryId = catIds[2], title = "Fresh Groceries & Produce", amount = 2450.0, type = "EXPENSE", encryptedNote = CryptoManager.encryptField("Weekly market run"), epochDay = today - 4, isSampleData = true))
            financeDao.insertTransaction(FinanceTransactionEntity(categoryId = catIds[4], title = "Technical Reference Books", amount = 1350.0, type = "EXPENSE", encryptedNote = CryptoManager.encryptField("Designing Data-Intensive Apps"), epochDay = today - 2, isSampleData = true))
            financeDao.insertTransaction(FinanceTransactionEntity(categoryId = catIds[5], title = "Metro Pass Renewal", amount = 850.0, type = "EXPENSE", epochDay = today - 1, isSampleData = true))
        }
        financeDao.insertSavingsGoal(SavingsGoalEntity(title = "Emergency Reserve (6 Months)", targetAmount = 150000.0, currentAmount = 64000.0, targetEpochDay = today + 180, isSampleData = true))
        financeDao.insertSavingsGoal(SavingsGoalEntity(title = "Workstation Upgrade", targetAmount = 40000.0, currentAmount = 18500.0, targetEpochDay = today + 90, isSampleData = true))

        // 7. Goals & Milestones
        val goalsDao = db.goalsDao()
        val g1 = goalsDao.insertGoal(LifeGoalEntity(title = "Complete 30-Day Consistency Sprint", horizon = "MONTHLY", category = "Discipline", priority = 1, targetEpochDay = today + 12, progressPercent = 67, isSampleData = true))
        goalsDao.insertMilestone(GoalMilestoneEntity(goalId = g1, title = "Set up 8:00 AM routine", isDone = true))
        goalsDao.insertMilestone(GoalMilestoneEntity(goalId = g1, title = "Complete 14 consecutive check-ins", isDone = true))
        goalsDao.insertMilestone(GoalMilestoneEntity(goalId = g1, title = "Review weekly analytics every Sunday", isDone = false))
        goalsDao.insertGoal(LifeGoalEntity(title = "Build 100% Offline Knowledge Vault", horizon = "YEARLY", category = "Career", priority = 1, targetEpochDay = today + 120, progressPercent = 45, isSampleData = true))
        goalsDao.insertGoal(LifeGoalEntity(title = "Finish today's top 3 priorities before 7 PM", horizon = "DAILY", category = "Productivity", priority = 2, targetEpochDay = today, progressPercent = 33, isSampleData = true))

        // 8. Private Journal, Reading & Digital Discipline
        val jrdDao = db.journalReadingDisciplineDao()
        jrdDao.insertJournalEntry(
            JournalEntryEntity(
                epochDay = today - 1,
                title = "Steady progress on deep work blocks",
                moodScore = 4,
                encryptedReflection = CryptoManager.encryptField("Kept phone in another room during the 9 AM study block and felt noticeably calmer."),
                encryptedGratitude = CryptoManager.encryptField("Grateful for quiet morning sunlight and supportive friends."),
                encryptedAchievements = CryptoManager.encryptField("Finished upper body workout and 2 Pomodoro blocks."),
                encryptedChallenges = CryptoManager.encryptField("Felt tired around 4 PM; a short walk helped."),
                encryptedLessons = CryptoManager.encryptField("Protecting the first 2 hours of the day sets the tone for everything else."),
                tags = "focus,calm,study",
                isSampleData = true,
            ),
        )
        jrdDao.insertBook(BookItemEntity(title = "Deep Work", author = "Cal Newport", totalPages = 296, pagesRead = 184, status = "READING", notes = "Chapter on scheduling every minute of your day.", isSampleData = true))
        jrdDao.insertBook(BookItemEntity(title = "Atomic Habits", author = "James Clear", totalPages = 320, pagesRead = 320, status = "COMPLETED", notes = "1% better every day; identity-based habits.", isSampleData = true))
        jrdDao.insertResource(LearningResourceEntity(title = "Android Offline-First Architecture Guide", resourceType = "ARTICLE", topic = "Android", notes = "Single source of truth with Room + Flow", isCompleted = true, isSampleData = true))

        for (offset in 0..6) {
            jrdDao.upsertScreenTimeLog(
                ScreenTimeLogEntity(
                    epochDay = today - offset,
                    totalMinutes = 125 + (offset % 3) * 20,
                    socialMediaMinutes = 25 + (offset % 4) * 10,
                    distractionCount = 3 + (offset % 3),
                    dailyGoalMinutes = 150,
                    source = "MANUAL",
                    isSampleData = true,
                ),
            )
        }

        prefsRepo.setSeededState(seeded = true, demoActive = true)
    }

    suspend fun clearSampleData(db: LifeOsDatabase, prefsRepo: UserPreferencesRepository) {
        db.habitDao().deleteSampleHabits()
        db.taskDao().deleteSampleTasks()
        db.routineDao().deleteSampleRoutine()
        db.studyDao().deleteSampleSubjects()
        db.studyDao().deleteSampleCareerItems()
        db.fitnessDao().deleteSampleWorkouts()
        db.fitnessDao().deleteSampleHealthMetrics()
        db.financeDao().deleteSampleTransactions()
        db.financeDao().deleteSampleSavingsGoals()
        db.goalsDao().deleteSampleGoals()
        db.journalReadingDisciplineDao().deleteSampleJournal()
        db.journalReadingDisciplineDao().deleteSampleBooks()
        db.journalReadingDisciplineDao().deleteSampleResources()
        db.journalReadingDisciplineDao().deleteSampleScreenTime()
        prefsRepo.setSeededState(seeded = true, demoActive = false)
    }
}
