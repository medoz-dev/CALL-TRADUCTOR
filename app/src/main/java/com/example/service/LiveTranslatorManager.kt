package com.example.service

import android.content.Context
import android.util.Log
import com.example.api.GeminiClient
import com.example.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

object LiveTranslatorManager {
    private const val TAG = "LiveTranslatorManager"

    private val _isTranslationActive = MutableStateFlow(false)
    val isTranslationActive = _isTranslationActive.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId = _currentSessionId.asStateFlow()

    private val _currentOriginalText = MutableStateFlow("")
    val currentOriginalText = _currentOriginalText.asStateFlow()

    private val _currentTranslatedText = MutableStateFlow("")
    val currentTranslatedText = _currentTranslatedText.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _currentSpeaker = MutableStateFlow("ME") // "ME" or "INTERLOCUTEUR"
    val currentSpeaker = _currentSpeaker.asStateFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var voiceManager: VoiceManager? = null
    private var voskSpeechManager: VoskSpeechManager? = null
    private var repository: TranslationRepository? = null

    // Session settings
    var clientLang = "fr"
    var partnerLang = "en"
    var voiceType = "FEMALE"
    var speechRate = 1.0f
    var speechPitch = 1.0f
    var localDialectsEnabled = false
    var offlineModeEnabled = false

    fun initialize(context: Context, repo: TranslationRepository) {
        repository = repo
        
        // Sync setting defaults
        coroutineScope.launch {
            val s = repo.getAppSettings()
            clientLang = s.clientLang
            partnerLang = s.partnerLang
            voiceType = s.voiceType
            speechRate = s.speechRate
            speechPitch = s.speechPitch
            localDialectsEnabled = s.localDialectEnabled
            offlineModeEnabled = s.offlineModeEnabled
        }

        voiceManager = VoiceManager(
            context = context,
            onSpeechResult = { text ->
                handleIncomingTranscription(text)
            },
            onSpeechPartialResult = { partial ->
                _currentOriginalText.value = partial
            },
            onSpeechStateChange = { listening ->
                if (!offlineModeEnabled) {
                    _isListening.value = listening
                }
            }
        )

        voskSpeechManager = VoskSpeechManager(
            context = context,
            onTranscriptResult = { text ->
                handleIncomingTranscription(text)
            },
            onPartialResult = { partial ->
                _currentOriginalText.value = partial
            }
        )
    }

    fun startSession(contactName: String, appName: String) {
        val newSessionId = UUID.randomUUID().toString()
        _currentSessionId.value = newSessionId
        _isTranslationActive.value = true
        _currentOriginalText.value = ""
        _currentTranslatedText.value = ""

        coroutineScope.launch {
            repository?.insertSession(
                CallSession(
                    id = newSessionId,
                    contactName = contactName,
                    appName = appName,
                    sourceLanguage = clientLang,
                    targetLanguage = partnerLang,
                    keyPhrasesSummary = "En attente des premiers échanges..."
                )
            )
        }
        
        Log.d(TAG, "Started call translation session: $newSessionId for $contactName via $appName")
    }

    fun stopSession() {
        val sessionId = _currentSessionId.value
        _isTranslationActive.value = false
        stopListening()
        
        if (sessionId != null && repository != null) {
            coroutineScope.launch {
                val existingSession = repository?.getSessionSync(sessionId)
                if (existingSession != null) {
                    val messages = repository?.getMessagesForSessionSync(sessionId) ?: emptyList()
                    val durationSecs = ((System.currentTimeMillis() - existingSession.timestamp) / 1000).toInt()
                    
                    // Generate AI-powered summary using Gemini Client with local/offline fallback
                    val summary = GeminiClient.generateSessionSummary(messages)
                    
                    repository?.insertSession(
                        existingSession.copy(
                            durationSeconds = durationSecs,
                            sourceLanguage = clientLang,
                            targetLanguage = partnerLang,
                            keyPhrasesSummary = summary
                        )
                    )
                }
            }
        }
        
        _currentSessionId.value = null
        _currentOriginalText.value = ""
        _currentTranslatedText.value = ""
    }

    fun toggleSpeaker() {
        val nextSpeaker = if (_currentSpeaker.value == "ME") "INTERLOCUTEUR" else "ME"
        _currentSpeaker.value = nextSpeaker
        _currentOriginalText.value = ""
        _currentTranslatedText.value = ""
        
        // If we are actively listening, restart listing in the other language
        if (_isTranslationActive.value) {
            stopListening()
            startListeningCurrentSpeaker()
        }
    }

    fun setSpeaker(speaker: String) {
        if (speaker == _currentSpeaker.value) return
        _currentSpeaker.value = speaker
        _currentOriginalText.value = ""
        _currentTranslatedText.value = ""
        
        if (_isTranslationActive.value) {
            stopListening()
            startListeningCurrentSpeaker()
        }
    }

    fun startListeningCurrentSpeaker() {
        if (offlineModeEnabled) {
            val success = voskSpeechManager?.startListening() ?: false
            if (success) {
                _isListening.value = true
            }
        } else {
            val listeningLanguage = if (_currentSpeaker.value == "ME") clientLang else partnerLang
            voiceManager?.startListening(listeningLanguage, preferOffline = false)
        }
    }

    fun stopListening() {
        if (offlineModeEnabled) {
            voskSpeechManager?.stopListening()
            _isListening.value = false
        } else {
            voiceManager?.stopListening()
        }
    }

    private fun handleIncomingTranscription(text: String) {
        _currentOriginalText.value = text
        _currentTranslatedText.value = "Traduction en cours..."

        val speaker = _currentSpeaker.value
        val source = if (speaker == "ME") clientLang else partnerLang
        val target = if (speaker == "ME") partnerLang else clientLang

        coroutineScope.launch {
            // Call Gemini API for high quality dialect aware translation
            val translated = withContext(Dispatchers.IO) {
                GeminiClient.translate(
                    text = text,
                    sourceLang = source,
                    targetLang = target,
                    localDialectsEnabled = localDialectsEnabled,
                    offlineModeEnabled = offlineModeEnabled
                )
            }

            _currentTranslatedText.value = translated

            // Speak the translation out loud
            // If speaker is ME, we speak the translated text in partner's language
            // If speaker is OTHER, we speak the translated text in our (client's) language
            val speechLanguageCode = if (speaker == "ME") partnerLang else clientLang
            voiceManager?.speak(
                text = translated,
                langCode = speechLanguageCode,
                voiceGender = voiceType,
                speechRate = speechRate,
                pitch = speechPitch
            )

            // Persist the message in history
            val sessionId = _currentSessionId.value
            if (sessionId != null && repository != null) {
                repository!!.insertMessage(
                    TranslationMessage(
                        sessionId = sessionId,
                        textOriginal = text,
                        textTranslated = translated,
                        sourceLanguage = source,
                        targetLanguage = target,
                        speakerType = speaker
                    )
                )
            }
        }
    }

    /**
     * Simulation manual input: Lets user type or select text triggers directly
     * useful for testing on emulators, in bed or during silent mode.
     */
    fun simulateSpeech(text: String, speaker: String) {
        setSpeaker(speaker)
        _currentOriginalText.value = text
        handleIncomingTranscription(text)
    }

    fun cleanUp() {
        voiceManager?.destroy()
        voskSpeechManager?.destroy()
    }
}
