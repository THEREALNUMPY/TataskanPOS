package com.tataskan.pos.ui.feedback

import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(tutorialViewModel: TutorialViewModel) {
    val context = LocalContext.current
    var rating by remember { mutableIntStateOf(0) }
    var suggestion by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }
    
    val scrollState = rememberScrollState()

    if (isSuccess) {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.CheckCircle, 
                contentDescription = null, 
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                "Feedback Submitted!", 
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Thank you for helping us improve TataskanPOS.", 
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(16.dp)
            )
            Button(onClick = { isSuccess = false; rating = 0; suggestion = "" }) {
                Text("Send More Feedback")
            }
        }
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Your Feedback Matters",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = "We're constantly working to improve TataskanPOS. Let us know what you think!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Rating Section
            Card(
                modifier = Modifier.fillMaxWidth().tutorialTarget("feedback_stars", tutorialViewModel),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Rate the App",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 1..5) {
                            Icon(
                                imageVector = if (i <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Star $i",
                                tint = if (i <= rating) Color(0xFFFFD700) else MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clickable { if (!isSubmitting) rating = i }
                            )
                        }
                    }
                    
                    Text(
                        text = when(rating) {
                            1 -> "Poor"
                            2 -> "Fair"
                            3 -> "Good"
                            4 -> "Great"
                            5 -> "Excellent"
                            else -> "Select a rating"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                }
            }

            // Suggestion Section
            OutlinedTextField(
                value = suggestion,
                onValueChange = { suggestion = it },
                label = { Text("Suggestions / Comments") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp),
                shape = MaterialTheme.shapes.medium,
                placeholder = { Text("How can we make the app better for you?") },
                enabled = !isSubmitting
            )

            Button(
                onClick = {
                    if (rating == 0) {
                        Toast.makeText(context, "Please select a rating", Toast.LENGTH_SHORT).show()
                    } else {
                        isSubmitting = true
                        submitFeedback(
                            rating = rating,
                            suggestion = suggestion,
                            onSuccess = {
                                isSubmitting = false
                                isSuccess = true
                                Toast.makeText(context, "Feedback sent! Thank you.", Toast.LENGTH_SHORT).show()
                            },
                            onFailure = { error ->
                                isSubmitting = false
                                android.util.Log.e("SukiPOS", "Firebase Error: $error")
                                Toast.makeText(context, "Could not send. Check internet connection.", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .tutorialTarget("feedback_submit_btn", tutorialViewModel),
                shape = MaterialTheme.shapes.large,
                enabled = !isSubmitting
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                } else {
                    Text("Submit Feedback", style = MaterialTheme.typography.titleMedium)
                }
            }
            
            Text(
                text = "Your feedback is securely sent to our team.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("💡 Suggested Feedback:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                    Text("• Which feature is most useful?", style = MaterialTheme.typography.bodySmall)
                    Text("• Is the scanner working smoothly?", style = MaterialTheme.typography.bodySmall)
                    Text("• What new feature should we add next?", style = MaterialTheme.typography.bodySmall)
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun submitFeedback(
    rating: Int,
    suggestion: String,
    onSuccess: () -> Unit,
    onFailure: (String) -> Unit
) {
    try {
        val db = FirebaseFirestore.getInstance()
        val feedbackData = hashMapOf(
            "rating" to rating,
            "suggestion" to suggestion,
            "timestamp" to Date(),
            "platform" to "Android"
        )

        db.collection("feedback")
            .add(feedbackData)
            .addOnSuccessListener { 
                onSuccess() 
            }
            .addOnFailureListener { e -> 
                onFailure(e.message ?: "Unknown error") 
            }
    } catch (e: Exception) {
        onFailure("Firebase Error: ${e.message}")
    }
}
