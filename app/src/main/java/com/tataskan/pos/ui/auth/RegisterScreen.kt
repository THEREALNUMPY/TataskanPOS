package com.tataskan.pos.ui.auth

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tataskan.pos.data.entity.User
import com.tataskan.pos.util.LegalConstants
import com.tataskan.pos.util.Strings
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    lang: String = "en",
    onRegisterSuccess: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var backupPin by remember { mutableStateOf("") }
    var shopName by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var agreedToTerms by remember { mutableStateOf(false) }
    var showLegalDialog by remember { mutableStateOf<String?>(null) }
    
    val error by viewModel.error.collectAsState()
    val userCount by viewModel.userCount.collectAsState()
    val registrationInProgress by viewModel.registrationInProgress.collectAsState()

    val canRegister by remember { derivedStateOf { userCount == 0 } }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

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

    DisposableEffect(Unit) {
        viewModel.resetTrialForTesting() // Reset timer every time we land on Register screen for testing
        onDispose { viewModel.clearError() }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.AppRegistration,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            val showWelcome = canRegister || registrationInProgress

            Text(
                text = if (showWelcome) Strings.get("welcome_title", lang) else Strings.get("account_already_exists", lang),
                style = MaterialTheme.typography.headlineMedium,
                color = if (showWelcome) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (showWelcome) Strings.get("welcome_msg", lang) else Strings.get("one_account_limit", lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (showWelcome) {
                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text(Strings.get("store_name", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Store, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !registrationInProgress,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it.filter { c -> !c.isWhitespace() } },
                    label = { Text(Strings.get("username", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !registrationInProgress,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text(Strings.get("password", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !registrationInProgress
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = confirmPassword,
                    onValueChange = { confirmPassword = it },
                    label = { Text(Strings.get("confirm_password", lang)) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        val icon = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(icon, contentDescription = null)
                        }
                    },
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !registrationInProgress
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = backupPin,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) backupPin = it },
                    label = { Text(Strings.get("backup_pin", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                    enabled = !registrationInProgress,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = agreedToTerms,
                        onCheckedChange = { agreedToTerms = it },
                        enabled = !registrationInProgress
                    )
                    Text(
                        text = "I agree to the ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Terms",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showLegalDialog = "TOS" }
                    )
                    Text(
                        text = " & ",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.clickable { showLegalDialog = "PRIVACY" }
                    )
                }

                if (error != null) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                        modifier = Modifier.padding(top = 16.dp).fillMaxWidth()
                    ) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        viewModel.register(
                            user = User(username = username, password = password, shopName = shopName, backupPin = backupPin),
                            confirmPass = confirmPassword,
                            lang = lang,
                            onRegisterSuccess = onRegisterSuccess
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 56.dp),
                    shape = MaterialTheme.shapes.medium,
                    contentPadding = PaddingValues(16.dp),
                    enabled = !registrationInProgress && agreedToTerms
                ) {
                    if (registrationInProgress) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Icon(Icons.Default.HowToReg, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = Strings.get("register", lang),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(32.dp))
                
                Button(
                    onClick = { importLauncher.launch(arrayOf("*/*")) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(Strings.get("restore_file", lang), style = MaterialTheme.typography.titleMedium)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

        }
    }

    if (showLegalDialog != null) {
        AlertDialog(
            onDismissRequest = { showLegalDialog = null },
            title = { Text(if (showLegalDialog == "PRIVACY") "Privacy Policy" else "Terms of Service") },
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
                    Text(Strings.get("close", lang))
                }
            }
        )
    }
}
