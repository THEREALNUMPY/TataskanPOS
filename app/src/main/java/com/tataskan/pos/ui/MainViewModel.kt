package com.tataskan.pos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.data.local.entity.Transaction
import com.tataskan.pos.data.local.entity.TransactionItem
import com.tataskan.pos.data.repository.PosRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Main ViewModel for SukiPOS, managing data from the repository.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PosRepository

    init {
        repository = (application as TataskanApplication).repository
    }

    val products = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val transactions = repository.allTransactions.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun addProduct(product: Product) {
        viewModelScope.launch {
            repository.addProduct(product)
        }
    }

    fun completeSale(total: Double, items: List<TransactionItem>) {
        viewModelScope.launch {
            val transaction = Transaction(total = total)
            repository.completeTransaction(transaction, items)
        }
    }
    
    // Additional logic for scanning and item selection can be added here
}
