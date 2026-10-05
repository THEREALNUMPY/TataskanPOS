package com.tataskan.pos.data.repository

import com.tataskan.pos.data.AuthDao
import com.tataskan.pos.data.entity.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Repository that handles authentication and user data operations.
 */
class AuthRepository(private val authDao: AuthDao) {

    suspend fun insertUser(user: User) = withContext(Dispatchers.Default) {
        authDao.insertUser(user)
    }

    suspend fun getUser(username: String): User? = withContext(Dispatchers.Default) {
        authDao.getUserByUsername(username)
    }

    suspend fun getUserCount(): Int = withContext(Dispatchers.Default) {
        authDao.getUserCount()
    }

    suspend fun updatePassword(username: String, newPassword: String) = withContext(Dispatchers.Default) {
        authDao.updatePassword(username, newPassword)
    }

    suspend fun updatePasswordAndPin(username: String, newPassword: String, newPin: String) = withContext(Dispatchers.Default) {
        authDao.updatePasswordAndPin(username, newPassword, newPin)
    }

    suspend fun getPrimaryUser(): User? = withContext(Dispatchers.Default) {
        authDao.getPrimaryUser()
    }

    suspend fun updatePrimaryUserPasswordAndPin(newPassword: String, newPin: String) = withContext(Dispatchers.Default) {
        authDao.updatePrimaryUserPasswordAndPin(newPassword, newPin)
    }
}
