package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BookItemEntity
import com.example.data.local.entity.LearningResourceEntity
import com.example.data.local.entity.ScreenTimeLogEntity
import com.example.data.preferences.CurrencyOption
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.DecryptedJournalEntry
import com.example.data.repository.LifeOsRepository
import com.example.domain.backup.BackupManager
import com.example.domain.backup.BackupValidationResult
import com.example.domain.backup.RestoreMode
import com.example.domain.util.DateUtils
import com.example.platform.ReminderScheduler
import com.example.platform.UsageStatsHelper
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class JournalUiState(
    val entries: List<DecryptedJournalEntry> = emptyList(),
    val searchQuery: String = "",
)

class JournalViewModel(
    private val repo: LifeOsRepository,
) : ViewModel() {
    private val query = MutableStateFlow("")

    val uiState: StateFlow<JournalUiState> = combine(
        repo.journalEntries,
        query,
    ) { entries, q ->
        val filtered = if (q.isBlank()) {
            entries
        } else {
            val needle = q.trim().lowercase()
            entries.filter {
                it.title.lowercase().contains(needle) ||
                    it.reflection.lowercase().contains(needle) ||
                    it.gratitude.lowercase().contains(needle) ||
                    it.achievements.lowercase().contains(needle) ||
                    it.challenges.lowercase().contains(needle) ||
                    it.lessons.lowercase().contains(needle) ||
                    it.notes.lowercase().contains(needle) ||
                    it.tags.lowercase().contains(needle)
            }
        }
        JournalUiState(entries = filtered, searchQuery = q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), JournalUiState())

    fun setSearchQuery(q: String) {
        query.value = q
    }

    fun saveEntry(entry: DecryptedJournalEntry) {
        if (entry.title.isBlank() && entry.reflection.isBlank() && entry.gratitude.isBlank()) return
        viewModelScope.launch { repo.saveJournalEntry(entry) }
    }

    fun deleteEntry(id: Long) {
        viewModelScope.launch { repo.deleteJournalEntry(id) }
    }
}

data class ReadingUiState(
    val books: List<BookItemEntity> = emptyList(),
    val resources: List<LearningResourceEntity> = emptyList(),
    val dailyReadingGoalPages: Int = 20,
)

