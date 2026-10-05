package com.tataskan.pos.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tataskan.pos.data.entity.User

/**
 * Data Access Object for authentication-related operations.
 */
@Dao
interface AuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username LIMIT 1")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    @Query("UPDATE users SET password = :newPassword WHERE username = :username")
    suspend fun updatePassword(username: String, newPassword: String)

    @Query("UPDATE users SET password = :newPassword, backupPin = :newPin WHERE username = :username")
    suspend fun updatePasswordAndPin(username: String, newPassword: String, newPin: String)

    @Query("SELECT * FROM users LIMIT 1")
    suspend fun getPrimaryUser(): User?

    @Query("UPDATE users SET password = :newPassword, backupPin = :newPin")
    suspend fun updatePrimaryUserPasswordAndPin(newPassword: String, newPin: String)
}
