package com.tataskan.pos.ui.navigation

import kotlinx.serialization.Serializable

/**
 * Navigation routes for SukiPOS using Jetpack Navigation 3.
 */
@Serializable
sealed interface TataskanNavKey {
    @Serializable
    data object Splash : TataskanNavKey

    @Serializable
    data object Login : TataskanNavKey

    @Serializable
    data object Register : TataskanNavKey

    @Serializable
    data object Home : TataskanNavKey

    @Serializable
    data object ProductList : TataskanNavKey

    @Serializable
    data class ProductDetail(val id: Long) : TataskanNavKey

    @Serializable
    data class ProductAddEdit(val id: Long? = null) : TataskanNavKey

    @Serializable
    data object Pos : TataskanNavKey

    @Serializable
    data object Checkout : TataskanNavKey

    @Serializable
    data class Receipt(val transactionId: Long) : TataskanNavKey

    @Serializable
    data object Scanner : TataskanNavKey

    @Serializable
    data object ProductSearch : TataskanNavKey

    @Serializable
    data object LabelGenerator : TataskanNavKey

    @Serializable
    data object Reports : TataskanNavKey

    @Serializable
    data object Feedback : TataskanNavKey

    @Serializable
    data object Settings : TataskanNavKey

    @Serializable
    data object PromoList : TataskanNavKey

    @Serializable
    data object More : TataskanNavKey
}
