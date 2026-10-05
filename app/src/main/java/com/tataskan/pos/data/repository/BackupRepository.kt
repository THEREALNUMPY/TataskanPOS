package com.tataskan.pos.data.repository

import android.content.Context
import com.tataskan.pos.data.local.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class BackupRepository(private val context: Context, private val database: AppDatabase) {

    /**
     * Ensures all WAL changes are committed to the main database file.
     */
    private fun checkpoint() {
        try {
            database.openHelper.writableDatabase.execSQL("PRAGMA wal_checkpoint(FULL)")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun addFolderToZip(folder: File, zipOut: ZipOutputStream, basePath: String, exclude: String? = null) {
        if (!folder.exists()) return
        folder.listFiles()?.forEach { file ->
            if (exclude != null && file.name == exclude) return@forEach
            val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"
            if (file.isDirectory) {
                addFolderToZip(file, zipOut, entryName, null)
            } else {
                zipOut.putNextEntry(ZipEntry(entryName))
                FileInputStream(file).use { it.copyTo(zipOut) }
                zipOut.closeEntry()
            }
        }
    }

    suspend fun exportDatabase(outputStream: OutputStream): Boolean = withContext(Dispatchers.IO) {
        checkpoint()
        val dataDir = File(context.applicationInfo.dataDir)
        
        try {
            ZipOutputStream(outputStream).use { zipOut ->
                // 1. Add Databases (entire folder)
                val databasesDir = File(dataDir, "databases")
                addFolderToZip(databasesDir, zipOut, "databases")

                // 2. Add SharedPrefs (entire folder)
                val sharedPrefsDir = File(dataDir, "shared_prefs")
                addFolderToZip(sharedPrefsDir, zipOut, "shared_prefs")

                // 3. Add Files (excluding backups)
                val filesDir = context.filesDir
                addFolderToZip(filesDir, zipOut, "files", exclude = "backups")
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun exportToInternalStorage(): File? = withContext(Dispatchers.IO) {
        val backupDir = File(context.filesDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val fileName = "Tataskan_FullBackup_${System.currentTimeMillis()}.tataskan"
        val backupFile = File(backupDir, fileName)

        try {
            FileOutputStream(backupFile).use { output ->
                if (exportDatabase(output)) backupFile else null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getInternalBackups(): List<File> {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.listFiles()?.toList()?.sortedByDescending { it.lastModified() } ?: emptyList()
    }

    fun deleteInternalBackup(file: File): Boolean {
        return if (file.exists()) file.delete() else false
    }

    fun clearAllBackups(): Boolean {
        val backupDir = File(context.filesDir, "backups")
        return backupDir.deleteRecursively()
    }

    suspend fun importDatabase(inputStream: InputStream): Boolean = withContext(Dispatchers.IO) {
        try {
            checkpoint()
            // 1. Close active connections
            database.close()

            val dataDir = File(context.applicationInfo.dataDir)
            val databasesDir = File(dataDir, "databases")
            val sharedPrefsDir = File(dataDir, "shared_prefs")
            val filesDir = context.filesDir
            
            // 2. Backup current state temporarily for recovery
            val tempDir = File(context.cacheDir, "restore_temp_${System.currentTimeMillis()}")
            tempDir.mkdirs()

            val tempDb = File(tempDir, "databases")
            val tempSp = File(tempDir, "shared_prefs")
            val tempFiles = File(tempDir, "files_restore")
            tempFiles.mkdirs()
            
            if (databasesDir.exists()) databasesDir.renameTo(tempDb)
            if (sharedPrefsDir.exists()) sharedPrefsDir.renameTo(tempSp)
            
            // Move non-backup files to temp
            filesDir.listFiles()?.forEach { file ->
                if (file.name != "backups") {
                    file.renameTo(File(tempFiles, file.name))
                }
            }

            try {
                // 3. Extract ZIP
                ZipInputStream(inputStream).use { zipIn ->
                    var entry = zipIn.nextEntry
                    while (entry != null) {
                        val targetFile = File(dataDir, entry.name)
                        
                        if (!entry.isDirectory) {
                            if (!targetFile.parentFile!!.exists()) targetFile.parentFile!!.mkdirs()
                            if (targetFile.exists()) targetFile.delete()
                            FileOutputStream(targetFile).use { zipIn.copyTo(it) }
                        }
                        
                        zipIn.closeEntry()
                        entry = zipIn.nextEntry
                    }
                }

                // 4. Verify DB was restored
                val dbFile = context.getDatabasePath("tataskan_database")
                if (!dbFile.exists() || dbFile.length() == 0L) {
                    throw Exception("Restore failed - database missing")
                }

                // 5. Set restore flag to force login after restart
                try {
                    File(context.filesDir, "restore_pending.flag").createNewFile()
                } catch (e: Exception) {}

                tempDir.deleteRecursively()
                // Extra sync to ensure OS has flushed all file writes to disk before restart
                try {
                    dbFile.setReadOnly()
                    dbFile.setWritable(true)
                } catch (e: Exception) {}
                
                true
            } catch (e: Exception) {
                // Restore old state on failure
                if (tempDb.exists()) {
                    if (databasesDir.exists()) databasesDir.deleteRecursively()
                    tempDb.renameTo(databasesDir)
                }
                if (tempSp.exists()) {
                    if (sharedPrefsDir.exists()) sharedPrefsDir.deleteRecursively()
                    tempSp.renameTo(sharedPrefsDir)
                }
                if (tempFiles.exists()) {
                    tempFiles.listFiles()?.forEach { it.renameTo(File(filesDir, it.name)) }
                }
                throw e
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
