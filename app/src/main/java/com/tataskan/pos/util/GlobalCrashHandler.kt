package com.tataskan.pos.util

import android.content.Context
import android.content.Intent
import android.os.Process
import android.util.Log
import com.tataskan.pos.ui.error.ErrorActivity
import kotlin.system.exitProcess

class GlobalCrashHandler private constructor(
    private val context: Context,
    private val defaultHandler: Thread.UncaughtExceptionHandler?
) : Thread.UncaughtExceptionHandler {

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        Log.e(TAG, "Uncaught exception in thread ${thread.name}", throwable)

        try {
            // Save report locally
            ErrorReporter.saveCrashReport(context, throwable, thread.name, isFatal = true)

            // Launch ErrorActivity
            val intent = Intent(context, ErrorActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(ErrorActivity.EXTRA_CRASH_OCCURRED, true)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Error handling uncaught exception", e)
            defaultHandler?.uncaughtException(thread, throwable)
            return
        }

        // Kill process cleanly
        Process.killProcess(Process.myPid())
        exitProcess(10)
    }

    companion object {
        private const val TAG = "GlobalCrashHandler"

        fun install(context: Context) {
            val currentHandler = Thread.getDefaultUncaughtExceptionHandler()
            if (currentHandler !is GlobalCrashHandler) {
                val handler = GlobalCrashHandler(context.applicationContext, currentHandler)
                Thread.setDefaultUncaughtExceptionHandler(handler)
                Log.i(TAG, "Global crash handler successfully installed")
            }
        }
    }
}
