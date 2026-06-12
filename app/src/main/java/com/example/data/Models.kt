package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "call_sessions")
data class CallSession(
    @PrimaryKey val id: String, // Typically a UUID string
    val contactName: String,
    val appName: String, // "WhatsApp", "Zoom", "Téléphone", etc.
    val timestamp: Long = System.currentTimeMillis(),
    val durationSeconds: Int = 0,
    val sourceLanguage: String = "fr",
    val targetLanguage: String = "en",
    val keyPhrasesSummary: String? = null
)

@Entity(tableName = "translation_messages")
data class TranslationMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // Foreign reference to call_sessions
    val textOriginal: String,
    val textTranslated: String,
    val sourceLanguage: String,
    val targetLanguage: String,
    val speakerType: String, // "ME" or "INTERLOCUTEUR"
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey val id: Int = 0, // Singleton setting row
    val clientLang: String = "fr", // Primary language of user
    val partnerLang: String = "en", // Target companion language
    val voiceType: String = "FEMALE", // "MALE" or "FEMALE" or "NEUTRAL"
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f,
    val offlineModeEnabled: Boolean = false,
    val localDialectEnabled: Boolean = false, // Fon, Yoruba
    val saveCallHistory: Boolean = true
)
