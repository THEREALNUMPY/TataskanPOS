package com.tataskan.pos.ui.product

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.ui.components.*
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.PdfUtils
import com.tataskan.pos.util.Strings

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListPane(
    products: List<Product>,
    categories: List<com.tataskan.pos.data.entity.Category> = emptyList(),
    currencySymbol: String = "₱",
    lang: String = "en",
    error: String? = null,
    onClearError: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onStockAdjust: (Product, Int) -> Unit,
    onAddProduct: () -> Unit,
    onNavigateToLabelGenerator: () -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var showLowStockOnly by remember { mutableStateOf(false) }
    var showCategoryMenu by remember { mutableStateOf(false) }
    var sortBy by remember { mutableStateOf("NEWEST") } // NEWEST, OLDEST, NAME, PRICE
    var showSortMenu by remember { mutableStateOf(false) }
    
    var currentPage by remember { mutableIntStateOf(0) }
    val pageSize = 15

    val allFilteredProducts by remember(products, searchQuery, selectedCategory, showLowStockOnly, sortBy) {
        derivedStateOf {
            val filtered = products.filter { product ->
                val matchesSearch = if (searchQuery.isEmpty()) true
                else product.name.contains(searchQuery, ignoreCase = true) || 
                     product.category.contains(searchQuery, ignoreCase = true) ||
                     (product.barcode?.contains(searchQuery) ?: false)
                
                val matchesCategory = if (selectedCategory == null) true
                else product.category == selectedCategory
                
                val matchesLowStock = if (!showLowStockOnly) true
                else product.stock <= 5

                matchesSearch && matchesCategory && matchesLowStock
            }

            when (sortBy) {
                "NEWEST" -> filtered.sortedByDescending { it.id }
                "OLDEST" -> filtered.sortedBy { it.id }
                "NAME" -> filtered.sortedBy { it.name }
                "PRICE_HIGH" -> filtered.sortedByDescending { it.price }
                "PRICE_LOW" -> filtered.sortedBy { it.price }
                else -> filtered
            }
        }
    }

    val filteredProducts by remember(allFilteredProducts, currentPage) {
        derivedStateOf {
            allFilteredProducts.drop(currentPage * pageSize).take(pageSize)
        }
    }

    val totalPages = remember(allFilteredProducts) {
        (allFilteredProducts.size + pageSize - 1) / pageSize
    }

    LaunchedEffect(searchQuery, selectedCategory) {
        currentPage = 0
    }

    val snackbarHostState = remember { SnackbarHostState() }
    
    LaunchedEffect(error) {
        error?.let {
            snackbarHostState.showSnackbar(it)
            onClearError()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(56.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.weight(1f).fillMaxHeight().tutorialTarget("inv_search", tutorialViewModel),
                    placeholder = { 
                        Text(
                            text = "Search product...", 
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.padding(top = 2.dp) // Shift downward slightly
                        ) 
                    },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                )

                Surface(
                    modifier = Modifier.weight(1f).fillMaxHeight().tutorialTarget("inv_low_stock", tutorialViewModel),
                    shape = MaterialTheme.shapes.medium,
                    color = if (showLowStockOnly) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (showLowStockOnly) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outlineVariant),
                    onClick = { showLowStockOnly = !showLowStockOnly }
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (showLowStockOnly) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Low Stock", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                    }
                }

                IconButton(
                    onClick = { showSortMenu = true },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        Icons.Default.Sort, 
                        contentDescription = "Sort products",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                IconButton(
                    onClick = { showCategoryMenu = true },
                    modifier = Modifier.size(48.dp).tutorialTarget("inv_filter", tutorialViewModel)
                ) {
                    Icon(
                        Icons.Default.FilterList, 
                        contentDescription = "Filter by category",
                        tint = if (selectedCategory != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
                
                Box {
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Newly Added") },
                            onClick = { sortBy = "NEWEST"; showSortMenu = false },
                            leadingIcon = { if (sortBy == "NEWEST") Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Oldest First") },
                            onClick = { sortBy = "OLDEST"; showSortMenu = false },
                            leadingIcon = { if (sortBy == "OLDEST") Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Name (A-Z)") },
                            onClick = { sortBy = "NAME"; showSortMenu = false },
                            leadingIcon = { if (sortBy == "NAME") Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Price (High to Low)") },
                            onClick = { sortBy = "PRICE_HIGH"; showSortMenu = false },
                            leadingIcon = { if (sortBy == "PRICE_HIGH") Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Price (Low to High)") },
                            onClick = { sortBy = "PRICE_LOW"; showSortMenu = false },
                            leadingIcon = { if (sortBy == "PRICE_LOW") Icon(Icons.Default.Check, contentDescription = null) }
                        )
                    }
                }
                
                Box {
                    DropdownMenu(
                        expanded = showCategoryMenu,
                        onDismissRequest = { showCategoryMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("All Categories") },
                            onClick = {
                                selectedCategory = null
                                showCategoryMenu = false
                            },
                            leadingIcon = { if (selectedCategory == null) Icon(Icons.Default.Check, contentDescription = null) }
                        )
                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategory = category.name
                                    showCategoryMenu = false
                                },
                                leadingIcon = { if (selectedCategory == category.name) Icon(Icons.Default.Check, contentDescription = null) }
                            )
                        }
                    }
                }
            }

            if (selectedCategory != null) {
                Surface(
                    modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Category: $selectedCategory",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        IconButton(
                            onClick = { selectedCategory = null },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Close, 
                                contentDescription = "Clear filter",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SukiEmptyState(
                        message = if (searchQuery.isEmpty()) Strings.get("inventory_empty", lang) else Strings.get("no_matching_products", lang),
                        icon = if (searchQuery.isEmpty()) Icons.Default.Inventory else Icons.Default.SearchOff
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 300.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        val isFirst = filteredProducts.firstOrNull()?.id == product.id
                        ProductItemCard(
                            product = product,
                            currencySymbol = currencySymbol,
                            lang = lang,
                            onStockAdjust = { change -> onStockAdjust(product, change) },
                            onClick = { onProductClick(product) },
                            modifier = if (isFirst) Modifier.tutorialTarget("inv_card", tutorialViewModel) else Modifier
                        )
                    }

                    if (totalPages > 1) {
                        item(span = { GridItemSpan(maxLineSpan) }) {
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

        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            horizontalAlignment = Alignment.End
        ) {
            FloatingActionButton(
                onClick = onNavigateToLabelGenerator,
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.tutorialTarget("inv_print", tutorialViewModel)
            ) {
                Icon(Icons.Default.Print, contentDescription = Strings.get("label_defaults", lang))
            }
            Spacer(modifier = Modifier.height(16.dp))
            FloatingActionButton(
                onClick = onAddProduct,
                modifier = Modifier.tutorialTarget("inv_add", tutorialViewModel)
            ) {
                Icon(Icons.Default.Add, contentDescription = Strings.get("save_product", lang))
            }
        }
    }
}

@Composable
fun ProductItemCard(
    product: Product,
    currencySymbol: String,
    lang: String = "en",
    onStockAdjust: (Int) -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isOutOfStock = product.stock <= 0
    val isLowStock = product.stock in 1..5
    
    com.tataskan.pos.ui.components.TataskanCard(
        onClick = onClick,
        modifier = modifier,
        containerColor = if (isOutOfStock) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f) 
                        else MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(product.imageUri)
                    .crossfade(true)
                    .size(150)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(60.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.shapes.small)
                    .padding(2.dp),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = if (isOutOfStock) MaterialTheme.colorScheme.error else Color.Unspecified
                )
                Text(
                    text = product.category,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (isOutOfStock) {
                    Text(
                        text = Strings.get("out_of_stock", lang),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = com.tataskan.pos.util.CurrencyUtils.formatCurrency(product.price, currencySymbol),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SukiIconButton(
                        icon = Icons.Default.Remove,
                        onClick = { onStockAdjust(-1) },
                        modifier = Modifier.size(40.dp)
                    )
                    
                    Text(
                        text = product.stock.toString(),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = if (isLowStock || isOutOfStock) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                    )
                    
                    SukiIconButton(
                        icon = Icons.Default.Add,
                        onClick = { onStockAdjust(1) },
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        backgroundColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
