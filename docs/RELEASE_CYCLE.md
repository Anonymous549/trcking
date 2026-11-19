# GitHub Build & Release Cycle

LIFE OS 365 uses an automated, verifiable GitHub Actions release pipeline designed for transparent open-source Android distribution.

## 1. Branching & Release Cadence

| Branch / Tag | Purpose | Triggered Workflow |
| --- | --- | --- |
| `main` | Always-releasable integration branch | `.github/workflows/ci.yml` |
| `develop` / PRs | Feature development & code review | `.github/workflows/ci.yml` |
| `vX.Y.Z-rc.N` | Pre-release candidate for validation | `.github/workflows/release.yml` (marked Pre-release) |
| `vX.Y.Z` | Production stable release | `.github/workflows/release.yml` |

### Semantic Versioning & `versionCode` Formula

Tags must follow `vMAJOR.MINOR.PATCH` (with optional `-alpha.N`, `-beta.N`, `-rc.N` suffix):
- `versionName`: Tag without leading `v` (e.g., `v1.2.0` -> `1.2.0`).
- `versionCode`: Computed deterministically as:
  ```text
  MAJOR * 10000 + MINOR * 100 + PATCH
  ```
  For example, `v1.2.3` produces `versionCode = 10203`.

Both values are injected at build time via `-PlifeOsVersionName` and `-PlifeOsVersionCode` into `app/build.gradle.kts` (`defaultConfig`).

---

## 2. Continuous Integration Gate (`.github/workflows/ci.yml`)

Every push to `main`/`develop` and every pull request runs:
1. **Offline-First Manifest Policy Check**: Fails immediately if `android.permission.INTERNET` is detected or `android:allowBackup="false"` is removed.
2. **Unit & Robolectric UI Suite**: `gradle :app:testDebugUnitTest`
3. **Android Lint**: `gradle :app:lintDebug`
4. **Debug Build Verification**: `gradle :app:assembleDebug`
5. Uploads test reports, lint results, and `app-debug.apk` as build artifacts.

---

## 3. Release Signing Setup (GitHub Secrets)

`app/build.gradle.kts` reads release signing credentials from environment variables (`KEYSTORE_PATH`, `STORE_PASSWORD`, `KEY_PASSWORD`) with key alias `upload`.

To enable signed `.apk` and `.aab` release artifacts on GitHub:

1. Generate an upload keystore (keep a secure offline backup—never commit `.jks` files):
   ```bash
   keytool -genkeypair -v -keystore my-upload-key.jks -alias upload \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64-encode the keystore:
   ```bash
   base64 -w 0 my-upload-key.jks
   ```
3. In your GitHub repository go to **Settings -> Secrets and variables -> Actions** and add:
   - `KEYSTORE_BASE64`: The single-line Base64 string of `my-upload-key.jks`
   - `STORE_PASSWORD`: Keystore password
   - `KEY_PASSWORD`: Key password for alias `upload`

> **Fork Fallback:** If signing secrets are not configured in a fork, `.github/workflows/release.yml` still runs the full test/lint QA gate and attaches a debug-signed APK (`LIFE-OS-365-<tag>-debug.apk`) with a notice in the release notes.

---

## 4. Cutting a Release (Step-by-Step)

1. Ensure `main` is green in **Actions -> Android CI**.
2. Move items in `CHANGELOG.md` from `## [Unreleased]` to `## [X.Y.Z] - YYYY-MM-DD`, commit, and push to `main`:
   ```bash
   git add CHANGELOG.md
   git commit -m "chore(release): prepare v1.0.0"
   git push origin main
   ```
3. Create and push an annotated SemVer tag:
   ```bash
   git tag -a v1.0.0 -m "LIFE OS 365 v1.0.0"
   git push origin v1.0.0
   ```
4. GitHub Actions (`.github/workflows/release.yml`) automatically:
   - Validates SemVer tag and offline-first manifest rules
   - Runs `:app:testDebugUnitTest` and `:app:lintDebug`
   - Builds `LIFE-OS-365-v1.0.0-release.apk` and `LIFE-OS-365-v1.0.0-release.aab`
   - Computes `SHA256SUMS.txt`
   - Publishes the GitHub Release with categorized PR notes (`.github/release.yml`) and checksums.

Alternatively, trigger **Actions -> Release Cycle -> Run workflow** manually with a target `version_tag`.

---

## 5. Verifying Downloaded Release Artifacts

Users can verify APK integrity locally before installing:
```bash
sha256sum -c SHA256SUMS.txt
```
