package com.tataskan.pos.util

import android.content.Context
import android.os.Environment
import android.provider.Settings
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object DeviceRedemptionManager {

    private const val TAG = "DeviceRedemptionManager"
    private const val FILE_NAME = ".tataskan_redemptions.json"

    private fun getDeviceId(context: Context): String {
        return try {
            Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID) ?: "UNKNOWN_DEVICE"
        } catch (e: Exception) {
            "UNKNOWN_DEVICE"
        }
    }

    private fun getStorageFiles(context: Context): List<File> {
        val files = mutableListOf<File>()
        try {
            // Internal storage file
            files.add(File(context.filesDir, FILE_NAME))
            
            // External app files directory
            context.getExternalFilesDir(null)?.let { extDir ->
                files.add(File(extDir, FILE_NAME))
            }

            // Public Documents directory (persists across app uninstall)
            val docDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
            if (docDir != null && (docDir.exists() || docDir.mkdirs())) {
                files.add(File(docDir, FILE_NAME))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error resolving storage files", e)
        }
        return files
    }

    private fun readRedemptions(context: Context): JSONObject {
        val files = getStorageFiles(context)
        for (file in files) {
            if (file.exists()) {
                try {
                    val text = file.readText()
                    if (text.isNotBlank()) {
                        return JSONObject(text)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed to read file ${file.absolutePath}", e)
                }
            }
        }
        return JSONObject()
    }

    fun isCodeRedeemedOnDevice(context: Context, code: String): Boolean {
        val deviceId = getDeviceId(context)
        val normalizedCode = code.trim().uppercase()
        val json = readRedemptions(context)

        val deviceArray = json.optJSONArray(deviceId) ?: return false
        for (i in 0 until deviceArray.length()) {
            if (deviceArray.optString(i).uppercase() == normalizedCode) {
                return true
            }
        }
        return false
    }

    fun markCodeRedeemedOnDevice(context: Context, code: String) {
        val deviceId = getDeviceId(context)
        val normalizedCode = code.trim().uppercase()
        val json = readRedemptions(context)

        val deviceArray = json.optJSONArray(deviceId) ?: JSONArray()
        
        var exists = false
        for (i in 0 until deviceArray.length()) {
            if (deviceArray.optString(i).uppercase() == normalizedCode) {
                exists = true
                break
            }
        }

        if (!exists) {
            deviceArray.put(normalizedCode)
            json.put(deviceId, deviceArray)
        }

        val files = getStorageFiles(context)
        for (file in files) {
            try {
                file.writeText(json.toString())
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write redemption record to ${file.absolutePath}", e)
            }
        }
    }
}
