package com.tataskan.pos.ui.settings

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.HelpCenter
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tataskan.pos.ui.auth.AuthViewModel
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.GoogleDriveBackupManager
import com.tataskan.pos.util.LegalConstants
import com.tataskan.pos.util.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.net.Uri
import com.google.android.gms.common.api.ApiException
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    authViewModel: AuthViewModel,
    tutorialViewModel: TutorialViewModel,
    onDemoLoaded: () -> Unit = {},
    onRestartTutorial: () -> Unit = {}
) {
    val currencySymbolState by viewModel.currencySymbol.collectAsState()
    val showNameOnLabel by viewModel.showNameOnLabel.collectAsState()
    val showPriceOnLabel by viewModel.showPriceOnLabel.collectAsState()
    
    val storeNameState by viewModel.storeName.collectAsState()
    val storeLogoUri by viewModel.storeLogoUri.collectAsState()
    val storeAddressState by viewModel.storeAddress.collectAsState()
    val phoneNumberState by viewModel.phoneNumber.collectAsState()
    val taxPercentageState by viewModel.taxPercentage.collectAsState()
    val receiptFooterState by viewModel.receiptFooter.collectAsState()
    val receiptEnabled by viewModel.receiptEnabled.collectAsState()
    val defaultBulkScan by viewModel.defaultBulkScan.collectAsState()
    val languageState by viewModel.language.collectAsState()
    val internalBackups by viewModel.internalBackups.collectAsState()
    val loggedInUsername by viewModel.loggedInUsername.collectAsState()
    
    var storeName by remember { mutableStateOf("") }
    var storeAddress by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var taxPercentage by remember { mutableStateOf("") }
    var receiptFooter by remember { mutableStateOf("") }
    var currencySymbol by remember { mutableStateOf("") }
    var tempLanguage by remember { mutableStateOf("") }

    var hasChanges by remember { mutableStateOf(false) }

    var newPassword by remember { mutableStateOf("") }
    var showPasswordDialog by remember { mutableStateOf(false) }

    var showLegalDialog by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        storeName = storeNameState
        storeAddress = storeAddressState
        phoneNumber = phoneNumberState
        taxPercentage = taxPercentageState.toString()
        receiptFooter = receiptFooterState
        currencySymbol = currencySymbolState
        tempLanguage = languageState
    }

    LaunchedEffect(storeName, storeAddress, phoneNumber, taxPercentage, receiptFooter, currencySymbol, tempLanguage) {
        val hasStoreNameChanged = storeName != storeNameState
        val hasAddressChanged = storeAddress != storeAddressState
        val hasPhoneChanged = phoneNumber != phoneNumberState
        val hasTaxChanged = (taxPercentage.toFloatOrNull() ?: 0f) != taxPercentageState
        val hasFooterChanged = receiptFooter != receiptFooterState
        val hasCurrencyChanged = currencySymbol != currencySymbolState
        val hasLangChanged = tempLanguage != languageState
        
        hasChanges = hasStoreNameChanged || hasAddressChanged || hasPhoneChanged || 
                     hasTaxChanged || hasFooterChanged || hasCurrencyChanged || hasLangChanged
    }

    val scrollState = rememberScrollState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    fun saveImageLocally(context: Context, uri: Uri, fileName: String): String? {
        return try {
            val destinationFile = File(context.filesDir, fileName)
            context.contentResolver.openInputStream(uri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            Uri.fromFile(destinationFile).toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val logoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            saveImageLocally(context, it, "store_logo.jpg")?.let { localUri ->
                viewModel.setStoreLogoUri(localUri)
            }
        }
    }

    val gcashLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            saveImageLocally(context, it, "gcash_qr.jpg")?.let { localUri ->
                viewModel.setGcashQrUri(localUri)
            }
        }
    }

    val mayaLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            saveImageLocally(context, it, "maya_qr.jpg")?.let { localUri ->
                viewModel.setMayaQrUri(localUri)
            }
        }
    }

    var showSyncConfirmDialog by remember { mutableStateOf(false) }
    var showSyncSuccessDialog by remember { mutableStateOf(false) }
    var showUnlinkConfirmDialog by remember { mutableStateOf(false) }
    var isSyncingProgress by remember { mutableStateOf(false) }

    val googleAuthConsentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            isSyncingProgress = true
            scope.launch {
                val backupRes = GoogleDriveBackupManager.performCloudBackupResult(context)
                isSyncingProgress = false
                if (backupRes is com.tataskan.pos.util.CloudBackupResult.Success) {
                    viewModel.setLastDriveBackupTime(System.currentTimeMillis())
                    showSyncSuccessDialog = true
                } else if (backupRes is com.tataskan.pos.util.CloudBackupResult.Failure) {
                    snackbarHostState.showSnackbar("Cloud Backup Failed: ${backupRes.message}")
                }
            }
        } else {
            isSyncingProgress = false
            scope.launch {
                snackbarHostState.showSnackbar("Google Drive access authorization denied.")
            }
        }
    }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val data = result.data
            var selectedEmail: String? = null
            if (data != null) {
                try {
                    val task = com.google.android.gms.auth.api.signin.GoogleSignIn.getSignedInAccountFromIntent(data)
                    val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                    selectedEmail = account?.email
                } catch (e: Exception) {
                    // Ignored
                }
                if (selectedEmail.isNullOrBlank()) {
                    selectedEmail = data.getStringExtra(android.accounts.AccountManager.KEY_ACCOUNT_NAME)
                        ?: data.extras?.getString("authAccount")
                        ?: data.extras?.getString("account_name")
                }
            }
            if (!selectedEmail.isNullOrBlank()) {
                viewModel.setGoogleDriveAccount(selectedEmail)
                GoogleDriveBackupManager.scheduleAutoBackup(context)
                scope.launch {
                    snackbarHostState.showSnackbar("Linked Google Account: $selectedEmail")
                }
            } else {
                scope.launch {
                    snackbarHostState.showSnackbar("Google Account linking failed. No account selected.")
                }
            }
        } else {
            scope.launch {
                snackbarHostState.showSnackbar("Google Account selection canceled.")
            }
        }
    }

    val gcashQrUri by viewModel.gcashQrUri.collectAsState()
    val mayaQrUri by viewModel.mayaQrUri.collectAsState()
    val googleDriveAccount by viewModel.googleDriveAccount.collectAsState()
    val lastDriveBackupTime by viewModel.lastDriveBackupTime.collectAsState()
    val lastLocalBackupTime by viewModel.lastLocalBackupTime.collectAsState()
    val isLocalBackupEnabled by viewModel.isLocalBackupEnabled.collectAsState()
    val backupFrequency by viewModel.backupFrequency.collectAsState()

    var activeDownloadFile by remember { mutableStateOf<File?>(null) }
    var activeRestoreFile by remember { mutableStateOf<File?>(null) }
    val downloadLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    activeDownloadFile?.let { file ->
                        context.contentResolver.openOutputStream(it)?.use { output ->
                            FileInputStream(file).use { input ->
                                input.copyTo(output)
                            }
                            output.flush()
                        }
                        snackbarHostState.showSnackbar(Strings.get("export_success", languageState))
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("${Strings.get("error", languageState)}: ${e.message}")
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    context.contentResolver.openInputStream(it)?.use { stream ->
                        val success = viewModel.importBackup(stream)
                        if (success) {
                            snackbarHostState.showSnackbar("Restore successful! Restarting...")
                            delay(1000)
                            val packageManager = context.packageManager
                            val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                            val componentName = intent?.component
                            val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                            context.startActivity(mainIntent)
                            Runtime.getRuntime().exit(0)
                        } else {
                            snackbarHostState.showSnackbar("Restore failed.")
                        }
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error: ${e.message}")
                }
            }
        }
    }

    var showImportConfirm by remember { mutableStateOf(false) }
    var showDemoConfirm by remember { mutableStateOf(false) }
    var showResetDataConfirm by remember { mutableStateOf(false) }
    var showDeleteAccountConfirm by remember { mutableStateOf(false) }
    var showRestartConfirm by remember { mutableStateOf(false) }
    var showWipeConfirmationText by remember { mutableStateOf("") }
    
    val userCount by authViewModel.userCount.collectAsState()

    if (showDemoConfirm) {
        AlertDialog(
            onDismissRequest = { showDemoConfirm = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Science, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("load_demo_title", languageState))
                }
            },
            text = { Text(Strings.get("load_demo_desc", languageState)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.seedDemoData {
                            scope.launch {
                                showDemoConfirm = false
                                snackbarHostState.showSnackbar(Strings.get("demo_loaded", languageState))
                                onDemoLoaded()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                ) {
                    Text(Strings.get("load", languageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDemoConfirm = false }) {
                    Text(Strings.get("cancel", languageState))
                }
            }
        )
    }

    if (showResetDataConfirm) {
        AlertDialog(
            onDismissRequest = { showResetDataConfirm = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.RestartAlt, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("reset_data_title", languageState), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            text = { Text(Strings.get("reset_data_desc", languageState)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetBusinessData {
                            scope.launch {
                                showResetDataConfirm = false
                                snackbarHostState.showSnackbar(Strings.get("data_reset_success", languageState))
                                onDemoLoaded()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.get("reset_now", languageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDataConfirm = false }) {
                    Text(Strings.get("cancel", languageState))
                }
            }
        )
    }

    if (showImportConfirm) {
        AlertDialog(
            onDismissRequest = { showImportConfirm = false },
            title = { 
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Restore Database?")
                }
            },
            text = { Text("This will overwrite ALL current data (products, sales, settings) with the backup content. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        showImportConfirm = false
                        activeRestoreFile?.let { file ->
                            scope.launch {
                                val success = viewModel.importBackup(FileInputStream(file))
                                if (success) {
                                    snackbarHostState.showSnackbar("Restore successful! Restarting...")
                                    delay(1000)
                                    val packageManager = context.packageManager
                                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                                    val componentName = intent?.component
                                    val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                                    context.startActivity(mainIntent)
                                    Runtime.getRuntime().exit(0)
                                } else {
                                    snackbarHostState.showSnackbar("Restore failed.")
                                }
                            }
                        } ?: importLauncher.launch(arrayOf("*/*"))
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Restore Now")
                }
            },
            dismissButton = {
                TextButton(onClick = { showImportConfirm = false }) {
                    Text(Strings.get("cancel", languageState))
                }
            }
        )
    }

    if (showRestartConfirm) {
        AlertDialog(
            onDismissRequest = { showRestartConfirm = false },
            title = { Text(Strings.get("restart_required", languageState)) },
            text = { Text(Strings.get("restart_required_desc", languageState)) },
            confirmButton = {
                Button(onClick = {
                    viewModel.setLanguage(tempLanguage)
                    val packageManager = context.packageManager
                    val intent = packageManager.getLaunchIntentForPackage(context.packageName)
                    val componentName = intent?.component
                    val mainIntent = android.content.Intent.makeRestartActivityTask(componentName)
                    context.startActivity(mainIntent)
                    Runtime.getRuntime().exit(0)
                }) {
                    Text(Strings.get("restart_now", languageState))
                }
            }
        )
    }

    if (showPasswordDialog) {
        AlertDialog(
            onDismissRequest = { showPasswordDialog = false },
            title = { Text(Strings.get("change_password", languageState)) },
            text = {
                OutlinedTextField(
                    value = newPassword,
                    onValueChange = { newPassword = it },
                    label = { Text(Strings.get("new_password", languageState)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(onClick = {
                    if (newPassword.length >= 4) {
                        authViewModel.updatePassword(newPassword) { success ->
                            scope.launch {
                                snackbarHostState.showSnackbar(if (success) Strings.get("password_updated", languageState) else Strings.get("error", languageState))
                            }
                            showPasswordDialog = false
                            newPassword = ""
                        }
                    }
                }) {
                    Text(Strings.get("update", languageState))
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasswordDialog = false }) {
                    Text(Strings.get("cancel", languageState))
                }
            }
        )
    }

    if (showDeleteAccountConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAccountConfirm = false },
            title = { Text("Delete Shop Account?", color = MaterialTheme.colorScheme.error) },
            text = {
                Column {
                    Text("This will PERMANENTLY delete all your data, including products and transaction history. There is no way to recover this data.")
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Type \"DELETE\" to confirm:", style = MaterialTheme.typography.labelSmall)
                    OutlinedTextField(
                        value = showWipeConfirmationText,
                        onValueChange = { showWipeConfirmationText = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("DELETE") }
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (showWipeConfirmationText == "DELETE") {
                            scope.launch {
                                viewModel.deleteAccountAndData {
                                    authViewModel.logout()
                                    showDeleteAccountConfirm = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = showWipeConfirmationText == "DELETE"
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAccountConfirm = false }) {
                    Text(Strings.get("cancel", languageState))
                }
            }
        )
    }

    if (showLegalDialog != null) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = null },
            title = { Text(if (showLegalDialog == "PRIVACY") Strings.get("view_privacy", languageState) else Strings.get("view_tos", languageState)) },
            text = {
                Box(modifier = Modifier.heightIn(max = 400.dp)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        Text(
                            text = if (showLegalDialog == "PRIVACY") LegalConstants.PRIVACY_POLICY else LegalConstants.TERMS_OF_SERVICE,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLegalDialog = null }) {
                    Text(Strings.get("close", languageState))
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Text(
                text = Strings.get("settings", languageState),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            SettingsSection(
                title = Strings.get("app_experience", languageState), 
                icon = Icons.AutoMirrored.Filled.HelpCenter
            ) {
                Button(
                    onClick = { onRestartTutorial() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.PlayCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("start_guided_tutorial", languageState))
                }
            }

            SettingsSection(title = Strings.get("security", languageState), icon = Icons.Default.Security) {
                Button(
                    onClick = { showPasswordDialog = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Lock, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("change_password", languageState))
                }
            }

            SettingsSection(title = Strings.get("legal_privacy", languageState), icon = Icons.Default.Gavel) {
                Text(
                    text = Strings.get("legal_notice", languageState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { showLegalDialog = "PRIVACY" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("view_privacy", languageState), fontSize = 11.sp)
                    }
                    OutlinedButton(
                        onClick = { showLegalDialog = "TOS" },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(Strings.get("view_tos", languageState), fontSize = 11.sp)
                    }
                }
            }

            SettingsSection(
                title = Strings.get("business_profile", languageState), 
                icon = Icons.Default.Business,
                tutorialViewModel = tutorialViewModel,
                tutorialId = "settings_business"
            ) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .clickable { logoLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (storeLogoUri != null) {
                            AsyncImage(model = storeLogoUri, contentDescription = Strings.get("store_logo", languageState), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        } else {
                            Icon(Icons.Default.PhotoCamera, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = storeName, 
                    onValueChange = { storeName = it }, 
                    label = { Text(Strings.get("store_name", languageState)) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = storeAddress, 
                    onValueChange = { storeAddress = it }, 
                    label = { Text(Strings.get("store_address", languageState)) }, 
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = phoneNumber, 
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() || c == '-' || c == '+' || c == ' ' }) phoneNumber = it }, 
                    label = { Text(Strings.get("phone_number", languageState)) }, 
                    modifier = Modifier.fillMaxWidth(), 
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Phone)
                )
            }

            SettingsSection(
                title = Strings.get("digital_payment_qrs", languageState),
                icon = Icons.Default.QrCode2
            ) {
                Text(
                    text = Strings.get("digital_payment_qrs_desc", languageState),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // GCash QR
                    OutlinedCard(
                        onClick = { gcashLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(190.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "GCash QR", 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF005CE6)
                            )
                            if (gcashQrUri != null) {
                                Box(
                                    modifier = Modifier.size(100.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = gcashQrUri,
                                        contentDescription = "GCash QR",
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.setGcashQrUri(null) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(Strings.get("delete", languageState), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF005CE6))
                                Text(Strings.get("upload_gcash_qr", languageState), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }

                    // Maya QR
                    OutlinedCard(
                        onClick = { mayaLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f).height(190.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Maya QR", 
                                fontWeight = FontWeight.Bold, 
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF00D632)
                            )
                            if (mayaQrUri != null) {
                                Box(
                                    modifier = Modifier.size(100.dp).clip(MaterialTheme.shapes.small).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    AsyncImage(
                                        model = mayaQrUri,
                                        contentDescription = "Maya QR",
                                        modifier = Modifier.fillMaxSize().padding(4.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                TextButton(
                                    onClick = { viewModel.setMayaQrUri(null) },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(Strings.get("delete", languageState), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                }
                            } else {
                                Icon(Icons.Default.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(48.dp), tint = Color(0xFF00D632))
                                Text(Strings.get("upload_maya_qr", languageState), style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }

            SettingsSection(
                title = Strings.get("receipts_tax", languageState), 
                icon = Icons.Default.Settings,
                tutorialViewModel = tutorialViewModel,
                tutorialId = "settings_tax"
            ) {
                SettingsSwitchItem(
                    label = Strings.get("enable_receipts", languageState),
                    description = Strings.get("enable_receipts_desc", languageState),
                    checked = receiptEnabled,
                    onCheckedChange = { viewModel.setReceiptEnabled(it) }
                )
                SettingsSwitchItem(
                    label = "Default Bulk Scan",
                    description = "Start the scanner in bulk mode by default.",
                    checked = defaultBulkScan,
                    onCheckedChange = { viewModel.setDefaultBulkScan(it) }
                )
                if (receiptEnabled) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    OutlinedTextField(
                        value = taxPercentage, 
                        onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null || it == ".") taxPercentage = it }, 
                        label = { Text(Strings.get("tax_percentage", languageState)) }, 
                        modifier = Modifier.fillMaxWidth(), 
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = receiptFooter, 
                        onValueChange = { receiptFooter = it }, 
                        label = { Text(Strings.get("receipt_footer", languageState)) }, 
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Text)
                    )
                }
            }

            SettingsSection(
                title = Strings.get("label_defaults", languageState), 
                icon = Icons.Default.QrCode
            ) {
                SettingsSwitchItem(
                    label = Strings.get("show_name_label", languageState),
                    description = Strings.get("show_name_label_desc", languageState),
                    checked = showNameOnLabel,
                    onCheckedChange = { viewModel.setShowNameOnLabel(it) }
                )
                SettingsSwitchItem(
                    label = Strings.get("show_price_label", languageState),
                    description = Strings.get("show_price_label_desc", languageState),
                    checked = showPriceOnLabel,
                    onCheckedChange = { viewModel.setShowPriceOnLabel(it) }
                )
            }

            SettingsSection(
                title = Strings.get("regional_display", languageState), 
                icon = Icons.Default.Language,
                tutorialViewModel = tutorialViewModel,
                tutorialId = "settings_regional"
            ) {
                Text(Strings.get("app_language", languageState), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                var showLangDialog by remember { mutableStateOf(false) }
                val languages = mapOf("en" to "English", "tl" to "Tagalog", "es" to "Español", "fr" to "Français", "hi" to "Hindi", "zh" to "Chinese")

                OutlinedCard(onClick = { showLangDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(languages[tempLanguage] ?: "English")
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                if (showLangDialog) {
                    AlertDialog(
                        onDismissRequest = { showLangDialog = false },
                        title = { Text(Strings.get("select_language", languageState)) },
                        text = {
                            Column {
                                languages.forEach { (code, name) ->
                                    ListItem(headlineContent = { Text(name) }, modifier = Modifier.clickable { tempLanguage = code; showLangDialog = false })
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { showLangDialog = false }) { Text(Strings.get("cancel", languageState)) } }
                    )
                }

                if (tempLanguage != languageState && !hasChanges) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { showRestartConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)) {
                        Text(Strings.get("apply_restart", languageState))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                
                Text(Strings.get("currency", languageState), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = currencySymbol,
                    onValueChange = { if (it.length <= 3) currencySymbol = it },
                    label = { Text(Strings.get("currency_symbol", languageState)) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(Strings.get("currency_placeholder", languageState)) },
                    singleLine = true
                )
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("₱", "$", "€", "£", "¥").forEach { symbol ->
                        InputChip(onClick = { currencySymbol = symbol }, label = { Text(symbol) }, selected = currencySymbol == symbol)
                    }
                }
            }

            SettingsSection(
                title = Strings.get("maintenance_account", languageState), 
                icon = Icons.Default.Backup,
                tutorialViewModel = tutorialViewModel,
                tutorialId = "settings_backup"
            ) {
                Text(
                    text = "Load sample products and sales to see how the app works. Warning: This will overwrite your current inventory and settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showDemoConfirm = true },
                        modifier = Modifier.weight(1f).tutorialTarget("settings_demo", tutorialViewModel),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.Science, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.get("load_demo_data", languageState), fontSize = 11.sp, maxLines = 1)
                    }

                    OutlinedButton(
                        onClick = { showResetDataConfirm = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                    ) {
                        Icon(Icons.Default.RestartAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(Strings.get("reset_data_title", languageState), fontSize = 11.sp, maxLines = 1)
                    }
                }

                // Automated Local Backup & Retention Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = if (isLocalBackupEnabled) 
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(
                                    Icons.Default.CloudDone, 
                                    contentDescription = null, 
                                    tint = if (isLocalBackupEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Automated Local Backup", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Surface(
                                        color = if (isLocalBackupEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.padding(top = 2.dp)
                                    ) {
                                        Text(
                                            text = if (isLocalBackupEnabled) "🟢 Active" else "🔴 Disabled",
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isLocalBackupEnabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.error
                                        )
                                    }
                                }
                            }

                            Switch(
                                checked = isLocalBackupEnabled,
                                onCheckedChange = { enabled ->
                                    viewModel.setIsLocalBackupEnabled(enabled)
                                    scope.launch {
                                        snackbarHostState.showSnackbar(if (enabled) "Automated Local Backup enabled." else "Automated Local Backup disabled.")
                                    }
                                }
                            )
                        }

                        Text(
                            text = if (isLocalBackupEnabled) 
                                "Automatic background ZIP archives of your store database, preferences, and product images. Keeps the 7 most recent backups to save storage."
                            else 
                                "Automated local auto-backup is disabled. You can still generate manual local backups anytime below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (isLocalBackupEnabled && lastLocalBackupTime > 0L) {
                            val lastBackupDate = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(lastLocalBackupTime))
                            Text("Last Automated Backup: $lastBackupDate", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                        }

                        if (isLocalBackupEnabled) {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Auto-Backup Frequency", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        "DAILY" to Strings.get("freq_daily", languageState),
                                        "WEEKLY" to Strings.get("freq_weekly", languageState),
                                        "MONTHLY" to Strings.get("freq_monthly", languageState)
                                    ).forEach { (freqKey, freqLabel) ->
                                        FilterChip(
                                            selected = backupFrequency.equals(freqKey, ignoreCase = true),
                                            onClick = {
                                                viewModel.setBackupFrequency(freqKey)
                                                scope.launch {
                                                    snackbarHostState.showSnackbar("Auto-Backup Frequency: $freqLabel")
                                                }
                                            },
                                            label = { Text(freqLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold) },
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (internalBackups.isEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.GppMaybe, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("No Backup Found", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                Text("Create a backup now to prevent data loss if your phone is lost.", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }

                Button(
                    onClick = { 
                        viewModel.generateBackup { success ->
                            scope.launch { 
                                snackbarHostState.showSnackbar(if (success) "Backup created!" else "Failed to create backup.") 
                            }
                        }
                    }, 
                    modifier = Modifier.fillMaxWidth(), 
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(Strings.get("generate_backup", languageState))
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                Text(Strings.get("local_backups", languageState), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                
                if (internalBackups.isEmpty()) {
                    Text("No local backups found.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        internalBackups.forEach { file ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    val date = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(file.lastModified()))
                                    Text(date, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                                    Text(file.name, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    
                                    Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            OutlinedButton(
                                                onClick = { viewModel.deleteBackup(file) },
                                                modifier = Modifier.weight(1f),
                                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = Strings.get("delete", languageState),
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                            
                                            OutlinedButton(
                                                onClick = { 
                                                    activeRestoreFile = file
                                                    showImportConfirm = true 
                                                },
                                                modifier = Modifier.weight(1f),
                                                contentPadding = PaddingValues(horizontal = 4.dp),
                                                shape = MaterialTheme.shapes.small
                                            ) {
                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Restore",
                                                    fontSize = 11.sp,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                        
                                        Button(
                                            onClick = { 
                                                activeDownloadFile = file
                                                downloadLauncher.launch(file.name) 
                                            },
                                            modifier = Modifier.fillMaxWidth(),
                                            contentPadding = PaddingValues(vertical = 12.dp),
                                            shape = MaterialTheme.shapes.medium
                                        ) {
                                            Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = Strings.get("download", languageState),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(onClick = { showImportConfirm = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.UploadFile, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text("Restore from External File")
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(Strings.get("account_management", languageState), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                
                loggedInUsername?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccountCircle, contentDescription = null, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Logged in as", style = MaterialTheme.typography.labelSmall)
                                Text(it, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                
                Button(onClick = { showDeleteAccountConfirm = true }, modifier = Modifier.fillMaxWidth(), colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                    Icon(Icons.Default.DeleteForever, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)); Text(Strings.get("delete_shop", languageState))
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedButton(
                    onClick = { authViewModel.logout() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("logout", languageState))
                }

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = Strings.get("copyright", languageState),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    if (showSyncConfirmDialog) {
        AlertDialog(
            onDismissRequest = { if (!isSyncingProgress) showSyncConfirmDialog = false },
            title = { Text(Strings.get("sync_confirm_title", languageState), fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = Strings.get("sync_confirm_desc", languageState),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    if (isSyncingProgress) {
                        Spacer(modifier = Modifier.height(8.dp))
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSyncingProgress = true
                        scope.launch {
                            val backupRes = GoogleDriveBackupManager.performCloudBackupResult(context)
                            when (backupRes) {
                                is com.tataskan.pos.util.CloudBackupResult.Success -> {
                                    isSyncingProgress = false
                                    showSyncConfirmDialog = false
                                    viewModel.setLastDriveBackupTime(System.currentTimeMillis())
                                    showSyncSuccessDialog = true
                                }
                                is com.tataskan.pos.util.CloudBackupResult.RequiresConsent -> {
                                    isSyncingProgress = false
                                    showSyncConfirmDialog = false
                                    googleAuthConsentLauncher.launch(backupRes.consentIntent)
                                }
                                is com.tataskan.pos.util.CloudBackupResult.Failure -> {
                                    isSyncingProgress = false
                                    showSyncConfirmDialog = false
                                    snackbarHostState.showSnackbar("Cloud Backup Failed: ${backupRes.message}")
                                }
                            }
                        }
                    },
                    enabled = !isSyncingProgress
                ) {
                    Text(Strings.get("upload_cloud_backup_now", languageState), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                if (!isSyncingProgress) {
                    TextButton(onClick = { showSyncConfirmDialog = false }) {
                        Text(Strings.get("cancel", languageState))
                    }
                }
            }
        )
    }

    if (showSyncSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showSyncSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text("Cloud Backup Complete!", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = "Your store inventory, transaction logs, and settings have been backed up successfully to the 'TataskanPOS Backups' folder on your Google Drive.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showSyncSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("OK", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showUnlinkConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showUnlinkConfirmDialog = false },
            title = { Text(Strings.get("unlink_confirm_title", languageState), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = Strings.get("unlink_confirm_desc", languageState),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showUnlinkConfirmDialog = false
                        try {
                            val client = GoogleDriveBackupManager.getSignInClient(context)
                            client.signOut().addOnCompleteListener {
                                viewModel.setGoogleDriveAccount(null)
                                scope.launch { snackbarHostState.showSnackbar("Google Account unlinked.") }
                            }
                        } catch (e: Exception) {
                            viewModel.setGoogleDriveAccount(null)
                            scope.launch { snackbarHostState.showSnackbar("Google Account unlinked.") }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(Strings.get("unlink_google_drive", languageState), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnlinkConfirmDialog = false }) {
                    Text(Strings.get("cancel", languageState))
                }
            }
        )
    }
}

@Composable
fun TutorialCard(text: String) {
    Card(
        modifier = Modifier.padding(bottom = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Help, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.secondary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(text, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
fun SettingsSection(
    title: String, 
    icon: androidx.compose.ui.graphics.vector.ImageVector, 
    modifier: Modifier = Modifier,
    tutorialViewModel: TutorialViewModel? = null,
    tutorialId: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = if (tutorialId != null) Modifier.tutorialTarget(tutorialId, tutorialViewModel) else Modifier
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
            Column(modifier = Modifier.padding(16.dp)) { content() }
        }
    }
}

@Composable
fun SettingsSwitchItem(label: String, description: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
