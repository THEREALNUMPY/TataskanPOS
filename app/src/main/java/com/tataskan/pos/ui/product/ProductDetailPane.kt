package com.tataskan.pos.ui.product

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.BarcodeUtils
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.PdfUtils
import com.tataskan.pos.util.Strings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailPane(
    product: Product?,
    currencySymbol: String = "₱",
    lang: String = "en",
    showNameOnLabel: Boolean = true,
    showPriceOnLabel: Boolean = true,
    onEdit: (Product) -> Unit,
    onDelete: (Product) -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()
    val activeStep by tutorialViewModel.activeStep.collectAsState()

    // Auto-scroll to bottom if tutorial is at download/delete steps
    LaunchedEffect(activeStep?.id) {
        if (activeStep?.id in listOf("inv_download", "inv_delete")) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    val barcodeBitmap = remember(product, currencySymbol, showNameOnLabel, showPriceOnLabel) {
        val code = product?.barcode ?: product?.id?.toString()
        code?.let { 
            BarcodeUtils.generateBarcodeWithText(
                barcodeText = it, 
                productName = product?.name ?: "", 
                price = CurrencyUtils.formatCurrency(product?.price ?: 0.0, currencySymbol), 
                width = 800, 
                height = 400,
                showName = showNameOnLabel,
                showPrice = showPriceOnLabel
            ) 
        }
    }
    val qrCodeBitmap = remember(product, currencySymbol, showNameOnLabel, showPriceOnLabel) {
        val code = product?.barcode ?: product?.id?.toString()
        code?.let { 
            BarcodeUtils.generateQRCodeWithText(
                qrText = it, 
                productName = product?.name ?: "", 
                price = CurrencyUtils.formatCurrency(product?.price ?: 0.0, currencySymbol), 
                size = 500,
                showName = showNameOnLabel,
                showPrice = showPriceOnLabel
            ) 
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (product == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(Strings.get("inventory_empty", lang))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                        .tutorialTarget("inv_detail", tutorialViewModel),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    if (product.imageUri != null) {
                        AsyncImage(
                            model = coil.request.ImageRequest.Builder(LocalContext.current)
                                .data(product.imageUri)
                                .crossfade(true)
                                .size(800) // Optimized for detail view
                                .build(),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Default.Image,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                DetailItem(Strings.get("product_name", lang), product.name)
                DetailItem(Strings.get("category", lang), product.category)
                DetailItem(Strings.get("price", lang), CurrencyUtils.formatCurrency(product.price, currencySymbol))
                DetailItem(Strings.get("cost", lang), CurrencyUtils.formatCurrency(product.cost, currencySymbol))
                DetailItem(Strings.get("stock_level", lang), product.stock.toString())
                DetailItem(Strings.get("barcode", lang), product.barcode ?: Strings.get("not_available", lang))

                if (barcodeBitmap != null || qrCodeBitmap != null) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = Strings.get("individual_downloads", lang),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (barcodeBitmap != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(120.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = barcodeBitmap.asImageBitmap(),
                                        contentDescription = "Barcode Label",
                                        modifier = Modifier.fillMaxHeight(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Button(
                                    onClick = {
                                        val uri = BarcodeUtils.saveBitmapToGallery(context, barcodeBitmap, "barcode_${product.name}.png")
                                        scope.launch {
                                            snackbarHostState.showSnackbar(if (uri != null) Strings.get("saved_to_gallery", lang) else Strings.get("error", lang))
                                        }
                                    },
                                    modifier = Modifier.padding(top = 12.dp).tutorialTarget("inv_download", tutorialViewModel)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Strings.get("download_barcode", lang))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    if (qrCodeBitmap != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
                        ) {
                            Column(
                                modifier = Modifier
                                    .padding(16.dp)
                                    .fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .background(Color.White),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = qrCodeBitmap.asImageBitmap(),
                                        contentDescription = "QR Label",
                                        modifier = Modifier.size(160.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Button(
                                    onClick = {
                                        val uri = BarcodeUtils.saveBitmapToGallery(context, qrCodeBitmap, "qr_${product.name}.png")
                                        scope.launch {
                                            snackbarHostState.showSnackbar(if (uri != null) Strings.get("saved_to_gallery", lang) else Strings.get("error", lang))
                                        }
                                    },
                                    modifier = Modifier.padding(top = 12.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(Strings.get("download_qr", lang))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(48.dp))
                
                OutlinedButton(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier.fillMaxWidth().tutorialTarget("inv_delete", tutorialViewModel),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("delete_product", lang))
                }
            }
        }

        if (showDeleteDialog && product != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text(Strings.get("delete_product", lang)) },
                text = { Text(Strings.get("delete_product_confirm", lang)) },
                confirmButton = {
                    Button(
                        onClick = {
                            showDeleteDialog = false
                            onDelete(product)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text(Strings.get("confirm", lang))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) {
                        Text(Strings.get("cancel", lang))
                    }
                }
            )
        }
    }
}

@Composable
fun DetailItem(label: String, value: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    ) {
        Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(text = value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Medium)
        HorizontalDivider(modifier = Modifier.padding(top = 8.dp), thickness = 0.5.dp)
    }
}

@Preview(showBackground = true)
@Composable
fun ProductDetailPanePreview() {
    MaterialTheme {
        ProductDetailPane(
            product = Product(1, "Apple", 1.0, 0.5, "Fruit", 100, "123", null),
            onEdit = {},
            onDelete = {},
            tutorialViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
        )
    }
}
