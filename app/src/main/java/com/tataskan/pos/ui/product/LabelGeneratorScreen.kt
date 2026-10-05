package com.tataskan.pos.ui.product

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.PdfUtils
import com.tataskan.pos.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabelGeneratorScreen(
    products: List<Product>,
    currencySymbol: String,
    showNameOnLabel: Boolean,
    showPriceOnLabel: Boolean,
    lang: String = "en",
    tutorialViewModel: TutorialViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedLabelType by remember { mutableStateOf("BARCODE") } // "BARCODE" or "QR"
    var selectedLabelSize by remember { mutableStateOf("MEDIUM") }  // "SMALL", "MEDIUM", "LARGE"
    
    var selectedProductIds by remember { mutableStateOf(setOf<Long>()) }
    var globalQuantity by remember { mutableStateOf("1") }
    
    val filteredProducts = remember(products, searchQuery) {
        if (searchQuery.isEmpty()) products
        else products.filter { 
            it.name.contains(searchQuery, ignoreCase = true) || 
            it.category.contains(searchQuery, ignoreCase = true) ||
            (it.barcode ?: "").contains(searchQuery, ignoreCase = true)
        }
    }

    val isAllSelected = remember(filteredProducts, selectedProductIds) {
        filteredProducts.isNotEmpty() && filteredProducts.all { it.id in selectedProductIds }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Configuration & Interactive Preview Card
            Card(
                modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Top Row: Code Type & Mini Live Label Preview
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = Strings.get("code_type", lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = selectedLabelType == "BARCODE",
                                    onClick = { selectedLabelType = "BARCODE" },
                                    label = { Text(Strings.get("barcode_code", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.BarChart, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                                FilterChip(
                                    selected = selectedLabelType == "QR",
                                    onClick = { selectedLabelType = "QR" },
                                    label = { Text(Strings.get("qr_code", lang), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }

                        // Dynamic Mini Label Preview Card
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = Strings.get("label_preview", lang),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .size(
                                        width = when(selectedLabelSize) { "SMALL" -> 80.dp; "LARGE" -> 120.dp; else -> 100.dp },
                                        height = when(selectedLabelSize) { "SMALL" -> 50.dp; "LARGE" -> 75.dp; else -> 60.dp }
                                    ),
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, Color.LightGray),
                                shadowElevation = 1.dp
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxSize().padding(4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.SpaceAround
                                ) {
                                    if (showNameOnLabel) {
                                        Text(
                                            text = "Sample Item",
                                            fontSize = when(selectedLabelSize) { "SMALL" -> 6.sp; "LARGE" -> 9.sp; else -> 7.sp },
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black,
                                            maxLines = 1
                                        )
                                    }
                                    Icon(
                                        imageVector = if (selectedLabelType == "BARCODE") Icons.Default.BarChart else Icons.Default.QrCode,
                                        contentDescription = null,
                                        tint = Color.Black,
                                        modifier = Modifier.size(when(selectedLabelSize) { "SMALL" -> 16.dp; "LARGE" -> 28.dp; else -> 22.dp })
                                    )
                                    if (showPriceOnLabel) {
                                        Text(
                                            text = CurrencyUtils.formatCurrency(285.0, currencySymbol),
                                            fontSize = when(selectedLabelSize) { "SMALL" -> 6.sp; "LARGE" -> 9.sp; else -> 7.sp },
                                            fontWeight = FontWeight.ExtraBold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                    // Size Selector & Quantity
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(modifier = Modifier.weight(1.2f)) {
                            Text(
                                text = Strings.get("select_size", lang),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                listOf("SMALL", "MEDIUM", "LARGE").forEach { size ->
                                    FilterChip(
                                        selected = selectedLabelSize == size,
                                        onClick = { selectedLabelSize = size },
                                        label = { Text(size, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = globalQuantity,
                            onValueChange = { if (it.isEmpty() || it.all { char -> char.isDigit() }) globalQuantity = it },
                            label = { Text(Strings.get("qty_per_product", lang), fontSize = 10.sp) },
                            modifier = Modifier.weight(0.8f),
                            leadingIcon = { Icon(Icons.Default.FormatListNumbered, contentDescription = null, modifier = Modifier.size(16.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = MaterialTheme.shapes.medium
                        )
                    }
                }
            }

            // Search Bar & "Select All / Deselect All" Shortcut
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(Strings.get("filter_products", lang)) },
                    trailingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium
                )

                OutlinedButton(
                    onClick = {
                        selectedProductIds = if (isAllSelected) {
                            emptySet()
                        } else {
                            filteredProducts.map { it.id }.toSet()
                        }
                    },
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = if (isAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isAllSelected) Strings.get("deselect_all", lang) else Strings.get("select_all", lang),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Product Selection List
            if (filteredProducts.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(Strings.get("no_products_found", lang), color = MaterialTheme.colorScheme.outline)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isSelected = product.id in selectedProductIds
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable { 
                                    selectedProductIds = if (isSelected) selectedProductIds - product.id else selectedProductIds + product.id 
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                               else MaterialTheme.colorScheme.surface
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp, 
                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { 
                                            selectedProductIds = if (isSelected) selectedProductIds - product.id else selectedProductIds + product.id 
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(product.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (product.category.isNotBlank()) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.secondaryContainer,
                                                    shape = MaterialTheme.shapes.extraSmall
                                                ) {
                                                    Text(
                                                        text = product.category,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontSize = 9.sp,
                                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "Code: ${product.barcode ?: "N/A"}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }

                                Text(
                                    text = CurrencyUtils.formatCurrency(product.price, currencySymbol),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Sticky Bottom Action Bar
        if (selectedProductIds.isNotEmpty()) {
            Surface(
                modifier = Modifier.align(Alignment.BottomCenter),
                tonalElevation = 8.dp,
                shadowElevation = 8.dp
            ) {
                Button(
                    onClick = {
                        val qty = globalQuantity.toIntOrNull() ?: 1
                        val labelData = products.filter { it.id in selectedProductIds }
                            .map { it to qty }
                        
                        val uri = PdfUtils.generateLabelSheetPdf(
                            context = context,
                            products = labelData,
                            showName = showNameOnLabel,
                            showPrice = showPriceOnLabel,
                            currencySymbol = currencySymbol,
                            labelSizeStr = selectedLabelSize,
                            forceType = selectedLabelType
                        )
                        if (uri != null) {
                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(intent, Strings.get("share", lang)))
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .tutorialTarget("label_gen_btn", tutorialViewModel),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Icon(Icons.Default.PictureAsPdf, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    val total = selectedProductIds.size * (globalQuantity.toIntOrNull() ?: 1)
                    Text(
                        text = "${Strings.get("generate_pdf", lang)} ($total ${Strings.get("total_labels", lang)})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
