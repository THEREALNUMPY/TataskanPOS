package com.tataskan.pos.ui.pos

import androidx.compose.foundation.background
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tataskan.pos.data.relation.TransactionWithItems
import com.tataskan.pos.ui.settings.SettingsViewModel
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.PdfUtils
import com.tataskan.pos.util.Strings
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReceiptScreen(
    transactionId: Long,
    posViewModel: PosViewModel,
    settingsViewModel: SettingsViewModel,
    onDone: () -> Unit
) {
    var transaction by remember { mutableStateOf<TransactionWithItems?>(null) }
    val storeName by settingsViewModel.storeName.collectAsState()
    val storeLogo by settingsViewModel.storeLogoUri.collectAsState()
    val storeAddress by settingsViewModel.storeAddress.collectAsState()
    val phoneNumber by settingsViewModel.phoneNumber.collectAsState()
    val currencySymbol by settingsViewModel.currencySymbol.collectAsState()
    val receiptFooter by settingsViewModel.receiptFooter.collectAsState()
    val lang by settingsViewModel.language.collectAsState()

    LaunchedEffect(transactionId) {
        transaction = posViewModel.getTransactionById(transactionId)
    }

    val context = LocalContext.current

    fun exportPdf(share: Boolean = false) {
        transaction?.let { tx ->
            val uri = PdfUtils.generateReceiptPdf(
                context, tx, storeName, storeAddress, phoneNumber, currencySymbol, receiptFooter
            )
            if (uri != null) {
                val intent = Intent(if (share) Intent.ACTION_SEND else Intent.ACTION_VIEW).apply {
                    if (share) {
                        type = "application/pdf"
                        putExtra(Intent.EXTRA_STREAM, uri)
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    } else {
                        setDataAndType(uri, "application/pdf")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    }
                }
                context.startActivity(Intent.createChooser(intent, if (share) Strings.get("share", lang) else Strings.get("receipt", lang)))
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (transaction != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Actions at the top
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(onClick = { exportPdf(false) }) {
                        Icon(Icons.Default.PictureAsPdf, contentDescription = Strings.get("save_pdf", lang), tint = MaterialTheme.colorScheme.primary)
                    }
                    IconButton(onClick = { exportPdf(true) }) {
                        Icon(Icons.Default.Share, contentDescription = Strings.get("share", lang), tint = MaterialTheme.colorScheme.primary)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Receipt Header
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (storeLogo != null) {
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(LocalContext.current)
                                    .data(storeLogo)
                                    .crossfade(true)
                                    .size(300)
                                    .build(),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp).clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        
                        Text(storeName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.ExtraBold)
                        if (storeAddress.isNotBlank()) {
                            Text(storeAddress, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, color = Color.Gray)
                        }
                        if (phoneNumber.isNotBlank()) {
                            Text("${Strings.get("phone_number", lang)}: $phoneNumber", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray)
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${Strings.get("receipt_no", lang)}: #${transaction!!.transaction.id}", style = MaterialTheme.typography.bodySmall)
                            val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(transaction!!.transaction.timestamp))
                            Text(date, style = MaterialTheme.typography.bodySmall)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Items list
                        transaction!!.items.forEach { item ->
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("${item.quantity}x ${item.productName}", modifier = Modifier.weight(1f))
                                Text(CurrencyUtils.formatCurrency(item.priceAtSale * item.quantity, currencySymbol))
                            }
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), thickness = 1.dp, color = Color.LightGray)
                        
                        // Totals Breakdown
                        val itemsSubtotal = transaction!!.items.sumOf { it.priceAtSale * it.quantity }
                        val txDiscount = transaction!!.transaction.discountAmount
                        val txTax = transaction!!.transaction.taxAmount

                        if (txDiscount > 0 || txTax > 0) {
                            SummaryRow("Subtotal", itemsSubtotal, currencySymbol)
                            if (txDiscount > 0) {
                                SummaryRow("Discount (${transaction!!.transaction.promoName ?: "Promo"})", -txDiscount, currencySymbol, color = MaterialTheme.colorScheme.error)
                            }
                            if (txTax > 0) {
                                SummaryRow("Tax", txTax, currencySymbol)
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp), thickness = 0.5.dp, color = Color.LightGray)
                        }
                        
                        val payMethodLabel = if (transaction!!.transaction.paymentMethod == "DIGITAL") Strings.get("payment_digital", lang) else Strings.get("payment_cash", lang)
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(Strings.get("payment_method", lang), style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                            Text(payMethodLabel, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                        }

                        SummaryRow(Strings.get("total", lang), transaction!!.transaction.total, currencySymbol, isBold = true)
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        SummaryRow(Strings.get("cash_received", lang), transaction!!.transaction.amountReceived, currencySymbol)
                        SummaryRow(Strings.get("change_label", lang), (transaction!!.transaction.amountReceived - transaction!!.transaction.total).coerceAtLeast(0.0), currencySymbol)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        Text(
                            receiptFooter,
                            style = MaterialTheme.typography.bodyMedium,
                            fontFamily = FontFamily.Monospace,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = onDone,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.Check, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("done", lang))
                }
            }
        } else {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
fun SummaryRow(label: String, amount: Double, symbol: String, isBold: Boolean = false, color: Color = Color.Unspecified) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal, color = color)
        Text(
            CurrencyUtils.formatCurrency(amount, symbol),
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontSize = if (isBold) 18.sp else 16.sp,
            color = color
        )
    }
}
