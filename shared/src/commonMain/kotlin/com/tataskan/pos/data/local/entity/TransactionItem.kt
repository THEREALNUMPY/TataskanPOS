package com.tataskan.pos.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents an item within a transaction.
 */
@Entity(
    tableName = "transaction_items",
    foreignKeys = [
        ForeignKey(
            entity = Transaction::class,
            parentColumns = ["id"],
            childColumns = ["transactionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("transactionId"), Index("productId")]
)
data class TransactionItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val transactionId: Long,
    val productId: Long,
    val productName: String = "",
    val quantity: Int,
    val priceAtSale: Double,
    val costAtSale: Double = 0.0
)
