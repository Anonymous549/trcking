package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.FinanceDao
import com.example.data.local.dao.FitnessDao
import com.example.data.local.dao.GoalsDao
import com.example.data.local.dao.HabitDao
import com.example.data.local.dao.JournalReadingDisciplineDao
import com.example.data.local.dao.RoutineDao
import com.example.data.local.dao.StudyDao
import com.example.data.local.dao.TaskDao
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

@Database(
    entities = [
        HabitEntity::class,
        HabitLogEntity::class,
        TaskEntity::class,
        RoutineBlockEntity::class,
        StudySubjectEntity::class,
        StudyTopicEntity::class,
        StudySessionEntity::class,
        CareerItemEntity::class,
        ExerciseTemplateEntity::class,
        WorkoutLogEntity::class,
        WorkoutExerciseEntity::class,
        HealthMetricEntity::class,
        FinanceCategoryEntity::class,
        FinanceTransactionEntity::class,
        SavingsGoalEntity::class,
        LifeGoalEntity::class,
        GoalMilestoneEntity::class,
        JournalEntryEntity::class,
        BookItemEntity::class,
        LearningResourceEntity::class,
        ScreenTimeLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class LifeOsDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun taskDao(): TaskDao
    abstract fun routineDao(): RoutineDao
    abstract fun studyDao(): StudyDao
    abstract fun fitnessDao(): FitnessDao
    abstract fun financeDao(): FinanceDao
    abstract fun goalsDao(): GoalsDao
    abstract fun journalReadingDisciplineDao(): JournalReadingDisciplineDao

    companion object {
        @Volatile
        private var INSTANCE: LifeOsDatabase? = null

        fun getInstance(context: Context): LifeOsDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    LifeOsDatabase::class.java,
                    "life_os_365.db",
                ).build().also { INSTANCE = it }
            }
        }
    }
}
