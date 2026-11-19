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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.BookItemEntity
import com.example.data.local.entity.LearningResourceEntity
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.viewmodel.ReadingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingScreen(
    viewModel: ReadingViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showBookDialog by remember { mutableStateOf(false) }
    var showResourceDialog by remember { mutableStateOf(false) }

    val readingCount = state.books.count { it.status == "READING" }
    val completedCount = state.books.count { it.status == "COMPLETED" }
    val totalPagesRead = state.books.sumOf { it.pagesRead }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Reading & Knowledge") },
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
                onClick = { showBookDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Book")
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
            item(key = "reading_stats") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MetricTile("Currently Reading", "$readingCount", Modifier.weight(1f))
                    MetricTile("Completed", "$completedCount", Modifier.weight(1f))
                    MetricTile("Pages Read", "$totalPagesRead", Modifier.weight(1f))
                }
            }

            item(key = "books_header") {
                SectionHeader(
                    title = "Books & Reading List",
                    subtitle = "Daily reading goal: ${state.dailyReadingGoalPages} pages",
                )
            }

            if (state.books.isEmpty()) {
                item(key = "empty_books") {
                    EmptyStateCard(
                        icon = Icons.Default.AutoStories,
                        title = "Your library is empty",
                        message = "Track books in progress, pages read, and personal takeaways.",
                        actionLabel = "Add Book",
                        onActionClick = { showBookDialog = true },
                    )
                }
            } else {
                items(state.books, key = { "book_${it.id}" }) { book ->
                    val ratio = if (book.totalPages > 0) (book.pagesRead.toFloat() / book.totalPages).coerceIn(0f, 1f) else 0f
                    LifeOsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                StatusBadge(text = book.status.replace("_", " "))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(book.title, style = MaterialTheme.typography.titleMedium)
                                Text(book.author, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text("${book.pagesRead}/${book.totalPages}p", style = MonoMetricSmall)
                            IconButton(onClick = { viewModel.deleteBook(book.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete book")
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(progress = { ratio }, modifier = Modifier.fillMaxWidth().height(6.dp))
                        if (book.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(book.notes, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        if (book.status != "COMPLETED") {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = { viewModel.addPages(book, 10) }) { Text("+10 pages") }
                                OutlinedButton(onClick = { viewModel.addPages(book, 25) }) { Text("+25 pages") }
                            }
                        }
                    }
                }
            }

            item(key = "resources_header") {
                SectionHeader(
                    title = "Knowledge Library & Resources",
                    actionLabel = "+ Resource",
                    onActionClick = { showResourceDialog = true },
                )
            }
            items(state.resources, key = { "res_${it.id}" }) { res ->
                LifeOsCard(onClick = { viewModel.toggleResourceComplete(res) }) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = if (res.isCompleted) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = "Toggle resource",
                            tint = if (res.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(res.title, style = MaterialTheme.typography.titleSmall)
                            Text("${res.resourceType} · ${res.topic}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            if (res.notes.isNotBlank()) {
                                Text(res.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { viewModel.deleteResource(res.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete resource")
                        }
                    }
                }
            }
        }
    }

    if (showBookDialog) {
        var title by remember { mutableStateOf("") }
        var author by remember { mutableStateOf("") }
        var totalPages by remember { mutableStateOf("250") }
        var notes by remember { mutableStateOf("") }
        var status by remember { mutableStateOf("READING") }
        AlertDialog(
            onDismissRequest = { showBookDialog = false },
            title = { Text("Add Book") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Book Title") }, singleLine = true)
                    OutlinedTextField(value = author, onValueChange = { author = it }, label = { Text("Author") }, singleLine = true)
                    OutlinedTextField(value = totalPages, onValueChange = { totalPages = it }, label = { Text("Total Pages") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("READING", "WANT_TO_READ", "COMPLETED").forEach { st ->
                            FilterChip(selected = status == st, onClick = { status = st }, label = { Text(st.replace("_", " ")) })
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Notes / Key Ideas") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val pages = (totalPages.toIntOrNull() ?: 200).coerceAtLeast(1)
                    viewModel.saveBook(
                        BookItemEntity(
                            title = title.trim(),
                            author = author.trim().ifBlank { "Unknown" },
                            totalPages = pages,
                            pagesRead = if (status == "COMPLETED") pages else 0,
                            status = status,
                            notes = notes.trim(),
                        ),
                    )
                    showBookDialog = false
                }, enabled = title.isNotBlank()) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showBookDialog = false }) { Text("Cancel") } },
        )
    }

    if (showResourceDialog) {
        var title by remember { mutableStateOf("") }
        var topic by remember { mutableStateOf("General") }
        var type by remember { mutableStateOf("ARTICLE") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showResourceDialog = false },
            title = { Text("Add Learning Resource") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true)
                    OutlinedTextField(value = topic, onValueChange = { topic = it }, label = { Text("Topic") }, singleLine = true)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ARTICLE", "COURSE", "VIDEO", "NOTE").forEach { t ->
                            FilterChip(selected = type == t, onClick = { type = t }, label = { Text(t) })
                        }
                    }
                    OutlinedTextField(value = notes, onValueChange = { notes = it }, label = { Text("Summary / Notes") }, minLines = 2)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveResource(
                        LearningResourceEntity(
                            title = title.trim(),
                            topic = topic.trim().ifBlank { "General" },
                            resourceType = type,
                            notes = notes.trim(),
                        ),
                    )
                    showResourceDialog = false
                }, enabled = title.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showResourceDialog = false }) { Text("Cancel") } },
        )
    }
}
