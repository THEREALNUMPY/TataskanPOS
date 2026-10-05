package com.tataskan.pos.util

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class DriveBackupWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val success = GoogleDriveBackupManager.performCloudBackup(applicationContext)
        return if (success) Result.success() else Result.retry()
    }
}
