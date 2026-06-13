package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import com.example.service.LiveTranslatorManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CallTranslatorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TranslationRepository(db.translationDao())

    // Live streams from singleton manager
    val isTranslationActive = LiveTranslatorManager.isTranslationActive
    val currentSessionId = LiveTranslatorManager.currentSessionId
    val currentOriginalText = LiveTranslatorManager.currentOriginalText
    val currentTranslatedText = LiveTranslatorManager.currentTranslatedText
    val isListening = LiveTranslatorManager.isListening
    val currentSpeaker = LiveTranslatorManager.currentSpeaker

    // Internet call streams
    val internetConnectionState = com.example.service.InternetCallManager.connectionState
    val internetRoomCode = com.example.service.InternetCallManager.roomCode
    val internetMyName = com.example.service.InternetCallManager.myName
    val internetPeerName = com.example.service.InternetCallManager.peerName
    val internetIsCallIncoming = com.example.service.InternetCallManager.isCallIncoming
    val internetIsCallActive = com.example.service.InternetCallManager.isCallActive
    val internetCallerName = com.example.service.InternetCallManager.callerName
    val internetActiveCallMessages = com.example.service.InternetCallManager.activeCallMessages
    val internetIsListening = com.example.service.InternetCallManager.isListening
    val internetCurrentOriginalText = com.example.service.InternetCallManager.currentOriginalText
    val internetCurrentTranslatedText = com.example.service.InternetCallManager.currentTranslatedText

    // Room DB streams
    val callHistory: StateFlow<List<CallSession>> = repository.allSessions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val settings: StateFlow<AppSettings> = repository.settings
        .filterNotNull()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _selectedSessionIdForDetails = MutableStateFlow<String?>(null)
    val selectedSessionIdForDetails = _selectedSessionIdForDetails.asStateFlow()

    val detailedMessages: StateFlow<List<TranslationMessage>> = _selectedSessionIdForDetails
        .flatMapLatest { sessionId ->
            if (sessionId == null) flowOf(emptyList())
            else repository.getMessagesForSession(sessionId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSessionMessages: StateFlow<List<TranslationMessage>> = currentSessionId
        .flatMapLatest { sessionId ->
            if (sessionId == null) {
                flowOf(emptyList())
            } else {
                repository.getMessagesForSession(sessionId)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // Initialize the central translation orchestrators
        LiveTranslatorManager.initialize(application, repository)
        com.example.service.InternetCallManager.initialize(application, repository)
    }

    fun startCallTranslation(contact: String, app: String) {
        // Feed settings state to LiveTranslatorManager prior to launching
        val currentSettings = settings.value
        LiveTranslatorManager.clientLang = currentSettings.clientLang
        LiveTranslatorManager.partnerLang = currentSettings.partnerLang
        LiveTranslatorManager.voiceType = currentSettings.voiceType
        LiveTranslatorManager.speechRate = currentSettings.speechRate
        LiveTranslatorManager.speechPitch = currentSettings.speechPitch
        LiveTranslatorManager.localDialectsEnabled = currentSettings.localDialectEnabled
        LiveTranslatorManager.offlineModeEnabled = currentSettings.offlineModeEnabled

        LiveTranslatorManager.startSession(contact, app)
    }

    fun stopCallTranslation() {
        LiveTranslatorManager.stopSession()
    }

    fun toggleSpeaker() {
        LiveTranslatorManager.toggleSpeaker()
    }

    fun setSpeaker(speaker: String) {
        LiveTranslatorManager.setSpeaker(speaker)
    }

    fun toggleMicrophone() {
        if (isListening.value) {
            LiveTranslatorManager.stopListening()
        } else {
            LiveTranslatorManager.startListeningCurrentSpeaker()
        }
    }

    fun simulateSpeech(text: String, speaker: String) {
        LiveTranslatorManager.simulateSpeech(text, speaker)
    }

    fun selectSessionForDetails(sessionId: String?) {
        _selectedSessionIdForDetails.value = sessionId
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            repository.deleteSession(sessionId)
            if (_selectedSessionIdForDetails.value == sessionId) {
                _selectedSessionIdForDetails.value = null
            }
        }
    }

    fun updateSettings(
        clientLang: String? = null,
        partnerLang: String? = null,
        voiceType: String? = null,
        speechRate: Float? = null,
        speechPitch: Float? = null,
        localDialectsEnabled: Boolean? = null,
        offlineModeEnabled: Boolean? = null,
        saveCallHistory: Boolean? = null
    ) {
        viewModelScope.launch {
            val current = settings.value
            val updated = current.copy(
                clientLang = clientLang ?: current.clientLang,
                partnerLang = partnerLang ?: current.partnerLang,
                voiceType = voiceType ?: current.voiceType,
                speechRate = speechRate ?: current.speechRate,
                speechPitch = speechPitch ?: current.speechPitch,
                localDialectEnabled = localDialectsEnabled ?: current.localDialectEnabled,
                offlineModeEnabled = offlineModeEnabled ?: current.offlineModeEnabled,
                saveCallHistory = saveCallHistory ?: current.saveCallHistory
            )
            repository.saveAppSettings(updated)

            // Direct update to active manager parameters
            LiveTranslatorManager.clientLang = updated.clientLang
            LiveTranslatorManager.partnerLang = updated.partnerLang
            LiveTranslatorManager.voiceType = updated.voiceType
            LiveTranslatorManager.speechRate = updated.speechRate
            LiveTranslatorManager.speechPitch = updated.speechPitch
            LiveTranslatorManager.localDialectsEnabled = updated.localDialectEnabled
            LiveTranslatorManager.offlineModeEnabled = updated.offlineModeEnabled
        }
    }

    // Internet Call room controls
    fun connectToInternetRoom(code: String, userName: String) {
        val currentSettings = settings.value
        com.example.service.InternetCallManager.myLanguage = currentSettings.clientLang
        com.example.service.InternetCallManager.partnerLanguage = currentSettings.partnerLang
        com.example.service.InternetCallManager.myVoiceType = currentSettings.voiceType
        com.example.service.InternetCallManager.speakerRate = currentSettings.speechRate
        com.example.service.InternetCallManager.speakerPitch = currentSettings.speechPitch
        
        com.example.service.InternetCallManager.connectToRoom(code, userName)
    }

    fun disconnectFromInternetRoom() {
        com.example.service.InternetCallManager.disconnect()
    }

    fun triggerInternetMic() {
        com.example.service.InternetCallManager.toggleMicrophone()
    }

    fun startInternetCallDial() {
        com.example.service.InternetCallManager.initiateOnlineCall()
    }

    fun acceptInternetCall() {
        com.example.service.InternetCallManager.acceptIncomingCall()
    }

    fun rejectInternetCall() {
        com.example.service.InternetCallManager.rejectIncomingCall()
    }

    override fun onCleared() {
        super.onCleared()
        // Standard cleanup on destroy
        LiveTranslatorManager.stopSession()
        com.example.service.InternetCallManager.disconnect()
    }
}
