package com.tataskan.pos.util

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object LocalBackupManager {

    private const val WORK_NAME = "TataskanLocalAutoBackupWork"

    fun scheduleAutoBackup(context: Context, frequency: String = "DAILY") {
        val intervalHours = when (frequency.uppercase()) {
            "WEEKLY" -> 168L // 7 days
            "MONTHLY" -> 720L // 30 days
            else -> 24L // 1 day
        }

        val backupWork = PeriodicWorkRequestBuilder<LocalBackupWorker>(intervalHours, TimeUnit.HOURS)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupWork
        )
    }

    fun cancelAutoBackup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
