package com.dc.checkinbb.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.*
import java.util.concurrent.TimeUnit

class UpdateWidgetWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            FeedingWidget().updateAll(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "feeding_widget_update"

        /** Encola actualización inmediata del widget (one-shot) */
        fun enqueueOnce(context: Context) {
            val request = OneTimeWorkRequestBuilder<UpdateWidgetWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }

        /** Encola actualización periódica cada 15 minutos */
        fun enqueuePeriodic(context: Context) {
            val request = PeriodicWorkRequestBuilder<UpdateWidgetWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().build())
                .build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
