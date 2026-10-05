package com.tataskan.pos.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tataskan.pos.util.currentTimeMillis

enum class AdjustmentReason {
    SOLD,
    RESTOCKED,
    DAMAGED,
    EXPIRED,
    LOST,
    MANUAL
}

@Entity(tableName = "stock_adjustments")
data class StockAdjustment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val productId: Long,
    val productName: String, // Cached for history if product deleted
    val quantityChange: Int, // e.g. -5 for damaged, +10 for restock
    val reason: AdjustmentReason,
    val timestamp: Long = currentTimeMillis(),
    val notes: String = ""
)
