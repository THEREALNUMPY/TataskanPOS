package com.tataskan.pos.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tataskan.pos.data.AuthDao
import com.tataskan.pos.data.ProductDao
import com.tataskan.pos.data.TransactionDao
import com.tataskan.pos.data.CategoryDao
import com.tataskan.pos.data.PromoDao
import com.tataskan.pos.data.StockAdjustmentDao
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.data.local.entity.Transaction
import com.tataskan.pos.data.local.entity.TransactionItem
import com.tataskan.pos.data.entity.User
import com.tataskan.pos.data.entity.Category
import com.tataskan.pos.data.entity.Promo
import com.tataskan.pos.data.entity.StockAdjustment

@Database(
    entities = [User::class, Product::class, Transaction::class, TransactionItem::class, Category::class, Promo::class, StockAdjustment::class],
    version = 19,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun authDao(): AuthDao
    abstract fun productDao(): ProductDao
    abstract fun transactionDao(): TransactionDao
    abstract fun categoryDao(): CategoryDao
    abstract fun promoDao(): PromoDao
    abstract fun stockAdjustmentDao(): StockAdjustmentDao
}
