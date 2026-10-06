package com.tataskan.pos.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.repository.BackupRepository
import com.tataskan.pos.data.settings.SettingsRepository
import com.google.android.gms.auth.GoogleAuthUtil
import com.google.android.gms.auth.UserRecoverableAuthException
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed interface CloudBackupResult {
    data object Success : CloudBackupResult
    data class Failure(val message: String) : CloudBackupResult
    data class RequiresConsent(val consentIntent: Intent) : CloudBackupResult
}

data class DriveUploadResult(
    val isSuccess: Boolean,
    val statusCode: Int = 0,
    val errorMessage: String? = null
)

object GoogleDriveBackupManager {

    private const val WORK_NAME = "TataskanDriveAutoBackupWork"
    val DRIVE_APPDATA_SCOPE = Scope("https://www.googleapis.com/auth/drive.appdata")
    val DRIVE_FILE_SCOPE = Scope("https://www.googleapis.com/auth/drive.file")

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun getSignInClient(context: Context) = GoogleSignIn.getClient(
        context,
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestEmail()
            .requestScopes(DRIVE_APPDATA_SCOPE, DRIVE_FILE_SCOPE)
            .build()
    )

    fun getLastSignedInAccount(context: Context): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    fun scheduleAutoBackup(context: Context, frequency: String = "DAILY") {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val intervalHours = when (frequency.uppercase()) {
            "WEEKLY" -> 168L // 7 days
            "MONTHLY" -> 720L // 30 days
            else -> 24L // 1 day
        }

        val backupWork = PeriodicWorkRequestBuilder<DriveBackupWorker>(intervalHours, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            backupWork
        )
    }

