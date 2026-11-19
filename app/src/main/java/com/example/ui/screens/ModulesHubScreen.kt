package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.components.LifeOsCard
import com.example.ui.components.PrivacyBanner
import com.example.ui.components.SectionHeader
import com.example.ui.theme.LifeOsThemeTokens

private data class ModuleHubEntry(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val tag: String,
    val onClick: () -> Unit,
)

@Composable
fun ModulesHubScreen(
    onOpenFitness: () -> Unit,
    onOpenFinance: () -> Unit,
    onOpenGoals: () -> Unit,
    onOpenJournal: () -> Unit,
    onOpenReading: () -> Unit,
    onOpenDiscipline: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val entries = listOf(
        ModuleHubEntry("Fitness & Health", "Workouts, steps, water, nutrition & sleep", Icons.Default.FitnessCenter, "module_fitness", onOpenFitness),
        ModuleHubEntry("Personal Finance", "Income, expenses, budgets & savings goals", Icons.Default.AccountBalanceWallet, "module_finance", onOpenFinance),
        ModuleHubEntry("Goals & Vision", "Daily to long-term goals & milestones", Icons.Default.Flag, "module_goals", onOpenGoals),
        ModuleHubEntry("Private Journal", "Encrypted reflections, mood & gratitude", Icons.Default.MenuBook, "module_journal", onOpenJournal),
        ModuleHubEntry("Reading & Knowledge", "Books, pages progress & learning vault", Icons.Default.AutoStories, "module_reading", onOpenReading),
        ModuleHubEntry("Digital Discipline", "Screen time, social limits & UsageStats", Icons.Default.PhoneAndroid, "module_discipline", onOpenDiscipline),
        ModuleHubEntry("Settings & Vault", "Backup JSON/CSV, biometric lock & privacy", Icons.Default.Settings, "module_settings", onOpenSettings),
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "hub_header") {
            SectionHeader(
                title = "Life Modules",
                subtitle = "100% offline · All personal records stored locally on your device",
            )
        }

        entries.chunked(2).forEachIndexed { rowIdx, pair ->
            item(key = "row_$rowIdx") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    pair.forEach { item ->
                        ModuleHubTile(
                            entry = item,
                            accent = if (rowIdx % 2 == 0) MaterialTheme.colorScheme.primary else LifeOsThemeTokens.colors.accent,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (pair.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }

        item(key = "privacy_footer") {
            PrivacyBanner(
                title = "Your Data, Your Control",
                body = "No accounts, no cloud sync, no ads, and no internet permission. Journal and financial notes are encrypted using Android Keystore.",
                icon = Icons.Default.Shield,
            )
        }
    }
}

@Composable
private fun ModuleHubTile(
    entry: ModuleHubEntry,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    LifeOsCard(
        modifier = modifier.testTag(entry.tag),
        onClick = entry.onClick,
    ) {
        Icon(
            imageVector = entry.icon,
            contentDescription = entry.title,
            tint = accent,
            modifier = Modifier.size(28.dp),
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = entry.title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = entry.subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            minLines = 2,
        )
    }
}
