package com.tataskan.pos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.tataskan.pos.data.entity.StockAdjustment
import kotlinx.coroutines.flow.Flow

@Dao
interface StockAdjustmentDao {
    @Query("SELECT * FROM stock_adjustments ORDER BY timestamp DESC")
    fun getAllAdjustments(): Flow<List<StockAdjustment>>

    @Query("SELECT * FROM stock_adjustments WHERE productId = :productId ORDER BY timestamp DESC")
    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>>

    @Insert
    suspend fun insertAdjustment(adjustment: StockAdjustment)

    @Query("DELETE FROM stock_adjustments")
    suspend fun deleteAll()
}
