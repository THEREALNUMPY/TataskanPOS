package com.tataskan.pos.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tataskan.pos.util.CurrencyUtils

/**
 * High-contrast primary button for important actions like "Checkout" or "Scan".
 * Designed for high visibility and large touch targets.
 */
@Composable
fun SukiPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    containerColor: Color? = null,
    contentColor: Color? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(64.dp), // Large touch target
        enabled = enabled,
        shape = MaterialTheme.shapes.large,
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.primary,
            contentColor = contentColor ?: MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            if (icon != null) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(28.dp))
                Spacer(modifier = Modifier.width(12.dp))
            }
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.sp
            )
        }
    }
}

/**
 * High-contrast display for currency and totals.
 * Prioritizes readability and size for outdoor use.
 */
@Composable
fun SukiPriceDisplay(
    label: String,
    amount: Double,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    large: Boolean = false, // Default to false for better fitting
    color: Color? = null,
    textAlign: TextAlign = TextAlign.Start
) {
    Column(modifier = modifier) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline,
            fontWeight = FontWeight.Black,
            letterSpacing = 1.sp
        )
        Text(
            text = CurrencyUtils.formatCurrency(amount, currencySymbol),
            style = if (large) MaterialTheme.typography.headlineLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Black,
            color = color ?: MaterialTheme.colorScheme.primary,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false
        )
    }
}

/**
 * Standard card for POS and Inventory items with high contrast and clear separation.
 */
@Composable
fun TataskanCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    containerColor: Color? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardModifier = if (onClick != null) modifier.fillMaxWidth().heightIn(min = 80.dp) else modifier.fillMaxWidth()
    
    Card(
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = cardModifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = containerColor ?: MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            content()
        }
    }
}

/**
 * Large icon button for quick actions like quantity adjustment or deletion.
 */
@Composable
fun SukiIconButton(
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tint: Color? = null,
    backgroundColor: Color? = null,
    contentDescription: String? = null
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.size(56.dp), // Minimum 48dp touch target, 56dp for better outdoor use
        shape = MaterialTheme.shapes.medium,
        color = backgroundColor ?: MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.size(32.dp),
                tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * High-contrast section header.
 */
@Composable
fun SukiSectionTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title.uppercase(),
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Black,
        color = MaterialTheme.colorScheme.outline,
        modifier = modifier.padding(vertical = 8.dp),
        letterSpacing = 1.5.sp
    )
}

/**
 * Reusable empty state component.
 */
@Composable
fun SukiEmptyState(
    message: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    description: String? = null
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.outlineVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.outline,
            textAlign = TextAlign.Center
        )
        if (description != null) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}
