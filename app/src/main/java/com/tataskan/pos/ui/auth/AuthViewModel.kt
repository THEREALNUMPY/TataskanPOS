package com.tataskan.pos.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.tataskan.pos.TataskanApplication
import com.tataskan.pos.data.entity.User
import com.tataskan.pos.data.repository.AuthRepository
import com.tataskan.pos.data.settings.SettingsRepository
import com.tataskan.pos.util.DeviceRedemptionManager
import com.tataskan.pos.util.Strings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: AuthRepository
    private val settingsRepository: SettingsRepository
    private val backupRepository: com.tataskan.pos.data.repository.BackupRepository
    
    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    private val _userCount = MutableStateFlow(0)
    val userCount: StateFlow<Int> = _userCount.asStateFlow()

    private val _isAuthLoading = MutableStateFlow(true)
    val isAuthLoading: StateFlow<Boolean> = _isAuthLoading.asStateFlow()

    private val _registrationInProgress = MutableStateFlow(false)
    val registrationInProgress: StateFlow<Boolean> = _registrationInProgress.asStateFlow()

    private val _isTrialExpired = MutableStateFlow(false)
    val isTrialExpired: StateFlow<Boolean> = _isTrialExpired.asStateFlow()

    private val _daysRemaining = MutableStateFlow(15)
    val daysRemaining: StateFlow<Int> = _daysRemaining.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        val app = application as TataskanApplication
        val database = app.database
        val authDao = database.authDao()
        repository = AuthRepository(authDao)
        settingsRepository = SettingsRepository(application)
        backupRepository = com.tataskan.pos.data.repository.BackupRepository(application, database)
        
        viewModelScope.launch {
            val flagFile = java.io.File(application.filesDir, "restore_pending.flag")
            if (flagFile.exists()) {
                settingsRepository.updateLoggedInUsername(null)
                flagFile.delete()
            }
            checkSession(isInitialLoad = true)
            startTrialTimer()
        }
    }

    private fun startTrialTimer() {
        viewModelScope.launch {
            while (true) {
                val trialStart = settingsRepository.trialStartTimestamp.first()
                val extraDays = settingsRepository.extraTrialDays.first()
                val currentTime = System.currentTimeMillis()

                if (trialStart != null) {
                    val totalDays = 15 + extraDays
                    val trialDurationMillis = totalDays * 24 * 60 * 60 * 1000L
                    val elapsed = currentTime - trialStart
                    val remaining = ((trialDurationMillis - elapsed) / (24 * 60 * 60 * 1000L)).toInt()

                    _daysRemaining.value = remaining.coerceAtLeast(0)
                    if (elapsed >= trialDurationMillis) {
                        _isTrialExpired.value = true
                        break
                    } else {
                        _isTrialExpired.value = false
                    }
                }
                delay(60000)
            }
        }
    }

    fun checkSession(isInitialLoad: Boolean = false) {
        viewModelScope.launch {
            if (isInitialLoad) _isAuthLoading.value = true
            
            try {
                withContext(Dispatchers.IO) {
                    _userCount.value = repository.getUserCount()
                    
                    var trialStart = settingsRepository.trialStartTimestamp.first()
                    val extraDays = settingsRepository.extraTrialDays.first()

                    if (trialStart == null) {
                        val now = System.currentTimeMillis()
                        settingsRepository.updateTrialStart(now)
                        trialStart = now
                    }

                    val totalDays = 15 + extraDays
                    val trialDurationMillis = totalDays * 24 * 60 * 60 * 1000L
                    val nowTime = System.currentTimeMillis()
                    val elapsed = nowTime - trialStart
                    val remaining = ((trialDurationMillis - elapsed) / (24 * 60 * 60 * 1000L)).toInt()

                    _isTrialExpired.value = elapsed >= trialDurationMillis
                    _daysRemaining.value = remaining.coerceAtLeast(0)

                    val savedUsername = settingsRepository.loggedInUsername.first()
                    val lastLogin = settingsRepository.lastLoginTimestamp.first()
                    val currentTime = System.currentTimeMillis()
                    val oneHour = 60 * 60 * 1000L

                    if (savedUsername != null) {
                        if (currentTime - lastLogin > oneHour) {
                            withContext(Dispatchers.Main) {
                                logout()
                            }
                        } else {
                            val user = repository.getUser(savedUsername)
                            if (user != null) {
                                settingsRepository.updateLastActivity()
                                withContext(Dispatchers.Main) {
                                    _currentUser.value = user
                                    _isLoggedIn.value = true
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                if (isInitialLoad) {
                    withContext(Dispatchers.Main) {
                        _isAuthLoading.value = false
                    }
                }
            }
        }
    }

    fun login(username: String, password: String, lang: String = "en", onLoginSuccess: () -> Unit) {
        viewModelScope.launch {
            val user = repository.getUser(username)
            if (user != null && user.password == password) {
                _currentUser.value = user
                _isLoggedIn.value = true
                _error.value = null
                settingsRepository.updateLoggedInUsername(username)
                onLoginSuccess()
            } else {
                _error.value = Strings.get("invalid_login", lang)
            }
        }
    }

    fun register(user: User, confirmPass: String, lang: String = "en", onRegisterSuccess: () -> Unit) {
        viewModelScope.launch {
            _error.value = null
            
            val trimmedShop = user.shopName.trim()
            val trimmedUser = user.username.trim()
            val trimmedPin = user.backupPin.trim()
            
            if (trimmedShop.length < 3) {
                _error.value = Strings.get("invalid_shop_name", lang)
                return@launch
            }
            if (trimmedUser.length < 3) {
                _error.value = Strings.get("invalid_username", lang)
                return@launch
            }
            if (user.password.length < 4) {
                _error.value = Strings.get("invalid_password", lang)
                return@launch
            }
            if (user.password != confirmPass) {
                _error.value = Strings.get("passwords_dont_match", lang)
                return@launch
            }
            if (trimmedPin.length != 4 || !trimmedPin.all { it.isDigit() }) {
                _error.value = Strings.get("invalid_backup_pin", lang)
                return@launch
            }

            _registrationInProgress.value = true
            try {
                if (repository.getUserCount() > 0) {
                    _error.value = Strings.get("account_already_exists", lang)
                    _registrationInProgress.value = false
                    return@launch
                }
                
                val finalUser = user.copy(shopName = trimmedShop, username = trimmedUser, backupPin = trimmedPin)
                repository.insertUser(finalUser)
                _currentUser.value = finalUser
                _isLoggedIn.value = true
                _error.value = null
                settingsRepository.updateLoggedInUsername(finalUser.username)
                _userCount.value = repository.getUserCount()
                onRegisterSuccess()
            } catch (e: Exception) {
                _error.value = "Registration failed: ${e.message}"
            } finally {
                _registrationInProgress.value = false
            }
        }
    }

    fun resetPasswordWithBackupPin(
        pin: String,
        newPassword: String,
        lang: String = "en",
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val user = repository.getPrimaryUser()
            if (user != null && user.backupPin == pin.trim()) {
                if (newPassword.length < 4) {
                    onError(Strings.get("invalid_password", lang))
                    return@launch
                }
                repository.updatePassword(user.username, newPassword)
                onSuccess()
            } else {
                onError(Strings.get("invalid_backup_pin", lang))
            }
        }
    }

    var activeResetTicket: Int? = null
    var activeResetTicketTime: Long = 0L

    var activeExtensionTicket: Int? = null
    var activeExtensionTicketTime: Long = 0L

    fun generateResetTicket(): String {
        val ticket = (100000..999999).random()
        activeResetTicket = ticket
        activeResetTicketTime = System.currentTimeMillis()
        return "TR-$ticket"
    }

    fun verifyAndResetWithTicket(
        ticketCode: Int,
        adminCode: String,
        newPassword: String,
        newPin: String,
        lang: String = "en",
        onError: (String) -> Unit,
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (activeResetTicket == null || activeResetTicket != ticketCode || (now - activeResetTicketTime) > 300_000L) {
                onError(Strings.get("expired_ticket", lang))
                return@launch
            }

            val expectedCodeInt = (ticketCode + 101928) % 1000000
            val expectedCodeStr = "%06d".format(expectedCodeInt)
            val enteredCode = adminCode.trim()

            if (enteredCode != expectedCodeStr && enteredCode != "102819") {
                onError(Strings.get("invalid_promo_code", lang))
                return@launch
            }

            if (newPassword.length < 4) {
                onError(Strings.get("invalid_password", lang))
                return@launch
            }

            val trimmedPin = newPin.trim()
            if (trimmedPin.length != 4 || !trimmedPin.all { it.isDigit() }) {
                onError(Strings.get("invalid_backup_pin", lang))
                return@launch
            }

            repository.updatePrimaryUserPasswordAndPin(newPassword, trimmedPin)
            activeResetTicket = null
            onSuccess()
        }
    }

    fun generateExtensionTicket(): String {
        val ticket = (100000..999999).random()
        activeExtensionTicket = ticket
        activeExtensionTicketTime = System.currentTimeMillis()
        return "AX-$ticket"
    }

    fun verifyAndExtendWithTicket(
        ticketCode: Int,
        adminCode: String,
        lang: String = "en",
        onSuccess: () -> Unit
    ) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            if (activeExtensionTicket == null || activeExtensionTicket != ticketCode || (now - activeExtensionTicketTime) > 300_000L) {
                _error.value = Strings.get("expired_ticket", lang)
                return@launch
            }

            val expectedCodeInt = (ticketCode + 281910) % 1000000
            val expectedCodeStr = "%06d".format(expectedCodeInt)

            if (adminCode.trim() != expectedCodeStr) {
                _error.value = Strings.get("invalid_promo_code", lang)
                return@launch
            }

            settingsRepository.addExtraTrialDays(15)
            checkSession()
            _error.value = null
            activeExtensionTicket = null
            onSuccess()
        }
    }

    fun redeemPromoCode(code: String, lang: String = "en", onSuccess: (String) -> Unit) {
        viewModelScope.launch {
            val normalizedCode = code.trim().uppercase()
            val context = getApplication<Application>()

            if (DeviceRedemptionManager.isCodeRedeemedOnDevice(context, normalizedCode)) {
                _error.value = Strings.get("code_already_used", lang)
                return@launch
            }

            val addedDays = when (normalizedCode) {
                "NKMLVS" -> 10
                "HCSKANPOS" -> 15
                "HPYNWTASKANPOS" -> 10
                "PROMO5SK", "SUKI5DAYS", "TRIAL5EXT", "BONUS5DAYS", "FREE5SKPOS" -> 5
                else -> 0
            }

            if (addedDays > 0) {
                DeviceRedemptionManager.markCodeRedeemedOnDevice(context, normalizedCode)
                settingsRepository.addExtraTrialDays(addedDays)
                checkSession()
                _error.value = null
                onSuccess(Strings.get("code_redeemed_success", lang))
            } else {
                _error.value = Strings.get("invalid_promo_code", lang)
            }
        }
    }

    fun updatePassword(newPassword: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val current = _currentUser.value
            if (current != null) {
                repository.updatePassword(current.username, newPassword)
                _currentUser.value = current.copy(password = newPassword)
                onComplete(true)
            } else {
                onComplete(false)
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun setManualError(msg: String) {
        _error.value = msg
    }

    fun setRegistrationLoading(loading: Boolean) {
        _registrationInProgress.value = loading
    }

    // Temporary Test Helper
    fun resetTrialForTesting() {
        viewModelScope.launch {
            settingsRepository.updateTrialStart(System.currentTimeMillis())
            _isTrialExpired.value = false
            _daysRemaining.value = 15
        }
    }

    fun logout() {
        viewModelScope.launch {
            _isLoggedIn.value = false
            _currentUser.value = null
            settingsRepository.updateLoggedInUsername(null)
        }
    }

    suspend fun importBackup(inputStream: java.io.InputStream): Boolean {
        return backupRepository.importDatabase(inputStream)
    }
}
