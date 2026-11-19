package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.example.BuildConfig
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.preferences.SUPPORTED_CURRENCIES
import com.example.domain.backup.RestoreMode
import com.example.platform.BiometricHelper
import com.example.ui.components.LifeOsCard
import com.example.ui.components.SectionHeader
import com.example.ui.theme.ThemePreference
import com.example.ui.viewmodel.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val prefs = state.prefs
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var nameDraft by remember(prefs.userName) { mutableStateOf(prefs.userName) }
    var showExportPasswordDialog by remember { mutableStateOf(false) }
    var pendingExportPassword by remember { mutableStateOf<String?>(null) }
    var showClearDemoConfirm by remember { mutableStateOf(false) }
    var showEraseAllConfirm by remember { mutableStateOf(false) }
    var infoDialogTitle by remember { mutableStateOf<String?>(null) }
    var infoDialogBody by remember { mutableStateOf("") }

    LaunchedEffect(state.statusMessage, state.errorMessage) {
        val msg = state.statusMessage ?: state.errorMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessages()
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.updateNotifications(true, prefs.habitReminderHour, prefs.habitReminderMinute, prefs.journalReminderHour, prefs.journalReminderMinute)
        } else {
            viewModel.notifyError("Notification permission denied. Local reminders stay disabled.")
        }
    }

    val jsonExportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val content = withContext(Dispatchers.IO) { viewModel.generateJsonBackup(pendingExportPassword) }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(content.toByteArray()) }
                    }
                    pendingExportPassword = null
                    viewModel.notifyStatus("Full JSON backup exported successfully")
                } catch (e: Exception) {
                    viewModel.notifyError("Export failed: ${e.message}")
                }
            }
        }
    }

    val financeCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = withContext(Dispatchers.IO) { viewModel.generateFinanceCsv() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                    }
                    viewModel.notifyStatus("Finance CSV exported")
                } catch (e: Exception) {
                    viewModel.notifyError("CSV export failed: ${e.message}")
                }
            }
        }
    }

    val habitsStudyCsvLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val csv = withContext(Dispatchers.IO) { viewModel.generateHabitsStudyCsv() }
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { it.write(csv.toByteArray()) }
                    }
                    viewModel.notifyStatus("Habits & Study CSV exported")
                } catch (e: Exception) {
                    viewModel.notifyError("CSV export failed: ${e.message}")
                }
            }
        }
    }

    val jsonImportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                try {
                    val raw = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() } ?: ""
                    }
                    viewModel.inspectImportedBackup(raw)
                } catch (e: Exception) {
                    viewModel.notifyError("Unable to read file: ${e.message}")
                }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Settings & Privacy Vault") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Profile
            item(key = "profile") {
                LifeOsCard {
                    SectionHeader(title = "Personal Profile")
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = nameDraft,
                            onValueChange = { nameDraft = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                        )
                        Button(onClick = { viewModel.updateName(nameDraft) }) { Text("Save") }
                    }
                }
            }

            // Appearance, Currency & Calendar
            item(key = "preferences") {
                LifeOsCard {
                    SectionHeader(title = "Appearance, Currency & Week Start")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Theme", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ThemePreference.entries.forEach { t ->
                            FilterChip(
                                selected = prefs.themeEnum == t,
                                onClick = { viewModel.updateTheme(t) },
                                label = { Text(t.name.lowercase().replaceFirstChar { it.uppercase() }) },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Default Currency", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        SUPPORTED_CURRENCIES.forEach { opt ->
                            FilterChip(
                                selected = prefs.currencyCode == opt.code,
                                onClick = { viewModel.updateCurrency(opt) },
                                label = { Text("${opt.code} (${opt.symbol})") },
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("Start week on Monday", style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = prefs.weekStartsMonday,
                            onCheckedChange = { viewModel.updateWeekStartsMonday(it) },
                        )
                    }
                }
            }

            // Security & Reminders
            item(key = "security_reminders") {
                LifeOsCard {
                    SectionHeader(title = "Security & Local Reminders")
                    Spacer(modifier = Modifier.height(8.dp))
                    val bioSupported = BiometricHelper.canAuthenticate(context)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Biometric / Device Lock", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    if (bioSupported) "Off by default. Prompts on app launch when enabled." else "Set a screen lock / biometric on device to enable.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = prefs.biometricLockEnabled,
                            enabled = bioSupported,
                            onCheckedChange = { viewModel.updateBiometricLock(it) },
                            modifier = Modifier.testTag("biometric_lock_switch"),
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Column {
                                Text("Daily Local Reminders", style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "Offline WorkManager alerts at 08:30 & 21:30",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                        Switch(
                            checked = prefs.notificationsEnabled,
                            onCheckedChange = { wantEnable ->
                                if (!wantEnable) {
                                    viewModel.updateNotifications(false, prefs.habitReminderHour, prefs.habitReminderMinute, prefs.journalReminderHour, prefs.journalReminderMinute)
                                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    viewModel.updateNotifications(true, prefs.habitReminderHour, prefs.habitReminderMinute, prefs.journalReminderHour, prefs.journalReminderMinute)
                                }
                            },
                        )
                    }
                }
            }

            // Backup & Data Portability
            item(key = "backup_portability") {
                LifeOsCard {
                    SectionHeader(
                        title = "Backup & Data Portability",
                        subtitle = "User-initiated exports via Android Storage Access Framework",
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { showExportPasswordDialog = true },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Upload, contentDescription = null)
                            Text(" JSON Export")
                        }
                        OutlinedButton(
                            onClick = { jsonImportLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                            modifier = Modifier.weight(1f),
                        ) {
                            Icon(Icons.Default.Download, contentDescription = null)
                            Text(" JSON Import")
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(
                            onClick = { financeCsvLauncher.launch("lifeos365_finance.csv") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Finance CSV")
                        }
                        OutlinedButton(
                            onClick = { habitsStudyCsvLauncher.launch("lifeos365_habits_study.csv") },
                            modifier = Modifier.weight(1f),
                        ) {
                            Text("Habits & Study CSV")
                        }
                    }
                }
            }

            // Data Management
            item(key = "data_management") {
                LifeOsCard {
                    SectionHeader(title = "Data Management")
                    Spacer(modifier = Modifier.height(8.dp))
                    if (prefs.demoDataActive) {
                        OutlinedButton(
                            onClick = { showClearDemoConfirm = true },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Remove Sample Demo Data")
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    OutlinedButton(
                        onClick = { showEraseAllConfirm = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) {
                        Icon(Icons.Default.DeleteForever, contentDescription = null)
                        Text(" Delete All Local Data")
                    }
                }
            }

            // Privacy, Open-Source Licenses & About
            item(key = "about_legal") {
                LifeOsCard {
                    SectionHeader(title = "Privacy, Licenses & About")
                    Spacer(modifier = Modifier.height(6.dp))
                    TextButton(onClick = {
                        infoDialogTitle = "Privacy & Security Limitations"
                        infoDialogBody = """
                            LIFE OS 365 operates 100% offline with zero servers, zero analytics SDKs, zero ads, and no internet permission.
                            
                            • Cloud auto-backup (allowBackup) is disabled by default.
                            • Sensitive journal entries and financial notes are encrypted using Android Keystore AES-256-GCM before storage.
                            • Optional JSON backups can be protected with PBKDF2-HMAC-SHA256 + AES-256-GCM encryption.
                            • UsageStats access (if granted) is processed strictly on-device and never leaves your phone.
                            
                            Security Limitation Notice: No mobile application is completely invulnerable. If a device is rooted, unlocked without a passcode, or compromised at the OS level, runtime memory or keystore-backed data may still be exposed. Always use a strong device lock screen and keep encrypted backups safely stored.
                        """.trimIndent()
                    }) {
                        Icon(Icons.Default.PrivacyTip, contentDescription = null)
                        Text("  Privacy Policy & Security Limitations")
                    }
                    TextButton(onClick = {
                        infoDialogTitle = "Open-Source MIT License & Dependencies"
                        infoDialogBody = """
                            LIFE OS 365 is open source under the MIT License.
                            
                            Open-Source Libraries Used:
                            • Kotlin & Kotlinx Coroutines / Serialization (Apache 2.0)
                            • Jetpack Compose, Material 3, Navigation Compose (Apache 2.0)
                            • AndroidX Room, DataStore, WorkManager, Biometric, Core KTX (Apache 2.0)
                            • Playfair Display, Plus Jakarta Sans & JetBrains Mono Fonts (SIL Open Font License 1.1)
                            • JUnit 4 (EPL 1.0) & Robolectric (MIT) for testing
                        """.trimIndent()
                    }) {
                        Icon(Icons.Default.Shield, contentDescription = null)
                        Text("  Open-Source Licenses (MIT / Apache 2.0)")
                    }
                    TextButton(onClick = {
                        infoDialogTitle = "About LIFE OS 365"
                        infoDialogBody = "LIFE OS 365 v${BuildConfig.VERSION_NAME} (build ${BuildConfig.VERSION_CODE})\nYour entire life. One dashboard. Your data. Your control.\n\n100% free forever. Offline-first personal life management & analytics."
                    }) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Text("  About LIFE OS 365")
                    }
                }
            }
        }
    }

    if (showExportPasswordDialog) {
        var pwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showExportPasswordDialog = false },
            title = { Text("Export Full JSON Backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Optionally enter a password to encrypt your JSON backup with PBKDF2 + AES-256-GCM. Leave blank for plain JSON.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    OutlinedTextField(
                        value = pwd,
                        onValueChange = { pwd = it },
                        label = { Text("Optional Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingExportPassword = pwd.ifBlank { null }
                    showExportPasswordDialog = false
                    jsonExportLauncher.launch("lifeos365_backup.json")
                }) { Text("Choose File Location") }
            },
            dismissButton = { TextButton(onClick = { showExportPasswordDialog = false }) { Text("Cancel") } },
        )
    }

    state.pendingPasswordPromptRawJson?.let { raw ->
        var pwd by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestoreDialogs() },
            title = { Text("Decrypt Backup") },
            text = {
                OutlinedTextField(
                    value = pwd,
                    onValueChange = { pwd = it },
                    label = { Text("Backup Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    singleLine = true,
                )
            },
            confirmButton = {
                Button(onClick = { viewModel.inspectImportedBackup(raw, pwd) }, enabled = pwd.isNotBlank()) {
                    Text("Unlock")
                }
            },
            dismissButton = { TextButton(onClick = { viewModel.cancelRestoreDialogs() }) { Text("Cancel") } },
        )
    }

    state.pendingRestore?.let { validation ->
        AlertDialog(
            onDismissRequest = { viewModel.cancelRestoreDialogs() },
            title = { Text("Backup Verified") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Records found: ${validation.totalRecords}")
                    Text("Potential duplicates detected: ${validation.duplicateCount}")
                    Text(
                        "Choose whether to merge new records (skipping duplicates) or replace your current database.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { viewModel.confirmRestore(RestoreMode.MERGE_SKIP_DUPLICATES) }) {
                    Text("Merge")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { viewModel.cancelRestoreDialogs() }) { Text("Cancel") }
                    TextButton(onClick = { viewModel.confirmRestore(RestoreMode.REPLACE_ALL) }) {
                        Text("Replace All", color = MaterialTheme.colorScheme.error)
                    }
                }
            },
        )
    }

    if (showClearDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDemoConfirm = false },
            title = { Text("Remove Sample Demo Data?") },
            text = { Text("This deletes only preloaded sample records while keeping everything you created.") },
            confirmButton = {
                Button(onClick = {
                    viewModel.clearSampleData()
                    showClearDemoConfirm = false
                }) { Text("Remove Sample Data") }
            },
            dismissButton = { TextButton(onClick = { showClearDemoConfirm = false }) { Text("Cancel") } },
        )
    }

    if (showEraseAllConfirm) {
        AlertDialog(
            onDismissRequest = { showEraseAllConfirm = false },
            title = { Text("Delete All Local Data?") },
            text = { Text("This permanently deletes all habits, tasks, workouts, finances, goals, and journal entries on this device. Export a JSON backup first if needed.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.eraseAllData()
                        showEraseAllConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete Everything") }
            },
            dismissButton = { TextButton(onClick = { showEraseAllConfirm = false }) { Text("Cancel") } },
        )
    }

    infoDialogTitle?.let { title ->
        AlertDialog(
            onDismissRequest = { infoDialogTitle = null },
            title = { Text(title) },
            text = { Text(infoDialogBody, style = MaterialTheme.typography.bodySmall) },
            confirmButton = { Button(onClick = { infoDialogTitle = null }) { Text("Close") } },
        )
    }
}