class ReadingViewModel(
    private val repo: LifeOsRepository,
    prefsRepo: UserPreferencesRepository,
) : ViewModel() {
    val uiState: StateFlow<ReadingUiState> = combine(
        repo.books,
        repo.learningResources,
        prefsRepo.preferences,
    ) { books, resources, prefs ->
        ReadingUiState(
            books = books,
            resources = resources,
            dailyReadingGoalPages = prefs.dailyReadingGoalPages,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingUiState())

    fun saveBook(book: BookItemEntity) {
        if (book.title.isBlank()) return
        val status = when {
            book.totalPages > 0 && book.pagesRead >= book.totalPages -> "COMPLETED"
            else -> book.status
        }
        viewModelScope.launch {
            repo.saveBook(book.copy(status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    fun addPages(book: BookItemEntity, deltaPages: Int) {
        val next = (book.pagesRead + deltaPages).coerceIn(0, book.totalPages.coerceAtLeast(1))
        val status = if (book.totalPages > 0 && next >= book.totalPages) "COMPLETED" else "READING"
        viewModelScope.launch {
            repo.saveBook(book.copy(pagesRead = next, status = status, updatedAt = System.currentTimeMillis()))
        }
    }

    fun deleteBook(id: Long) {
        viewModelScope.launch { repo.deleteBook(id) }
    }

    fun saveResource(resource: LearningResourceEntity) {
        if (resource.title.isBlank()) return
        viewModelScope.launch { repo.saveLearningResource(resource) }
    }

    fun toggleResourceComplete(resource: LearningResourceEntity) {
        viewModelScope.launch { repo.saveLearningResource(resource.copy(isCompleted = !resource.isCompleted)) }
    }

    fun deleteResource(id: Long) {
        viewModelScope.launch { repo.deleteLearningResource(id) }
    }
}

data class DisciplineUiState(
    val logs: List<ScreenTimeLogEntity> = emptyList(),
    val todayLog: ScreenTimeLogEntity = ScreenTimeLogEntity(DateUtils.todayEpochDay()),
    val hasUsageStatsPermission: Boolean = false,
)

class DisciplineViewModel(
    private val repo: LifeOsRepository,
    private val usageStatsHelper: UsageStatsHelper,
) : ViewModel() {
    private val permissionGranted = MutableStateFlow(usageStatsHelper.hasUsageStatsPermission())

    val uiState: StateFlow<DisciplineUiState> = combine(
        repo.screenTimeLogs,
        permissionGranted,
    ) { logs, granted ->
        val today = DateUtils.todayEpochDay()
        val todayLog = logs.firstOrNull { it.epochDay == today } ?: ScreenTimeLogEntity(today)
        DisciplineUiState(
            logs = logs,
            todayLog = todayLog,
            hasUsageStatsPermission = granted,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DisciplineUiState())

    fun refreshUsagePermissionAndSync() {
        val granted = usageStatsHelper.hasUsageStatsPermission()
        permissionGranted.value = granted
        if (granted) {
            importFromDeviceUsage()
        }
    }

    fun importFromDeviceUsage() {
        val snapshot = usageStatsHelper.queryTodayUsage() ?: return
        viewModelScope.launch {
            val today = DateUtils.todayEpochDay()
            val current = uiState.value.todayLog
            repo.upsertScreenTimeLog(
                current.copy(
                    epochDay = today,
                    totalMinutes = snapshot.totalMinutes,
                    socialMediaMinutes = snapshot.socialMediaMinutes,
                    source = "USAGE_STATS",
                    topAppsSummary = snapshot.topAppsSummary,
                    isSampleData = false,
                ),
            )
        }
    }

    fun saveManualLog(totalMinutes: Int, socialMinutes: Int, distractions: Int, goalMinutes: Int) {
        viewModelScope.launch {
            val today = DateUtils.todayEpochDay()
            repo.upsertScreenTimeLog(
                ScreenTimeLogEntity(
                    epochDay = today,
                    totalMinutes = totalMinutes.coerceAtLeast(0),
                    socialMediaMinutes = socialMinutes.coerceAtLeast(0),
                    distractionCount = distractions.coerceAtLeast(0),
                    dailyGoalMinutes = goalMinutes.coerceAtLeast(15),
                    source = "MANUAL",
                    topAppsSummary = uiState.value.todayLog.topAppsSummary,
                    isSampleData = false,
                ),
            )
        }
    }

    fun buildUsageSettingsIntent() = usageStatsHelper.buildUsageAccessSettingsIntent()
}

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val pendingRestore: BackupValidationResult? = null,
    val pendingPasswordPromptRawJson: String? = null,
)

class SettingsViewModel(
    private val repo: LifeOsRepository,
    private val prefsRepo: UserPreferencesRepository,
    private val backupManager: BackupManager,
    private val reminderScheduler: ReminderScheduler,
) : ViewModel() {

    private val _status = MutableStateFlow<String?>(null)
    private val _error = MutableStateFlow<String?>(null)
    private val _pendingRestore = MutableStateFlow<BackupValidationResult?>(null)
    private val _pendingPasswordRaw = MutableStateFlow<String?>(null)

    val uiState: StateFlow<SettingsUiState> = combine(
        prefsRepo.preferences,
        _status,
        _error,
        _pendingRestore,
        _pendingPasswordRaw,
    ) { prefs, status, error, restore, pwdRaw ->
        SettingsUiState(
            prefs = prefs,
            statusMessage = status,
            errorMessage = error,
            pendingRestore = restore,
            pendingPasswordPromptRawJson = pwdRaw,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    fun clearMessages() {
        _status.value = null
        _error.value = null
    }

    fun updateName(name: String) {
        viewModelScope.launch { prefsRepo.updateProfileName(name) }
    }

    fun updateCurrency(option: CurrencyOption) {
        viewModelScope.launch { prefsRepo.updateCurrency(option) }
    }

    fun updateTheme(theme: ThemePreference) {
        viewModelScope.launch { prefsRepo.updateTheme(theme) }
    }

    fun updateWeekStartsMonday(monday: Boolean) {
        viewModelScope.launch { prefsRepo.updateWeekStartsMonday(monday) }
    }

    fun updateBiometricLock(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.updateBiometricLock(enabled) }
    }

    fun updateNotifications(enabled: Boolean, habitH: Int, habitM: Int, journalH: Int, journalM: Int) {
        viewModelScope.launch {
            prefsRepo.updateNotifications(enabled, habitH, habitM, journalH, journalM)
            reminderScheduler.scheduleDailyReminders(enabled, habitH, habitM, journalH, journalM)
            _status.value = if (enabled) "Daily local reminders scheduled" else "Local reminders disabled"
        }
    }

    suspend fun generateJsonBackup(password: String?): String =
        backupManager.exportToJson(password)

    suspend fun generateFinanceCsv(): String = backupManager.exportFinanceCsv()
    suspend fun generateHabitsStudyCsv(): String = backupManager.exportHabitsAndStudyCsv()

    fun inspectImportedBackup(rawJson: String, password: String? = null) {
        viewModelScope.launch {
            try {
                if (password == null && backupManager.isBackupPasswordProtected(rawJson)) {
                    _pendingPasswordRaw.value = rawJson
                    return@launch
                }
                val result = backupManager.validateAndInspectBackup(rawJson, password)
                _pendingPasswordRaw.value = null
                _pendingRestore.value = result
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to read backup file"
            }
        }
    }

    fun cancelRestoreDialogs() {
        _pendingRestore.value = null
        _pendingPasswordRaw.value = null
    }

    fun confirmRestore(mode: RestoreMode) {
        val pending = _pendingRestore.value ?: return
        viewModelScope.launch {
            try {
                backupManager.restoreBackup(pending.payload, mode)
                _pendingRestore.value = null
                _status.value = "Restored ${pending.totalRecords} records (${if (mode == RestoreMode.MERGE_SKIP_DUPLICATES) "merged" else "replaced"})"
            } catch (e: Exception) {
                _error.value = e.message ?: "Restore failed"
            }
        }
    }

    fun clearSampleData() {
        viewModelScope.launch {
            repo.clearSampleData()
            _status.value = "Sample demo data removed"
        }
    }

    fun eraseAllData() {
        viewModelScope.launch {
            repo.clearAllUserData()
            _status.value = "All personal data deleted locally"
        }
    }

    fun notifyStatus(msg: String) {
        _status.value = msg
    }

    fun notifyError(msg: String) {
        _error.value = msg
    }
}
