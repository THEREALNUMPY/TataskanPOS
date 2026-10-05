package com.tataskan.pos.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents a product in the inventory.
 */
@Entity(
    tableName = "products",
    indices = [
        Index(value = ["barcode"], unique = true),
        Index(value = ["name"]),
        Index(value = ["category"])
    ]
)
data class Product(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val price: Double,
    val cost: Double,
    val category: String,
    val stock: Int,
    val barcode: String?,
    val imageUri: String?
)
