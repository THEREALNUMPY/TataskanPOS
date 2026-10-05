package com.tataskan.pos.ui.product

import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.FileProvider
import coil.compose.AsyncImage
import com.tataskan.pos.data.entity.Category
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.BarcodeUtils
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.Strings
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ProductAddEditPane(
    product: Product?,
    categories: List<Category> = emptyList(),
    currencySymbol: String = "₱",
    lang: String = "en",
    tutorialViewModel: TutorialViewModel? = null,
    onSave: (name: String, price: Double, cost: Double, category: String, stock: Int, barcode: String?, imageUri: String?) -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    
    var name by remember { mutableStateOf(product?.name ?: "") }
    var price by remember { mutableStateOf(product?.price?.toString() ?: "") }
    var cost by remember { mutableStateOf(product?.cost?.toString() ?: "") }
    var category by remember { mutableStateOf(product?.category ?: "") }
    var stock by remember { mutableStateOf(product?.stock?.toString() ?: "") }
    var barcode by remember { mutableStateOf(product?.barcode ?: "") }
    var imageUri by remember { mutableStateOf(product?.imageUri) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(product?.id) {
        if (product != null) {
            name = product.name
            price = product.price.toString()
            cost = product.cost.toString()
            category = product.category
            stock = product.stock.toString()
            barcode = product.barcode ?: ""
            imageUri = product.imageUri
        }
    }

    val filteredOptions by remember(category, categories) {
        derivedStateOf {
            categories.filter { it.name.contains(category, ignoreCase = true) }
        }
    }

    var expanded by remember { mutableStateOf(false) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    val storagePermissionState = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
        rememberPermissionState(android.Manifest.permission.READ_MEDIA_IMAGES)
    } else {
        rememberPermissionState(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }
    
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { imageUri = it.toString() }
    }

    var tempCameraUri by remember { mutableStateOf<Uri?>(null) }
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri = tempCameraUri.toString()
        }
    }

    fun launchGallery() {
        if (storagePermissionState.status.isGranted) {
            galleryLauncher.launch("image/*")
        } else {
            storagePermissionState.launchPermissionRequest()
        }
    }

    fun launchCamera() {
        try {
            if (cameraPermissionState.status.isGranted) {
                val uri = createTempImageUri(context)
                tempCameraUri = uri
                cameraLauncher.launch(uri)
            } else {
                cameraPermissionState.launchPermissionRequest()
            }
        } catch (e: Exception) {
            android.util.Log.e("SukiPOS", "Camera Error", e)
            Toast.makeText(context, Strings.get("guide_barcode", lang), Toast.LENGTH_SHORT).show()
        }
    }

    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Select Image Source") },
            text = {
                Column {
                    ListItem(
                        headlineContent = { Text("Camera") },
                        leadingContent = { Icon(Icons.Default.CameraAlt, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            launchCamera()
                        }
                    )
                    ListItem(
                        headlineContent = { Text("Gallery") },
                        leadingContent = { Icon(Icons.Default.PhotoLibrary, contentDescription = null) },
                        modifier = Modifier.clickable {
                            showImageSourceDialog = false
                            launchGallery()
                        }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showImageSourceDialog = false }) {
                    Text(Strings.get("cancel", lang))
                }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { showImageSourceDialog = true },
                tonalElevation = 2.dp
            ) {
                if (imageUri != null) {
                    Box {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(12.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ) {
                            Icon(
                                Icons.Default.CameraAlt, 
                                contentDescription = "Change Image",
                                modifier = Modifier.padding(8.dp).size(20.dp)
                            )
                        }
                    }
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Image, 
                                contentDescription = null, 
                                modifier = Modifier.size(56.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add Product Image",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Camera or Gallery",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        Strings.get("basic_info", lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(Strings.get("product_name", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = { Text(Strings.get("desc_prod_name", lang)) },
                        shape = MaterialTheme.shapes.medium,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                    )

                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { 
                                category = it
                                expanded = true
                            },
                            label = { Text(Strings.get("category", lang)) },
                            supportingText = { Text(Strings.get("desc_prod_category", lang)) },
                            modifier = Modifier.fillMaxWidth(),
                            trailingIcon = { 
                                IconButton(onClick = { expanded = !expanded }) {
                                    Icon(
                                        if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                        contentDescription = null
                                    )
                                }
                            },
                            shape = MaterialTheme.shapes.medium,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        if (filteredOptions.isNotEmpty()) {
                            DropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                filteredOptions.forEach { item ->
                                    DropdownMenuItem(
                                        text = { Text(item.name) },
                                        onClick = {
                                            category = item.name
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        Strings.get("pricing_stock", lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") price = it },
                            label = { Text(Strings.get("price", lang)) },
                            supportingText = { Text(Strings.get("desc_prod_price", lang)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.medium,
                            prefix = { Text(currencySymbol) }
                        )
                        OutlinedTextField(
                            value = cost,
                            onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") cost = it },
                            label = { Text(Strings.get("cost", lang)) },
                            supportingText = { Text(Strings.get("desc_prod_cost", lang)) },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = MaterialTheme.shapes.medium,
                            prefix = { Text(currencySymbol) }
                        )
                    }

                    OutlinedTextField(
                        value = stock,
                        onValueChange = { if (it.isEmpty() || it.toIntOrNull() != null) stock = it },
                        label = { Text(Strings.get("stock_level", lang)) },
                        supportingText = { Text(Strings.get("desc_prod_stock", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = MaterialTheme.shapes.medium
                    )
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        Strings.get("barcode", lang),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { barcode = it },
                            label = { Text(Strings.get("enter_barcode", lang)) },
                            supportingText = { Text(Strings.get("desc_prod_barcode", lang)) },
                            modifier = Modifier.weight(1f),
                            shape = MaterialTheme.shapes.medium
                        )
                        IconButton(
                            onClick = { barcode = BarcodeUtils.generateRandomBarcodeString() },
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            modifier = Modifier.tutorialTarget("inv_barcode_gen", tutorialViewModel)
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = "Generate")
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Button(
                onClick = {
                    if (name.isNotBlank() && price.isNotBlank() && !isSaving) {
                        try {
                            isSaving = true
                            expanded = false
                            onSave(
                                name,
                                price.toDoubleOrNull() ?: 0.0,
                                cost.toDoubleOrNull() ?: 0.0,
                                category,
                                stock.toIntOrNull() ?: 0,
                                barcode.ifBlank { null },
                                imageUri
                            )
                        } catch (e: Exception) {
                            isSaving = false
                            android.util.Log.e("SukiPOS", "Save Error", e)
                            scope.launch {
                                snackbarHostState.showSnackbar("Error: ${e.localizedMessage ?: "Could not save"}")
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().tutorialTarget("inv_save_btn", tutorialViewModel),
                enabled = !isSaving
            ) {
                if (isSaving) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("save_product", lang))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ProductAddEditPanePreview() {
    MaterialTheme {
        ProductAddEditPane(
            product = null,
            tutorialViewModel = null,
            onSave = { _, _, _, _, _, _, _ -> }
        )
    }
}

private fun createTempImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    val file = File.createTempFile("JPEG_${timeStamp}_", ".jpg", storageDir)
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
}
