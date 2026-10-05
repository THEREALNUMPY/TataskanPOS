package com.tataskan.pos.ui.promo

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tataskan.pos.data.entity.Promo
import com.tataskan.pos.data.entity.PromoType
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.Strings
import kotlinx.coroutines.launch

@Composable
fun PromoManagementScreen(
    viewModel: PromoViewModel,
    products: List<com.tataskan.pos.data.local.entity.Product>,
    currencySymbol: String = "₱",
    lang: String = "en",
    tutorialViewModel: TutorialViewModel
) {
    val promos by viewModel.promos.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        if (promos.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(Strings.get("no_promos", lang), color = MaterialTheme.colorScheme.outline)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp, 16.dp, 16.dp, 100.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(promos) { promo ->
                    PromoItemCard(
                        promo = promo, 
                        lang = lang, 
                        currencySymbol = currencySymbol,
                        products = products,
                        onDelete = { viewModel.deletePromo(promo) },
                        onDownload = {
                            val success = com.tataskan.pos.util.PdfUtils.savePromoToGallery(context, promo, currencySymbol)
                            scope.launch {
                                snackbarHostState.showSnackbar(
                                    if (success) Strings.get("saved_to_gallery", lang) 
                                    else Strings.get("export_failed", lang)
                                )
                            }
                        }
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .tutorialTarget("promo_add_fab", tutorialViewModel)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Promo")
        }

        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))

        if (showAddDialog) {
            AddEditPromoDialog(
                onDismiss = { showAddDialog = false },
                products = products,
                onSave = { name, code, type, value, prodId, limit ->
                    viewModel.savePromo(name = name, code = code, type = type, value = value, productId = prodId, usageLimit = limit)
                    showAddDialog = false
                },
                generateCode = { viewModel.generateRandomCode() },
                lang = lang
            )
        }
    }
}

@Composable
fun PromoItemCard(
    promo: Promo, 
    lang: String, 
    currencySymbol: String,
    products: List<com.tataskan.pos.data.local.entity.Product>,
    onDelete: () -> Unit,
    onDownload: () -> Unit
) {
    val isExpired = promo.isExpired
    val targetProduct = remember(promo.productId, products) {
        products.find { it.id == promo.productId }
    }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isExpired) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
                           else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(promo.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                
                if (targetProduct != null) {
                    Text(
                        text = "Target: ${targetProduct.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                
                Text("Code: ${promo.code}", style = MaterialTheme.typography.bodySmall)
                
                val typeLabel = when(promo.type) {
                    PromoType.PERCENTAGE_TOTAL -> "${promo.value}% OFF Total"
                    PromoType.FIXED_TOTAL -> "₱${promo.value} OFF Total"
                    PromoType.PERCENTAGE_PRODUCT -> "${promo.value}% OFF Product"
                    PromoType.FIXED_PRODUCT -> "₱${promo.value} OFF Product"
                }
                
                Text(
                    text = typeLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                if (promo.usageLimit != null) {
                    Text(
                        text = "Used: ${promo.currentUsage} / ${promo.usageLimit}",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(text = "Usage: ${promo.currentUsage} (Unlimited)", style = MaterialTheme.typography.labelSmall)
                }

                if (isExpired) {
                    Text("EXPIRED / LIMIT REACHED", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            }
            
            Row {
                IconButton(onClick = onDownload) {
                    Icon(Icons.Default.Download, contentDescription = "Download Image", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPromoDialog(
    onDismiss: () -> Unit,
    products: List<com.tataskan.pos.data.local.entity.Product>,
    onSave: (String, String, PromoType, Double, Long?, Int?) -> Unit,
    generateCode: () -> String,
    lang: String
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf(generateCode()) }
    var type by remember { mutableStateOf(PromoType.PERCENTAGE_TOTAL) }
    var value by remember { mutableStateOf("") }
    var selectedProductId by remember { mutableStateOf<Long?>(null) }
    var usageLimit by remember { mutableStateOf("") }

    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(Strings.get("add_promo", lang)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = name, 
                    onValueChange = { name = it }, 
                    label = { Text("Promo Name") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    singleLine = true
                )
                OutlinedTextField(
                    value = code, 
                    onValueChange = { code = it.filter { c -> !c.isWhitespace() }.uppercase() }, 
                    label = { Text("Promo Code") }, 
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { code = generateCode() }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Regenerate")
                        }
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    singleLine = true
                )
                
                Text("Type", style = MaterialTheme.typography.labelSmall)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == PromoType.PERCENTAGE_TOTAL,
                            onClick = { type = PromoType.PERCENTAGE_TOTAL },
                            label = { Text("% Total") }
                        )
                        FilterChip(
                            selected = type == PromoType.FIXED_TOTAL,
                            onClick = { type = PromoType.FIXED_TOTAL },
                            label = { Text("₱ Total") }
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = type == PromoType.PERCENTAGE_PRODUCT,
                            onClick = { type = PromoType.PERCENTAGE_PRODUCT },
                            label = { Text("% Product") }
                        )
                        FilterChip(
                            selected = type == PromoType.FIXED_PRODUCT,
                            onClick = { type = PromoType.FIXED_PRODUCT },
                            label = { Text("₱ Product") }
                        )
                    }
                }

                if (type == PromoType.PERCENTAGE_PRODUCT || type == PromoType.FIXED_PRODUCT) {
                    val selectedProd = products.find { it.id == selectedProductId }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = selectedProd?.name ?: "Select Product",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Target Product") },
                            trailingIcon = { 
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown, contentDescription = null)
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            products.forEach { prod ->
                                DropdownMenuItem(
                                    text = { Text(prod.name) },
                                    onClick = {
                                        selectedProductId = prod.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = value, 
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") value = it }, 
                    label = { Text(if (type == PromoType.PERCENTAGE_TOTAL || type == PromoType.PERCENTAGE_PRODUCT) "Percentage (%)" else "Amount (₱)") }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true
                )

                OutlinedTextField(
                    value = usageLimit,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) usageLimit = it },
                    label = { Text("Usage Limit (Optional)") },
                    placeholder = { Text("Unlimited") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank() && code.isNotBlank() && value.toDoubleOrNull() != null) {
                        onSave(
                            name.trim(), 
                            code.trim(), 
                            type, 
                            value.toDoubleOrNull() ?: 0.0, 
                            selectedProductId,
                            usageLimit.toIntOrNull()
                        )
                    }
                }
            ) { Text(Strings.get("confirm", lang)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(Strings.get("cancel", lang)) }
        }
    )
}
