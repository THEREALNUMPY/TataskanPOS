package com.tataskan.pos.ui.auth

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tataskan.pos.util.Strings
import kotlinx.coroutines.delay

/**
 * Screen for user login.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    lang: String = "en",
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    val error by viewModel.error.collectAsState()

    Scaffold { padding ->
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
                imageVector = Icons.Default.LockPerson,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = Strings.get("welcome_back", lang),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.ExtraBold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = Strings.get("login_subtitle", lang),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            OutlinedTextField(
                value = username,
                onValueChange = { username = it },
                label = { Text(Strings.get("username", lang)) },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                singleLine = true,
                shape = MaterialTheme.shapes.medium
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
                shape = MaterialTheme.shapes.medium
            )

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
                    if (username.isNotBlank() && password.isNotBlank()) {
                        viewModel.login(username, password, lang, onLoginSuccess)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
                shape = MaterialTheme.shapes.medium,
                contentPadding = PaddingValues(16.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = Strings.get("login", lang),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            var showForgotDialog by remember { mutableStateOf(false) }
            var showResetSuccessDialog by remember { mutableStateOf(false) }

            TextButton(onClick = { showForgotDialog = true; viewModel.clearError() }) {
                Text(Strings.get("forgot_password", lang), color = MaterialTheme.colorScheme.secondary)
            }

            TextButton(onClick = onNavigateToRegister) {
                Text(Strings.get("dont_have_account", lang), fontWeight = FontWeight.Bold)
            }

            if (showForgotDialog) {
                ForgotPasswordDialog(
                    viewModel = viewModel,
                    lang = lang,
                    onDismiss = { showForgotDialog = false; viewModel.clearError() },
                    onResetSuccess = { 
                        showForgotDialog = false
                        showResetSuccessDialog = true
                    }
                )
            }

            if (showResetSuccessDialog) {
                AlertDialog(
                    onDismissRequest = { showResetSuccessDialog = false },
                    icon = {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF2E7D32),
                            modifier = Modifier.size(48.dp)
                        )
                    },
                    title = { Text(Strings.get("reset_success_title", lang), fontWeight = FontWeight.Bold) },
                    text = {
                        Text(
                            text = Strings.get("reset_success_desc", lang),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = { showResetSuccessDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Strings.get("ok_login", lang), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordDialog(
    viewModel: AuthViewModel,
    lang: String = "en",
    onDismiss: () -> Unit,
    onResetSuccess: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) } // 0: PIN, 1: Facebook TR- Ticket

    var pinCode by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var newPin by remember { mutableStateOf("") }
    var adminCode by remember { mutableStateOf("") }
    var dialogError by remember { mutableStateOf<String?>(null) }

    var pinVisible by remember { mutableStateOf(false) }
    var newPasswordVisible by remember { mutableStateOf(false) }
    var newPinVisible by remember { mutableStateOf(false) }

    var generatedTicketCode by remember { mutableStateOf<Int?>(null) }
    var ticketText by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(300) }

    LaunchedEffect(generatedTicketCode) {
        if (generatedTicketCode != null) {
            secondsLeft = 300
            while (secondsLeft > 0) {
                delay(1000)
                secondsLeft--
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.LockReset,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        Text(
                            text = Strings.get("forgot_password", lang),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = Strings.get("cancel", lang),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Tabs
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0; dialogError = null },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Backup PIN", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1; dialogError = null },
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(Icons.Default.ConfirmationNumber, contentDescription = null, modifier = Modifier.size(16.dp))
                                Text("Facebook Ticket", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Scrollable Content Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (selectedTab == 0) {
                        // Option 1: Backup PIN
                        Text(
                            text = "Reset your password using your registered 4-digit security Backup PIN.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        OutlinedTextField(
                            value = pinCode,
                            onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinCode = it },
                            label = { Text(Strings.get("enter_backup_pin", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                            leadingIcon = {
                                Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { pinVisible = !pinVisible }) {
                                    Icon(
                                        imageVector = if (pinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (pinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                        )

                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text(Strings.get("new_password", lang)) },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                    Icon(
                                        imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = null
                                    )
                                }
                            },
                            visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                        )

                        OutlinedButton(
                            onClick = { selectedTab = 1; dialogError = null },
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Icon(Icons.Default.SupportAgent, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Forgot Backup PIN? Request Support via Facebook Ticket",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        // Option 2: Facebook Ticket
                        if (generatedTicketCode == null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        Icons.Default.ConfirmationNumber,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Text(
                                        text = "Generate a Reset Ticket Code and send it to Facebook Support for verification.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Button(
                                        onClick = {
                                            val fullTicket = viewModel.generateResetTicket()
                                            ticketText = fullTicket
                                            generatedTicketCode = viewModel.activeResetTicket
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.AutoMode, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Generate Reset Ticket")
                                    }
                                }
                            }
                        } else {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.large
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "YOUR RESET TICKET CODE",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = ticketText,
                                        style = MaterialTheme.typography.headlineLarge,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MaterialTheme.colorScheme.primary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (secondsLeft > 0) "Expires in %d:%02d".format(secondsLeft / 60, secondsLeft % 60) else "Ticket Expired",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (secondsLeft < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                                val clip = ClipData.newPlainText("Reset Ticket Code", ticketText)
                                                clipboard.setPrimaryClip(clip)
                                                Toast.makeText(context, "Ticket code copied!", Toast.LENGTH_SHORT).show()
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Copy Code", style = MaterialTheme.typography.labelSmall)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                val fullTicket = viewModel.generateResetTicket()
                                                ticketText = fullTicket
                                                generatedTicketCode = viewModel.activeResetTicket
                                            },
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Regenerate Ticket", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                }
                            }

                            Button(
                                onClick = {
                                    try {
                                        val intent = Intent(
                                            Intent.ACTION_VIEW,
                                            Uri.parse("https://www.facebook.com/profile.php?id=61594957198671")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1877F2)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = MaterialTheme.shapes.medium
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Send Ticket to Facebook Support", fontWeight = FontWeight.Bold)
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

                            Text(
                                text = "Step 2: Enter Verification & Set Credentials",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            OutlinedTextField(
                                value = adminCode,
                                onValueChange = { if (it.length <= 6) adminCode = it },
                                label = { Text(Strings.get("enter_admin_code", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Icon(Icons.Default.Badge, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )

                            OutlinedTextField(
                                value = newPassword,
                                onValueChange = { newPassword = it },
                                label = { Text(Strings.get("new_password", lang)) },
                                leadingIcon = {
                                    Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { newPasswordVisible = !newPasswordVisible }) {
                                        Icon(
                                            imageVector = if (newPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (newPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            OutlinedTextField(
                                value = newPin,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) newPin = it },
                                label = { Text(Strings.get("new_backup_pin", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                leadingIcon = {
                                    Icon(Icons.Default.VpnKey, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                },
                                trailingIcon = {
                                    IconButton(onClick = { newPinVisible = !newPinVisible }) {
                                        Icon(
                                            imageVector = if (newPinVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                            contentDescription = null
                                        )
                                    }
                                },
                                visualTransformation = if (newPinVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword)
                            )
                        }
                    }

                    if (dialogError != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                                Text(
                                    text = dialogError ?: "",
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Footer Actions
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (selectedTab == 0 || generatedTicketCode != null) {
                        Button(
                            onClick = {
                                dialogError = null
                                if (selectedTab == 0) {
                                    viewModel.resetPasswordWithBackupPin(
                                        pin = pinCode,
                                        newPassword = newPassword,
                                        lang = lang,
                                        onError = { dialogError = it },
                                        onSuccess = onResetSuccess
                                    )
                                } else {
                                    val ticket = generatedTicketCode
                                    if (ticket != null) {
                                        viewModel.verifyAndResetWithTicket(
                                            ticketCode = ticket,
                                            adminCode = adminCode,
                                            newPassword = newPassword,
                                            newPin = newPin,
                                            lang = lang,
                                            onError = { dialogError = it },
                                            onSuccess = onResetSuccess
                                        )
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(Strings.get("verify_and_reset", lang), fontWeight = FontWeight.Bold)
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.get("cancel", lang))
                    }
                }
            }
        }
    }
}
