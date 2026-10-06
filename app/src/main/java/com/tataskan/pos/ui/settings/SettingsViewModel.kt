package com.tataskan.pos.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.local.entity.Transaction
import com.tataskan.pos.data.repository.BackupRepository
import com.tataskan.pos.data.repository.PosRepository
import com.tataskan.pos.data.settings.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.InputStream
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TataskanApplication
    private val repository = SettingsRepository(application)
    private val backupRepository = BackupRepository(application, app.database)

    private val _internalBackups = MutableStateFlow<List<File>>(emptyList())
    val internalBackups: StateFlow<List<File>> = _internalBackups.asStateFlow()

    private val _isSettingsLoading = MutableStateFlow(true)
    val isSettingsLoading: StateFlow<Boolean> = _isSettingsLoading.asStateFlow()

    init {
        refreshBackups()
        com.tataskan.pos.util.LocalBackupManager.scheduleAutoBackup(application)
        viewModelScope.launch {
            try {
                // Pre-load critical UI settings without unnecessary timeout that might block start
                repository.isTutorialComplete.first()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSettingsLoading.value = false
            }
        }
    }

    private fun refreshBackups() {
        _internalBackups.value = backupRepository.getInternalBackups()
    }

    val currencySymbol: StateFlow<String> = repository.currencySymbol.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = "₱"
    )

    val showNameOnLabel: StateFlow<Boolean> = repository.showNameOnLabel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val showPriceOnLabel: StateFlow<Boolean> = repository.showPriceOnLabel.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = true
    )

    val storeName: StateFlow<String> = repository.storeName.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "TataskanPOS Store"
    )
    val storeLogoUri: StateFlow<String?> = repository.storeLogoUri.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )
    val storeAddress: StateFlow<String> = repository.storeAddress.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ""
    )
    val phoneNumber: StateFlow<String> = repository.phoneNumber.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = ""
    )
    val receiptFooter: StateFlow<String> = repository.receiptFooter.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "Thank you!"
    )
    val taxPercentage: StateFlow<Float> = repository.taxPercentage.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0f
    )
    val language: StateFlow<String> = repository.language.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "en"
    )
    val receiptEnabled: StateFlow<Boolean> = repository.receiptEnabled.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true
    )
    val barcodeFormats: StateFlow<String> = repository.barcodeFormats.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "QR_CODE,EAN_13,UPC_A"
    )

    val loggedInUsername: StateFlow<String?> = repository.loggedInUsername.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )

    val isTutorialComplete: StateFlow<Boolean?> = repository.isTutorialComplete.stateIn(
        scope = viewModelScope, started = SharingStarted.Eagerly, initialValue = null
    )
    val defaultBulkScan: StateFlow<Boolean> = repository.defaultBulkScan.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val gcashQrUri: StateFlow<String?> = repository.gcashQrUri.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )
    val mayaQrUri: StateFlow<String?> = repository.mayaQrUri.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )
    val googleDriveAccount: StateFlow<String?> = repository.googleDriveAccount.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = null
    )
    val lastDriveBackupTime: StateFlow<Long> = repository.lastDriveBackupTime.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0L
    )
    val lastLocalBackupTime: StateFlow<Long> = repository.lastLocalBackupTime.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = 0L
    )
    val isLocalBackupEnabled: StateFlow<Boolean> = repository.isLocalBackupEnabled.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = true
    )
    val backupFrequency: StateFlow<String> = repository.backupFrequency.stateIn(
        scope = viewModelScope, started = SharingStarted.WhileSubscribed(5000), initialValue = "DAILY"
    )

    fun setCurrencySymbol(symbol: String) = viewModelScope.launch { repository.updateCurrencySymbol(symbol.trim()) }
    fun setShowNameOnLabel(show: Boolean) = viewModelScope.launch { repository.updateShowNameOnLabel(show) }
    fun setBackupFrequency(frequency: String) = viewModelScope.launch { 
        repository.updateBackupFrequency(frequency)
        if (isLocalBackupEnabled.value) {
            com.tataskan.pos.util.LocalBackupManager.scheduleAutoBackup(app, frequency)
        }
    }
    fun setIsLocalBackupEnabled(enabled: Boolean) = viewModelScope.launch {
        repository.updateIsLocalBackupEnabled(enabled)
        if (enabled) {
            com.tataskan.pos.util.LocalBackupManager.scheduleAutoBackup(app, backupFrequency.value)
        } else {
            com.tataskan.pos.util.LocalBackupManager.cancelAutoBackup(app)
        }
    }
    fun setShowPriceOnLabel(show: Boolean) = viewModelScope.launch { repository.updateShowPriceOnLabel(show) }
    
    fun setStoreName(name: String) = viewModelScope.launch { repository.updateStoreName(name.trim()) }
    fun setStoreLogoUri(uri: String?) = viewModelScope.launch { repository.updateStoreLogoUri(uri) }
    fun setGcashQrUri(uri: String?) = viewModelScope.launch { repository.updateGcashQrUri(uri) }
    fun setMayaQrUri(uri: String?) = viewModelScope.launch { repository.updateMayaQrUri(uri) }
    fun setGoogleDriveAccount(account: String?) = viewModelScope.launch { repository.updateGoogleDriveAccount(account) }
    fun setLastDriveBackupTime(timestamp: Long) = viewModelScope.launch { repository.updateLastDriveBackupTime(timestamp) }
    fun setLastLocalBackupTime(timestamp: Long) = viewModelScope.launch { repository.updateLastLocalBackupTime(timestamp) }
    fun setStoreAddress(address: String) = viewModelScope.launch { repository.updateStoreAddress(address.trim()) }
    fun setPhoneNumber(phone: String) = viewModelScope.launch { repository.updatePhoneNumber(phone.trim()) }
    fun setReceiptFooter(footer: String) = viewModelScope.launch { repository.updateReceiptFooter(footer.trim()) }
    fun setTaxPercentage(tax: Float) = viewModelScope.launch { repository.updateTaxPercentage(tax) }
    fun setLanguage(lang: String) = viewModelScope.launch { repository.updateLanguage(lang) }
    fun setReceiptEnabled(enabled: Boolean) = viewModelScope.launch { repository.updateReceiptEnabled(enabled) }
    fun setTutorialComplete(complete: Boolean) = viewModelScope.launch { repository.updateTutorialComplete(complete) }
    fun setDefaultBulkScan(enabled: Boolean) = viewModelScope.launch { repository.updateDefaultBulkScan(enabled) }

    fun resetBusinessData(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                app.database.productDao().deleteAll()
                app.database.transactionDao().deleteAll()
                app.database.transactionDao().deleteAllItems()
                app.database.categoryDao().deleteAll()
                app.database.promoDao().deleteAll()
                app.database.stockAdjustmentDao().deleteAll()

                repository.updateGcashQrUri(null)
                repository.updateMayaQrUri(null)
                repository.updateTutorialComplete(false)
            }
            onComplete()
        }
    }

    fun seedDemoData(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                // 0. Clear existing business data but KEEP the user account
                app.database.productDao().deleteAll()
                app.database.transactionDao().deleteAll()
                app.database.transactionDao().deleteAllItems()
                app.database.categoryDao().deleteAll()
                app.database.promoDao().deleteAll()
                app.database.stockAdjustmentDao().deleteAll()

                // 1. Seed Professional Shop Profile & Sample QR Codes
                repository.updateStoreName("Small Mall Mart")
                repository.updateStoreAddress("G/F City Mall, Central District")
                repository.updatePhoneNumber("02-123-4567")
                repository.updateCurrencySymbol("₱")
                repository.updateTaxPercentage(12f)
                repository.updateTutorialComplete(false)
                repository.updateReceiptFooter("Thank you for shopping at Small Mall Mart!")
                repository.updateStoreLogoUri("https://images.unsplash.com/photo-1542838132-92c53300491e?w=400")
                repository.updateGcashQrUri("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=GCashMerchantSmallMallMart")
                repository.updateMayaQrUri("https://api.qrserver.com/v1/create-qr-code/?size=300x300&data=MayaMerchantSmallMallMart")

                // 2. Seed Diverse Product Catalog (Including Low Stock & Out of Stock items)
                val demoProducts = listOf(
                    com.tataskan.pos.data.local.entity.Product(name = "Jasmine Rice 5kg", price = 285.0, cost = 210.0, category = "Grocery", stock = 50, barcode = "G001", imageUri = "https://images.unsplash.com/photo-1586201375761-83865001e31c?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Whole Wheat Bread", price = 65.0, cost = 42.0, category = "Bakery", stock = 12, barcode = "B001", imageUri = "https://images.unsplash.com/photo-1509440159596-0249088772ff?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Fresh Eggs (12pcs)", price = 115.0, cost = 85.0, category = "Dairy", stock = 20, barcode = "D001", imageUri = "https://images.unsplash.com/photo-1518562180175-34a163b1a9a6?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Fresh Milk 1L", price = 95.0, cost = 75.0, category = "Dairy", stock = 15, barcode = "D002", imageUri = "https://images.unsplash.com/photo-1550583724-125581cc2532?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Wired Headphones", price = 599.0, cost = 380.0, category = "Electronics", stock = 3, barcode = "E001", imageUri = "https://images.unsplash.com/photo-1505740420928-5e560c06d30e?w=400"), // Low stock
                    com.tataskan.pos.data.local.entity.Product(name = "Wireless Mouse", price = 850.0, cost = 550.0, category = "Electronics", stock = 8, barcode = "E002", imageUri = "https://images.unsplash.com/photo-1527864550417-7fd91fc51a46?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Mini Bluetooth Speaker", price = 1200.0, cost = 850.0, category = "Electronics", stock = 0, barcode = "E004", imageUri = "https://images.unsplash.com/photo-1608156639585-34054e815958?w=400"), // Out of stock
                    com.tataskan.pos.data.local.entity.Product(name = "Coca-Cola 1.5L", price = 68.0, cost = 52.0, category = "Beverages", stock = 48, barcode = "V001", imageUri = "https://images.unsplash.com/photo-1622483767028-3f66f32aef97?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Potato Chips Lg", price = 75.0, cost = 55.0, category = "Snacks", stock = 30, barcode = "S001", imageUri = "https://images.unsplash.com/photo-1566478989037-eec170784d0b?w=400"),
                    com.tataskan.pos.data.local.entity.Product(name = "Cotton T-Shirt M", price = 299.0, cost = 180.0, category = "Fashion", stock = 2, barcode = "F001", imageUri = "https://images.unsplash.com/photo-1521572267360-ee0c2909d518?w=400") // Low stock
                )
                
                val repeatedItems = (1..20).map { i ->
                    com.tataskan.pos.data.local.entity.Product(
                        name = "Stock Item #$i", price = 10.0 + i, cost = 5.0 + i, 
                        category = if (i % 2 == 0) "General" else "Misc", 
                        stock = if (i == 5) 0 else if (i == 12) 3 else 100, 
                        barcode = "STOCK_$i", imageUri = null
                    )
                }
                
                val allProducts = demoProducts + repeatedItems
                allProducts.forEach { product ->
                    app.database.productDao().insertProduct(product)
                }

                val allInsertedProducts = app.database.productDao().getAllProducts().first()
                val insertedIds = allInsertedProducts.map { it.id }

                // 3. Seed Categories
                allProducts.map { it.category }.distinct().forEach { catName ->
                    if (catName.isNotBlank() && app.repository.getCategoryByName(catName) == null) {
                        app.repository.addCategory(com.tataskan.pos.data.entity.Category(name = catName))
                    }
                }

                // 4. Seed Promos & Vouchers
                val promos = listOf(
                    com.tataskan.pos.data.entity.Promo(
                        name = "Storewide 10% OFF",
                        code = "PROMO10",
                        type = com.tataskan.pos.data.entity.PromoType.PERCENTAGE_TOTAL,
                        value = 10.0,
                        isActive = true
                    ),
                    com.tataskan.pos.data.entity.Promo(
                        name = "₱50 OFF Minimum Spend",
                        code = "PROMO50",
                        type = com.tataskan.pos.data.entity.PromoType.FIXED_TOTAL,
                        value = 50.0,
                        isActive = true
                    ),
                    com.tataskan.pos.data.entity.Promo(
                        name = "20% OFF Electronics",
                        code = "TECH20",
                        type = com.tataskan.pos.data.entity.PromoType.PERCENTAGE_PRODUCT,
                        value = 20.0,
                        productId = insertedIds.getOrNull(4),
                        isActive = true
                    )
                )
                promos.forEach { app.database.promoDao().insertPromo(it) }

                // 5. Seed Stock Adjustments Timeline
                val adjustments = listOf(
                    com.tataskan.pos.data.entity.StockAdjustment(
                        productId = insertedIds.firstOrNull() ?: 1L,
                        productName = "Jasmine Rice 5kg",
                        quantityChange = 50,
                        reason = com.tataskan.pos.data.entity.AdjustmentReason.RESTOCKED,
                        notes = "Initial delivery restock",
                        timestamp = System.currentTimeMillis() - 86400000 * 3
                    ),
                    com.tataskan.pos.data.entity.StockAdjustment(
                        productId = insertedIds.getOrNull(4) ?: 5L,
                        productName = "Wired Headphones",
                        quantityChange = -2,
                        reason = com.tataskan.pos.data.entity.AdjustmentReason.DAMAGED,
                        notes = "Box damaged during transport",
                        timestamp = System.currentTimeMillis() - 86400000 * 2
                    )
                )
                adjustments.forEach { app.database.stockAdjustmentDao().insertAdjustment(it) }
                
                // 6. Seed 30-Day Transaction History for Sales Analytics & Charts
                val cal = Calendar.getInstance()
                val now = System.currentTimeMillis()

                // Generate sales entries across past 30 days
                for (dayOffset in 0..29) {
                    cal.timeInMillis = now - (dayOffset * 86400000L)
                    val baseTime = cal.timeInMillis

                    // Vary transactions count based on day
                    val txCount = when (dayOffset % 7) {
                        0, 1 -> 3 // Weekend peak
                        3, 4 -> 2 // Mid-week
                        else -> 1 // Regular day
                    }

                    for (t in 0 until txCount) {
                        val time = baseTime + (t * 3600000L) + (1000 * 60 * 15)
                        val payMethod = if ((dayOffset + t) % 2 == 0) "CASH" else "DIGITAL"
                        
                        // Select products
                        val p1Id = insertedIds.getOrElse(t % insertedIds.size) { 1L }
                        val p2Id = insertedIds.getOrElse((t + 3) % insertedIds.size) { 2L }

                        val p1 = app.repository.getProductById(p1Id)
                        val p2 = app.repository.getProductById(p2Id)

                        val items = mutableListOf<com.tataskan.pos.data.local.entity.TransactionItem>()
                        var subtotal = 0.0

                        if (p1 != null) {
                            items.add(
                                com.tataskan.pos.data.local.entity.TransactionItem(
                                    transactionId = 0, productId = p1.id, productName = p1.name,
                                    quantity = (1..3).random(), priceAtSale = p1.price, costAtSale = p1.cost
                                )
                            )
                            subtotal += items.last().priceAtSale * items.last().quantity
                        }
                        if (p2 != null) {
                            items.add(
                                com.tataskan.pos.data.local.entity.TransactionItem(
                                    transactionId = 0, productId = p2.id, productName = p2.name,
                                    quantity = (1..2).random(), priceAtSale = p2.price, costAtSale = p2.cost
                                )
                            )
                            subtotal += items.last().priceAtSale * items.last().quantity
                        }

                        if (items.isNotEmpty()) {
                            val tax = subtotal * 0.12
                            val grandTotal = subtotal + tax
                            app.repository.completeTransaction(
                                Transaction(
                                    total = grandTotal,
                                    amountReceived = if (payMethod == "DIGITAL") grandTotal else (grandTotal + 50.0),
                                    taxAmount = tax,
                                    discountAmount = 0.0,
                                    paymentMethod = payMethod,
                                    timestamp = time
                                ),
                                items
                            )
                        }
                    }
                }
            }
            onComplete()
        }
    }

    fun generateBackup(onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val file = backupRepository.performLocalAutoBackup()
            if (file != null) {
                repository.updateLastLocalBackupTime(System.currentTimeMillis())
                refreshBackups()
                onResult(true)
            } else {
                onResult(false)
            }
        }
    }

    fun deleteBackup(file: File) {
        viewModelScope.launch {
            backupRepository.deleteInternalBackup(file)
            refreshBackups()
        }
    }

    suspend fun importBackup(inputStream: InputStream): Boolean {
        return backupRepository.importDatabase(inputStream)
    }

    fun deleteAccountAndData(onComplete: () -> Unit) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                app.database.close()
                val dbName = "tataskan_database"
                val dbFile = app.getDatabasePath(dbName)
                if (dbFile.exists()) dbFile.delete()
                val walFile = File(dbFile.path + "-wal")
                val shmFile = File(dbFile.path + "-shm")
                if (walFile.exists()) walFile.delete()
                if (shmFile.exists()) shmFile.delete()
                backupRepository.clearAllBackups()
                repository.clearAllSettings()
            }
            onComplete()
        }
    }
}
