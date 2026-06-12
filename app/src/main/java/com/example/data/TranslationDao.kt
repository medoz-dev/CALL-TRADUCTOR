package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TranslationDao {
    @Query("SELECT * FROM call_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<CallSession>>

    @Query("SELECT * FROM call_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionSync(sessionId: String): CallSession?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: CallSession)

    @Query("SELECT * FROM translation_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<TranslationMessage>>

    @Query("SELECT * FROM translation_messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionSync(sessionId: String): List<TranslationMessage>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: TranslationMessage)

    @Query("DELETE FROM call_sessions WHERE id = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM translation_messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("SELECT * FROM app_settings WHERE id = 0")
    suspend fun getAppSettings(): AppSettings?

    @Query("SELECT * FROM app_settings WHERE id = 0")
    fun getAppSettingsFlow(): Flow<AppSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAppSettings(settings: AppSettings)
}
