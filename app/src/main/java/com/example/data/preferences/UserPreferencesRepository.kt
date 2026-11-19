package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.ui.theme.ThemePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "life_os_prefs")

enum class DashboardWidget(val id: String, val label: String) {
    DAILY_PROGRESS("daily_progress", "Daily Completion & Streaks"),
    TOP_PRIORITIES("top_priorities", "Today's Top 3 Priorities"),
    HABIT_CHECKLIST("habit_checklist", "Daily Habit Checklist"),
    UPCOMING_TASKS("upcoming_tasks", "Upcoming Tasks & Routine"),
    STUDY_AND_FITNESS("study_and_fitness", "Study & Workout Summary"),
    FINANCIAL_SUMMARY("financial_summary", "Financial Summary"),
    GOAL_PROGRESS("goal_progress", "Goal Progress"),
    WEEKLY_CHART("weekly_chart", "Weekly Productivity Chart"),
}

@Serializable
data class CurrencyOption(
    val code: String,
    val symbol: String,
    val label: String,
)

val SUPPORTED_CURRENCIES = listOf(
    CurrencyOption("INR", "₹", "Indian Rupee (₹)"),
    CurrencyOption("USD", "$", "US Dollar ($)"),
    CurrencyOption("EUR", "€", "Euro (€)"),
    CurrencyOption("GBP", "£", "British Pound (£)"),
    CurrencyOption("JPY", "¥", "Japanese Yen (¥)"),
    CurrencyOption("CAD", "C$", "Canadian Dollar (C$)"),
    CurrencyOption("AUD", "A$", "Australian Dollar (A$)"),
)

@Serializable
data class AnalyticsWeights(
    val habitWeight: Int = 30,
    val taskWeight: Int = 25,
    val studyWeight: Int = 20,
    val fitnessWeight: Int = 15,
    val disciplineWeight: Int = 10,
    val exemptRestDays: Boolean = true,
)

