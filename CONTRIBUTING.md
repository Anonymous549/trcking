# Contributing to LIFE OS 365

Thank you for contributing to **LIFE OS 365**, an offline-first, open-source personal life management system.

## Non-Negotiable Project Rules

1. **100% Offline-First**: Never add `android.permission.INTERNET`, cloud sync SDKs, telemetry, analytics, or advertising libraries.
2. **Local Privacy**: Keep `android:allowBackup="false"` in `AndroidManifest.xml`. Sensitive text fields (journal entries, financial notes) must pass through `CryptoManager`.
3. **API 24 Compatibility**: `minSdk` is `24`. Use `DateUtils` (`java.util.Calendar` / `SimpleDateFormat`) rather than `java.time.*` APIs unless core library desugaring is explicitly enabled.
4. **Material 3 & Accessibility**: Use `MaterialTheme.colorScheme` / `LifeOsThemeTokens`, include `contentDescription` on meaningful icons/images, maintain `>= 48.dp` touch targets, and assign `Modifier.testTag("snake_case")` to interactive UI elements.

## Local Development Workflow

1. Fork and clone the repository.
2. Create a feature or fix branch from `main` (e.g., `feat/habit-csv-import` or `fix/pomodoro-timer`).
3. Make changes and add or update unit/Robolectric tests under `app/src/test/`.
4. Run verification locally before opening a PR:
   ```bash
   gradle :app:testDebugUnitTest
   gradle :app:lintDebug
   gradle :app:assembleDebug
   ```
5. Update `CHANGELOG.md` under `## [Unreleased]`.
6. Open a Pull Request against `main` using the PR template checklist.
