package com.tataskan.pos.util

import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

object CurrencyUtils {
    /**
     * Returns the currency symbol based on the device's current locale.
     * Defaults to Peso (₱) if detection fails.
     */
    fun getCurrencySymbol(): String {
        return try {
            val currency = Currency.getInstance(Locale.getDefault())
            currency.symbol
        } catch (e: Exception) {
            "₱"
        }
    }
    
    /**
     * Formats a double value as a currency string with comma thousands separators.
     */
    fun formatCurrency(amount: Double, symbol: String, includeDecimals: Boolean = true): String {
        val formatter = NumberFormat.getNumberInstance(Locale.US).apply {
            minimumFractionDigits = if (includeDecimals) 2 else 0
            maximumFractionDigits = if (includeDecimals) 2 else 0
        }
        return "$symbol${formatter.format(amount)}"
    }
}
