package com.tataskan.pos.ui.scanner

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.tataskan.pos.util.BarcodeScanner
import com.tataskan.pos.util.Strings
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

/**
 * A Composable that displays a camera preview and scans for barcodes.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ScannerScreen(
    onBarcodeDetected: (String, isContinuous: Boolean, onProcessed: (Boolean) -> Unit) -> Unit,
    barcodeFormats: Int = Barcode.FORMAT_ALL_FORMATS,
    lang: String = "en",
    defaultContinuousMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember { PreviewView(context) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    
    var isContinuousMode by remember { mutableStateOf(defaultContinuousMode) }
    var lastScannedCode by remember { mutableStateOf("") }
    var lastScanTime by remember { mutableLongStateOf(0L) }
    var isProcessing by remember { mutableStateOf(false) }
    var showNotFoundDialog by remember { mutableStateOf<String?>(null) }

    val scannerAnalyzer = remember(barcodeFormats) { 
        BarcodeScanner(barcodeFormats) { barcode ->
            val currentTime = System.currentTimeMillis()
            // 3 second cooldown for the SAME barcode to prevent duplicates
            // 500ms cooldown for DIFFERENT barcodes for speed
            val cooldown = if (barcode == lastScannedCode) 3000L else 500L
            
            if (!isProcessing && (currentTime - lastScanTime > cooldown)) {
                isProcessing = true
                lastScannedCode = barcode
                lastScanTime = currentTime
                
                android.util.Log.d("ScannerScreen", "Barcode detected: $barcode")
                
                onBarcodeDetected(barcode, isContinuousMode) { success ->
                    isProcessing = false
                    if (success) {
                        android.util.Log.d("ScannerScreen", "Scan successful: $barcode")
                        // Vibrate on success
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            vibrator.vibrate(VibrationEffect.createOneShot(100, VibrationEffect.DEFAULT_AMPLITUDE))
                        } else {
                            vibrator.vibrate(100)
                        }
                        
                        if (isContinuousMode) {
                            scope.launch {
                                snackbarHostState.showSnackbar("${Strings.get("added_to_cart", lang)}: $barcode")
                            }
                        }
                    } else {
                        android.util.Log.d("ScannerScreen", "Product not found: $barcode")
                        showNotFoundDialog = barcode
                        scope.launch {
                            delay(2000)
                            if (showNotFoundDialog == barcode) {
                                showNotFoundDialog = null
                            }
                        }
                    }
                }
            }
        }
    }
    
    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)

    DisposableEffect(Unit) {
        onDispose {
            scannerAnalyzer.close()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (cameraPermissionState.status.isGranted) {
            Box(modifier = modifier.fillMaxSize().padding(padding)) {
                AndroidView(
                    factory = { previewView },
                    modifier = Modifier.fillMaxSize()
                ) { view ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build().also {
                            it.surfaceProvider = view.surfaceProvider
                        }

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                            .also {
                                it.setAnalyzer(cameraExecutor, scannerAnalyzer)
                            }

                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                cameraSelector,
                                preview,
                                imageAnalysis
                            )
                        } catch (e: Exception) {
                            // Handle binding failure
                        }
                    }, ContextCompat.getMainExecutor(context))
                }

                // Scanner Overlay (Focus Area)
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.size(250.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                        border = androidx.compose.foundation.BorderStroke(2.dp, Color.White.copy(alpha = 0.8f))
                    ) {}
                }

                // Controls
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (isProcessing) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    FilterChip(
                        selected = isContinuousMode,
                        onClick = { isContinuousMode = !isContinuousMode },
                        label = { Text(Strings.get("bulk_scan_mode", lang), color = if (isContinuousMode) Color.White else Color.Unspecified) },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Repeat,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary
                        )
                    )
                }
                
                // Mode indicator
                if (isContinuousMode) {
                    Surface(
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 16.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = CircleShape
                    ) {
                        Text(
                            Strings.get("bulk_mode_active", lang),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                // Not Found Overlay
                if (showNotFoundDialog != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.padding(32.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning, 
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    Strings.get("product_not_found", lang),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                                Text(
                                    "${Strings.get("barcode", lang)}: ${showNotFoundDialog}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(Strings.get("camera_permission_required", lang))
                    Button(onClick = { cameraPermissionState.launchPermissionRequest() }) {
                        Text(Strings.get("grant_permission", lang))
                    }
                }
            }
        }
    }
}
