package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class TranslationRepository(private val dao: TranslationDao) {

    val allSessions: Flow<List<CallSession>> = dao.getAllSessions()
        .flowOn(Dispatchers.IO)

    val settings: Flow<AppSettings?> = dao.getAppSettingsFlow()
        .flowOn(Dispatchers.IO)

    fun getMessagesForSession(sessionId: String): Flow<List<TranslationMessage>> =
        dao.getMessagesForSession(sessionId)
            .flowOn(Dispatchers.IO)

    suspend fun getMessagesForSessionSync(sessionId: String): List<TranslationMessage> = withContext(Dispatchers.IO) {
        dao.getMessagesForSessionSync(sessionId)
    }

    suspend fun getSessionSync(sessionId: String): CallSession? = withContext(Dispatchers.IO) {
        dao.getSessionSync(sessionId)
    }

    suspend fun insertSession(session: CallSession) = withContext(Dispatchers.IO) {
        dao.insertSession(session)
    }

    suspend fun insertMessage(message: TranslationMessage) = withContext(Dispatchers.IO) {
        dao.insertMessage(message)
    }

    suspend fun deleteSession(sessionId: String) = withContext(Dispatchers.IO) {
        dao.deleteMessagesForSession(sessionId)
        dao.deleteSessionById(sessionId)
    }

    suspend fun getAppSettings(): AppSettings = withContext(Dispatchers.IO) {
        dao.getAppSettings() ?: AppSettings().also {
            dao.saveAppSettings(it)
        }
    }

    suspend fun saveAppSettings(settings: AppSettings) = withContext(Dispatchers.IO) {
        dao.saveAppSettings(settings)
    }
}
