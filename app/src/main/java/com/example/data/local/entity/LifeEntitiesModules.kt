package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "exercise_templates",
    indices = [Index("muscleGroup")],
)
data class ExerciseTemplateEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val muscleGroup: String,
    val equipment: String = "Bodyweight",
    val isCustom: Boolean = false,
)

@Serializable
@Entity(
    tableName = "workout_logs",
    indices = [Index("epochDay")],
)
data class WorkoutLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val routineName: String = "",
    val durationMinutes: Int,
    val epochDay: Long,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "workout_exercises",
    foreignKeys = [
        ForeignKey(
            entity = WorkoutLogEntity::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workoutId")],
)
data class WorkoutExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val exerciseName: String,
    val sets: Int,
    val reps: Int,
    val weightKg: Float = 0f,
)

@Serializable
@Entity(tableName = "health_metrics")
data class HealthMetricEntity(
    @PrimaryKey val epochDay: Long,
    val steps: Int = 0,
    val weightKg: Float? = null,
    val waistCm: Float? = null,
    val chestCm: Float? = null,
    val waterMl: Int = 0,
    val calories: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatsG: Int = 0,
    val sleepHours: Float = 0f,
    val nutritionNote: String = "",
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "finance_categories",
    indices = [Index("type")],
)
data class FinanceCategoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** INCOME or EXPENSE */
    val type: String = "EXPENSE",
    val monthlyBudget: Double = 0.0,
    val isCustom: Boolean = false,
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "finance_transactions",
    foreignKeys = [
        ForeignKey(
            entity = FinanceCategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId"), Index("epochDay"), Index("type")],
)
data class FinanceTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val title: String,
    val amount: Double,
    /** INCOME or EXPENSE */
    val type: String,
    /** Encrypted via Android Keystore AES-GCM */
    val encryptedNote: String = "",
    val epochDay: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetEpochDay: Long? = null,
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "life_goals",
    indices = [Index("horizon"), Index("isCompleted")],
)
data class LifeGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** DAILY, WEEKLY, MONTHLY, YEARLY, SHORT_TERM, LONG_TERM */
    val horizon: String = "MONTHLY",
    val category: String = "Personal",
    /** 1 = High, 2 = Medium, 3 = Low */
    val priority: Int = 2,
    val targetEpochDay: Long? = null,
    val progressPercent: Int = 0,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val notes: String = "",
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "goal_milestones",
    foreignKeys = [
        ForeignKey(
            entity = LifeGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("goalId")],
)
data class GoalMilestoneEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val title: String,
    val isDone: Boolean = false,
)

@Serializable
@Entity(
    tableName = "journal_entries",
    indices = [Index("epochDay")],
)
data class JournalEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val title: String,
    /** 1..5 */
    val moodScore: Int = 4,
    /** AES-GCM encrypted fields */
    val encryptedReflection: String = "",
    val encryptedGratitude: String = "",
    val encryptedAchievements: String = "",
    val encryptedChallenges: String = "",
    val encryptedLessons: String = "",
    val encryptedNotes: String = "",
    val tags: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "books",
    indices = [Index("status")],
)
data class BookItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String,
    val totalPages: Int,
    val pagesRead: Int = 0,
    /** READING, COMPLETED, WANT_TO_READ */
    val status: String = "READING",
    val notes: String = "",
    val updatedAt: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "learning_resources",
    indices = [Index("resourceType")],
)
data class LearningResourceEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** ARTICLE, COURSE, VIDEO, NOTE */
    val resourceType: String = "ARTICLE",
    val topic: String = "General",
    val notes: String = "",
    val isCompleted: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(tableName = "screen_time_logs")
data class ScreenTimeLogEntity(
    @PrimaryKey val epochDay: Long,
    val totalMinutes: Int = 0,
    val socialMediaMinutes: Int = 0,
    val distractionCount: Int = 0,
    val dailyGoalMinutes: Int = 150,
    /** MANUAL or USAGE_STATS */
    val source: String = "MANUAL",
    val topAppsSummary: String = "",
    val isSampleData: Boolean = false,
)
