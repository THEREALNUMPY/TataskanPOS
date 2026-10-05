package com.tataskan.pos.ui.product

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.data.entity.Category
import com.tataskan.pos.data.repository.PosRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ProductViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PosRepository = (application as TataskanApplication).repository

    init {
        viewModelScope.launch {
            val currentCats = repository.allCategories.first()
            if (currentCats.isEmpty()) {
                val productList = repository.allProducts.first()
                val distinctCats = productList.map { it.category }.distinct()
                distinctCats.forEach { catName ->
                    if (catName.isNotBlank()) {
                        repository.addCategory(Category(name = catName))
                    }
                }
            }
        }
    }

    val products: StateFlow<List<Product>> = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val categories: StateFlow<List<Category>> = repository.allCategories.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun clearError() {
        _error.value = null
    }

    fun getProduct(id: Long?): Product? {
        if (id == null) return null
        return products.value.find { it.id == id }
    }

    fun saveProduct(
        id: Long = 0,
        name: String,
        price: Double,
        cost: Double,
        categoryName: String,
        stock: Int,
        barcode: String?,
        imageUri: String?
    ) {
        if (name.isBlank()) {
            _error.value = "Product name cannot be empty"
            return
        }
        if (price < 0) {
            _error.value = "Price cannot be negative"
            return
        }

        viewModelScope.launch {
            try {
                val trimmedName = name.trim()
                val trimmedCategory = categoryName.trim()
                val trimmedBarcode = barcode?.trim()?.ifBlank { null }
                
                if (trimmedCategory.isNotBlank()) {
                    val existingCategory = repository.getCategoryByName(trimmedCategory)
                    if (existingCategory == null) {
                        repository.addCategory(Category(name = trimmedCategory))
                    }
                }

                val product = Product(
                    id = id,
                    name = trimmedName,
                    price = price,
                    cost = cost,
                    category = trimmedCategory,
                    stock = stock,
                    barcode = trimmedBarcode,
                    imageUri = imageUri
                )
                if (id == 0L) {
                    repository.addProduct(product)
                } else {
                    repository.updateProduct(product)
                }
                _error.value = null
            } catch (e: Exception) {
                android.util.Log.e("ProductViewModel", "Error saving product", e)
                _error.value = "Failed to save product: ${e.message}"
            }
        }
    }

    fun deleteProduct(product: Product) {
        viewModelScope.launch {
            try {
                repository.deleteProduct(product)
                _error.value = null
            } catch (e: Exception) {
                android.util.Log.e("ProductViewModel", "Error deleting product", e)
                _error.value = "Failed to delete product: ${e.message}"
            }
        }
    }

    fun quickStockAdjustment(product: Product, change: Int) {
        viewModelScope.launch {
            try {
                val newStock = (product.stock + change).coerceAtLeast(0)
                if (newStock == product.stock) return@launch

                repository.updateProduct(product.copy(stock = newStock))

                repository.addAdjustment(
                    com.tataskan.pos.data.entity.StockAdjustment(
                        productId = product.id,
                        productName = product.name,
                        quantityChange = change,
                        reason = if (change > 0) com.tataskan.pos.data.entity.AdjustmentReason.RESTOCKED 
                                 else com.tataskan.pos.data.entity.AdjustmentReason.MANUAL,
                        notes = "Quick Adjustment from Inventory"
                    )
                )
                _error.value = null
            } catch (e: Exception) {
                android.util.Log.e("ProductViewModel", "Error adjusting stock", e)
                _error.value = "Failed to adjust stock: ${e.message}"
            }
        }
    }
}
