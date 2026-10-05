package com.tataskan.pos.ui.navigation

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.ui.NavDisplay
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import com.tataskan.pos.ui.auth.AuthViewModel
import com.tataskan.pos.ui.auth.LoginScreen
import com.tataskan.pos.ui.auth.RegisterScreen
import com.tataskan.pos.ui.pos.CheckoutScreen
import com.tataskan.pos.ui.pos.PosCartScreen
import com.tataskan.pos.ui.pos.PosTabletScreen
import com.tataskan.pos.ui.pos.PosViewModel
import com.tataskan.pos.ui.pos.ReceiptScreen
import com.tataskan.pos.ui.product.ProductAddEditPane
import com.tataskan.pos.ui.product.ProductDetailPane
import com.tataskan.pos.ui.product.ProductListPane
import com.tataskan.pos.ui.product.ProductViewModel
import com.tataskan.pos.ui.product.LabelGeneratorScreen
import com.tataskan.pos.ui.promo.PromoManagementScreen
import com.tataskan.pos.ui.promo.PromoViewModel
import com.tataskan.pos.ui.report.ReportViewModel
import com.tataskan.pos.ui.report.ReportingScreen
import com.tataskan.pos.ui.feedback.FeedbackScreen
import com.tataskan.pos.ui.settings.SettingsViewModel
import com.tataskan.pos.ui.settings.SettingsScreen
import com.tataskan.pos.ui.scanner.ScannerScreen
import com.tataskan.pos.ui.tutorial.TutorialViewModel
import com.tataskan.pos.ui.tutorial.GuidedTutorialOverlay
import com.tataskan.pos.ui.tutorial.TutorialStep
import com.tataskan.pos.ui.tutorial.tutorialTarget
import com.tataskan.pos.util.Strings
import com.google.mlkit.vision.barcode.common.Barcode
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3AdaptiveApi::class, ExperimentalMaterial3Api::class)
@Composable
fun TataskanNavigation(
    backStack: MutableList<TataskanNavKey>,
    authViewModel: AuthViewModel = viewModel(),
    windowSizeClass: WindowSizeClass
) {
    val settingsViewModel: SettingsViewModel = viewModel()
    val tutorialViewModel: TutorialViewModel = viewModel()
    
    val listDetailStrategy = rememberListDetailSceneStrategy<TataskanNavKey>()

    val isTutorialComplete by settingsViewModel.isTutorialComplete.collectAsStateWithLifecycle()
    
    val isWelcomeDismissed by tutorialViewModel.isWelcomeDismissed.collectAsStateWithLifecycle()
    val targetRect by tutorialViewModel.targetBounds.collectAsStateWithLifecycle()

    val hasCheckedTutorialThisSession = remember { mutableStateOf(false) }

    val userCount by authViewModel.userCount.collectAsStateWithLifecycle()
    val isLoggedIn by authViewModel.isLoggedIn.collectAsStateWithLifecycle()
    val isAuthLoading by authViewModel.isAuthLoading.collectAsStateWithLifecycle()
    val isSettingsLoading by settingsViewModel.isSettingsLoading.collectAsStateWithLifecycle()
    val activeStep by tutorialViewModel.activeStep.collectAsStateWithLifecycle()

    val isTrialExpired by authViewModel.isTrialExpired.collectAsStateWithLifecycle()
    val daysRemaining by authViewModel.daysRemaining.collectAsStateWithLifecycle()
    val lang by settingsViewModel.language.collectAsStateWithLifecycle()

    if (isTrialExpired) {
        TrialExpiredScreen(authViewModel = authViewModel, lang = lang)
        return
    }

    val currentKey = backStack.lastOrNull() ?: TataskanNavKey.Splash
    val currencySymbol by settingsViewModel.currencySymbol.collectAsStateWithLifecycle()

    val showBottomBar by remember(isLoggedIn, currentKey) {
        derivedStateOf {
            isLoggedIn && currentKey !in listOf(
                TataskanNavKey.Splash, 
                TataskanNavKey.Login, 
                TataskanNavKey.Register, 
                TataskanNavKey.Checkout,
                TataskanNavKey.Scanner
            ) && currentKey !is TataskanNavKey.Receipt && currentKey !is TataskanNavKey.ProductAddEdit && currentKey !is TataskanNavKey.ProductDetail
        }
    }

    LaunchedEffect(currentKey, activeStep?.id) {
        if (isLoggedIn && isWelcomeDismissed) {
            val stepId = activeStep?.id ?: return@LaunchedEffect
            
            delay(100.milliseconds)
            
            when {
                currentKey == TataskanNavKey.Pos && stepId == "pos" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.Scanner && stepId == "scanner_icon" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.Checkout && stepId == "checkout_btn" -> tutorialViewModel.nextStep()
                currentKey is TataskanNavKey.Receipt && stepId == "complete_sale_btn" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.Home && stepId == "finish_pos" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.ProductList && stepId == "inventory_home" -> tutorialViewModel.nextStep()
                currentKey is TataskanNavKey.ProductAddEdit && stepId == "inv_add" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.ProductList && stepId == "inv_save_btn" -> tutorialViewModel.nextStep()
                currentKey is TataskanNavKey.ProductDetail && stepId == "inv_card" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.ProductList && stepId == "inv_delete" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.More && stepId == "more_nav" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.Reports && stepId == "more_reports_item" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.PromoList && stepId == "more_promos_item" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.LabelGenerator && stepId == "more_labels_item" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.Settings && stepId == "more_settings_item" -> tutorialViewModel.nextStep()
                currentKey == TataskanNavKey.Feedback && stepId == "more_feedback_item" -> tutorialViewModel.nextStep()
            }
        }
    }

    LaunchedEffect(isTutorialComplete, isSettingsLoading, isLoggedIn, currentKey) {
        if (!isSettingsLoading && isTutorialComplete == false && isLoggedIn && currentKey == TataskanNavKey.Home && activeStep == null && !hasCheckedTutorialThisSession.value) {
            hasCheckedTutorialThisSession.value = true
            tutorialViewModel.reset()
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                authViewModel.checkSession(isInitialLoad = false)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    
    LaunchedEffect(isAuthLoading, isSettingsLoading, isLoggedIn, userCount) {
        if (isAuthLoading || isSettingsLoading) return@LaunchedEffect
        if (backStack.lastOrNull() == TataskanNavKey.Splash) {
            backStack.clear()
            if (isLoggedIn) backStack.add(TataskanNavKey.Home)
            else if (userCount == 0) backStack.add(TataskanNavKey.Register)
            else backStack.add(TataskanNavKey.Login)
        } else if (!isLoggedIn && currentKey != TataskanNavKey.Login && currentKey != TataskanNavKey.Register) {
            backStack.clear()
            backStack.add(TataskanNavKey.Login)
        }
    }

    if (isAuthLoading || isSettingsLoading || currentKey == TataskanNavKey.Splash) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary))
        return
    }

    val productViewModel: ProductViewModel = viewModel()
    val posViewModel: PosViewModel = viewModel()
    val reportViewModel: ReportViewModel = viewModel()
    val promoViewModel: PromoViewModel = viewModel()
    
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                SukiPosTopAppBar(
                    currentKey = currentKey,
                    lang = lang,
                    onBack = { 
                        val currentId = activeStep?.id
                        if (currentId != null && currentId.endsWith("_back_tip")) {
                            tutorialViewModel.nextStep()
                        }
                        if (backStack.size > 1) backStack.removeAt(backStack.size - 1) 
                    },
                    onProductEdit = { id -> backStack.add(TataskanNavKey.ProductAddEdit(id)) },
                    onProductDelete = { id ->
                        productViewModel.getProduct(id)?.let { productViewModel.deleteProduct(it) }
                        if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                    },
                    onPosSearch = { backStack.add(TataskanNavKey.ProductSearch) },
                    onPosScan = { backStack.add(TataskanNavKey.Scanner) },
                    tutorialViewModel = tutorialViewModel
                )
            },
            bottomBar = {
                if (showBottomBar) {
                    SukiPosBottomBar(
                        currentKey = currentKey,
                        onNavigate = { key ->
                            if (currentKey != key) {
                                while (backStack.size > 1) {
                                    backStack.removeAt(backStack.size - 1)
                                }
                                if (key != TataskanNavKey.Home) {
                                    backStack.add(key)
                                }
                            }
                        },
                        tutorialViewModel = tutorialViewModel
                    )
                }
            }
        ) { padding ->
            NavDisplay(
                backStack = backStack,
                onBack = { 
                    val currentId = activeStep?.id
                    if (currentId != null && currentId.endsWith("_back_tip")) {
                        tutorialViewModel.nextStep()
                    }
                    if (backStack.isNotEmpty()) backStack.removeAt(backStack.size - 1) 
                },
                modifier = Modifier.fillMaxSize().padding(padding),
                transitionSpec = { 
                    fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                },
                sceneStrategy = listDetailStrategy,
                entryProvider = { key ->
                    when (key) {
                        TataskanNavKey.Splash -> NavEntry(key) { Box(Modifier.fillMaxSize()) }
                        TataskanNavKey.Login -> NavEntry(key) {
                            LoginScreen(viewModel = authViewModel, lang = lang, onLoginSuccess = { 
                                backStack.clear()
                                backStack.add(TataskanNavKey.Home) 
                            }, onNavigateToRegister = { backStack.add(TataskanNavKey.Register) })
                        }
                        TataskanNavKey.Register -> NavEntry(key) {
                            RegisterScreen(viewModel = authViewModel, lang = lang, onRegisterSuccess = { 
                                backStack.clear()
                                backStack.add(TataskanNavKey.Home) 
                            })
                        }
                        TataskanNavKey.Home -> NavEntry(key) {
                            val storeName by settingsViewModel.storeName.collectAsStateWithLifecycle()
                            HomeScreen(
                                onNavigateToPos = { backStack.add(TataskanNavKey.Pos) },
                                onNavigateToInventory = { backStack.add(TataskanNavKey.ProductList) },
                                onNavigateToReports = { backStack.add(TataskanNavKey.Reports) },
                                storeName = storeName,
                                lang = lang,
                                reportViewModel = reportViewModel,
                                currencySymbol = currencySymbol,
                                onTransactionClick = { id -> backStack.add(TataskanNavKey.Receipt(id)) },
                                tutorialViewModel = tutorialViewModel,
                                windowSizeClass = windowSizeClass,
                                daysRemaining = daysRemaining
                            )
                        }
                        TataskanNavKey.Pos -> NavEntry(key) {
                            val isExpanded = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
                            
                            if (isExpanded) {
                                val products by productViewModel.products.collectAsStateWithLifecycle()
                                val categories by productViewModel.categories.collectAsStateWithLifecycle()
                                PosTabletScreen(
                                    posViewModel = posViewModel,
                                    productViewModel = productViewModel,
                                    products = products,
                                    categories = categories,
                                    currencySymbol = currencySymbol,
                                    lang = lang,
                                    onNavigateToCheckout = { backStack.add(TataskanNavKey.Checkout) },
                                    onNavigateToScanner = { backStack.add(TataskanNavKey.Scanner) },
                                    tutorialViewModel = tutorialViewModel
                                )
                            } else {
                                PosCartScreen(
                                    viewModel = posViewModel, 
                                    currencySymbol = currencySymbol, 
                                    lang = lang, 
                                    onNavigateToCheckout = { backStack.add(TataskanNavKey.Checkout) },
                                    onNavigateToScanner = { backStack.add(TataskanNavKey.Scanner) },
                                    onNavigateToSearch = { backStack.add(TataskanNavKey.ProductSearch) },
                                    tutorialViewModel = tutorialViewModel
                                )
                            }
                        }
                        TataskanNavKey.ProductList -> NavEntry(key, ListDetailSceneStrategy.listPane()) {
                            val products by productViewModel.products.collectAsStateWithLifecycle()
                            val categories by productViewModel.categories.collectAsStateWithLifecycle()
                            ProductListPane(
                                products = products, 
                                categories = categories, 
                                currencySymbol = currencySymbol, 
                                lang = lang, 
                                onProductClick = { p -> backStack.add(TataskanNavKey.ProductDetail(p.id)) }, 
                                onStockAdjust = { p, c -> productViewModel.quickStockAdjustment(p, c) },
                                onAddProduct = { backStack.add(TataskanNavKey.ProductAddEdit(null)) }, 
                                onNavigateToLabelGenerator = { backStack.add(TataskanNavKey.LabelGenerator) },
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        TataskanNavKey.ProductSearch -> NavEntry(key) {
                            val products by productViewModel.products.collectAsStateWithLifecycle()
                            val categories by productViewModel.categories.collectAsStateWithLifecycle()
                            ProductListPane(
                                products = products, 
                                categories = categories, 
                                currencySymbol = currencySymbol, 
                                lang = lang, 
                                onProductClick = { p -> 
                                    posViewModel.addToCart(p)
                                    backStack.removeAt(backStack.size - 1) 
                                }, 
                                onStockAdjust = { p, c -> productViewModel.quickStockAdjustment(p, c) },
                                onAddProduct = { backStack.add(TataskanNavKey.ProductAddEdit(null)) }, 
                                onNavigateToLabelGenerator = { backStack.add(TataskanNavKey.LabelGenerator) },
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        TataskanNavKey.LabelGenerator -> NavEntry(key) {
                            val products by productViewModel.products.collectAsStateWithLifecycle()
                            val showName by settingsViewModel.showNameOnLabel.collectAsStateWithLifecycle()
                            val showPrice by settingsViewModel.showPriceOnLabel.collectAsStateWithLifecycle()
                            LabelGeneratorScreen(
                                products = products, 
                                currencySymbol = currencySymbol, 
                                showNameOnLabel = showName, 
                                showPriceOnLabel = showPrice, 
                                lang = lang,
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        is TataskanNavKey.ProductDetail -> NavEntry(key, ListDetailSceneStrategy.detailPane()) {
                            val product = productViewModel.getProduct(key.id)
                            ProductDetailPane(
                                product = product, 
                                currencySymbol = currencySymbol, 
                                lang = lang, 
                                onEdit = { p -> backStack.add(TataskanNavKey.ProductAddEdit(p.id)) }, 
                                onDelete = { p -> 
                                    productViewModel.deleteProduct(p)
                                    backStack.removeAt(backStack.size - 1) 
                                },
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        is TataskanNavKey.ProductAddEdit -> NavEntry(key) {
                            val product = productViewModel.getProduct(key.id)
                            val categories by productViewModel.categories.collectAsStateWithLifecycle()
                            ProductAddEditPane(
                                product = product, 
                                categories = categories, 
                                currencySymbol = currencySymbol, 
                                lang = lang, 
                                tutorialViewModel = tutorialViewModel,
                                onSave = { n, p, c, cat, s, b, i -> 
                                    productViewModel.saveProduct(key.id ?: 0, n, p, c, cat, s, b, i)
                                    backStack.removeAt(backStack.size - 1) 
                                }
                            )
                        }
                        TataskanNavKey.Checkout -> NavEntry(key) {
                            val taxPercentage by settingsViewModel.taxPercentage.collectAsStateWithLifecycle()
                            val gcashQrUri by settingsViewModel.gcashQrUri.collectAsStateWithLifecycle()
                            val mayaQrUri by settingsViewModel.mayaQrUri.collectAsStateWithLifecycle()
                            CheckoutScreen(
                                viewModel = posViewModel, 
                                currencySymbol = currencySymbol, 
                                taxPercentage = taxPercentage, 
                                gcashQrUri = gcashQrUri,
                                mayaQrUri = mayaQrUri,
                                lang = lang, 
                                onSaleCompleted = { id -> 
                                    backStack.removeAt(backStack.size - 1)
                                    backStack.removeAt(backStack.size - 1)
                                    backStack.add(TataskanNavKey.Receipt(id)) 
                                },
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        is TataskanNavKey.Receipt -> NavEntry(key) {
                            ReceiptScreen(
                                transactionId = key.transactionId, 
                                posViewModel = posViewModel, 
                                settingsViewModel = settingsViewModel, 
                                onDone = { 
                                    if (backStack.lastOrNull() is TataskanNavKey.Receipt) {
                                        backStack.removeAt(backStack.size - 1)
                                    } else {
                                        backStack.clear()
                                        backStack.add(TataskanNavKey.Home) 
                                    }
                                }
                            )
                        }
                        TataskanNavKey.Scanner -> NavEntry(key) {
                            val defaultBulkScan by settingsViewModel.defaultBulkScan.collectAsStateWithLifecycle()
                            ScannerScreen(
                                barcodeFormats = Barcode.FORMAT_ALL_FORMATS, 
                                lang = lang, 
                                defaultContinuousMode = defaultBulkScan,
                                onBarcodeDetected = { b, isC, onP -> 
                                    posViewModel.onBarcodeDetected(b) { found -> 
                                        onP(found)
                                        if (found && !isC) backStack.removeAt(backStack.size - 1) 
                                    } 
                                }
                            )
                        }
                        TataskanNavKey.Reports -> NavEntry(key) {
                            ReportingScreen(
                                viewModel = reportViewModel, 
                                currencySymbol = currencySymbol, 
                                lang = lang, 
                                onTransactionClick = { id -> backStack.add(TataskanNavKey.Receipt(id)) },
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        TataskanNavKey.Feedback -> NavEntry(key) { FeedbackScreen(tutorialViewModel = tutorialViewModel) }
                        TataskanNavKey.Settings -> NavEntry(key) {
                            val scope = rememberCoroutineScope()
                            SettingsScreen(
                                viewModel = settingsViewModel, 
                                authViewModel = authViewModel, 
                                tutorialViewModel = tutorialViewModel,
                                onRestartTutorial = {
                                    scope.launch {
                                        settingsViewModel.setTutorialComplete(false)
                                        while(backStack.size > 1) {
                                            backStack.removeAt(backStack.size - 1)
                                        }
                                        hasCheckedTutorialThisSession.value = false
                                        tutorialViewModel.reset()
                                    }
                                }
                            )
                        }
                        TataskanNavKey.PromoList -> NavEntry(key) {
                            val products by productViewModel.products.collectAsStateWithLifecycle()
                            PromoManagementScreen(
                                viewModel = promoViewModel, 
                                products = products, 
                                currencySymbol = currencySymbol, 
                                lang = lang,
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                        TataskanNavKey.More -> NavEntry(key) {
                            MoreScreen(
                                lang = lang,
                                onNavigateToReports = { backStack.add(TataskanNavKey.Reports) },
                                onNavigateToPromos = { backStack.add(TataskanNavKey.PromoList) },
                                onNavigateToSettings = { backStack.add(TataskanNavKey.Settings) },
                                onNavigateToFeedback = { backStack.add(TataskanNavKey.Feedback) },
                                onNavigateToLabelGenerator = { backStack.add(TataskanNavKey.LabelGenerator) },
                                tutorialViewModel = tutorialViewModel
                            )
                        }
                    }
                }
            )
        }

        val currentActiveStep = activeStep
        if (currentActiveStep != null) {
            val isExpectedScreen by remember(currentActiveStep.id, currentKey) {
                derivedStateOf {
                    when(currentActiveStep.id) {
                        "pos" -> currentKey == TataskanNavKey.Home
                        "scanner_icon" -> currentKey == TataskanNavKey.Pos
                        "checkout_btn" -> currentKey == TataskanNavKey.Pos
                        "amount_input" -> currentKey == TataskanNavKey.Checkout
                        "complete_sale_btn" -> currentKey == TataskanNavKey.Checkout
                        "finish_pos" -> currentKey is TataskanNavKey.Receipt || currentKey == TataskanNavKey.Home
                        "inventory_home" -> currentKey == TataskanNavKey.Home
                        "inv_search", "inv_low_stock", "inv_filter", "inv_add", "inv_card", "inv_print" -> currentKey == TataskanNavKey.ProductList
                        "inv_detail", "inv_download", "inv_delete" -> currentKey is TataskanNavKey.ProductDetail
                        "more_nav" -> true 
                        "more_reports_item", "more_promos_item", "more_labels_item", "more_settings_item", "more_feedback_item" -> currentKey == TataskanNavKey.More
                        "reporting_export_btn" -> currentKey == TataskanNavKey.Reports
                        "promo_add_fab" -> currentKey == TataskanNavKey.PromoList
                        "label_gen_btn" -> currentKey == TataskanNavKey.LabelGenerator
                        "settings_business", "settings_tax", "settings_regional", "settings_backup", "settings_demo" -> currentKey == TataskanNavKey.Settings
                        "feedback_stars", "feedback_submit_btn" -> currentKey == TataskanNavKey.Feedback
                        "final" -> currentKey == TataskanNavKey.Home
                        else -> true
                    }
                }
            }

            if (isExpectedScreen) {
                GuidedTutorialOverlay(
                    activeStep = currentActiveStep,
                    onNext = {
                        val currentId = currentActiveStep.id
                        
                        when (currentId) {
                            "pos" -> if (currentKey != TataskanNavKey.Pos) backStack.add(TataskanNavKey.Pos) else tutorialViewModel.nextStep()
                            "scanner_icon" -> if (currentKey != TataskanNavKey.Scanner) backStack.add(TataskanNavKey.Scanner) else tutorialViewModel.nextStep()
                            "scanner_explain" -> {
                                if (backStack.lastOrNull() == TataskanNavKey.Scanner) backStack.removeAt(backStack.size - 1)
                                posViewModel.addToCart(com.tataskan.pos.data.local.entity.Product(
                                    id = 0, name = "Dummy Product", price = 100.0, cost = 50.0,
                                    category = "Demo", stock = 99, barcode = "123456789", imageUri = null
                                ))
                                tutorialViewModel.nextStep()
                            }
                            "checkout_btn" -> if (currentKey != TataskanNavKey.Checkout) backStack.add(TataskanNavKey.Checkout) else tutorialViewModel.nextStep()
                            "complete_sale_btn" -> {
                                val currentTotal = posViewModel.grandTotal.value
                                posViewModel.completeSale(currentTotal, 0f) { id ->
                                    while (backStack.lastOrNull() != TataskanNavKey.Pos && backStack.size > 1) {
                                        backStack.removeAt(backStack.size - 1)
                                    }
                                    if (backStack.lastOrNull() == TataskanNavKey.Pos) backStack.removeAt(backStack.size - 1)
                                    backStack.add(TataskanNavKey.Receipt(id))
                                }
                            }
                            "finish_pos" -> {
                                if (backStack.size > 1) while (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                else tutorialViewModel.nextStep()
                            }
                            "inventory_home" -> if (currentKey != TataskanNavKey.ProductList) backStack.add(TataskanNavKey.ProductList) else tutorialViewModel.nextStep()
                            "inv_add" -> {
                                if (currentKey != TataskanNavKey.ProductAddEdit()) {
                                    backStack.add(TataskanNavKey.ProductAddEdit())
                                } else {
                                    // Already on screen, just force next step manually if needed
                                    tutorialViewModel.nextStep()
                                }
                            }
                            "inv_save_btn" -> {
                                if (currentKey is TataskanNavKey.ProductAddEdit) {
                                    productViewModel.saveProduct(name = "Tutorial Product", price = 10.0, cost = 5.0, categoryName = "Tutorial", stock = 10, barcode = "999", imageUri = null)
                                    if (backStack.size > 1) {
                                        backStack.removeAt(backStack.size - 1)
                                    }
                                    tutorialViewModel.nextStep()
                                } else {
                                    tutorialViewModel.nextStep()
                                }
                            }
                            "inv_card" -> if (currentKey == TataskanNavKey.ProductList) {
                                productViewModel.products.value.firstOrNull()?.let { backStack.add(TataskanNavKey.ProductDetail(it.id)) }
                            } else tutorialViewModel.nextStep()
                            "inv_delete" -> {
                                if (currentKey is TataskanNavKey.ProductDetail) {
                                    backStack.removeAt(backStack.size - 1)
                                }
                                tutorialViewModel.nextStep()
                            }
                            "more_nav" -> if (currentKey != TataskanNavKey.More) {
                                backStack.clear()
                                backStack.add(TataskanNavKey.Home)
                                backStack.add(TataskanNavKey.More)
                            } else tutorialViewModel.nextStep()
                            "more_reports_item" -> if (currentKey != TataskanNavKey.Reports) backStack.add(TataskanNavKey.Reports) else tutorialViewModel.nextStep()
                            "more_promos_item" -> if (currentKey != TataskanNavKey.PromoList) backStack.add(TataskanNavKey.PromoList) else tutorialViewModel.nextStep()
                            "more_labels_item" -> if (currentKey != TataskanNavKey.LabelGenerator) backStack.add(TataskanNavKey.LabelGenerator) else tutorialViewModel.nextStep()
                            "more_settings_item" -> if (currentKey != TataskanNavKey.Settings) backStack.add(TataskanNavKey.Settings) else tutorialViewModel.nextStep()
                            "reporting_export_btn" -> {
                                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                tutorialViewModel.nextStep()
                            }
                            "promo_add_fab" -> {
                                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                tutorialViewModel.nextStep()
                            }
                            "label_gen_btn" -> {
                                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                tutorialViewModel.nextStep()
                            }
                            "settings_demo" -> {
                                if (backStack.size > 1) backStack.removeAt(backStack.size - 1)
                                tutorialViewModel.nextStep()
                            }
                            "more_feedback_item" -> if (currentKey != TataskanNavKey.Feedback) backStack.add(TataskanNavKey.Feedback) else tutorialViewModel.nextStep()
                            "feedback_submit_btn" -> {
                                // Go all the way back to Home (Dashboard)
                                while (backStack.size > 1) {
                                    backStack.removeAt(backStack.size - 1)
                                }
                                tutorialViewModel.nextStep()
                            }
                            "final" -> {
                                tutorialViewModel.setStepIndex(-1)
                                tutorialViewModel.updateTargetBounds(null)
                                settingsViewModel.setTutorialComplete(true)
                            }
                            else -> tutorialViewModel.nextStep()
                        }
                    },
                    onSkip = {
                        tutorialViewModel.setStepIndex(-1)
                        tutorialViewModel.updateTargetBounds(null)
                        settingsViewModel.setTutorialComplete(true)
                    },
                    canSkip = true,
                    lang = lang,
                    targetBounds = targetRect
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SukiPosTopAppBar(
    currentKey: TataskanNavKey,
    lang: String,
    onBack: () -> Unit,
    onProductEdit: (Long) -> Unit,
    onProductDelete: (Long) -> Unit,
    onPosSearch: () -> Unit,
    onPosScan: () -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    val activeStep by tutorialViewModel.activeStep.collectAsStateWithLifecycle()
    if (activeStep?.id == "pos") return
    
    val title = when (currentKey) {
        TataskanNavKey.Splash -> "SukiPOS"
        TataskanNavKey.Login -> "Login"
        TataskanNavKey.Register -> "Register"
        TataskanNavKey.Home -> "SukiPOS"
        TataskanNavKey.ProductList -> Strings.get("inventory", lang)
        is TataskanNavKey.ProductDetail -> Strings.get("product_details", lang)
        is TataskanNavKey.ProductAddEdit -> if (currentKey.id == null) Strings.get("add_product", lang) else Strings.get("edit_product", lang)
        TataskanNavKey.Pos -> Strings.get("shopping_cart", lang)
        TataskanNavKey.Checkout -> Strings.get("checkout", lang)
        is TataskanNavKey.Receipt -> Strings.get("receipt", lang)
        TataskanNavKey.Scanner -> Strings.get("scan_barcode", lang)
        TataskanNavKey.ProductSearch -> Strings.get("search_products", lang)
        TataskanNavKey.LabelGenerator -> Strings.get("bulk_labels", lang)
        TataskanNavKey.Reports -> Strings.get("reports", lang)
        TataskanNavKey.Feedback -> Strings.get("feedback", lang)
        TataskanNavKey.Settings -> Strings.get("settings", lang)
        TataskanNavKey.PromoList -> Strings.get("promos", lang)
        TataskanNavKey.More -> "More"
    }

    val showBackButton = when (currentKey) {
        TataskanNavKey.Home, TataskanNavKey.Login, TataskanNavKey.Splash, is TataskanNavKey.Receipt -> false
        else -> true
    }

    CenterAlignedTopAppBar(
        title = { Text(title, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            if (showBackButton) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
            }
        },
        actions = {
            when (currentKey) {
                is TataskanNavKey.ProductDetail -> {
                    IconButton(onClick = { onProductEdit(currentKey.id) }) { Icon(Icons.Default.Edit, contentDescription = null) }
                    IconButton(onClick = { onProductDelete(currentKey.id) }) { Icon(Icons.Default.Delete, contentDescription = null) }
                }
                TataskanNavKey.Pos -> {
                    IconButton(onClick = onPosSearch) { Icon(Icons.Rounded.Search, contentDescription = null) }
                    IconButton(
                        onClick = onPosScan,
                        modifier = Modifier.tutorialTarget("scanner_icon", tutorialViewModel)
                    ) { 
                        Icon(Icons.Rounded.QrCodeScanner, contentDescription = null) 
                    }
                }
                else -> {}
            }
        }
    )
}

@Composable
fun SukiPosBottomBar(
    currentKey: TataskanNavKey,
    onNavigate: (TataskanNavKey) -> Unit,
    tutorialViewModel: TutorialViewModel
) {
    NavigationBar(
        tonalElevation = 8.dp,
        windowInsets = WindowInsets.navigationBars
    ) {
        NavigationBarItem(
            selected = currentKey == TataskanNavKey.Home,
            onClick = { onNavigate(TataskanNavKey.Home) },
            icon = { Icon(Icons.Default.Dashboard, contentDescription = null) },
            label = { Text("Dashboard") }
        )
        NavigationBarItem(
            selected = currentKey == TataskanNavKey.Pos,
            onClick = { onNavigate(TataskanNavKey.Pos) },
            icon = { Icon(Icons.Default.PointOfSale, contentDescription = null) },
            label = { Text("POS") },
            modifier = Modifier.tutorialTarget("pos", tutorialViewModel)
        )
        NavigationBarItem(
            selected = currentKey == TataskanNavKey.ProductList,
            onClick = { onNavigate(TataskanNavKey.ProductList) },
            icon = { Icon(Icons.Default.Inventory, contentDescription = null) },
            label = { Text("Inventory") },
            modifier = Modifier.tutorialTarget("inventory_home", tutorialViewModel)
        )
        NavigationBarItem(
            selected = currentKey == TataskanNavKey.More || currentKey in listOf(TataskanNavKey.Reports, TataskanNavKey.PromoList, TataskanNavKey.Settings),
            onClick = { onNavigate(TataskanNavKey.More) },
            icon = { Icon(Icons.Default.MoreHoriz, contentDescription = null) },
            label = { Text("More") },
            modifier = Modifier.tutorialTarget("more_nav", tutorialViewModel)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialExpiredScreen(
    authViewModel: AuthViewModel,
    lang: String = "en"
) {
    val context = LocalContext.current
    var promoCodeInput by remember { mutableStateOf("") }
    var showExtensionDialog by remember { mutableStateOf(false) }
    var showExtensionSuccessDialog by remember { mutableStateOf(false) }
    val error by authViewModel.error.collectAsStateWithLifecycle()

    val emailIntent = remember {
        android.content.Intent(android.content.Intent.ACTION_SENDTO).apply {
            data = android.net.Uri.parse("mailto:")
            putExtra(android.content.Intent.EXTRA_EMAIL, arrayOf("renzrojo692@gmail.com"))
            putExtra(Intent.EXTRA_SUBJECT, "TataskanPOS Trial Subscription Request")
            putExtra(Intent.EXTRA_TEXT, "Hello, I would like to continue using TataskanPOS after my trial. My store name is: ...")
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.LockClock,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = MaterialTheme.colorScheme.error
            )

            Text(
                text = "Trial Period Expired",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your trial of TataskanPOS has ended. Enter a secret extension promo code or request a ticket via Facebook support below.",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Redeem Trial Extension Promo Code", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = promoCodeInput,
                        onValueChange = { promoCodeInput = it.uppercase() },
                        label = { Text(Strings.get("enter_promo_code", lang)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Button(
                        onClick = {
                            if (promoCodeInput.isNotBlank()) {
                                authViewModel.redeemPromoCode(promoCodeInput, lang) { msg ->
                                    showExtensionSuccessDialog = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(Strings.get("redeem_code", lang))
                    }

                    if (error != null) {
                        Text(
                            text = error ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            Button(
                onClick = { showExtensionDialog = true; authViewModel.clearError() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ConfirmationNumber, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Request Extension Ticket (AX-)")
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
                        e.printStackTrace()
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Share, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open Facebook Support")
            }

            TextButton(
                onClick = {
                    try {
                        context.startActivity(emailIntent)
                    } catch (e: Exception) {
                        // Handle failure
                    }
                }
            ) {
                Text("Contact Support via Email")
            }
        }
    }

    if (showExtensionDialog) {
        TrialExtensionTicketDialog(
            authViewModel = authViewModel,
            lang = lang,
            onDismiss = { showExtensionDialog = false; authViewModel.clearError() },
            onSuccess = { 
                showExtensionDialog = false
                showExtensionSuccessDialog = true 
            }
        )
    }

    if (showExtensionSuccessDialog) {
        AlertDialog(
            onDismissRequest = { showExtensionSuccessDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = Color(0xFF2E7D32),
                    modifier = Modifier.size(48.dp)
                )
            },
            title = { Text(Strings.get("trial_extended_title", lang), fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    text = Strings.get("trial_extended_desc", lang),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(
                    onClick = { showExtensionSuccessDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(Strings.get("continue_dashboard", lang), fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrialExtensionTicketDialog(
    authViewModel: com.tataskan.pos.ui.auth.AuthViewModel,
    lang: String = "en",
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val context = LocalContext.current
    var generatedTicketCode by remember { mutableStateOf<Int?>(null) }
    var ticketText by remember { mutableStateOf("") }
    var secondsLeft by remember { mutableIntStateOf(300) }
    var adminCode by remember { mutableStateOf("") }
    val error by authViewModel.error.collectAsStateWithLifecycle()

    LaunchedEffect(generatedTicketCode) {
        if (generatedTicketCode != null) {
            secondsLeft = 300
            while (secondsLeft > 0) {
                kotlinx.coroutines.delay(1000)
                secondsLeft--
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Request Trial Extension", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (generatedTicketCode == null) {
                    Text("Generate an extension ticket (AX-) and send it to our Facebook Support page. An admin will give you a verification code to extend your trial for 15 days.")

                    Button(
                        onClick = {
                            val fullTicket = authViewModel.generateExtensionTicket()
                            ticketText = fullTicket
                            generatedTicketCode = authViewModel.activeExtensionTicket
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Generate AX- Ticket")
                    }
                } else {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Your Extension Ticket Code:", style = MaterialTheme.typography.labelMedium)
                            Text(
                                ticketText,
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Expires in: ${secondsLeft / 60}:%02d".format(secondsLeft % 60),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (secondsLeft < 60) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(
                                    android.content.Intent.ACTION_VIEW,
                                    android.net.Uri.parse("https://www.facebook.com/profile.php?id=61594957198671")
                                )
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Send Ticket to Facebook Support")
                    }

                    OutlinedTextField(
                        value = adminCode,
                        onValueChange = { if (it.length <= 6) adminCode = it },
                        label = { Text(Strings.get("enter_admin_code", lang), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                    )
                }

                if (error != null) {
                    Text(
                        text = error ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            if (generatedTicketCode != null) {
                Button(
                    onClick = {
                        val ticket = generatedTicketCode
                        if (ticket != null) {
                            authViewModel.verifyAndExtendWithTicket(ticket, adminCode, lang, onSuccess)
                        }
                    }
                ) {
                    Text("Verify & Extend Trial")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(Strings.get("cancel", lang))
            }
        }
    )
}
