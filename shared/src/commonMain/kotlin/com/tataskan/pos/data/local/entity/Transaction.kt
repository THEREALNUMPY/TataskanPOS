package com.tataskan.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tataskan.pos.util.currentTimeMillis

/**
 * Represents a completed POS transaction.
 */
@Entity(
    tableName = "transactions",
    indices = [Index(value = ["timestamp"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val total: Double,
    val amountReceived: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val promoName: String? = null,
    val paymentMethod: String = "CASH",
    val timestamp: Long = currentTimeMillis()
)
