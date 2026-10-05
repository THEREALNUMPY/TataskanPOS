package com.tataskan.pos.data.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

enum class PromoType {
    PERCENTAGE_TOTAL,
    FIXED_TOTAL,
    PERCENTAGE_PRODUCT,
    FIXED_PRODUCT
}

@Entity(
    tableName = "promos",
    indices = [Index(value = ["code"], unique = true)]
)
data class Promo(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val code: String,
    val type: PromoType,
    val value: Double,
    val productId: Long? = null, // Used if type is _PRODUCT
    val usageLimit: Int? = null, // Null means unlimited
    val currentUsage: Int = 0,
    val isActive: Boolean = true
) {
    val isExpired: Boolean get() = usageLimit != null && currentUsage >= usageLimit
}
