package com.tataskan.pos.ui.report

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tataskan.pos.data.relation.TransactionWithItems
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.PdfUtils
import com.tataskan.pos.util.Strings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportingScreen(
    viewModel: ReportViewModel,
    currencySymbol: String = "₱",
    lang: String = "en",
    onTransactionClick: (Long) -> Unit = {},
    tutorialViewModel: TutorialViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 10
    
    val allFilteredTransactions by remember {
        derivedStateOf {
            if (searchQuery.isEmpty()) uiState.recentTransactions
            else uiState.recentTransactions.filter { transaction ->
                transaction.items.any { item ->
                    item.productName.contains(searchQuery, ignoreCase = true)
                }
            }
        }
    }

    val filteredTransactions by remember {
        derivedStateOf {
            allFilteredTransactions.drop(currentPage * pageSize).take(pageSize)
        }
    }

    val totalPages by remember {
        derivedStateOf {
            (allFilteredTransactions.size + pageSize - 1) / pageSize
        }
    }

    LaunchedEffect(searchQuery) {
        currentPage = 0
    }

    fun exportToPdf() {
        if (uiState.recentTransactions.isEmpty()) return
        
        val uri = PdfUtils.generateSalesReportPdf(
            context = context,
            transactions = uiState.recentTransactions,
            currencySymbol = currencySymbol,
            todayRevenue = uiState.today.revenue,
            todayCount = uiState.today.transactionCount,
            monthlyRevenue = uiState.monthly.revenue,
            monthlyCount = uiState.monthly.transactionCount,
            weeklyTrend = uiState.weeklyTrends,
            topProductsList = uiState.topProducts
        )
        
        if (uri != null) {
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, Strings.get("share_report", lang)))
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            Strings.get("overview", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        IconButton(
                            onClick = { exportToPdf() },
                            modifier = Modifier.tutorialTarget("reporting_export_btn", tutorialViewModel)
                        ) {
                            Icon(Icons.Default.PictureAsPdf, contentDescription = "Export PDF", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        SummaryCard(
                            modifier = Modifier.weight(1f),
                            title = Strings.get("today", lang),
                            revenue = uiState.today.revenue,
                            count = uiState.today.transactionCount,
                            currencySymbol = currencySymbol,
                            lang = lang,
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                        SummaryCard(
                            modifier = Modifier.weight(1.2f),
                            title = Strings.get("this_month", lang),
                            revenue = uiState.monthly.revenue,
                            count = uiState.monthly.transactionCount,
                            currencySymbol = currencySymbol,
                            containerColor = MaterialTheme.colorScheme.surface,
                            lang = lang
                        )
                    }
                }

                item {
                    Text(
                        "Weekly Sales Trend",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    SalesBarChart(
                        salesData = uiState.weeklyTrends,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }

                if (uiState.topProducts.isNotEmpty()) {
                    item {
                        Text(
                            "Top Selling Products",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                uiState.topProducts.forEach { product ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(product.name, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                        Text(
                                            "${product.quantity} units",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(horizontal = 8.dp)
                                        )
                                        Text(
                                            CurrencyUtils.formatCurrency(product.revenue, currencySymbol),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    if (product != uiState.topProducts.last()) {
                                        HorizontalDivider(
                                            modifier = Modifier.padding(vertical = 4.dp),
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            Strings.get("recent_transactions", lang),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        placeholder = { Text("Search by product...", modifier = Modifier.padding(top = 2.dp)) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                }

                items(filteredTransactions, key = { it.transaction.id }) { transaction ->
                    TransactionItem(
                        transactionWithItems = transaction,
                        currencySymbol = currencySymbol,
                        lang = lang,
                        onClick = { onTransactionClick(transaction.transaction.id) }
                    )
                }

                if (totalPages > 1) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { if (currentPage > 0) currentPage-- },
                                enabled = currentPage > 0
                            ) {
                                Icon(Icons.Default.ChevronLeft, contentDescription = null)
                                Text("Prev")
                            }

                            Text(
                                "Page ${currentPage + 1} of $totalPages",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.outline
                            )

                            TextButton(
                                onClick = { if (currentPage < totalPages - 1) currentPage++ },
                                enabled = currentPage < totalPages - 1
                            ) {
                                Text("Next")
                                Icon(Icons.Default.ChevronRight, contentDescription = null)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SalesBarChart(
    salesData: List<DailySales>,
    modifier: Modifier = Modifier
) {
    val maxRevenue = (salesData.maxOfOrNull { it.revenue } ?: 0.0).coerceAtLeast(100.0)
    val today = remember { SimpleDateFormat("EE", Locale.getDefault()).format(Date()) }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(modifier = Modifier.padding(20.dp)) {
            // Y-Axis Labels and Gridlines
            Column(
                modifier = Modifier.fillMaxSize().padding(bottom = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                listOf(maxRevenue, maxRevenue * 0.5, 0.0).forEach { valLabel ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (valLabel >= 1000) "${(valLabel/1000).toInt()}k" else valLabel.toInt().toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f),
                            modifier = Modifier.width(28.dp)
                        )
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                            thickness = 0.5.dp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxSize().padding(start = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                salesData.forEach { data ->
                    val isToday = data.date == today
                    val barHeightFactor = (data.revenue / maxRevenue).toFloat()
                    
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        if (data.revenue > 0) {
                            Text(
                                text = if (data.revenue >= 1000) "${(data.revenue/1000).toInt()}k" else data.revenue.toInt().toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 8.sp,
                                color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                            )
                        }
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(barHeightFactor.coerceAtLeast(0.05f))
                                .background(
                                    color = if (isToday) MaterialTheme.colorScheme.primary 
                                            else if (barHeightFactor > 0) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                            else MaterialTheme.colorScheme.outlineVariant,
                                    shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                )
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = data.date,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isToday) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    modifier: Modifier = Modifier,
    title: String,
    revenue: Double,
    count: Int,
    currencySymbol: String = "₱",
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    lang: String = "en"
) {
    val isZero = revenue == 0.0
    ElevatedCard(
        modifier = modifier,
        colors = CardDefaults.elevatedCardColors(
            containerColor = containerColor,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(), 
                style = MaterialTheme.typography.labelSmall, 
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
            
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = CurrencyUtils.formatCurrency(revenue, currencySymbol),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Black,
                    color = if (isZero) MaterialTheme.colorScheme.outline.copy(alpha = 0.4f) else MaterialTheme.colorScheme.primary,
                    maxLines = 1
                )
                if (!isZero) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .padding(bottom = 6.dp)
                            .background(Color(0xFFE8F5E9), CircleShape)
                            .padding(4.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.TrendingUp, 
                            contentDescription = null, 
                            tint = Color(0xFF2E7D32), 
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            Text(
                text = "$count ${Strings.get("transactions_count", lang)}", 
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
fun TransactionItem(
    transactionWithItems: TransactionWithItems,
    currencySymbol: String = "₱",
    lang: String = "en",
    onClick: () -> Unit = {}
) {
    val transaction = transactionWithItems.transaction
    val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    val dateString = dateFormat.format(Date(transaction.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${Strings.get("receipt_no", lang)}: #${transaction.id}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Surface(
                        color = if (transaction.paymentMethod == "DIGITAL") MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(4.dp)
                    ) {
                        Text(
                            text = if (transaction.paymentMethod == "DIGITAL") "DIGITAL" else "CASH",
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
                Text(dateString, style = MaterialTheme.typography.bodySmall)
                Text(
                    "${transactionWithItems.items.size} ${Strings.get("items_count", lang)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                CurrencyUtils.formatCurrency(transaction.total, currencySymbol),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
