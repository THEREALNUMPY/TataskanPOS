package com.tataskan.pos.ui.pos

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.data.local.entity.Transaction
import com.tataskan.pos.data.local.entity.TransactionItem
import com.tataskan.pos.data.repository.PosRepository
import com.tataskan.pos.data.settings.SettingsRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CartItem(
    val product: Product,
    val quantity: Int
) {
    val subtotal: Double get() = product.price * quantity
}

class PosViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository = (application as TataskanApplication).repository
    private val settingsRepository = SettingsRepository(application)

    private val _cartItemsMap = MutableStateFlow<Map<Long, CartItem>>(emptyMap())
    private var lastRemovedItem: CartItem? = null
    
    private val _appliedPromo = MutableStateFlow<com.tataskan.pos.data.entity.Promo?>(null)
    val appliedPromo: StateFlow<com.tataskan.pos.data.entity.Promo?> = _appliedPromo.asStateFlow()

    val taxPercentage: StateFlow<Float> = settingsRepository.taxPercentage.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0f
    )

    val cartItems: StateFlow<List<CartItem>> = _cartItemsMap
        .map { it.values.toList() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val subtotal: StateFlow<Double> = _cartItemsMap
        .map { map -> map.values.sumOf { it.subtotal } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val discountAmount: StateFlow<Double> = combine(subtotal, _cartItemsMap, _appliedPromo) { sub, cartMap, promo ->
        if (promo == null) 0.0
        else {
            when (promo.type) {
                com.tataskan.pos.data.entity.PromoType.PERCENTAGE_TOTAL -> {
                    sub * (promo.value / 100.0)
                }
                com.tataskan.pos.data.entity.PromoType.FIXED_TOTAL -> {
                    promo.value.coerceAtMost(sub)
                }
                com.tataskan.pos.data.entity.PromoType.PERCENTAGE_PRODUCT -> {
                    val targetItem = cartMap[promo.productId]
                    if (targetItem != null) {
                        targetItem.subtotal * (promo.value / 100.0)
                    } else 0.0
                }
                com.tataskan.pos.data.entity.PromoType.FIXED_PRODUCT -> {
                    val targetItem = cartMap[promo.productId]
                    if (targetItem != null) {
                        (promo.value * targetItem.quantity).coerceAtMost(targetItem.subtotal)
                    } else 0.0
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val taxableAmount: StateFlow<Double> = combine(subtotal, discountAmount) { sub, disc ->
        (sub - disc).coerceAtLeast(0.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val taxAmount: StateFlow<Double> = combine(taxableAmount, taxPercentage) { taxable, taxRate ->
        taxable * (taxRate.toDouble() / 100.0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val grandTotal: StateFlow<Double> = combine(taxableAmount, taxAmount) { taxable, tax ->
        taxable + tax
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _error = MutableStateFlow<String?>(null)
    val error = _error.asStateFlow()

    fun addToCart(product: Product) {
        if (product.stock <= 0) {
            _error.value = "Product is out of stock"
            return
        }
        
        _cartItemsMap.update { currentCart ->
            val existing = currentCart[product.id]
            val updated = if (existing != null) {
                if (existing.quantity >= product.stock) {
                    _error.value = "Cannot add more than available stock"
                    return@update currentCart
                }
                existing.copy(quantity = existing.quantity + 1)
            } else {
                CartItem(product, 1)
            }
            _error.value = null
            currentCart + (product.id to updated)
        }
    }

    fun onBarcodeDetected(barcode: String, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val product = repository.getProductByBarcode(barcode)
                if (product != null) {
                    addToCart(product)
                    onResult(true)
                    return@launch
                }
                
                val promo = repository.getPromoByCode(barcode)
                if (promo != null) {
                    if (promo.isExpired) {
                        _error.value = "Promo code has reached its usage limit"
                        onResult(false)
                        return@launch
                    }
                    _appliedPromo.value = promo
                    _error.value = "Promo '${promo.name}' applied!"
                    onResult(true)
                    return@launch
                }

                _error.value = "Code $barcode not found"
                onResult(false)
            } catch (e: Exception) {
                android.util.Log.e("PosViewModel", "Error scanning code", e)
                _error.value = "Error scanning code: ${e.message}"
                onResult(false)
            }
        }
    }

    fun updateQuantity(productId: Long, quantity: Int) {
        _cartItemsMap.update { currentCart ->
            val existing = currentCart[productId]
            if (existing != null) {
                if (quantity <= 0) {
                    currentCart - productId
                } else {
                    val maxStock = existing.product.stock
                    val finalQty = if (quantity > maxStock) {
                        _error.value = "Cannot exceed available stock ($maxStock)"
                        maxStock
                    } else {
                        _error.value = null
                        quantity
                    }
                    currentCart + (productId to existing.copy(quantity = finalQty))
                }
            } else {
                currentCart
            }
        }
    }

    fun removeFromCart(productId: Long) {
        _cartItemsMap.update { currentCart ->
            val item = currentCart[productId]
            if (item != null) {
                lastRemovedItem = item
                _error.value = "Item removed. Tap to Undo."
            }
            currentCart - productId 
        }
    }

    fun undoRemoveFromCart() {
        lastRemovedItem?.let { item ->
            _cartItemsMap.update { it + (item.product.id to item) }
            lastRemovedItem = null
            _error.value = null
        }
    }

    fun clearCart() {
        _cartItemsMap.value = emptyMap()
        _error.value = null
    }

    fun clearError() {
        _error.value = null
    }

    suspend fun getTransactionById(id: Long) = repository.getTransactionById(id)

    fun completeSale(amountReceived: Double, taxPercentage: Float = 0f, paymentMethod: String = "CASH", onComplete: (Long) -> Unit) {
        val currentItems = _cartItemsMap.value.values.toList()
        if (currentItems.isEmpty()) return

        val total = currentItems.sumOf { it.subtotal }
        val discount = discountAmount.value
        val effectiveTaxRate = if (taxPercentage > 0f) taxPercentage else this.taxPercentage.value
        val taxable = (total - discount).coerceAtLeast(0.0)
        val calculatedTax = taxable * (effectiveTaxRate.toDouble() / 100.0)
        val finalTotal = taxable + calculatedTax
        val promoName = _appliedPromo.value?.name
        
        viewModelScope.launch {
            try {
                val transaction = Transaction(
                    total = finalTotal,
                    amountReceived = amountReceived,
                    taxAmount = calculatedTax,
                    discountAmount = discount,
                    promoName = promoName,
                    paymentMethod = paymentMethod
                )
                val transactionItems = currentItems.map {
                    TransactionItem(
                        transactionId = 0,
                        productId = it.product.id,
                        productName = it.product.name,
                        quantity = it.quantity,
                        priceAtSale = it.product.price,
                        costAtSale = it.product.cost
                    )
                }
                val id = repository.completeTransaction(transaction, transactionItems)
                
                _appliedPromo.value?.let { promo ->
                    repository.updatePromo(promo.copy(currentUsage = promo.currentUsage + 1))
                    _appliedPromo.value = null
                }

                clearCart()
                onComplete(id)
            } catch (e: Exception) {
                _error.value = "Failed to complete transaction: ${e.message}"
            }
        }
    }
}
