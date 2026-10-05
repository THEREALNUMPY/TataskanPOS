package com.tataskan.pos.ui.pos

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tataskan.pos.data.local.entity.Product
import com.tataskan.pos.ui.components.*
import com.tataskan.pos.ui.product.ProductListPane
import com.tataskan.pos.ui.product.ProductViewModel
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.util.Strings

@Composable
fun PosTabletScreen(
    posViewModel: PosViewModel,
    productViewModel: ProductViewModel,
    products: List<Product>,
    categories: List<com.tataskan.pos.data.entity.Category>,
    currencySymbol: String,
    lang: String,
    onNavigateToCheckout: () -> Unit,
    onNavigateToScanner: () -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    Row(modifier = Modifier.fillMaxSize()) {
        // Left Column: Product Selection
        Box(modifier = Modifier.weight(1.3f).fillMaxHeight()) {
            ProductListPane(
                products = products,
                categories = categories,
                currencySymbol = currencySymbol,
                lang = lang,
                onProductClick = { product -> posViewModel.addToCart(product) },
                onStockAdjust = { product, change -> productViewModel.quickStockAdjustment(product, change) },
                onAddProduct = {}, // Disable in POS view
                onNavigateToLabelGenerator = {}, // Disable in POS view
                tutorialViewModel = tutorialViewModel
            )
        }

        VerticalDivider(modifier = Modifier.width(1.dp), color = MaterialTheme.colorScheme.outlineVariant)

        // Right Column: Cart & Checkout
        Box(modifier = Modifier.weight(1f).fillMaxHeight()) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                SukiSectionTitle(title = "Shopping Cart")
                
                // Scanner Shortcut
                SukiPrimaryButton(
                    text = "Quick Scan",
                    icon = Icons.Rounded.QrCodeScanner,
                    onClick = onNavigateToScanner,
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                // The Cart Content (reusing extracted logic)
                PosCartContent(
                    viewModel = posViewModel,
                    currencySymbol = currencySymbol,
                    lang = lang,
                    onNavigateToCheckout = onNavigateToCheckout,
                    tutorialViewModel = tutorialViewModel
                )
            }
        }
    }
}