    suspend fun performCloudBackupResult(context: Context): CloudBackupResult = withContext(Dispatchers.IO) {
        try {
            val app = context.applicationContext as TataskanApplication
            val repository = BackupRepository(context, app.database)
            val settingsRepository = SettingsRepository(context)

            // 1. Resolve linked Google Account
            val account = getLastSignedInAccount(context)
            val savedEmail = settingsRepository.googleDriveAccount.first()

            val targetEmail = account?.email ?: savedEmail

            if (targetEmail.isNullOrBlank()) {
                Log.e("GoogleDriveBackupManager", "Cloud backup aborted: No Google Account linked.")
                return@withContext CloudBackupResult.Failure("No Google account linked. Please link a Google account in Settings.")
            }

            val accountObj: android.accounts.Account = account?.account
                ?: android.accounts.Account(targetEmail, "com.google")

            // 2. Fetch OAuth Token
            val scopeStr = "oauth2:https://www.googleapis.com/auth/drive.file https://www.googleapis.com/auth/drive.appdata"
            var accessToken: String? = null

            try {
                accessToken = GoogleAuthUtil.getToken(context, accountObj, scopeStr)
            } catch (e: UserRecoverableAuthException) {
                Log.w("GoogleDriveBackupManager", "User consent required for Google Drive access.", e)
                val intent = e.intent
                return@withContext if (intent != null) {
                    CloudBackupResult.RequiresConsent(intent)
                } else {
                    CloudBackupResult.Failure("Google Drive permission required.")
                }
            } catch (e: Exception) {
                Log.e("GoogleDriveBackupManager", "OAuth token retrieval exception: ${e.localizedMessage}", e)
                return@withContext CloudBackupResult.Failure("OAuth token retrieval failed: ${e.localizedMessage}")
            }

            if (accessToken.isNullOrBlank()) {
                Log.e("GoogleDriveBackupManager", "Cloud backup failed: Access token is null or blank.")
                return@withContext CloudBackupResult.Failure("Unable to obtain Google Drive access token.")
            }

            // 3. Export local backup archive
            val backupFile: File? = repository.exportToInternalStorage()
            if (backupFile == null || !backupFile.exists()) {
                Log.e("GoogleDriveBackupManager", "Cloud backup failed: Could not create local backup ZIP archive.")
                return@withContext CloudBackupResult.Failure("Could not create local backup archive.")
            }

            // 4. Upload file to Google Drive
            var uploadResult = uploadFileToGoogleDriveFolder(backupFile, accessToken)

            // If upload failed with HTTP 401 Unauthorized, clear cached token & retry once
            if (uploadResult.statusCode == 401) {
                Log.w("GoogleDriveBackupManager", "HTTP 401 received. Invalidating cached OAuth token and retrying...")
                try {
                    GoogleAuthUtil.clearToken(context, accessToken)
                    accessToken = GoogleAuthUtil.getToken(context, accountObj, scopeStr)
                    if (!accessToken.isNullOrBlank()) {
                        uploadResult = uploadFileToGoogleDriveFolder(backupFile, accessToken)
                    }
                } catch (e: UserRecoverableAuthException) {
                    val intent = e.intent
                    if (intent != null) {
                        return@withContext CloudBackupResult.RequiresConsent(intent)
                    }
                } catch (retryEx: Exception) {
                    Log.e("GoogleDriveBackupManager", "Token refresh/retry failed: ${retryEx.localizedMessage}", retryEx)
                }
            }

            if (uploadResult.isSuccess) {
                val now = System.currentTimeMillis()
                settingsRepository.updateLastDriveBackupTime(now)
                Log.i("GoogleDriveBackupManager", "Cloud backup created and successfully uploaded to Google Drive: ${backupFile.name}")
                return@withContext CloudBackupResult.Success
            } else {
                val errMsg = uploadResult.errorMessage ?: "Upload to Google Drive failed."
                Log.e("GoogleDriveBackupManager", "Cloud backup upload failed: $errMsg")
                return@withContext CloudBackupResult.Failure(errMsg)
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveBackupManager", "Cloud backup exception", e)
            return@withContext CloudBackupResult.Failure(e.localizedMessage ?: "Cloud backup exception")
        }
    }

    suspend fun performCloudBackup(context: Context): Boolean {
        return performCloudBackupResult(context) is CloudBackupResult.Success
    }

    private fun uploadFileToGoogleDriveFolder(backupFile: File, authToken: String): DriveUploadResult {
        try {
            val folderName = "TataskanPOS Backups"
            val authHeader = "Bearer $authToken"

            // 1. Search for existing folder
            val query = "mimeType='application/vnd.google-apps.folder' and name='$folderName' and trashed=false"
            val searchUrl = "https://www.googleapis.com/drive/v3/files?q=${URLEncoder.encode(query, "UTF-8")}"
            
            val searchRequest = Request.Builder()
                .url(searchUrl)
                .addHeader("Authorization", authHeader)
                .get()
                .build()

            var folderId: String? = null
            var lastCode = 0
            var lastError: String? = null

            httpClient.newCall(searchRequest).execute().use { response ->
                lastCode = response.code
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: ""
                    val json = JSONObject(bodyStr)
                    val files = json.optJSONArray("files")
                    if (files != null && files.length() > 0) {
                        folderId = files.getJSONObject(0).getString("id")
                    }
                } else {
                    lastError = "Folder search failed HTTP ${response.code}: ${response.message}"
                    Log.e("GoogleDriveBackupManager", lastError)
                    if (response.code == 401) {
                        return DriveUploadResult(isSuccess = false, statusCode = 401, errorMessage = lastError)
                    }
                }
            }

            // 2. Create folder if not found
            if (folderId == null) {
                val createFolderJson = JSONObject().apply {
                    put("name", folderName)
                    put("mimeType", "application/vnd.google-apps.folder")
                }
                val createRequest = Request.Builder()
                    .url("https://www.googleapis.com/drive/v3/files")
                    .addHeader("Authorization", authHeader)
                    .post(createFolderJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType()))
                    .build()

                httpClient.newCall(createRequest).execute().use { response ->
                    lastCode = response.code
                    if (response.isSuccessful) {
                        val bodyStr = response.body?.string() ?: ""
                        folderId = JSONObject(bodyStr).getString("id")
                    } else {
                        lastError = "Folder creation failed HTTP ${response.code}: ${response.message}"
                        Log.e("GoogleDriveBackupManager", lastError)
                        if (response.code == 401) {
                            return DriveUploadResult(isSuccess = false, statusCode = 401, errorMessage = lastError)
                        }
                    }
                }
            }

            // 3. Upload file inside 'TataskanPOS Backups' folder
            if (folderId != null) {
                val metadataJson = JSONObject().apply {
                    put("name", backupFile.name)
                    put("parents", JSONArray().put(folderId))
                }

                val metadataPart = metadataJson.toString().toRequestBody("application/json; charset=UTF-8".toMediaType())
                val filePart = backupFile.asRequestBody("application/octet-stream".toMediaType())

                val multipartBody = MultipartBody.Builder()
                    .setType("multipart/related".toMediaType())
                    .addPart(metadataPart)
                    .addPart(filePart)
                    .build()

                val uploadRequest = Request.Builder()
                    .url("https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart")
                    .addHeader("Authorization", authHeader)
                    .post(multipartBody)
                    .build()

                httpClient.newCall(uploadRequest).execute().use { response ->
                    if (response.isSuccessful) {
                        Log.i("GoogleDriveBackupManager", "File successfully uploaded to '$folderName' folder on Google Drive!")
                        return DriveUploadResult(isSuccess = true, statusCode = response.code)
                    } else {
                        val errBody = response.body?.string() ?: ""
                        lastError = "Upload failed HTTP ${response.code} ${response.message}: $errBody"
                        Log.e("GoogleDriveBackupManager", lastError)
                        return DriveUploadResult(isSuccess = false, statusCode = response.code, errorMessage = lastError)
                    }
                }
            } else {
                return DriveUploadResult(isSuccess = false, statusCode = lastCode, errorMessage = lastError ?: "Could not resolve or create Google Drive folder.")
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveBackupManager", "Exception during Google Drive upload", e)
            return DriveUploadResult(isSuccess = false, errorMessage = e.localizedMessage ?: "Upload exception")
        }
    }
}
