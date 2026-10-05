package com.tataskan.pos.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private lateinit var appContext: Context

fun setAppContext(context: Context) {
    appContext = context
}

val MIGRATION_17_18 = object : Migration(17, 18) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN backupPin TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            db.execSQL("ALTER TABLE stock_adjustments ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

val MIGRATION_18_19 = object : Migration(18, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

val MIGRATION_17_19 = object : Migration(17, 19) {
    override fun migrate(db: SupportSQLiteDatabase) {
        try {
            db.execSQL("ALTER TABLE users ADD COLUMN backupPin TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            db.execSQL("ALTER TABLE stock_adjustments ADD COLUMN notes TEXT NOT NULL DEFAULT ''")
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            db.execSQL("ALTER TABLE transactions ADD COLUMN paymentMethod TEXT NOT NULL DEFAULT 'CASH'")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

actual fun getDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = appContext.getDatabasePath("tataskan_database").absolutePath
    )
    .addMigrations(MIGRATION_17_18, MIGRATION_18_19, MIGRATION_17_19)
    .fallbackToDestructiveMigrationFrom(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16)
    .fallbackToDestructiveMigrationOnDowngrade()
}
