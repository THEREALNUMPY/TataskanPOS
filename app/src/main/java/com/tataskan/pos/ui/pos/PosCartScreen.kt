package com.tataskan.pos.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tataskan.pos.util.CurrencyUtils
import com.tataskan.pos.util.Strings
import java.util.Locale

import com.tataskan.pos.ui.components.*
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.tutorialTarget

@Composable
fun PosCartScreen(
    viewModel: PosViewModel,
    currencySymbol: String = "₱",
    lang: String = "en",
    onNavigateToCheckout: () -> Unit,
    onNavigateToScanner: () -> Unit,
    onNavigateToSearch: () -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    val error by viewModel.error.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(error) {
        error?.let {
            val result = snackbarHostState.showSnackbar(
                message = it,
                actionLabel = if (it.contains("Undo")) "Undo" else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoRemoveFromCart()
            }
            viewModel.clearError()
        }
    }

    val activeStep by tutorialViewModel.activeStep.collectAsState()
    val isTutorialActive = activeStep != null

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = onNavigateToScanner,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .tutorialTarget("scanner_icon", tutorialViewModel),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                enabled = !isTutorialActive || activeStep?.id == "scanner_icon"
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SCAN")
            }
            
            Button(
                onClick = onNavigateToSearch,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                enabled = !isTutorialActive
            ) {
                Icon(Icons.Rounded.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SEARCH")
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            PosCartContent(
                viewModel = viewModel,
                currencySymbol = currencySymbol,
                lang = lang,
                onNavigateToCheckout = onNavigateToCheckout,
                tutorialViewModel = tutorialViewModel
            )
        }
    }
}

@Composable
fun PosCartContent(
    viewModel: PosViewModel,
    currencySymbol: String,
    lang: String,
    onNavigateToCheckout: () -> Unit,
    tutorialViewModel: TutorialViewModel,
    showCheckoutButton: Boolean = true
) {
    val cartItems by viewModel.cartItems.collectAsState()
    val subtotal by viewModel.subtotal.collectAsState()
    val discountAmount by viewModel.discountAmount.collectAsState()
    val grandTotal by viewModel.grandTotal.collectAsState()
    val appliedPromo by viewModel.appliedPromo.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            if (cartItems.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    SukiEmptyState(
                        message = Strings.get("cart_empty", lang),
                        icon = Icons.Default.ShoppingCart,
                        description = Strings.get("guide_barcode", lang)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(cartItems, key = { it.product.id }) { item ->
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = {
                                if (it == SwipeToDismissBoxValue.EndToStart) {
                                    viewModel.removeFromCart(item.product.id)
                                    true
                                } else false
                            }
                        )

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color = when (dismissState.dismissDirection) {
                                    SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                                    else -> Color.Transparent
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(color, MaterialTheme.shapes.large)
                                        .padding(horizontal = 24.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                                }
                            }
                        ) {
                            CartItemRow(
                                item = item,
                                currencySymbol = currencySymbol,
                                lang = lang,
                                onIncrease = { viewModel.updateQuantity(item.product.id, item.quantity + 1) },
                                onDecrease = { viewModel.updateQuantity(item.product.id, item.quantity - 1) },
                                onRemove = { viewModel.removeFromCart(item.product.id) }
                            )
                        }
                    }
                }
            }
        }

        Surface(
            tonalElevation = 4.dp,
            shadowElevation = 16.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                if (appliedPromo != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(CurrencyUtils.formatCurrency(subtotal, currencySymbol), style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Discount (${appliedPromo?.name})", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                        Text("- ${CurrencyUtils.formatCurrency(discountAmount, currencySymbol)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                    }
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = Strings.get("grand_total", lang),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = CurrencyUtils.formatCurrency(grandTotal, currencySymbol),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                }
                
                if (showCheckoutButton) {
                    val activeStep by tutorialViewModel.activeStep.collectAsState()
                    val isTutorialActive = activeStep != null
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    SukiPrimaryButton(
                        text = Strings.get("checkout", lang),
                        icon = Icons.Default.Payments,
                        onClick = onNavigateToCheckout,
                        enabled = cartItems.isNotEmpty() && (!isTutorialActive || activeStep?.id == "checkout_btn"),
                        modifier = Modifier.tutorialTarget("checkout_btn", tutorialViewModel)
                    )
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    item: CartItem,
    currencySymbol: String = "₱",
    lang: String = "en",
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
    onRemove: () -> Unit
) {
    TataskanCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = item.product.imageUri,
                contentDescription = null,
                modifier = Modifier
                    .size(72.dp)
                    .padding(4.dp),
                contentScale = ContentScale.Crop
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Text(
                    text = "${CurrencyUtils.formatCurrency(item.product.price, currencySymbol)} x ${item.quantity}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = CurrencyUtils.formatCurrency(item.subtotal, currencySymbol),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                SukiIconButton(
                    icon = Icons.Default.Remove,
                    onClick = onDecrease,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(32.dp)
                )
                
                Text(
                    text = item.quantity.toString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                
                SukiIconButton(
                    icon = Icons.Default.Add,
                    onClick = onIncrease,
                    enabled = item.quantity < item.product.stock,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    backgroundColor = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}
