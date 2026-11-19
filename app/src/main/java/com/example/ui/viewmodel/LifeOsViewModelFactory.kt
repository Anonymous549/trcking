package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.LifeOsApplication

class LifeOsViewModelFactory(private val app: LifeOsApplication) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(AppViewModel::class.java) ->
                AppViewModel(app.preferencesRepository) as T
            modelClass.isAssignableFrom(DashboardViewModel::class.java) ->
                DashboardViewModel(app.repository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(HabitsViewModel::class.java) ->
                HabitsViewModel(app.repository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(FocusViewModel::class.java) ->
                FocusViewModel(app.repository) as T
            modelClass.isAssignableFrom(AnalyticsViewModel::class.java) ->
                AnalyticsViewModel(app.repository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(FitnessViewModel::class.java) ->
                FitnessViewModel(app.repository) as T
            modelClass.isAssignableFrom(FinanceViewModel::class.java) ->
                FinanceViewModel(app.repository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(GoalsViewModel::class.java) ->
                GoalsViewModel(app.repository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(JournalViewModel::class.java) ->
                JournalViewModel(app.repository) as T
            modelClass.isAssignableFrom(ReadingViewModel::class.java) ->
                ReadingViewModel(app.repository, app.preferencesRepository) as T
            modelClass.isAssignableFrom(DisciplineViewModel::class.java) ->
                DisciplineViewModel(app.repository, app.usageStatsHelper) as T
            modelClass.isAssignableFrom(SettingsViewModel::class.java) ->
                SettingsViewModel(app.repository, app.preferencesRepository, app.backupManager, app.reminderScheduler) as T
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
