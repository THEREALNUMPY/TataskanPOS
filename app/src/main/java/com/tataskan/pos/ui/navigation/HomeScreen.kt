package com.tataskan.pos.ui.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tataskan.pos.data.relation.TransactionWithItems
import com.tataskan.pos.ui.components.*
import com.tataskan.pos.ui.report.ReportViewModel
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.Strings

@Composable
fun HomeScreen(
    onNavigateToPos: () -> Unit,
    onNavigateToInventory: () -> Unit,
    onNavigateToReports: () -> Unit,
    storeName: String,
    lang: String,
    reportViewModel: ReportViewModel,
    currencySymbol: String,
    onTransactionClick: (Long) -> Unit,
    tutorialViewModel: TutorialViewModel,
    windowSizeClass: WindowSizeClass,
    daysRemaining: Int = 15
) {
    val uiState by reportViewModel.uiState.collectAsStateWithLifecycle()
    val stats by remember { derivedStateOf { uiState.stats } }
    val recentTransactions by remember { derivedStateOf { uiState.recentTransactions } }
    
    val scrollState = rememberScrollState()
    val activeStep by tutorialViewModel.activeStep.collectAsStateWithLifecycle()

    LaunchedEffect(activeStep?.id) {
        if (activeStep?.id == "pos") {
            scrollState.animateScrollTo(0)
        }
    }

    val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val horizontalPadding = if (isExpanded) 32.dp else 16.dp

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = horizontalPadding, vertical = 24.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.fillMaxWidth().widthIn(max = 1200.dp)) {
            Column {
                Text(
                    text = "Dashboard",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Black
                )
                Text(
                    text = storeName,
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = when {
                        daysRemaining <= 3 -> MaterialTheme.colorScheme.errorContainer
                        daysRemaining <= 5 -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.secondaryContainer
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Trial Days Remaining",
                            modifier = Modifier.size(14.dp),
                            tint = when {
                                daysRemaining <= 3 -> MaterialTheme.colorScheme.onErrorContainer
                                daysRemaining <= 5 -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                        Text(
                            text = "Trial Version: $daysRemaining days left",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when {
                                daysRemaining <= 3 -> MaterialTheme.colorScheme.onErrorContainer
                                daysRemaining <= 5 -> MaterialTheme.colorScheme.onTertiaryContainer
                                else -> MaterialTheme.colorScheme.onSecondaryContainer
                            }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        TataskanCard(modifier = Modifier.widthIn(max = 1200.dp)) {
            Text(
                text = "TODAY'S PERFORMANCE",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SukiPriceDisplay(
                    label = "Sales",
                    amount = stats.todayRevenue,
                    currencySymbol = currencySymbol,
                    modifier = Modifier.weight(1f)
                )
                
                SukiPriceDisplay(
                    label = "Profit",
                    amount = stats.todayProfit,
                    currencySymbol = currencySymbol,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.weight(1f)
                )
                
                Column(modifier = Modifier.weight(0.7f)) {
                    Text(
                        text = "COUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = stats.todayTransactions.toString(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.secondary,
                        maxLines = 1
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (stats.outOfStockCount > 0 || stats.lowStockCount > 0) {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 1200.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (stats.outOfStockCount > 0) {
                        StockAlertItem(
                            count = stats.outOfStockCount,
                            label = "Out of Stock",
                            isCritical = true,
                            onClick = onNavigateToInventory
                        )
                    }
                    if (stats.lowStockCount > 0) {
                        StockAlertItem(
                            count = stats.lowStockCount,
                            label = "Low in Stock",
                            isCritical = false,
                            onClick = onNavigateToInventory
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Spacer(modifier = Modifier.height(32.dp))

        if (recentTransactions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().widthIn(max = 1200.dp)) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SukiSectionTitle(title = "Recent Transactions")
                        TextButton(onClick = onNavigateToReports) { 
                            Text("SEE ALL", fontWeight = FontWeight.Bold) 
                        }
                    }
                    
                    recentTransactions.take(if (isExpanded) 4 else 2).forEach { tx ->
                        com.tataskan.pos.ui.report.TransactionItem(
                            transactionWithItems = tx,
                            currencySymbol = currencySymbol,
                            lang = lang,
                            onClick = { onTransactionClick(tx.transaction.id) }
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun StockAlertItem(
    count: Int,
    label: String,
    isCritical: Boolean,
    onClick: () -> Unit
) {
    val containerColor = if (isCritical) MaterialTheme.colorScheme.error else Color(0xFFFF9800)
    val contentColor = if (isCritical) MaterialTheme.colorScheme.onError else Color.White

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = containerColor,
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (isCritical) Icons.Default.Error else Icons.Default.Warning,
                contentDescription = null,
                tint = contentColor
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "$count Items $label",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Black,
                color = contentColor
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = contentColor)
        }
    }
}
