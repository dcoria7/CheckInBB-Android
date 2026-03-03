package com.dc.checkinbb.workers

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedingNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleFeeding(
        intervalHours: Double,
        windowMinHours: Double,
        windowMaxHours: Double,
        timeSinceLastFeedingMs: Long
    ) {
        cancelAll()

        val intervalMs = (intervalHours * 3600 * 1000).toLong()
        val windowMinMs = (windowMinHours * 3600 * 1000).toLong()

        val remainingIntervalMs = intervalMs - timeSinceLastFeedingMs
        val remainingWindowMinMs = windowMinMs - timeSinceLastFeedingMs

        if (windowMinHours > 0 && remainingWindowMinMs > 0) {
            val triggerAtMillis = System.currentTimeMillis() + remainingWindowMinMs
            scheduleAlarm(
                triggerAtMillis,
                ID_FEEDING_WINDOW,
                "🍼 Ventana de alimentación",
                "Tu bebé puede pedir su biberón pronto",
                false
            )
        }

        if (intervalHours > 0 && remainingIntervalMs > 0) {
            val triggerAtMillis = System.currentTimeMillis() + remainingIntervalMs
            scheduleAlarm(
                triggerAtMillis,
                ID_FEEDING_OPTIMAL,
                "🍼 Hora de alimentar",
                "Es el momento ideal para la siguiente toma",
                true
            )
        }
    }

    private fun scheduleAlarm(triggerAtMillis: Long, id: Int, title: String, message: String, isCritical: Boolean) {
        val intent = Intent(context, FeedingAlarmReceiver::class.java).apply {
            putExtra(FeedingAlarmReceiver.KEY_TITLE, title)
            putExtra(FeedingAlarmReceiver.KEY_MESSAGE, message)
            putExtra(FeedingAlarmReceiver.KEY_IS_CRITICAL, isCritical)
            putExtra(FeedingAlarmReceiver.KEY_NOTIFICATION_ID, id)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (e: SecurityException) {
            // Fallback in case permission is revoked somehow
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelAll() {
        cancelAlarm(ID_FEEDING_WINDOW)
        cancelAlarm(ID_FEEDING_OPTIMAL)
    }

    private fun cancelAlarm(id: Int) {
        val intent = Intent(context, FeedingAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        alarmManager.cancel(pendingIntent)
    }

    companion object {
        const val ID_FEEDING_WINDOW = 1001
        const val ID_FEEDING_OPTIMAL = 1002
    }
}
