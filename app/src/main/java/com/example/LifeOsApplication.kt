package com.example

import android.app.Application
import com.example.data.local.LifeOsDatabase
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.LifeOsRepository
import com.example.data.sample.SampleDataSeeder
import com.example.domain.backup.BackupManager
import com.example.platform.ReminderScheduler
import com.example.platform.UsageStatsHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class LifeOsApplication : Application() {
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: LifeOsDatabase by lazy { LifeOsDatabase.getInstance(this) }
    val preferencesRepository: UserPreferencesRepository by lazy { UserPreferencesRepository(this) }
    val repository: LifeOsRepository by lazy { LifeOsRepository(database, preferencesRepository) }
    val backupManager: BackupManager by lazy { BackupManager(database, preferencesRepository) }
    val reminderScheduler: ReminderScheduler by lazy { ReminderScheduler(this) }
    val usageStatsHelper: UsageStatsHelper by lazy { UsageStatsHelper(this) }

    override fun onCreate() {
        super.onCreate()
        applicationScope.launch {
            SampleDataSeeder.seedInitialIfNeeded(database, preferencesRepository)
        }
    }
}
