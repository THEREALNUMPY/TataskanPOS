package com.tataskan.pos.data.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val SHOW_NAME_ON_LABEL = booleanPreferencesKey("show_name_on_label")
        val SHOW_PRICE_ON_LABEL = booleanPreferencesKey("show_price_on_label")
        val STORE_NAME = stringPreferencesKey("store_name")
        val STORE_LOGO_URI = stringPreferencesKey("store_logo_uri")
        val STORE_ADDRESS = stringPreferencesKey("store_address")
        val PHONE_NUMBER = stringPreferencesKey("phone_number")
        val RECEIPT_FOOTER = stringPreferencesKey("receipt_footer")
        val TAX_PERCENTAGE = floatPreferencesKey("tax_percentage")
        val LANGUAGE = stringPreferencesKey("language")
        val RECEIPT_ENABLED = booleanPreferencesKey("receipt_enabled")
        val BARCODE_FORMATS = stringPreferencesKey("barcode_formats")
        val IS_TUTORIAL_COMPLETE = booleanPreferencesKey("is_tutorial_complete")
        val LOGGED_IN_USERNAME = stringPreferencesKey("logged_in_username")
        val LAST_LOGIN_TIMESTAMP = longPreferencesKey("last_login_timestamp")
        val DEFAULT_BULK_SCAN = booleanPreferencesKey("default_bulk_scan")
        val TRIAL_START_TIMESTAMP = longPreferencesKey("trial_start_timestamp")
        val EXTRA_TRIAL_DAYS = intPreferencesKey("extra_trial_days")
        val GCASH_QR_URI = stringPreferencesKey("gcash_qr_uri")
        val MAYA_QR_URI = stringPreferencesKey("maya_qr_uri")
        val GOOGLE_DRIVE_ACCOUNT = stringPreferencesKey("google_drive_account")
        val LAST_DRIVE_BACKUP_TIME = longPreferencesKey("last_drive_backup_time")
        val LAST_LOCAL_BACKUP_TIME = longPreferencesKey("last_local_backup_time")
        val IS_LOCAL_BACKUP_ENABLED = booleanPreferencesKey("is_local_backup_enabled")
        val BACKUP_FREQUENCY = stringPreferencesKey("backup_frequency")
    }

    val currencySymbol: Flow<String> = context.dataStore.data.map { it[Keys.CURRENCY_SYMBOL] ?: "₱" }
    val showNameOnLabel: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_NAME_ON_LABEL] ?: true }
    val showPriceOnLabel: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_PRICE_ON_LABEL] ?: true }
    
    val storeName: Flow<String> = context.dataStore.data.map { it[Keys.STORE_NAME] ?: "TataskanPOS Store" }
    val storeLogoUri: Flow<String?> = context.dataStore.data.map { it[Keys.STORE_LOGO_URI] }
    val storeAddress: Flow<String> = context.dataStore.data.map { it[Keys.STORE_ADDRESS] ?: "" }
    val phoneNumber: Flow<String> = context.dataStore.data.map { it[Keys.PHONE_NUMBER] ?: "" }
    val receiptFooter: Flow<String> = context.dataStore.data.map { it[Keys.RECEIPT_FOOTER] ?: "Thank you for shopping!" }
    val taxPercentage: Flow<Float> = context.dataStore.data.map { it[Keys.TAX_PERCENTAGE] ?: 0f }
    val language: Flow<String> = context.dataStore.data.map { it[Keys.LANGUAGE] ?: "en" }
    val receiptEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.RECEIPT_ENABLED] ?: true }
    val barcodeFormats: Flow<String> = context.dataStore.data.map { it[Keys.BARCODE_FORMATS] ?: "QR_CODE,EAN_13,UPC_A" }
    val isTutorialComplete: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_TUTORIAL_COMPLETE] ?: false }
    val loggedInUsername: Flow<String?> = context.dataStore.data.map { it[Keys.LOGGED_IN_USERNAME] }
    val lastLoginTimestamp: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_LOGIN_TIMESTAMP] ?: 0L }
    val defaultBulkScan: Flow<Boolean> = context.dataStore.data.map { it[Keys.DEFAULT_BULK_SCAN] ?: false }
    val trialStartTimestamp: Flow<Long?> = context.dataStore.data.map { it[Keys.TRIAL_START_TIMESTAMP] }
    val extraTrialDays: Flow<Int> = context.dataStore.data.map { it[Keys.EXTRA_TRIAL_DAYS] ?: 0 }
    val gcashQrUri: Flow<String?> = context.dataStore.data.map { it[Keys.GCASH_QR_URI] }
    val mayaQrUri: Flow<String?> = context.dataStore.data.map { it[Keys.MAYA_QR_URI] }
    val googleDriveAccount: Flow<String?> = context.dataStore.data.map { it[Keys.GOOGLE_DRIVE_ACCOUNT] }
    val lastDriveBackupTime: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_DRIVE_BACKUP_TIME] ?: 0L }
    val lastLocalBackupTime: Flow<Long> = context.dataStore.data.map { it[Keys.LAST_LOCAL_BACKUP_TIME] ?: 0L }
    val isLocalBackupEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.IS_LOCAL_BACKUP_ENABLED] ?: true }
    val backupFrequency: Flow<String> = context.dataStore.data.map { it[Keys.BACKUP_FREQUENCY] ?: "DAILY" }

    suspend fun updateCurrencySymbol(symbol: String) = context.dataStore.edit { it[Keys.CURRENCY_SYMBOL] = symbol }
    suspend fun updateShowNameOnLabel(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_NAME_ON_LABEL] = show }
    suspend fun updateShowPriceOnLabel(show: Boolean) = context.dataStore.edit { it[Keys.SHOW_PRICE_ON_LABEL] = show }
    suspend fun updateBackupFrequency(frequency: String) = context.dataStore.edit { it[Keys.BACKUP_FREQUENCY] = frequency }
    
    suspend fun updateGcashQrUri(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.GCASH_QR_URI) else it[Keys.GCASH_QR_URI] = uri
    }
    suspend fun updateMayaQrUri(uri: String?) = context.dataStore.edit {
        if (uri == null) it.remove(Keys.MAYA_QR_URI) else it[Keys.MAYA_QR_URI] = uri
    }
    suspend fun updateGoogleDriveAccount(account: String?) = context.dataStore.edit {
        if (account == null) it.remove(Keys.GOOGLE_DRIVE_ACCOUNT) else it[Keys.GOOGLE_DRIVE_ACCOUNT] = account
    }
    suspend fun updateLastDriveBackupTime(timestamp: Long) = context.dataStore.edit {
        it[Keys.LAST_DRIVE_BACKUP_TIME] = timestamp
    }
    suspend fun updateLastLocalBackupTime(timestamp: Long) = context.dataStore.edit {
        it[Keys.LAST_LOCAL_BACKUP_TIME] = timestamp
    }
    suspend fun updateIsLocalBackupEnabled(enabled: Boolean) = context.dataStore.edit {
        it[Keys.IS_LOCAL_BACKUP_ENABLED] = enabled
    }
    
    suspend fun updateStoreName(name: String) = context.dataStore.edit { it[Keys.STORE_NAME] = name }
    suspend fun updateStoreLogoUri(uri: String?) = context.dataStore.edit { 
        if (uri == null) it.remove(Keys.STORE_LOGO_URI) else it[Keys.STORE_LOGO_URI] = uri 
    }
    suspend fun updateStoreAddress(address: String) = context.dataStore.edit { it[Keys.STORE_ADDRESS] = address }
    suspend fun updatePhoneNumber(phone: String) = context.dataStore.edit { it[Keys.PHONE_NUMBER] = phone }
    suspend fun updateReceiptFooter(footer: String) = context.dataStore.edit { it[Keys.RECEIPT_FOOTER] = footer }
    suspend fun updateTaxPercentage(tax: Float) = context.dataStore.edit { it[Keys.TAX_PERCENTAGE] = tax }
    suspend fun updateLanguage(lang: String) = context.dataStore.edit { it[Keys.LANGUAGE] = lang }
    suspend fun updateReceiptEnabled(enabled: Boolean) = context.dataStore.edit { it[Keys.RECEIPT_ENABLED] = enabled }
    suspend fun updateBarcodeFormats(formats: String) = context.dataStore.edit { it[Keys.BARCODE_FORMATS] = formats }
    suspend fun updateTutorialComplete(complete: Boolean) = context.dataStore.edit { it[Keys.IS_TUTORIAL_COMPLETE] = complete }
    suspend fun updateDefaultBulkScan(enabled: Boolean) = context.dataStore.edit { it[Keys.DEFAULT_BULK_SCAN] = enabled }
    suspend fun updateTrialStart(timestamp: Long) = context.dataStore.edit { it[Keys.TRIAL_START_TIMESTAMP] = timestamp }
    suspend fun addExtraTrialDays(days: Int) = context.dataStore.edit { 
        val current = it[Keys.EXTRA_TRIAL_DAYS] ?: 0
        it[Keys.EXTRA_TRIAL_DAYS] = current + days 
    }
    suspend fun updateLoggedInUsername(username: String?) = context.dataStore.edit {
        if (username == null) {
            it.remove(Keys.LOGGED_IN_USERNAME)
            it.remove(Keys.LAST_LOGIN_TIMESTAMP)
        } else {
            it[Keys.LOGGED_IN_USERNAME] = username
            it[Keys.LAST_LOGIN_TIMESTAMP] = System.currentTimeMillis()
        }
    }

    suspend fun updateLastActivity() = context.dataStore.edit {
        it[Keys.LAST_LOGIN_TIMESTAMP] = System.currentTimeMillis()
    }

    suspend fun clearAllSettings() {
        context.dataStore.edit { it.clear() }
    }
}
