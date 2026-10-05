package com.tataskan.pos.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val path = documentDirectory?.path + "/tataskan_database"
    return Room.databaseBuilder<AppDatabase>(
        name = path,
        factory = { AppDatabase::class.instantiateImpl() }
    ).fallbackToDestructiveMigration(true)
}
