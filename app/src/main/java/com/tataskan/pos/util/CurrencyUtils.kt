package com.tataskan.pos.util

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
     * Formats a double value as a currency string with the given symbol.
     */
    fun formatCurrency(amount: Double, symbol: String): String {
        return String.format(Locale.US, "%s%.2f", symbol, amount)
    }
}
