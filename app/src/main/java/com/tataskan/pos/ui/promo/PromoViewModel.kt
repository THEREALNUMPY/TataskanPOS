package com.tataskan.pos.ui.promo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.entity.Promo
import com.tataskan.pos.data.entity.PromoType
import com.tataskan.pos.data.repository.PosRepository
import com.tataskan.pos.util.BarcodeUtils
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PromoViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: PosRepository = (application as TataskanApplication).repository

    val promos: StateFlow<List<Promo>> = repository.allPromos.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val products: StateFlow<List<com.tataskan.pos.data.local.entity.Product>> = repository.allProducts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun savePromo(
        id: Int = 0,
        name: String,
        code: String,
        type: PromoType,
        value: Double,
        productId: Long? = null,
        usageLimit: Int? = null,
        isActive: Boolean = true
    ) {
        viewModelScope.launch {
            val promo = Promo(id, name, code, type, value, productId, usageLimit, 0, isActive)
            if (id == 0) repository.addPromo(promo)
            else repository.updatePromo(promo)
        }
    }

    fun deletePromo(promo: Promo) {
        viewModelScope.launch {
            repository.deletePromo(promo)
        }
    }

    fun generateRandomCode(): String {
        return "PROMO_" + BarcodeUtils.generateRandomBarcodeString().take(6)
    }
}
