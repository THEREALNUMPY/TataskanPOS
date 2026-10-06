package com.tataskan.pos.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.repository.BackupRepository
import com.tataskan.pos.data.settings.SettingsRepository

class LocalBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as TataskanApplication
            val repository = BackupRepository(applicationContext, app.database)
            val settingsRepository = SettingsRepository(applicationContext)

            val backupFile = repository.performLocalAutoBackup()
            if (backupFile != null && backupFile.exists()) {
                settingsRepository.updateLastLocalBackupTime(System.currentTimeMillis())
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.retry()
        }
    }
}
