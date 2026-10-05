package com.tataskan.pos.ui.report

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.relation.TransactionWithItems
import com.tataskan.pos.data.repository.PosRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class SalesSummary(
    val revenue: Double,
    val profit: Double,
    val transactionCount: Int
)

data class DailySales(
    val date: String,
    val revenue: Double,
    val profit: Double
)

data class TopProduct(
    val name: String,
    val quantity: Int,
    val revenue: Double,
    val profit: Double
)

data class DashboardStats(
    val todayRevenue: Double = 0.0,
    val todayProfit: Double = 0.0,
    val todayTransactions: Int = 0,
    val lowStockCount: Int = 0,
    val outOfStockCount: Int = 0,
    val totalProducts: Int = 0,
    val bestSellerName: String = "N/A",
    val bestSellerQuantity: Int = 0,
    val inventoryValue: Double = 0.0,
    val monthlyProfit: Double = 0.0,
    val pendingBackups: Int = 0
)

data class ReportingUiState(
    val stats: DashboardStats = DashboardStats(),
    val today: SalesSummary = SalesSummary(0.0, 0.0, 0),
    val monthly: SalesSummary = SalesSummary(0.0, 0.0, 0),
    val weeklyTrends: List<DailySales> = emptyList(),
    val topProducts: List<TopProduct> = emptyList(),
    val recentTransactions: List<TransactionWithItems> = emptyList(),
    val isLoading: Boolean = true
)

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as TataskanApplication
    private val repository: PosRepository = app.repository
    private val backupRepository = com.tataskan.pos.data.repository.BackupRepository(application, app.database)

    private fun getTodayStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getMonthStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private fun getWeekStart(): Long {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -6)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    private val backupCountFlow = flow { 
        while(true) { 
            emit(backupRepository.getInternalBackups().size)
            kotlinx.coroutines.delay(60000) // Check every minute
        } 
    }.flowOn(Dispatchers.IO)

    val uiState: StateFlow<ReportingUiState> = combine(
        repository.allTransactions,
        repository.allProducts,
        backupCountFlow
    ) { transactions, products, backupCount ->
        val todayStart = getTodayStart()
        val monthStart = getMonthStart()
        val weekStart = getWeekStart()

        val todayTransactions = transactions.filter { it.transaction.timestamp >= todayStart }
        val monthTransactions = transactions.filter { it.transaction.timestamp >= monthStart }
        val weekTransactions = transactions.filter { it.transaction.timestamp >= weekStart }

        // 1. Dashboard Stats
        val todayRevenue = todayTransactions.sumOf { it.transaction.total }
        val todayProfit = todayTransactions.sumOf { tx -> 
            tx.items.sumOf { (it.priceAtSale - it.costAtSale) * it.quantity } 
        }
        val monthlyProfit = monthTransactions.sumOf { tx -> 
            tx.items.sumOf { (it.priceAtSale - it.costAtSale) * it.quantity } 
        }
        val lowStockCount = products.count { it.stock in 1..5 }
        val outOfStockCount = products.count { it.stock <= 0 }
        val totalProducts = products.size
        val inventoryValue = products.sumOf { it.cost * it.stock }
        
        val bestSellerGroup = transactions.flatMap { it.items }
            .groupBy { it.productName }
            .maxByOrNull { it.value.sumOf { item -> item.quantity } }
            
        val bestSellerName = bestSellerGroup?.key ?: "N/A"
        val bestSellerQty = bestSellerGroup?.value?.sumOf { it.quantity } ?: 0

        val stats = DashboardStats(
            todayRevenue = todayRevenue,
            todayProfit = todayProfit,
            todayTransactions = todayTransactions.size,
            lowStockCount = lowStockCount,
            outOfStockCount = outOfStockCount,
            totalProducts = totalProducts,
            bestSellerName = bestSellerName,
            bestSellerQuantity = bestSellerQty,
            inventoryValue = inventoryValue,
            monthlyProfit = monthlyProfit,
            pendingBackups = if (backupCount == 0) 1 else 0
        )

        // 2. Sales Summaries
        val todaySummary = SalesSummary(todayRevenue, todayProfit, todayTransactions.size)
        val monthlySummary = SalesSummary(
            revenue = monthTransactions.sumOf { it.transaction.total },
            profit = monthlyProfit,
            transactionCount = monthTransactions.size
        )

        // 3. Weekly Trends
        val dateFormat = SimpleDateFormat("EE", Locale.getDefault())
        val trendsMap = mutableMapOf<String, Pair<Double, Double>>()
        val cal = Calendar.getInstance()
        repeat(7) {
            trendsMap[dateFormat.format(cal.time)] = 0.0 to 0.0
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }
        weekTransactions.forEach {
            val day = dateFormat.format(Date(it.transaction.timestamp))
            val current = trendsMap[day] ?: (0.0 to 0.0)
            val txProfit = it.items.sumOf { item -> (item.priceAtSale - item.costAtSale) * item.quantity }
            trendsMap[day] = (current.first + it.transaction.total) to (current.second + txProfit)
        }
        val weeklyTrends = mutableListOf<DailySales>()
        val orderCal = Calendar.getInstance()
        orderCal.add(Calendar.DAY_OF_YEAR, -6)
        repeat(7) {
            val day = dateFormat.format(orderCal.time)
            val vals = trendsMap[day] ?: (0.0 to 0.0)
            weeklyTrends.add(DailySales(day, vals.first, vals.second))
            orderCal.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 4. Top Products
        val topProducts = transactions.asSequence()
            .flatMap { it.items }
            .groupBy { it.productName }
            .map { (name, items) ->
                TopProduct(
                    name = name,
                    quantity = items.sumOf { it.quantity },
                    revenue = items.sumOf { it.priceAtSale * it.quantity },
                    profit = items.sumOf { (it.priceAtSale - it.costAtSale) * it.quantity }
                )
            }
            .sortedByDescending { it.quantity }
            .take(5)
            .toList()

        ReportingUiState(
            stats = stats,
            today = todaySummary,
            monthly = monthlySummary,
            weeklyTrends = weeklyTrends,
            topProducts = topProducts,
            recentTransactions = transactions,
            isLoading = false
        )
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportingUiState())

    // Keep dashboardStats for Home screen compatibility
    val dashboardStats: StateFlow<DashboardStats> = uiState.map { it.stats }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())
        
    val recentTransactions: StateFlow<List<TransactionWithItems>> = uiState.map { it.recentTransactions }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
