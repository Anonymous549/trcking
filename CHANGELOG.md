# Changelog

All notable changes to **LIFE OS 365** are documented in this file.

The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Added
- GitHub Actions CI pipeline (`.github/workflows/ci.yml`) running offline-manifest verification, unit/Robolectric tests, Android Lint, and Debug APK builds on pushes and pull requests.
- Automated GitHub Release pipeline (`.github/workflows/release.yml`) triggered by `v*.*.*` semantic tags or manual dispatch, producing signed APK/AAB artifacts (with debug fallback when signing secrets are absent), `SHA256SUMS.txt`, and categorized release notes.
- Dynamic `BuildConfig.VERSION_NAME` and `BuildConfig.VERSION_CODE` injection from Gradle properties (`lifeOsVersionName`, `lifeOsVersionCode`) displayed in Settings -> About.
- GitHub release notes config, Dependabot weekly schedule, PR template, Issue templates, `CONTRIBUTING.md`, and `docs/RELEASE_CYCLE.md`.

## [1.0.0] - 2026-11-19

### Added
- **Today Dashboard**: Date header, personalized welcome, daily completion percentage ring, top habit streaks, responsive Quick Actions grid (Add Habit, Add Task, Log Expense, Quick Note), customizable/reorderable widget sections, and removable sample data setup.
- **Habits & Discipline**: Unlimited habits, daily/weekly/custom schedules, rest-day-aware streaks, 12-week calendar heatmap, missed habit tracking, and local WorkManager reminders.
- **Focus, Study & Routine**: Priority & recurring tasks, Pomodoro focus timer, study subjects/chapters/revision schedules, exam countdowns, career skills, weekly study reports, and customizable 8:00 AM daily routine blocks.
- **Personal Analytics**: Configurable productivity formula, 7-day & 14-day trend charts, period comparisons, and holistic 12-week heatmap.
- **Modules Hub**: Personal Finance (INR `₹` default + multi-currency, budgets, savings goals, alerts), Fitness & Health, Goals & Personal Vision, Encrypted Private Journal, Reading & Knowledge Tracker, and Digital Discipline (manual + optional `UsageStats`).
- **Offline Security & Portability**: Zero `INTERNET` permission, `allowBackup="false"`, Android Keystore AES-256-GCM field encryption, optional BiometricPrompt lock, JSON backup export/import (SHA-256 + optional PBKDF2-HMAC-SHA256 encryption), and CSV exports.
