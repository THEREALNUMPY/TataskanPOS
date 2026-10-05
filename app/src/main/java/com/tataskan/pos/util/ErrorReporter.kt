package com.tataskan.pos.util

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import org.json.JSONObject

data class CrashReport(
    val exceptionType: String,
    val message: String,
    val stackTrace: String,
    val threadName: String,
    val timestamp: Long = System.currentTimeMillis(),
    val appVersion: String = "2.1.1",
    val deviceModel: String = "${Build.MANUFACTURER} ${Build.MODEL}",
    val androidVersion: String = "Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})",
    val isFatal: Boolean = true
)

object ErrorReporter {

    private const val TAG = "ErrorReporter"
    private const val CRASH_FILE_NAME = "last_crash_report.json"
    private const val COLLECTION_CRASH_REPORTS = "crash_reports"

    fun saveCrashReport(context: Context, throwable: Throwable, threadName: String, isFatal: Boolean = true) {
        try {
            val report = CrashReport(
                exceptionType = throwable.javaClass.name,
                message = throwable.message ?: "No error message provided",
                stackTrace = throwable.stackTraceToString(),
                threadName = threadName,
                isFatal = isFatal
            )

            val json = JSONObject().apply {
                put("exceptionType", report.exceptionType)
                put("message", report.message)
                put("stackTrace", report.stackTrace)
                put("threadName", report.threadName)
                put("timestamp", report.timestamp)
                put("appVersion", report.appVersion)
                put("deviceModel", report.deviceModel)
                put("androidVersion", report.androidVersion)
                put("isFatal", report.isFatal)
            }

            val file = File(context.filesDir, CRASH_FILE_NAME)
            file.writeText(json.toString())
            Log.d(TAG, "Crash report saved locally")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash report locally", e)
        }
    }

    fun getLatestCrashReport(context: Context): CrashReport? {
        return try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (!file.exists()) return null

            val jsonStr = file.readText()
            val json = JSONObject(jsonStr)

            CrashReport(
                exceptionType = json.optString("exceptionType", "Unknown"),
                message = json.optString("message", "Unknown error"),
                stackTrace = json.optString("stackTrace", ""),
                threadName = json.optString("threadName", "main"),
                timestamp = json.optLong("timestamp", System.currentTimeMillis()),
                appVersion = json.optString("appVersion", "2.1.1"),
                deviceModel = json.optString("deviceModel", "Unknown Device"),
                androidVersion = json.optString("androidVersion", "Unknown Android"),
                isFatal = json.optBoolean("isFatal", true)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read crash report", e)
            null
        }
    }

    fun clearCrashReport(context: Context) {
        try {
            val file = File(context.filesDir, CRASH_FILE_NAME)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear crash report file", e)
        }
    }

    fun sendReportToFirebase(
        context: Context,
        report: CrashReport,
        userNotes: String? = null,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        try {
            val db = FirebaseFirestore.getInstance()
            val reportData = hashMapOf(
                "exceptionType" to report.exceptionType,
                "message" to report.message,
                "stackTrace" to report.stackTrace,
                "threadName" to report.threadName,
                "timestamp" to report.timestamp,
                "appVersion" to report.appVersion,
                "deviceModel" to report.deviceModel,
                "androidVersion" to report.androidVersion,
                "isFatal" to report.isFatal,
                "userNotes" to (userNotes ?: ""),
                "reportedAt" to System.currentTimeMillis()
            )

            db.collection(COLLECTION_CRASH_REPORTS)
                .add(reportData)
                .addOnSuccessListener {
                    Log.d(TAG, "Crash report successfully uploaded to Firebase Firestore")
                    try {
                        val analytics = FirebaseAnalytics.getInstance(context)
                        val bundle = Bundle().apply {
                            putString("error_type", report.exceptionType)
                            putString("error_message", report.message.take(100))
                        }
                        analytics.logEvent("app_crash_reported", bundle)
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to log event to Firebase Analytics", e)
                    }
                    clearCrashReport(context)
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Failed to upload crash report to Firebase", e)
                    onError(e.localizedMessage ?: "Failed to send report to Firebase")
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Firebase Firestore for crash reporting", e)
            onError(e.localizedMessage ?: "Firebase error occurred")
        }
    }

    fun reportNonFatalError(context: Context, throwable: Throwable, contextTag: String) {
        try {
            val report = CrashReport(
                exceptionType = throwable.javaClass.name,
                message = "[$contextTag] ${throwable.message ?: "Non-fatal error"}",
                stackTrace = throwable.stackTraceToString(),
                threadName = Thread.currentThread().name,
                isFatal = false
            )
            sendReportToFirebase(context, report, userNotes = "Auto-reported non-fatal error", onSuccess = {}, onError = {})
        } catch (e: Exception) {
            Log.e(TAG, "Failed to auto-report non-fatal error", e)
        }
    }
}
