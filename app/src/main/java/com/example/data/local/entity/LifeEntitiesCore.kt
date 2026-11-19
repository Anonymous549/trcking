package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(
    tableName = "habits",
    indices = [Index("category"), Index("isArchived")],
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Discipline",
    /** DAILY, WEEKLY, CUSTOM */
    val scheduleType: String = "DAILY",
    /** Comma-separated ISO days 1..7 (e.g. "1,2,3,4,5") */
    val targetDaysCsv: String = "1,2,3,4,5,6,7",
    val weeklyTargetCount: Int = 7,
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val allowRestDayExemption: Boolean = true,
    val isSampleData: Boolean = false,
    val isArchived: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("habitId"), Index("epochDay"), Index(value = ["habitId", "epochDay"], unique = true)],
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: Long,
    val epochDay: Long,
    /** COMPLETED, SKIPPED_REST, MISSED */
    val status: String = "COMPLETED",
    val note: String = "",
    val loggedAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "tasks",
    indices = [Index("dueEpochDay"), Index("isCompleted"), Index("isTopThree")],
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val notes: String = "",
    /** 1 = High, 2 = Medium, 3 = Low */
    val priority: Int = 2,
    val dueEpochDay: Long? = null,
    /** NONE, DAILY, WEEKLY, MONTHLY */
    val recurrence: String = "NONE",
    val reminderHour: Int? = null,
    val reminderMinute: Int? = null,
    val isTopThree: Boolean = false,
    val isCompleted: Boolean = false,
    val completedAt: Long? = null,
    val focusMinutes: Int = 0,
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "routine_blocks",
    indices = [Index("startMinuteOfDay"), Index("sortOrder")],
)
data class RoutineBlockEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val startMinuteOfDay: Int,
    val title: String,
    val category: String = "Routine",
    val lastCompletedEpochDay: Long? = null,
    val sortOrder: Int = 0,
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "study_subjects",
    indices = [Index("name")],
)
data class StudySubjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String = "Academic",
    val targetWeeklyHours: Float = 6f,
    val examEpochDay: Long? = null,
    val examTitle: String = "",
    val isSampleData: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
@Entity(
    tableName = "study_topics",
    foreignKeys = [
        ForeignKey(
            entity = StudySubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId"), Index("nextRevisionEpochDay")],
)
data class StudyTopicEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val title: String,
    val isMastered: Boolean = false,
    val revisionCount: Int = 0,
    val nextRevisionEpochDay: Long? = null,
)

@Serializable
@Entity(
    tableName = "study_sessions",
    foreignKeys = [
        ForeignKey(
            entity = StudySubjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["subjectId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("subjectId"), Index("epochDay")],
)
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subjectId: Long,
    val durationMinutes: Int,
    /** POMODORO, DEEP_WORK, REVISION */
    val mode: String = "POMODORO",
    val notes: String = "",
    val epochDay: Long,
    val timestamp: Long = System.currentTimeMillis(),
    val isSampleData: Boolean = false,
)

@Serializable
@Entity(
    tableName = "career_items",
    indices = [Index("itemType"), Index("isCompleted")],
)
data class CareerItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    /** SKILL, CAREER_GOAL, PROJECT, ASSIGNMENT */
    val itemType: String = "SKILL",
    val progressPercent: Int = 0,
    val dueEpochDay: Long? = null,
    val notes: String = "",
    val isCompleted: Boolean = false,
    val isSampleData: Boolean = false,
)
