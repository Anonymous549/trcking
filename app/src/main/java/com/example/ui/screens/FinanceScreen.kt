package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WarningAmber
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.local.entity.SavingsGoalEntity
import com.example.domain.util.DateUtils
import com.example.ui.components.CategoryDonutChart
import com.example.ui.components.DonutSlice
import com.example.ui.components.EmptyStateCard
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.PrivacyBanner
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.theme.MonoMetricSmall
import com.example.ui.viewmodel.FinanceViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceScreen(
    viewModel: FinanceViewModel,
    onBack: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sym = state.prefs.currencySymbol
    val code = state.prefs.currencyCode
    var showTxDialog by remember { mutableStateOf(false) }
    var showCatDialog by remember { mutableStateOf(false) }
    var showSavingsDialog by remember { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text("Personal Finance") },
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
                onClick = { showTxDialog = true },
                modifier = Modifier.testTag("add_transaction_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add transaction")
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
            item(key = "local_finance_note") {
                PrivacyBanner(
                    title = "100% Local Ledger ($code $sym)",
                    body = "Zero bank connections or credentials. All calculations run offline and private notes are AES-GCM encrypted.",
                    icon = Icons.Default.Lock,
                )
            }

            state.summary?.let { summary ->
                item(key = "finance_summary") {
                    LifeOsCard {
                        SectionHeader(title = "Monthly Financial Summary")
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(
                                label = "Income",
                                value = DateUtils.formatCurrency(summary.totalIncome, sym, code),
                                modifier = Modifier.weight(1f),
                                accentColor = LifeOsThemeTokens.colors.positive,
                            )
                            MetricTile(
                                label = "Expenses",
                                value = DateUtils.formatCurrency(summary.totalExpense, sym, code),
                                modifier = Modifier.weight(1f),
                                accentColor = LifeOsThemeTokens.colors.accent,
                            )
                            MetricTile(
                                label = "Savings",
                                value = "${summary.savingsRatePercent}%",
                                subtitle = DateUtils.formatCurrency(summary.netSavings, sym, code),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }

                if (summary.warnings.isNotEmpty()) {
                    item(key = "budget_warnings") {
                        LifeOsCard(containerColor = LifeOsThemeTokens.colors.warningContainer.copy(alpha = 0.65f)) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.WarningAmber, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                Text("Budget Alerts", style = MaterialTheme.typography.titleSmall)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            summary.warnings.forEach { w ->
                                val status = if (w.isExceeded) "Exceeded" else "Approaching limit"
                                Text(
                                    text = "• ${w.category.name}: ${DateUtils.formatCurrency(w.spent, sym, code)} / ${DateUtils.formatCurrency(w.category.monthlyBudget, sym, code)} ($status)",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                        }
                    }
                }

                item(key = "spending_chart") {
                    val palette = LifeOsThemeTokens.colors
                    val sliceColors = listOf(
                        palette.chartPrimary,
                        palette.chartSecondary,
                        palette.chartTertiary,
                        palette.chartQuaternary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.6f),
                    )
                    val slices = summary.categoryBreakdown.filter { it.spent > 0.0 }.mapIndexed { idx, item ->
                        DonutSlice(item.category.name, item.spent.toFloat(), sliceColors[idx % sliceColors.size])
                    }
                    LifeOsCard {
                        SectionHeader(
                            title = "Spending Breakdown",
                            actionLabel = "+ Category",
                            onActionClick = { showCatDialog = true },
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        CategoryDonutChart(
                            slices = slices,
                            centerValue = DateUtils.formatCurrency(summary.totalExpense, sym, code),
                            centerSubtitle = "This Month",
                        )
                    }
                }
            }

            item(key = "savings_header") {
                SectionHeader(
                    title = "Savings Goals",
                    actionLabel = "+ Goal",
                    onActionClick = { showSavingsDialog = true },
                )
            }
            items(state.savingsGoals, key = { "sg_${it.id}" }) { sg ->
                val pct = ((sg.currentAmount / sg.targetAmount.coerceAtLeast(1.0)) * 100).toInt().coerceIn(0, 100)
                LifeOsCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(sg.title, style = MaterialTheme.typography.titleSmall)
                            Text(
                                "${DateUtils.formatCurrency(sg.currentAmount, sym, code)} / ${DateUtils.formatCurrency(sg.targetAmount, sym, code)} ($pct%)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        OutlinedButton(onClick = { viewModel.contributeToSavings(sg, 1000.0) }) {
                            Text("+1k")
                        }
                        IconButton(onClick = { viewModel.deleteSavingsGoal(sg.id) }) {
                            Icon(Icons.Default.DeleteOutline, contentDescription = "Delete savings goal")
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(progress = { pct / 100f }, modifier = Modifier.fillMaxWidth().height(6.dp))
                }
            }

            item(key = "tx_header") {
                SectionHeader(title = "Transaction History")
            }
            if (state.transactions.isEmpty()) {
                item(key = "empty_tx") {
                    EmptyStateCard(
                        icon = Icons.Default.AccountBalanceWallet,
                        title = "No transactions yet",
                        message = "Record income and expenses offline to calculate monthly savings.",
                        actionLabel = "Add Transaction",
                        onActionClick = { showTxDialog = true },
                    )
                }
            } else {
                val catMap = state.categories.associateBy { it.id }
                items(state.transactions, key = { "tx_${it.id}" }) { tx ->
                    val catName = catMap[tx.categoryId]?.name ?: "General"
                    LifeOsCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(tx.title, style = MaterialTheme.typography.titleSmall)
                                Text(
                                    "${DateUtils.formatShortDate(tx.epochDay)} · $catName",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                if (tx.note.isNotBlank()) {
                                    Text(tx.note, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(
                                text = (if (tx.type == "INCOME") "+" else "-") + DateUtils.formatCurrency(tx.amount, sym, code),
                                style = MonoMetricSmall,
                                color = if (tx.type == "INCOME") LifeOsThemeTokens.colors.positive else LifeOsThemeTokens.colors.accent,
                            )
                            IconButton(onClick = { viewModel.deleteTransaction(tx.id) }) {
                                Icon(Icons.Default.DeleteOutline, contentDescription = "Delete transaction")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showTxDialog) {
        var title by remember { mutableStateOf("") }
        var amountText by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("EXPENSE") }
        var note by remember { mutableStateOf("") }
        val matchingCats = state.categories.filter { it.type == type }
        var selectedCatId by remember(type) { mutableStateOf(matchingCats.firstOrNull()?.id ?: 1L) }

        AlertDialog(
            onDismissRequest = { showTxDialog = false },
            title = { Text("Add Transaction") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = type == "EXPENSE", onClick = { type = "EXPENSE" }, label = { Text("Expense") })
                        FilterChip(selected = type == "INCOME", onClick = { type = "INCOME" }, label = { Text("Income") })
                    }
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Title") }, singleLine = true, modifier = Modifier.testTag("tx_title_input"))
                    OutlinedTextField(value = amountText, onValueChange = { amountText = it }, label = { Text("Amount ($sym)") }, singleLine = true, modifier = Modifier.testTag("tx_amount_input"))
                    Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        matchingCats.forEach { c ->
                            FilterChip(selected = selectedCatId == c.id, onClick = { selectedCatId = c.id }, label = { Text(c.name) })
                        }
                    }
                    OutlinedTextField(value = note, onValueChange = { note = it }, label = { Text("Private Note (encrypted)") }, singleLine = true)
                }
            },
            confirmButton = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                Button(
                    onClick = {
                        viewModel.addTransaction(selectedCatId, title, amt, type, note)
                        showTxDialog = false
                    },
                    enabled = title.isNotBlank() && amt > 0.0,
                    modifier = Modifier.testTag("save_tx_button"),
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showTxDialog = false }) { Text("Cancel") } },
        )
    }

    if (showCatDialog) {
        var name by remember { mutableStateOf("") }
        var budget by remember { mutableStateOf("2000") }
        AlertDialog(
            onDismissRequest = { showCatDialog = false },
            title = { Text("New Expense Category") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Category Name") }, singleLine = true)
                    OutlinedTextField(value = budget, onValueChange = { budget = it }, label = { Text("Monthly Budget ($sym)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.saveCategory(name, "EXPENSE", budget.toDoubleOrNull() ?: 0.0)
                    showCatDialog = false
                }, enabled = name.isNotBlank()) { Text("Add") }
            },
            dismissButton = { TextButton(onClick = { showCatDialog = false }) { Text("Cancel") } },
        )
    }

    if (showSavingsDialog) {
        var title by remember { mutableStateOf("") }
        var target by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSavingsDialog = false },
            title = { Text("New Savings Goal") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Goal Name") }, singleLine = true)
                    OutlinedTextField(value = target, onValueChange = { target = it }, label = { Text("Target Amount ($sym)") }, singleLine = true)
                }
            },
            confirmButton = {
                val tAmt = target.toDoubleOrNull() ?: 0.0
                Button(onClick = {
                    viewModel.saveSavingsGoal(SavingsGoalEntity(title = title.trim(), targetAmount = tAmt))
                    showSavingsDialog = false
                }, enabled = title.isNotBlank() && tAmt > 0.0) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSavingsDialog = false }) { Text("Cancel") } },
        )
    }
}