@Serializable
data class UserPreferences(
    val userName: String = "Explorer",
    val currencyCode: String = "INR",
    val currencySymbol: String = "₹",
    val themePreference: String = ThemePreference.SYSTEM.name,
    val weekStartsMonday: Boolean = true,
    val biometricLockEnabled: Boolean = false,
    val notificationsEnabled: Boolean = false,
    val habitReminderHour: Int = 8,
    val habitReminderMinute: Int = 30,
    val journalReminderHour: Int = 21,
    val journalReminderMinute: Int = 30,
    val dashboardWidgetOrder: List<String> = DashboardWidget.entries.map { it.id },
    val hiddenDashboardWidgets: Set<String> = emptySet(),
    val personalVision: String = "Build consistent daily habits, deepen mastery in my craft, stay strong in body and mind, and live intentionally.",
    val hasSeededInitialData: Boolean = false,
    val demoDataActive: Boolean = true,
    val quickSetupDismissed: Boolean = false,
    val dailyWaterGoalMl: Int = 2500,
    val dailyStepGoal: Int = 8000,
    val dailyReadingGoalPages: Int = 20,
    val habitWeight: Int = 30,
    val taskWeight: Int = 25,
    val studyWeight: Int = 20,
    val fitnessWeight: Int = 15,
    val disciplineWeight: Int = 10,
    val exemptRestDays: Boolean = true,
) {
    val themeEnum: ThemePreference
        get() = runCatching { ThemePreference.valueOf(themePreference) }.getOrDefault(ThemePreference.SYSTEM)

    val analyticsWeights: AnalyticsWeights
        get() = AnalyticsWeights(
            habitWeight = habitWeight,
            taskWeight = taskWeight,
            studyWeight = studyWeight,
            fitnessWeight = fitnessWeight,
            disciplineWeight = disciplineWeight,
            exemptRestDays = exemptRestDays,
        )
}

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val USER_NAME = stringPreferencesKey("user_name")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val WEEK_STARTS_MONDAY = booleanPreferencesKey("week_starts_monday")
        val BIOMETRIC_LOCK = booleanPreferencesKey("biometric_lock")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val HABIT_REMINDER_HOUR = intPreferencesKey("habit_reminder_hour")
        val HABIT_REMINDER_MINUTE = intPreferencesKey("habit_reminder_minute")
        val JOURNAL_REMINDER_HOUR = intPreferencesKey("journal_reminder_hour")
        val JOURNAL_REMINDER_MINUTE = intPreferencesKey("journal_reminder_minute")
        val DASHBOARD_ORDER = stringPreferencesKey("dashboard_order")
        val HIDDEN_WIDGETS = stringPreferencesKey("hidden_widgets")
        val PERSONAL_VISION = stringPreferencesKey("personal_vision")
        val HAS_SEEDED_DATA = booleanPreferencesKey("has_seeded_data")
        val DEMO_DATA_ACTIVE = booleanPreferencesKey("demo_data_active")
        val QUICK_SETUP_DISMISSED = booleanPreferencesKey("quick_setup_dismissed")
        val WATER_GOAL_ML = intPreferencesKey("water_goal_ml")
        val STEP_GOAL = intPreferencesKey("step_goal")
        val READING_GOAL_PAGES = intPreferencesKey("reading_goal_pages")
        val WEIGHT_HABIT = intPreferencesKey("weight_habit")
        val WEIGHT_TASK = intPreferencesKey("weight_task")
        val WEIGHT_STUDY = intPreferencesKey("weight_study")
        val WEIGHT_FITNESS = intPreferencesKey("weight_fitness")
        val WEIGHT_DISCIPLINE = intPreferencesKey("weight_discipline")
        val EXEMPT_REST_DAYS = booleanPreferencesKey("exempt_rest_days")
    }

    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val defaultOrder = DashboardWidget.entries.map { it.id }
        val savedOrder = prefs[Keys.DASHBOARD_ORDER]
            ?.split(",")
            ?.filter { id -> defaultOrder.contains(id) }
            ?.let { list ->
                list + defaultOrder.filterNot { it in list }
            } ?: defaultOrder
        val hidden = prefs[Keys.HIDDEN_WIDGETS]
            ?.split(",")
            ?.filter { it.isNotBlank() }
            ?.toSet() ?: emptySet()

        UserPreferences(
            userName = prefs[Keys.USER_NAME] ?: "Explorer",
            currencyCode = prefs[Keys.CURRENCY_CODE] ?: "INR",
            currencySymbol = prefs[Keys.CURRENCY_SYMBOL] ?: "₹",
            themePreference = prefs[Keys.THEME_MODE] ?: ThemePreference.SYSTEM.name,
            weekStartsMonday = prefs[Keys.WEEK_STARTS_MONDAY] ?: true,
            biometricLockEnabled = prefs[Keys.BIOMETRIC_LOCK] ?: false,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: false,
            habitReminderHour = prefs[Keys.HABIT_REMINDER_HOUR] ?: 8,
            habitReminderMinute = prefs[Keys.HABIT_REMINDER_MINUTE] ?: 30,
            journalReminderHour = prefs[Keys.JOURNAL_REMINDER_HOUR] ?: 21,
            journalReminderMinute = prefs[Keys.JOURNAL_REMINDER_MINUTE] ?: 30,
            dashboardWidgetOrder = savedOrder,
            hiddenDashboardWidgets = hidden,
            personalVision = prefs[Keys.PERSONAL_VISION]
                ?: "Build consistent daily habits, deepen mastery in my craft, stay strong in body and mind, and live intentionally.",
            hasSeededInitialData = prefs[Keys.HAS_SEEDED_DATA] ?: false,
            demoDataActive = prefs[Keys.DEMO_DATA_ACTIVE] ?: true,
            quickSetupDismissed = prefs[Keys.QUICK_SETUP_DISMISSED] ?: false,
            dailyWaterGoalMl = prefs[Keys.WATER_GOAL_ML] ?: 2500,
            dailyStepGoal = prefs[Keys.STEP_GOAL] ?: 8000,
            dailyReadingGoalPages = prefs[Keys.READING_GOAL_PAGES] ?: 20,
            habitWeight = prefs[Keys.WEIGHT_HABIT] ?: 30,
            taskWeight = prefs[Keys.WEIGHT_TASK] ?: 25,
            studyWeight = prefs[Keys.WEIGHT_STUDY] ?: 20,
            fitnessWeight = prefs[Keys.WEIGHT_FITNESS] ?: 15,
            disciplineWeight = prefs[Keys.WEIGHT_DISCIPLINE] ?: 10,
            exemptRestDays = prefs[Keys.EXEMPT_REST_DAYS] ?: true,
        )
    }

    suspend fun updateProfileName(name: String) {
        context.dataStore.edit { it[Keys.USER_NAME] = name.trim().ifEmpty { "Explorer" } }
    }

    suspend fun updateCurrency(option: CurrencyOption) {
        context.dataStore.edit {
            it[Keys.CURRENCY_CODE] = option.code
            it[Keys.CURRENCY_SYMBOL] = option.symbol
        }
    }

    suspend fun updateTheme(theme: ThemePreference) {
        context.dataStore.edit { it[Keys.THEME_MODE] = theme.name }
    }

    suspend fun updateWeekStartsMonday(monday: Boolean) {
        context.dataStore.edit { it[Keys.WEEK_STARTS_MONDAY] = monday }
    }

    suspend fun updateBiometricLock(enabled: Boolean) {
        context.dataStore.edit { it[Keys.BIOMETRIC_LOCK] = enabled }
    }

    suspend fun updateNotifications(enabled: Boolean, habitHour: Int? = null, habitMin: Int? = null, journalHour: Int? = null, journalMin: Int? = null) {
        context.dataStore.edit {
            it[Keys.NOTIFICATIONS_ENABLED] = enabled
            if (habitHour != null) it[Keys.HABIT_REMINDER_HOUR] = habitHour
            if (habitMin != null) it[Keys.HABIT_REMINDER_MINUTE] = habitMin
            if (journalHour != null) it[Keys.JOURNAL_REMINDER_HOUR] = journalHour
            if (journalMin != null) it[Keys.JOURNAL_REMINDER_MINUTE] = journalMin
        }
    }

    suspend fun updateDashboardCustomization(order: List<String>, hidden: Set<String>) {
        context.dataStore.edit {
            it[Keys.DASHBOARD_ORDER] = order.joinToString(",")
            it[Keys.HIDDEN_WIDGETS] = hidden.joinToString(",")
        }
    }

    suspend fun updatePersonalVision(vision: String) {
        context.dataStore.edit { it[Keys.PERSONAL_VISION] = vision }
    }

    suspend fun setSeededState(seeded: Boolean, demoActive: Boolean) {
        context.dataStore.edit {
            it[Keys.HAS_SEEDED_DATA] = seeded
            it[Keys.DEMO_DATA_ACTIVE] = demoActive
        }
    }

    suspend fun dismissQuickSetup() {
        context.dataStore.edit { it[Keys.QUICK_SETUP_DISMISSED] = true }
    }

    suspend fun updateAnalyticsWeights(weights: AnalyticsWeights) {
        context.dataStore.edit {
            it[Keys.WEIGHT_HABIT] = weights.habitWeight
            it[Keys.WEIGHT_TASK] = weights.taskWeight
            it[Keys.WEIGHT_STUDY] = weights.studyWeight
            it[Keys.WEIGHT_FITNESS] = weights.fitnessWeight
            it[Keys.WEIGHT_DISCIPLINE] = weights.disciplineWeight
            it[Keys.EXEMPT_REST_DAYS] = weights.exemptRestDays
        }
    }

    suspend fun restoreFromPreferences(restored: UserPreferences) {
        context.dataStore.edit {
            it[Keys.USER_NAME] = restored.userName
            it[Keys.CURRENCY_CODE] = restored.currencyCode
            it[Keys.CURRENCY_SYMBOL] = restored.currencySymbol
            it[Keys.THEME_MODE] = restored.themePreference
            it[Keys.WEEK_STARTS_MONDAY] = restored.weekStartsMonday
            it[Keys.BIOMETRIC_LOCK] = restored.biometricLockEnabled
            it[Keys.NOTIFICATIONS_ENABLED] = restored.notificationsEnabled
            it[Keys.HABIT_REMINDER_HOUR] = restored.habitReminderHour
            it[Keys.HABIT_REMINDER_MINUTE] = restored.habitReminderMinute
            it[Keys.JOURNAL_REMINDER_HOUR] = restored.journalReminderHour
            it[Keys.JOURNAL_REMINDER_MINUTE] = restored.journalReminderMinute
            it[Keys.DASHBOARD_ORDER] = restored.dashboardWidgetOrder.joinToString(",")
            it[Keys.HIDDEN_WIDGETS] = restored.hiddenDashboardWidgets.joinToString(",")
            it[Keys.PERSONAL_VISION] = restored.personalVision
            it[Keys.WATER_GOAL_ML] = restored.dailyWaterGoalMl
            it[Keys.STEP_GOAL] = restored.dailyStepGoal
            it[Keys.READING_GOAL_PAGES] = restored.dailyReadingGoalPages
            it[Keys.WEIGHT_HABIT] = restored.habitWeight
            it[Keys.WEIGHT_TASK] = restored.taskWeight
            it[Keys.WEIGHT_STUDY] = restored.studyWeight
            it[Keys.WEIGHT_FITNESS] = restored.fitnessWeight
            it[Keys.WEIGHT_DISCIPLINE] = restored.disciplineWeight
            it[Keys.EXEMPT_REST_DAYS] = restored.exemptRestDays
        }
    }
}
