# Privacy Policy & Security Limitations — LIFE OS 365

**Effective Date:** 2026

## 1. Zero Data Collection
LIFE OS 365 does **not** collect, transmit, rent, sell, or share any personal data. The application does not request the Android `INTERNET` permission and contains no analytics SDKs, advertising SDKs, crash-reporting telemetry, or cloud backends.

## 2. Local Storage & Cloud Backup
- All records are stored exclusively on your device inside Room SQLite and Jetpack DataStore.
- Android automatic cloud backup (`android:allowBackup`) is **disabled by default**.
- Exports (JSON or CSV) are strictly initiated by you through the Android Storage Access Framework (`CreateDocument`).

## 3. Permissions Used (Least-Privilege)
- `USE_BIOMETRIC` (Optional): Enables the optional Biometric/Device Lock screen in Settings (off by default).
- `POST_NOTIFICATIONS` (Optional, Android 13+): Requested only if you toggle on local WorkManager reminders.
- `PACKAGE_USAGE_STATS` (Optional): Requested inside the Digital Discipline tab with explicit explanation to summarize local screen time on-device. Usage statistics never leave your device.

## 4. Security Architecture & Honest Limitations
- Sensitive journal reflections, gratitude entries, challenges, lessons, and financial transaction notes are encrypted using an AES-256-GCM key stored in the Android Keystore.
- Password-protected backups use PBKDF2-HMAC-SHA256 (100,000 iterations) + AES-256-GCM.
- **Limitations**: No software system is 100% invulnerable. If your device is unlocked without a strong passcode, rooted, or compromised at the operating system level, runtime memory or local application sandboxes may be exposed. Always maintain a secure device lock screen and protect exported backup files.
