package com.tataskan.pos.data.repository

import com.tataskan.pos.data.ProductDao
import com.tataskan.pos.data.TransactionDao
import com.tataskan.pos.data.CategoryDao
import com.tataskan.pos.data.PromoDao
import com.tataskan.pos.data.StockAdjustmentDao
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.data.local.entity.Transaction
import com.tataskan.pos.data.local.entity.TransactionItem
import com.tataskan.pos.data.relation.TransactionWithItems
import com.tataskan.pos.data.entity.Category
import com.tataskan.pos.data.entity.Promo
import com.tataskan.pos.data.entity.StockAdjustment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

/**
 * Repository layer that provides offline-first data access.
 */
class PosRepository(
    private val productDao: ProductDao,
    private val transactionDao: TransactionDao,
    private val categoryDao: CategoryDao,
    private val promoDao: PromoDao,
    private val stockAdjustmentDao: StockAdjustmentDao
) {
    // Products
    val allProducts: Flow<List<Product>> = productDao.getAllProducts()
        .flowOn(Dispatchers.Default)

    suspend fun getProductById(id: Long) = withContext(Dispatchers.Default) {
        productDao.getProductById(id)
    }

    suspend fun getProductByBarcode(barcode: String) = withContext(Dispatchers.Default) {
        productDao.getProductByBarcode(barcode)
    }

    suspend fun addProduct(product: Product) = withContext(Dispatchers.Default) {
        productDao.insertProduct(product)
    }

    suspend fun updateProduct(product: Product) = withContext(Dispatchers.Default) {
        productDao.updateProduct(product)
    }

    suspend fun deleteProduct(product: Product) = withContext(Dispatchers.Default) {
        productDao.deleteProduct(product)
    }

    // Categories
    val allCategories: Flow<List<Category>> = categoryDao.getAllCategories()
        .flowOn(Dispatchers.Default)

    suspend fun addCategory(category: Category) = withContext(Dispatchers.Default) {
        categoryDao.insertCategory(category)
    }

    suspend fun deleteCategory(id: Int) = withContext(Dispatchers.Default) {
        categoryDao.deleteCategory(id)
    }

    suspend fun getCategoryByName(name: String) = withContext(Dispatchers.Default) {
        categoryDao.getCategoryByName(name)
    }

    // Transactions
    val allTransactions: Flow<List<TransactionWithItems>> = transactionDao.getAllTransactionsWithItems()
        .flowOn(Dispatchers.Default)

    suspend fun getTransactionById(id: Long) = withContext(Dispatchers.Default) {
        transactionDao.getTransactionById(id)
    }

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionWithItems>> =
        transactionDao.getTransactionsInRangeWithItems(startTime, endTime)
            .flowOn(Dispatchers.Default)

    suspend fun completeTransaction(transaction: Transaction, items: List<TransactionItem>): Long = withContext(Dispatchers.Default) {
        transactionDao.insertFullTransaction(transaction, items)
    }

    // Promos
    val allPromos: Flow<List<Promo>> = promoDao.getAllPromos()
        .flowOn(Dispatchers.Default)

    suspend fun getPromoByCode(code: String) = withContext(Dispatchers.Default) {
        promoDao.getPromoByCode(code)
    }

    suspend fun addPromo(promo: Promo) = withContext(Dispatchers.Default) {
        promoDao.insertPromo(promo)
    }

    suspend fun updatePromo(promo: Promo) = withContext(Dispatchers.Default) {
        promoDao.updatePromo(promo)
    }

    suspend fun deletePromo(promo: Promo) = withContext(Dispatchers.Default) {
        promoDao.deletePromo(promo)
    }

    // Stock Adjustments
    val allAdjustments: Flow<List<StockAdjustment>> = stockAdjustmentDao.getAllAdjustments()
        .flowOn(Dispatchers.Default)

    fun getAdjustmentsForProduct(productId: Long): Flow<List<StockAdjustment>> =
        stockAdjustmentDao.getAdjustmentsForProduct(productId)
            .flowOn(Dispatchers.Default)

    suspend fun addAdjustment(adjustment: StockAdjustment) = withContext(Dispatchers.Default) {
        stockAdjustmentDao.insertAdjustment(adjustment)
    }
}
