package com.tataskan.pos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tataskan.pos.data.local.entity.Transaction as TransactionEntity
import com.tataskan.pos.data.local.entity.TransactionItem
import com.tataskan.pos.data.relation.TransactionWithItems
import kotlinx.coroutines.flow.Flow

@Dao
abstract class TransactionDao {
    @Transaction
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    abstract fun getAllTransactionsWithItems(): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    abstract fun getTransactionsInRangeWithItems(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>>

    @Transaction
    @Query("SELECT * FROM transactions WHERE id = :id")
    abstract suspend fun getTransactionById(id: Long): TransactionWithItems?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertTransactionItems(items: List<TransactionItem>)

    @Query("UPDATE products SET stock = stock - :quantity WHERE id = :productId")
    abstract suspend fun reduceStock(productId: Long, quantity: Int)

    /**
     * Inserts a transaction, its items, and updates stock in a single database transaction.
     */
    @Transaction
    open suspend fun insertFullTransaction(transaction: TransactionEntity, items: List<TransactionItem>): Long {
        val transactionId = insertTransaction(transaction)
        val itemsWithId = items.map { it.copy(transactionId = transactionId) }
        insertTransactionItems(itemsWithId)
        
        // Reduce stock for each item (skip for dummy items)
        items.forEach { item ->
            if (item.productId > 0) {
                reduceStock(item.productId, item.quantity)
            }
        }
        
        return transactionId
    }

    @Query("DELETE FROM transactions")
    abstract suspend fun deleteAll()

    @Query("DELETE FROM transaction_items")
    abstract suspend fun deleteAllItems()
}
