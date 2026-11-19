package com.example.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.util.DateUtils
import com.example.ui.components.CalendarHeatmapGrid
import com.example.ui.components.CompletionRingCanvas
import com.example.ui.components.LifeOsCard
import com.example.ui.components.MetricTile
import com.example.ui.components.SectionHeader
import com.example.ui.components.TrendLineChart
import com.example.ui.components.WeeklyBarChart
import com.example.ui.theme.LifeOsThemeTokens
import com.example.ui.viewmodel.AnalyticsViewModel
import com.example.ui.viewmodel.ComparisonMode

@Composable
fun AnalyticsScreen(viewModel: AnalyticsViewModel) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showFormulaConfig by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .widthIn(max = 760.dp),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item(key = "analytics_header") {
            SectionHeader(
                title = "Personal Analytics",
                subtitle = "Transparent local calculations · Rest days protected",
                actionLabel = "Configure",
                onActionClick = { showFormulaConfig = true },
            )
        }

        // Daily Productivity Score Breakdown
        item(key = "daily_score") {
            val b = state.todayBreakdown
            LifeOsCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    CompletionRingCanvas(
                        progress = b.totalScore / 100f,
                        centerLabel = "${b.totalScore}",
                        size = 88.dp,
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Daily Productivity Score", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Habits ${b.habitScore}% · Tasks ${b.taskScore}% · Study ${b.studyScore}% · Fitness ${b.fitnessScore}% · Discipline ${b.disciplineScore}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = if (state.prefs.exemptRestDays) "Rest-day exemption: Active" else "Rest-day exemption: Off",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }

        // Period Comparison (Week vs Week / Month vs Month)
        item(key = "period_comparison") {
            LifeOsCard {
                SectionHeader(
                    title = "Period Comparison",
                    subtitle = if (state.comparisonMode == ComparisonMode.WEEK) "This 7 days vs previous 7 days" else "This 30 days vs previous 30 days",
                )
                Spacer(modifier = Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    ComparisonMode.entries.forEachIndexed { idx, mode ->
                        SegmentedButton(
                            selected = state.comparisonMode == mode,
                            onClick = { viewModel.setComparisonMode(mode) },
                            shape = SegmentedButtonDefaults.itemShape(idx, ComparisonMode.entries.size),
                        ) {
                            Text(if (mode == ComparisonMode.WEEK) "Weekly" else "Monthly")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                state.comparison?.let { comp ->
                    val curr = comp.current
                    val prev = comp.previous
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(
                                label = "Productivity",
                                value = "${curr.avgProductivityScore} pts",
                                subtitle = deltaLabel(curr.avgProductivityScore - prev.avgProductivityScore, "pts"),
                                modifier = Modifier.weight(1f),
                            )
                            MetricTile(
                                label = "Habits",
                                value = "${curr.habitConsistencyPercent}%",
                                subtitle = deltaLabel(curr.habitConsistencyPercent - prev.habitConsistencyPercent, "%"),
                                modifier = Modifier.weight(1f),
                                accentColor = LifeOsThemeTokens.colors.positive,
                            )
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MetricTile(
                                label = "Study Time",
                                value = DateUtils.formatMinutesReadable(curr.totalStudyMinutes),
                                subtitle = "Prev: ${DateUtils.formatMinutesReadable(prev.totalStudyMinutes)}",
                                modifier = Modifier.weight(1f),
                            )
                            MetricTile(
                                label = "Workouts",
                                value = "${curr.totalWorkouts}",
                                subtitle = "Prev: ${prev.totalWorkouts} · Goals: ${state.completedGoalsCount}/${state.activeGoalsCount + state.completedGoalsCount}",
                                modifier = Modifier.weight(1f),
                                accentColor = LifeOsThemeTokens.colors.accent,
                            )
                        }
                        MetricTile(
                            label = "Net Savings (Period)",
                            value = DateUtils.formatCurrency(curr.netSavings, state.prefs.currencySymbol, state.prefs.currencyCode),
                            subtitle = "Previous period: ${DateUtils.formatCurrency(prev.netSavings, state.prefs.currencySymbol, state.prefs.currencyCode)}",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }

        // 14-Day Personal Growth Trend
        item(key = "growth_trend") {
            LifeOsCard {
                SectionHeader(title = "14-Day Personal Growth Trend", subtitle = "Daily balanced score over the past 2 weeks")
                Spacer(modifier = Modifier.height(10.dp))
                TrendLineChart(values = state.trendScores, labels = state.trendLabels)
            }
        }

        // Study Hours Bar Chart
        item(key = "study_chart") {
            LifeOsCard {
                SectionHeader(title = "Study Hours (Last 7 Days)")
                Spacer(modifier = Modifier.height(10.dp))
                WeeklyBarChart(points = state.studyBars)
            }
        }

        // 12-Week Holistic Heatmap
        item(key = "life_heatmap") {
            LifeOsCard {
                SectionHeader(title = "12-Week Life Consistency Heatmap")
                Spacer(modifier = Modifier.height(10.dp))
                CalendarHeatmapGrid(days = state.heatmapDays)
            }
        }
    }

    if (showFormulaConfig) {
        val w = state.prefs.analyticsWeights
        var h by remember { mutableIntStateOf(w.habitWeight) }
        var t by remember { mutableIntStateOf(w.taskWeight) }
        var s by remember { mutableIntStateOf(w.studyWeight) }
        var f by remember { mutableIntStateOf(w.fitnessWeight) }
        var d by remember { mutableIntStateOf(w.disciplineWeight) }
        var exempt by remember { mutableStateOf(w.exemptRestDays) }

        AlertDialog(
            onDismissRequest = { showFormulaConfig = false },
            title = { Text("Transparent Score Formula") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "Adjust pillar weights. Rest-day exemption prevents penalizing intentional recovery.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    WeightSliderRow("Habits ($h)", h) { h = it }
                    WeightSliderRow("Tasks ($t)", t) { t = it }
                    WeightSliderRow("Study ($s)", s) { s = it }
                    WeightSliderRow("Fitness ($f)", f) { f = it }
                    WeightSliderRow("Discipline ($d)", d) { d = it }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                        Text("Exempt rest days", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = exempt, onCheckedChange = { exempt = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.updateAnalyticsWeights(w.copy(h, t, s, f, d, exempt))
                    showFormulaConfig = false
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showFormulaConfig = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun WeightSliderRow(label: String, value: Int, onChange: (Int) -> Unit) {
    Column {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..50f,
        )
    }
}

private fun deltaLabel(delta: Int, unit: String): String {
    return when {
        delta > 0 -> "+$delta $unit vs prev"
        delta < 0 -> "$delta $unit vs prev"
        else -> "Same as prev"
    }
}
