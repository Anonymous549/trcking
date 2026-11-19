# Architecture — LIFE OS 365

LIFE OS 365 follows **MVVM + Clean Repository Architecture** with unidirectional data flow using Kotlin Coroutines and `StateFlow`.

## Layers

1. **UI Layer (`com.example.ui.*`)**:
   - Jetpack Compose Material 3 screens with `collectAsStateWithLifecycle()`.
   - Pure Compose `Canvas` charts (`CompletionRingCanvas`, `WeeklyBarChart`, `TrendLineChart`, `CategoryDonutChart`, `CalendarHeatmapGrid`).
   - Type-safe `@Serializable` routes via Navigation Compose and adaptive `NavigationBar` / `NavigationRail` layouts via `BoxWithConstraints`.
2. **ViewModel & Domain Layer (`com.example.ui.viewmodel`, `com.example.domain.*`)**:
   - Feature ViewModels (`DashboardViewModel`, `HabitsViewModel`, `FocusViewModel`, `AnalyticsViewModel`, `FitnessViewModel`, `FinanceViewModel`, `GoalsViewModel`, `JournalViewModel`, `ReadingViewModel`, `DisciplineViewModel`, `SettingsViewModel`).
   - `AnalyticsEngine`: Pure, deterministic Kotlin calculators for habit streaks (with rest-day exemption), daily weighted productivity scores, monthly finance summaries, and week/month comparisons.
   - `BackupManager`: Full JSON snapshot serialization with SHA-256 integrity verification, duplicate detection, merge/replace modes, optional password encryption, and RFC-4180 CSV generators.
3. **Data & Security Layer (`com.example.data.*`)**:
   - `LifeOsDatabase` (Room SQLite with 21 normalized tables, foreign keys, cascade deletes, and indexes).
   - `UserPreferencesRepository` (Jetpack DataStore Preferences for theme, currency, profile, dashboard customization, and analytics weights).
   - `CryptoManager` (Android Keystore AES-256-GCM field encryption and PBKDF2-HMAC-SHA256 backup encryption).
   - `SampleDataSeeder` (removable initial sample data).
4. **Platform Integration Layer (`com.example.platform.*`)**:
   - `ReminderScheduler` & `ReminderWorker` (`WorkManager` daily local notifications).
   - `UsageStatsHelper` (optional local-only `UsageStatsManager` insights).
   - `BiometricHelper` (`BiometricPrompt` with device credential fallback).
