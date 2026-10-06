package com.tataskan.pos.ui.pos

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Money
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.Strings
import java.util.Locale
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: PosViewModel,
    currencySymbol: String = "₱",
    taxPercentage: Float = 0f,
    gcashQrUri: String? = null,
    mayaQrUri: String? = null,
    lang: String = "en",
    onSaleCompleted: (Long) -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    val grandTotal by viewModel.grandTotal.collectAsState()
    val hasDigitalQr = gcashQrUri != null || mayaQrUri != null
    var selectedPaymentMethod by remember { mutableStateOf("CASH") } // "CASH" or "DIGITAL"
    var amountReceivedText by remember(grandTotal) {
        mutableStateOf(if (grandTotal > 0) String.format(Locale.US, "%.2f", grandTotal) else "")
    }
    var showConfirmDialog by remember { mutableStateOf(false) }
    var showQrModal by remember { mutableStateOf(false) }
    var activeQrTab by remember(gcashQrUri, mayaQrUri) {
        mutableStateOf(if (gcashQrUri != null) "GCASH" else "MAYA")
    }
    
    val amountReceived = amountReceivedText.toDoubleOrNull() ?: 0.0
    val change = if (amountReceived >= grandTotal) amountReceived - grandTotal else 0.0

    LaunchedEffect(selectedPaymentMethod, grandTotal) {
        if (selectedPaymentMethod == "DIGITAL") {
            amountReceivedText = String.format(Locale.US, "%.2f", grandTotal)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Grand Total Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(Strings.get("grand_total", lang), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = CurrencyUtils.formatCurrency(grandTotal, currencySymbol),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Payment Method Selector
            Text(
                text = Strings.get("payment_method", lang),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = selectedPaymentMethod == "CASH",
                    onClick = {
                        selectedPaymentMethod = "CASH"
                        if (amountReceivedText.isBlank()) {
                            amountReceivedText = String.format(Locale.US, "%.2f", grandTotal)
                        }
                    },
                    label = { Text(Strings.get("payment_cash", lang), fontWeight = FontWeight.Bold) },
                    leadingIcon = { Icon(Icons.Default.Money, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = if (hasDigitalQr) Modifier.weight(1f).height(48.dp) else Modifier.fillMaxWidth().height(48.dp),
                    shape = MaterialTheme.shapes.medium
                )

                if (hasDigitalQr) {
                    FilterChip(
                        selected = selectedPaymentMethod == "DIGITAL",
                        onClick = { 
                            selectedPaymentMethod = "DIGITAL"
                            amountReceivedText = String.format(Locale.US, "%.2f", grandTotal)
                            showQrModal = true
                        },
                        label = { Text("Digital (QR)", fontWeight = FontWeight.Bold) },
                        leadingIcon = { Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier.weight(1f).height(48.dp),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            if (selectedPaymentMethod == "DIGITAL" && hasDigitalQr) {
                OutlinedButton(
                    onClick = { showQrModal = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("show_merchant_qr", lang), fontWeight = FontWeight.Bold)
                }
            }

            OutlinedTextField(
                value = amountReceivedText,
                onValueChange = { 
                    if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") {
                        amountReceivedText = it
                        val valDouble = it.toDoubleOrNull() ?: 0.0
                        if (valDouble >= grandTotal && grandTotal > 0) {
                            tutorialViewModel.nextStep()
                        }
                    }
                },
                label = { Text(Strings.get("amount_received", lang)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .tutorialTarget("amount_input", tutorialViewModel),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                singleLine = true,
                prefix = { Text(currencySymbol, style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(end = 4.dp)) },
                shape = MaterialTheme.shapes.medium
            )

            if (selectedPaymentMethod == "CASH") {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val totals = listOf(grandTotal, 20.0, 50.0, 100.0, 500.0, 1000.0)
                        .filter { it >= grandTotal && it > 0 }
                        .distinct()
                        .sorted()
                    
                    totals.forEach { amount ->
                        SuggestionChip(
                            onClick = { amountReceivedText = String.format(Locale.US, "%.2f", amount) },
                            label = { Text(CurrencyUtils.formatCurrency(amount, currencySymbol)) }
                        )
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (amountReceived >= grandTotal) 
                        MaterialTheme.colorScheme.secondaryContainer 
                    else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(Strings.get("change", lang), style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = CurrencyUtils.formatCurrency(change, currencySymbol),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = if (amountReceived >= grandTotal) 
                            MaterialTheme.colorScheme.primary 
                        else MaterialTheme.colorScheme.error
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = {
                    if (amountReceived >= grandTotal && grandTotal > 0) {
                        showConfirmDialog = true
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp)
                    .tutorialTarget("complete_sale_btn", tutorialViewModel),
                enabled = (amountReceived >= grandTotal && grandTotal > 0) || (grandTotal == 0.0 && amountReceived >= 0),
                shape = MaterialTheme.shapes.medium,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp),
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("complete_sale", lang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Tabbed Merchant GCash / Maya QR Modal Dialog
        if (showQrModal && hasDigitalQr) {
            AlertDialog(
                onDismissRequest = { showQrModal = false },
                title = {
                    Column {
                        Text(Strings.get("digital_payment_qr", lang), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "${Strings.get("scan_qr_to_pay", lang)} ${CurrencyUtils.formatCurrency(grandTotal, currencySymbol)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (gcashQrUri != null && mayaQrUri != null) {
                            PrimaryTabRow(
                                selectedTabIndex = if (activeQrTab == "GCASH") 0 else 1,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Tab(
                                    selected = activeQrTab == "GCASH",
                                    onClick = { activeQrTab = "GCASH" },
                                    text = { Text("GCash QR", fontWeight = FontWeight.Bold) }
                                )
                                Tab(
                                    selected = activeQrTab == "MAYA",
                                    onClick = { activeQrTab = "MAYA" },
                                    text = { Text("Maya QR", fontWeight = FontWeight.Bold) }
                                )
                            }
                        }

                        val targetUri = if (activeQrTab == "GCASH") (gcashQrUri ?: mayaQrUri) else (mayaQrUri ?: gcashQrUri)
                        val targetTitle = if (activeQrTab == "GCASH") "GCash QR Code" else "Maya QR Code"

                        if (targetUri != null) {
                            Text(
                                text = targetTitle,
                                fontWeight = FontWeight.Bold,
                                color = if (activeQrTab == "GCASH") Color(0xFF005CE6) else Color(0xFF00D632)
                            )
                            Card(
                                shape = MaterialTheme.shapes.large,
                                elevation = CardDefaults.cardElevation(3.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.size(240.dp)
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize().padding(12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = targetUri,
                                        contentDescription = targetTitle,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    Button(onClick = { showQrModal = false }) {
                        Text(Strings.get("done", lang))
                    }
                }
            )
        }

        if (showConfirmDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmDialog = false },
                title = { Text(Strings.get("confirm_sale", lang)) },
                text = { 
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("${Strings.get("payment_method", lang)}: ${if (selectedPaymentMethod == "DIGITAL") "Digital (QR)" else Strings.get("payment_cash", lang)}", fontWeight = FontWeight.SemiBold)
                        Text("${Strings.get("total", lang)}: ${CurrencyUtils.formatCurrency(grandTotal, currencySymbol)}")
                        Text("${Strings.get("received", lang)}: ${CurrencyUtils.formatCurrency(amountReceived, currencySymbol)}")
                        Text("${Strings.get("change", lang)}: ${CurrencyUtils.formatCurrency(change, currencySymbol)}", fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            showConfirmDialog = false
                            viewModel.completeSale(amountReceived, taxPercentage, selectedPaymentMethod) { transactionId ->
                                onSaleCompleted(transactionId)
                            }
                        }
                    ) {
                        Text(Strings.get("confirm", lang))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmDialog = false }) {
                        Text(Strings.get("cancel", lang))
                    }
                }
            )
        }
    }
}
