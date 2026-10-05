package com.tataskan.pos.data.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.tataskan.pos.data.local.entity.Transaction
import com.tataskan.pos.data.local.entity.TransactionItem

/**
 * Data class representing a transaction with its associated items.
 */
data class TransactionWithItems(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "transactionId"
    )
    val items: List<TransactionItem>
)
