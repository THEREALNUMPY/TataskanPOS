package com.tataskan.pos.data

import androidx.room.*
import com.tataskan.pos.data.entity.Promo
import kotlinx.coroutines.flow.Flow

@Dao
interface PromoDao {
    @Query("SELECT * FROM promos ORDER BY name ASC")
    fun getAllPromos(): Flow<List<Promo>>

    @Query("SELECT * FROM promos WHERE code = :code AND isActive = 1 LIMIT 1")
    suspend fun getPromoByCode(code: String): Promo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPromo(promo: Promo)

    @Update
    suspend fun updatePromo(promo: Promo)

    @Delete
    suspend fun deletePromo(promo: Promo)

    @Query("DELETE FROM promos")
    suspend fun deleteAll()
}
