package com.tataskan.pos.ui.error

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tataskan.pos.MainActivity
import com.tataskan.pos.ui.theme.TataskanTheme
import com.tataskan.pos.util.CrashReport
import com.tataskan.pos.util.ErrorReporter
import kotlin.jvm.java

class ErrorActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val crashReport = ErrorReporter.getLatestCrashReport(this) ?: CrashReport(
            exceptionType = "Unexpected Error",
            message = intent.getStringExtra("EXTRA_ERROR_MESSAGE") ?: "An unexpected error occurred in TataskanPOS.",
            stackTrace = intent.getStringExtra("EXTRA_STACK_TRACE") ?: "No stack trace available.",
            threadName = "main"
        )

        setContent {
            TataskanTheme {
                ErrorScreen(
                    crashReport = crashReport,
                    onRestartApp = { restartApp() }
                )
            }
        }
    }

    private fun restartApp() {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        startActivity(intent)
        finish()
    }

    companion object {
        const val EXTRA_CRASH_OCCURRED = "EXTRA_CRASH_OCCURRED"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ErrorScreen(
    crashReport: CrashReport,
    onRestartApp: () -> Unit
) {
    val context = LocalContext.current
    var userNotes by remember { mutableStateOf("") }
    var isSending by remember { mutableStateOf(false) }
    var isReportSent by remember { mutableStateOf(false) }
    var sendErrorMessage by remember { mutableStateOf<String?>(null) }
    var showStackTrace by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "App Error Notice",
                        fontWeight = FontWeight.Bold
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    titleContentColor = MaterialTheme.colorScheme.onErrorContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Friendly Header Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Error",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Oops! Something went wrong",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "TataskanPOS ran into an unexpected problem. Please copy the error report below and send it to our Facebook Support team.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Copy Error Details Action Card
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Error Summary",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "Type: ${crashReport.exceptionType}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Message: ${crashReport.message}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val fullDetails = """
                            TataskanPOS Error Report
                            Type: ${crashReport.exceptionType}
                            Message: ${crashReport.message}
                            Device: ${crashReport.deviceModel} (${crashReport.androidVersion})
                            Thread: ${crashReport.threadName}
                            App Version: ${crashReport.appVersion}
                            User Notes: ${userNotes.ifBlank { "None" }}

                            Stack Trace:
                            ${crashReport.stackTrace}
                            """.trimIndent()
                            val clip = ClipData.newPlainText("TataskanPOS Error Details", fullDetails)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Error details copied to clipboard!", Toast.LENGTH_LONG).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Error Details to Clipboard")
                    }

                    OutlinedButton(
                        onClick = {
                            try {
                                val fbIntent = Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.facebook.com/profile.php?id=61594957198671")
                                )
                                context.startActivity(fbIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser.", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Facebook Support Page")
                    }

                    TextButton(
                        onClick = { showStackTrace = !showStackTrace },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(if (showStackTrace) "Hide Technical Stack Trace" else "Show Technical Stack Trace")
                            Icon(
                                imageVector = if (showStackTrace) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = null
                            )
                        }
                    }

                    AnimatedVisibility(visible = showStackTrace) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 200.dp)
                                .background(
                                    color = Color.Black.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(12.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = crashReport.stackTrace,
                                color = Color.Green,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // User Notes Section
            OutlinedTextField(
                value = userNotes,
                onValueChange = { userNotes = it },
                label = { Text("Describe what happened (Optional)") },
                placeholder = { Text("e.g. Tapped checkout button while adding items") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !isSending && !isReportSent,
                leadingIcon = {
                    Icon(imageVector = Icons.Default.BugReport, contentDescription = null)
                }
            )

            if (isReportSent) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "✓ Error report sent successfully to Support Team. Thank you!",
                        color = Color(0xFF2E7D32),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            if (sendErrorMessage != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Error sending report: $sendErrorMessage",
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Buttons: Send Report & Restart
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRestartApp,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Restart App")
                }

                Button(
                    onClick = {
                        isSending = true
                        sendErrorMessage = null
                        ErrorReporter.sendReportToFirebase(
                            context = context,
                            report = crashReport,
                            userNotes = userNotes.trim(),
                            onSuccess = {
                                isSending = false
                                isReportSent = true
                                Toast.makeText(context, "Report sent to Support Team!", Toast.LENGTH_SHORT).show()
                            },
                            onError = { err ->
                                isSending = false
                                sendErrorMessage = err
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isSending && !isReportSent,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onError,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isReportSent) "Report Sent" else "Send Report")
                    }
                }
            }
        }
    }
}
