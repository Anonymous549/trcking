package com.example.platform

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.MainActivity
import com.example.R
import java.util.Calendar
import java.util.concurrent.TimeUnit

class ReminderWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val title = inputData.getString(KEY_TITLE) ?: "LIFE OS 365 Check-In"
        val body = inputData.getString(KEY_BODY) ?: "Take a mindful moment to update your habits, priorities, and journal."
        val notifId = inputData.getInt(KEY_ID, 1001)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return Result.success()
        }

        ReminderScheduler.ensureChannel(applicationContext)

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notifId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(notifId, notification)
        return Result.success()
    }

    companion object {
        const val KEY_TITLE = "title"
        const val KEY_BODY = "body"
        const val KEY_ID = "notif_id"
    }
}

class ReminderScheduler(private val context: Context) {

    fun scheduleDailyReminders(
        enabled: Boolean,
        habitHour: Int,
        habitMinute: Int,
        journalHour: Int,
        journalMinute: Int,
    ) {
        val wm = WorkManager.getInstance(context)
        if (!enabled) {
            wm.cancelUniqueWork(WORK_HABIT)
            wm.cancelUniqueWork(WORK_JOURNAL)
            return
        }
        ensureChannel(context)
        scheduleDaily(
            wm = wm,
            uniqueName = WORK_HABIT,
            hour = habitHour,
            minute = habitMinute,
            title = "Morning Habits & Top 3 Priorities",
            body = "Start your day strong — review your routine and check off morning habits.",
            notifId = 1001,
        )
        scheduleDaily(
            wm = wm,
            uniqueName = WORK_JOURNAL,
            hour = journalHour,
            minute = journalMinute,
            title = "Evening Reflection & Progress Review",
            body = "Log your wins, gratitude, and tomorrow's focus in LIFE OS 365.",
            notifId = 1002,
        )
    }

    private fun scheduleDaily(
        wm: WorkManager,
        uniqueName: String,
        hour: Int,
        minute: Int,
        title: String,
        body: String,
        notifId: Int,
    ) {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (before(now)) add(Calendar.DAY_OF_YEAR, 1)
        }
        val initialDelayMillis = (target.timeInMillis - now.timeInMillis).coerceAtLeast(60_000L)
        val request = PeriodicWorkRequestBuilder<ReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(initialDelayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    ReminderWorker.KEY_TITLE to title,
                    ReminderWorker.KEY_BODY to body,
                    ReminderWorker.KEY_ID to notifId,
                ),
            )
            .build()
        wm.enqueueUniquePeriodicWork(uniqueName, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    companion object {
        const val CHANNEL_ID = "life_os_reminders"
        private const val WORK_HABIT = "life_os_habit_reminder"
        private const val WORK_JOURNAL = "life_os_journal_reminder"

        fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Daily Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Offline local reminders for habits, tasks, routine, and journaling"
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
