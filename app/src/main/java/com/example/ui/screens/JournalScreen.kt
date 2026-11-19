package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.repository.DecryptedJournalEntry
import com.example.domain.util.DateUtils
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.PrivacyBanner
import com.example.ui.components.StatusBadge
import com.example.ui.viewmodel.JournalViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    viewModel: JournalViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Private Journal & Wellness") },
                windowInsets = WindowInsets(0, 0, 0, 0),
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                modifier = Modifier.testTag("add_journal_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Journal Entry")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .widthIn(max = 760.dp),
            contentPadding = PaddingValues(16.dp, 12.dp, 16.dp, 88.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "journal_privacy") {
                PrivacyBanner(
                    title = "Locally Encrypted Journal",
                    body = "Entries are encrypted with Android Keystore AES-256-GCM before being saved to local storage.",
                    icon = Icons.Default.Lock,
                )
            }

            item(key = "journal_search") {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    label = { Text("Search reflections, gratitude, lessons, tags...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("journal_search_input"),
                )
            }

            if (state.entries.isEmpty()) {
                item(key = "empty_journal") {
                    EmptyStateCard(
                        icon = Icons.Default.MenuBook,
                        title = "No journal entries match",
                        message = "Record daily mood, gratitude, achievements, challenges, and lessons learned.",
                        actionLabel = "Write Entry",
                        onActionClick = { showCreateDialog = true },
                    )
                }
            } else {
                items(state.entries, key = { it.id }) { entry ->
                    LifeOsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = DateUtils.formatShortDate(entry.epochDay),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(entry.title, style = MaterialTheme.typography.titleMedium)
                            }
                            StatusBadge(text = "Mood ${entry.moodScore}/5")
                            IconButton(onClick = { viewModel.deleteEntry(entry.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete entry")
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        if (entry.reflection.isNotBlank()) JournalSectionLine("Reflection", entry.reflection)
                        if (entry.gratitude.isNotBlank()) JournalSectionLine("Gratitude", entry.gratitude)
                        if (entry.achievements.isNotBlank()) JournalSectionLine("Achievements", entry.achievements)
                        if (entry.challenges.isNotBlank()) JournalSectionLine("Challenges", entry.challenges)
                        if (entry.lessons.isNotBlank()) JournalSectionLine("Lessons Learned", entry.lessons)
                        if (entry.notes.isNotBlank()) JournalSectionLine("Notes", entry.notes)
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var title by remember { mutableStateOf("") }
        var mood by remember { mutableIntStateOf(4) }
        var reflection by remember { mutableStateOf("") }
        var gratitude by remember { mutableStateOf("") }
        var achievements by remember { mutableStateOf("") }
        var challenges by remember { mutableStateOf("") }
        var lessons by remember { mutableStateOf("") }
        var tags by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Journal Entry") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.testTag("journal_title_input"))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        (1..5).forEach { m ->
                            FilterChip(selected = mood == m, onClick = { mood = m }, label = { Text("$m") })
                        }
                    }
                    OutlinedTextField(value = reflection, onValueChange = { reflection = it }, label = { Text("Daily Reflection") }, minLines = 2)
                    OutlinedTextField(value = gratitude, onValueChange = { gratitude = it }, label = { Text("Gratitude") }, singleLine = true)
                    OutlinedTextField(value = achievements, onValueChange = { achievements = it }, label = { Text("Positive Achievements") }, singleLine = true)
                    OutlinedTextField(value = challenges, onValueChange = { challenges = it }, label = { Text("Challenges Faced") }, singleLine = true)
                    OutlinedTextField(value = lessons, onValueChange = { lessons = it }, label = { Text("Lessons Learned") }, singleLine = true)
                    OutlinedTextField(value = tags, onValueChange = { tags = it }, label = { Text("Tags (comma separated)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveEntry(
                            DecryptedJournalEntry(
                                id = 0,
                                epochDay = DateUtils.todayEpochDay(),
                                title = title.trim().ifBlank { "Daily Check-In" },
                                moodScore = mood,
                                reflection = reflection.trim(),
                                gratitude = gratitude.trim(),
                                achievements = achievements.trim(),
                                challenges = challenges.trim(),
                                lessons = lessons.trim(),
                                notes = "",
                                tags = tags.trim(),
                                timestamp = System.currentTimeMillis(),
                                isSampleData = false,
                            ),
                        )
                        showCreateDialog = false
                    },
                    enabled = title.isNotBlank() || reflection.isNotBlank() || gratitude.isNotBlank(),
                    modifier = Modifier.testTag("save_journal_button"),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun JournalSectionLine(label: String, body: String) {
    Column(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}
