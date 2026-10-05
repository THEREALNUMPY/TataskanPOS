package com.tataskan.pos.ui.navigation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.Strings

@Composable
fun MoreScreen(
    lang: String,
    onNavigateToReports: () -> Unit,
    onNavigateToPromos: () -> Unit,
    onNavigateToSettings: () -> Unit,
    onNavigateToFeedback: () -> Unit,
    onNavigateToLabelGenerator: () -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "More Options",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        MoreItem(
            title = Strings.get("reports", lang),
            icon = Icons.Default.BarChart,
            onClick = onNavigateToReports,
            modifier = Modifier.tutorialTarget("more_reports_item", tutorialViewModel)
        )
        MoreItem(
            title = Strings.get("promos", lang),
            icon = Icons.Default.LocalOffer,
            onClick = onNavigateToPromos,
            modifier = Modifier.tutorialTarget("more_promos_item", tutorialViewModel)
        )
        MoreItem(
            title = Strings.get("bulk_labels", lang),
            icon = Icons.Default.QrCode,
            onClick = onNavigateToLabelGenerator,
            modifier = Modifier.tutorialTarget("more_labels_item", tutorialViewModel)
        )
        
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
        
        MoreItem(
            title = Strings.get("settings", lang),
            icon = Icons.Default.Settings,
            onClick = onNavigateToSettings,
            modifier = Modifier.tutorialTarget("more_settings_item", tutorialViewModel)
        )
        MoreItem(
            title = Strings.get("feedback", lang),
            icon = Icons.Default.Feedback,
            onClick = onNavigateToFeedback,
            modifier = Modifier.tutorialTarget("more_feedback_item", tutorialViewModel)
        )
    }
}

@Composable
private fun MoreItem(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}
