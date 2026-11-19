package com.example.platform

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Process
import android.provider.Settings
import java.util.Calendar
import java.util.concurrent.TimeUnit

data class DeviceUsageSnapshot(
    val totalMinutes: Int,
    val socialMediaMinutes: Int,
    val topAppsSummary: String,
)

class UsageStatsHelper(private val context: Context) {

    private val socialKeywords = listOf(
        "instagram", "facebook", "katana", "twitter", "android.gm", "reddit",
        "snapchat", "musically", "tiktok", "youtube", "whatsapp", "threads",
    )

    @Suppress("DEPRECATION")
    fun hasUsageStatsPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        } else {
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun buildUsageAccessSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }

    fun queryTodayUsage(): DeviceUsageSnapshot? {
        if (!hasUsageStatsPermission()) return null
        val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val end = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end).orEmpty()
        if (stats.isEmpty()) return DeviceUsageSnapshot(0, 0, "No foreground usage recorded today")

        val mergedByPkg = mutableMapOf<String, Long>()
        stats.forEach { s ->
            if (s.totalTimeInForeground > 0L) {
                mergedByPkg[s.packageName] = (mergedByPkg[s.packageName] ?: 0L) + s.totalTimeInForeground
            }
        }
        val totalMs = mergedByPkg.values.sum()
        val socialMs = mergedByPkg.entries
            .filter { entry -> socialKeywords.any { kw -> entry.key.lowercase().contains(kw) } }
            .sumOf { it.value }

        val topSummary = mergedByPkg.entries
            .sortedByDescending { it.value }
            .take(3)
            .joinToString(", ") { entry ->
                val shortName = entry.key.substringAfterLast('.').replaceFirstChar { it.uppercase() }
                val mins = TimeUnit.MILLISECONDS.toMinutes(entry.value)
                "$shortName (${mins}m)"
            }

        return DeviceUsageSnapshot(
            totalMinutes = TimeUnit.MILLISECONDS.toMinutes(totalMs).toInt(),
            socialMediaMinutes = TimeUnit.MILLISECONDS.toMinutes(socialMs).toInt(),
            topAppsSummary = topSummary.ifBlank { "Minimal usage recorded" },
        )
    }
}
