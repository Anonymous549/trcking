# LIFE OS 365

**Your entire life. One dashboard. Your data. Your control.**

LIFE OS 365 is an offline-first, 100% free, open-source personal life management and analytics Android application built with Kotlin, Jetpack Compose, Material 3, Room, and DataStore.

## Core Principles

- **100% Offline-First**: No mandatory accounts, no cloud dependencies, no internet permission, and no external APIs.
- **Zero Telemetry & Zero Ads**: No analytics SDKs, no tracking, and no subscriptions or paywalls.
- **Privacy by Design**: Cloud auto-backup (`allowBackup`) is disabled by default; journal entries and financial notes are encrypted locally with Android Keystore AES-256-GCM; optional BiometricPrompt app lock.
- **Full Data Portability**: User-initiated JSON export/import (with SHA-256 verification, duplicate detection, and optional PBKDF2 + AES-GCM password encryption) plus CSV exports for Finance, Habits, and Study logs.

## Modules Included

1. **Today Dashboard**: Customisable & reorderable widgets, daily completion ring, top 3 priorities, quick-add actions, streaks, and weekly productivity chart.
2. **Habits & Discipline**: Unlimited habits, daily/weekly/custom schedules, one-tap check-offs, rest-day streak protection, 12-week calendar heatmaps, and local reminders.
3. **Focus, Study & Routine**: Priority & recurring to-do lists, working Pomodoro timer, study subjects/chapters/spaced revision, exam countdowns, career/skills tracker, weekly study reports, and customizable 8:00 AM starter routine.
4. **Personal Analytics**: Transparent, configurable productivity formula, weekly & monthly comparisons, 14-day growth trend, and holistic 12-week consistency heatmap.
5. **Fitness & Health**: Workout & exercise logging, custom exercise library, steps, weight trend, body measurements, water, nutrition, and sleep tracking (non-medical personal log).
6. **Personal Finance**: Local ledger with INR (`₹`) default & multi-currency support, custom categories, monthly budgets, overspending alerts, savings goals, and spending breakdown charts.
7. **Goals & Vision**: Daily, weekly, monthly, yearly, short-term, and long-term goals with milestones and a personal vision statement.
8. **Private Journal**: Encrypted daily reflections, mood tracking, gratitude, achievements, challenges, lessons learned, and searchable history.
9. **Reading & Knowledge**: Books in progress, page tracking, completed library, and learning resources.
10. **Digital Discipline**: Daily digital budget, manual screen-time & distraction logs, plus optional on-device `UsageStats` integration.

## Installation & Build Instructions

1. Clone the repository and open it in Android Studio (JDK 11+, Android SDK 36).
2. Build the debug APK:
   ```bash
   gradle :app:assembleDebug
   ```
3. Run unit and Robolectric UI tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
4. Install the generated APK from `app/build/outputs/apk/debug/app-debug.apk` onto your Android device (Android 7.0 / API 24+).

## GitHub CI & Release Cycle

- **Continuous Integration** (`.github/workflows/ci.yml`): Verifies offline-first manifest rules, runs unit/Robolectric tests, executes Android Lint, and builds the debug APK on every PR and push to `main`.
- **Automated Releases** (`.github/workflows/release.yml`): Triggered on `v*.*.*` tags (or manual dispatch), runs the full test + lint gate, builds signed release APK & AAB artifacts, generates `SHA256SUMS.txt`, and publishes a GitHub Release.

## Documentation

- [Release Cycle & Signing Guide](docs/RELEASE_CYCLE.md)
- [Changelog](CHANGELOG.md)
- [Contributing Guide](CONTRIBUTING.md)
- [Architecture Guide](docs/ARCHITECTURE.md)
- [Database Schema](docs/DATABASE.md)
- [Privacy Policy & Security Limitations](docs/PRIVACY_POLICY.md)
- [Open-Source Dependencies & Licenses](docs/LICENSES.md)

## License

Released under the [MIT License](LICENSE).
