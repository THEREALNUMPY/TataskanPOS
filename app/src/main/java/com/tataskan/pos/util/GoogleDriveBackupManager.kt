package com.tataskan.pos.util

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.repository.BackupRepository
import com.tataskan.pos.data.settings.SettingsRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

object GoogleDriveBackupManager {

    private const val WORK_NAME = "TataskanDriveAutoBackupWork"
    val DRIVE_APPDATA_SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")

    fun getSignInClient(context: Context) = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_APPDATA_SCOPE)
            .build()
    )

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun scheduleAutoBackup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val backupWork = PeriodicWorkRequestBuilder<DriveBackupWorker>(24, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            backupWork
        )
    }

    suspend fun performCloudBackup(context: Context): Boolean = withContext(Dispatchers.IO) {
        try {
            val app = context.applicationContext as TataskanApplication
            val repository = BackupRepository(context, app.database)
            val settingsRepository = SettingsRepository(context)

            val backupFile: File? = repository.exportToInternalStorage()
            if (backupFile != null && backupFile.exists()) {
                val now = System.currentTimeMillis()
                settingsRepository.updateLastDriveBackupTime(now)
                Log.i("GoogleDriveBackupManager", "Auto backup created successfully: ${backupFile.name}")
                return@withContext true
            }
            false
        } catch (e: Exception) {
            Log.e("GoogleDriveBackupManager", "Cloud backup error", e)
            false
        }
    }
}
