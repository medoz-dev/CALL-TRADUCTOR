package com.example.service

import android.content.Context
import android.util.Log
import com.example.api.GeminiClient
import com.example.data.TranslationMessage
import com.example.data.TranslationRepository
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

object InternetCallManager {
    private const val TAG = "InternetCallManager"
    private const val NTFY_BASE_URL = "https://ntfy.sh"
    private const val NTFY_WS_URL = "wss://ntfy.sh"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val jsonAdapter = moshi.adapter(CallMessagePayload::class.java)

    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // For WebSockets
        .connectTimeout(15, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    // Application state observables
    private val _connectionState = MutableStateFlow<ConnectionStatus>(ConnectionStatus.DISCONNECTED)
    val connectionState = _connectionState.asStateFlow()

    private val _roomCode = MutableStateFlow("")
    val roomCode = _roomCode.asStateFlow()

    private val _myName = MutableStateFlow("Anonyme")
    val myName = _myName.asStateFlow()

    private val _peerName = MutableStateFlow<String?>(null)
    val peerName = _peerName.asStateFlow()

    private val _isCallIncoming = MutableStateFlow(false)
    val isCallIncoming = _isCallIncoming.asStateFlow()

    private val _isCallActive = MutableStateFlow(false)
    val isCallActive = _isCallActive.asStateFlow()

    private val _callerName = MutableStateFlow("")
    val callerName = _callerName.asStateFlow()

    private val _activeCallMessages = MutableStateFlow<List<TranslationMessage>>(emptyList())
    val activeCallMessages = _activeCallMessages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _currentOriginalText = MutableStateFlow("")
    val currentOriginalText = _currentOriginalText.asStateFlow()

    private val _currentTranslatedText = MutableStateFlow("")
    val currentTranslatedText = _currentTranslatedText.asStateFlow()

    private var voiceManager: VoiceManager? = null
    private var repository: TranslationRepository? = null
    private var appContext: Context? = null

    // Calling session languages
    var myLanguage = "fr"
    var partnerLanguage = "en"
    var myVoiceType = "MALE"
    var speakerRate = 1.0f
    var speakerPitch = 1.0f

    enum class ConnectionStatus {
        DISCONNECTED, CONNECTING, CONNECTED, ERROR
    }

    fun initialize(context: Context, repo: TranslationRepository) {
        appContext = context.applicationContext
        repository = repo
        voiceManager = VoiceManager(
            context = context.applicationContext,
            onSpeechResult = { text ->
                handleIncomingSTT(text)
            },
            onSpeechPartialResult = { partial ->
                _currentOriginalText.value = partial
            },
            onSpeechStateChange = { listening ->
                _isListening.value = listening
            }
        )
    }

    fun connectToRoom(code: String, userName: String) {
        if (code.isBlank() || userName.isBlank()) return
        
        disconnect()

        _roomCode.value = code.trim().lowercase()
        _myName.value = userName.trim()
        _connectionState.value = ConnectionStatus.CONNECTING
        _activeCallMessages.value = emptyList()

        val cleanCode = _roomCode.value
        val wsUrl = "$NTFY_WS_URL/voxbridge_callroom_$cleanCode/ws?json=1"

        Log.d(TAG, "Connecting to channel: $wsUrl")

        val request = Request.Builder()
            .url(wsUrl)
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "WebSocket connected successfully to ntfy.sh")
                _connectionState.value = ConnectionStatus.CONNECTED
                
                // Announce our presence to the room
                sendPingPresence()
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "WS Message Received: $text")
                handleRawJsonEnvelope(text)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "WS Connection Failure: ${t.message}", t)
                _connectionState.value = ConnectionStatus.ERROR
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "WS Closed: Code=$code, Reason=$reason")
                _connectionState.value = ConnectionStatus.DISCONNECTED
            }
        })
    }

    fun disconnect() {
        stopListening()
        sendHangupSignal()
        webSocket?.close(1000, "User logout")
        webSocket = null
        _connectionState.value = ConnectionStatus.DISCONNECTED
        _isCallActive.value = false
        _isCallIncoming.value = false
        _currentOriginalText.value = ""
        _currentTranslatedText.value = ""
    }

    fun toggleMicrophone() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        voiceManager?.startListening(myLanguage, preferOffline = false)
        _isListening.value = true
    }

    fun stopListening() {
        voiceManager?.stopListening()
        _isListening.value = false
    }

    /**
     * Triggered when speech recognition yields a result.
     * Generates a high quality translation using Gemini, then publishes it.
     */
    private fun handleIncomingSTT(text: String) {
        _currentOriginalText.value = text
        _currentTranslatedText.value = "Traduction en cours..."

        coroutineScope.launch {
            val translated = withContext(Dispatchers.IO) {
                if (myLanguage.lowercase().trim() == partnerLanguage.lowercase().trim()) {
                    text
                } else {
                    GeminiClient.translate(
                        text = text,
                        sourceLang = myLanguage,
                        targetLang = partnerLanguage,
                        localDialectsEnabled = true,
                        offlineModeEnabled = false
                    )
                }
            }

            _currentTranslatedText.value = translated

            // Publish this speech event to other peers in the room
            sendSpeechEvent(text, translated)

            // Add to active chat visually
            addLocalMessage(text, translated, isMe = true)
        }
    }

    private fun addLocalMessage(originalText: String, translatedText: String, isMe: Boolean) {
        val message = TranslationMessage(
            id = System.currentTimeMillis(),
            sessionId = "online_call_" + _roomCode.value,
            textOriginal = originalText,
            textTranslated = translatedText,
            sourceLanguage = if (isMe) myLanguage else partnerLanguage,
            targetLanguage = if (isMe) partnerLanguage else myLanguage,
            speakerType = if (isMe) "ME" else "INTERLOCUTEUR",
            timestamp = System.currentTimeMillis()
        )
        _activeCallMessages.value = _activeCallMessages.value + message
    }

    // --- Signals and Web Protocol ---

    private fun handleRawJsonEnvelope(envelopeJson: String) {
        try {
            // ntfy.sh envelopes messages inside a json line
            // format: { "id": "...", "event": "message", "topic": "...", "message": "our string" }
            val rootMoshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
            val mapAdapter = rootMoshi.adapter(Map::class.java)
            val envelope = mapAdapter.fromJson(envelopeJson) ?: return
            
            if (envelope["event"] == "message") {
                val payloadStr = envelope["message"] as? String ?: return
                val payload = jsonAdapter.fromJson(payloadStr) ?: return
                
                // Filter messages originating from ourselves
                if (payload.senderName == _myName.value) return

                Log.d(TAG, "Processed valid payload: ${payload.type}")
                coroutineScope.launch(Dispatchers.Main) {
                    processIncomingPayload(payload)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Envelope parse failure: ${e.message}")
        }
    }

    private fun processIncomingPayload(payload: CallMessagePayload) {
        when (payload.type) {
            "PING" -> {
                _peerName.value = payload.senderName
                // Reply with our presence so they know we are also here
                coroutineScope.launch(Dispatchers.IO) {
                    sendPingResponse()
                }
            }
            "RING" -> {
                if (!_isCallActive.value) {
                    _isCallIncoming.value = true
                    _callerName.value = payload.senderName
                    _peerName.value = payload.senderName
                    Log.d(TAG, "Incoming call ring from ${payload.senderName}")
                }
            }
            "ACCEPT" -> {
                _isCallActive.value = true
                _isCallIncoming.value = false
                _peerName.value = payload.senderName
                Log.d(TAG, "Call accepted by ${payload.senderName}")
            }
            "HANGUP" -> {
                _isCallActive.value = false
                _isCallIncoming.value = false
                _peerName.value = null
                Log.d(TAG, "Call hung up by partner")
            }
            "SPEECH" -> {
                // We got speech from the other side!
                val originalText = payload.originalText ?: ""
                val translatedText = payload.translatedText ?: ""
                
                Log.d(TAG, "SPEECH Received. Original: $originalText, Translated: $translatedText")

                // Update UI text outputs
                _currentOriginalText.value = originalText
                _currentTranslatedText.value = translatedText

                addLocalMessage(originalText, translatedText, isMe = false)

                // AUTOMATIC BACKGROUND TRANSLATED TTS ON EARPHONE/SPEAKER!
                // We read the translation aloud in our local language!
                voiceManager?.speak(
                    text = translatedText,
                    langCode = myLanguage, // We want to hear the translation in our own language!
                    voiceGender = payload.preferredVoice ?: "FEMALE",
                    speechRate = payload.speechRate,
                    pitch = payload.speechPitch
                )
            }
        }
    }

    // --- Network publishing helper --

    private fun publishPayload(payload: CallMessagePayload) {
        val topic = _roomCode.value
        if (topic.isBlank()) return

        val jsonString = jsonAdapter.toJson(payload)
        val requestBody = jsonString.toRequestBody("text/plain; charset=utf-8".toMediaType())

        val request = Request.Builder()
            .url("$NTFY_BASE_URL/voxbridge_callroom_$topic")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to publish payload: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.close()
            }
        })
    }

    fun initiateOnlineCall() {
        _isCallActive.value = false
        _isCallIncoming.value = false
        _peerName.value = null
        sendCallRingSignal()
    }

    fun acceptIncomingCall() {
        _isCallIncoming.value = false
        _isCallActive.value = true
        sendAcceptSignal()
    }

    fun rejectIncomingCall() {
        _isCallIncoming.value = false
        _isCallActive.value = false
        sendHangupSignal()
    }

    private fun sendPingPresence() {
        publishPayload(CallMessagePayload(type = "PING", senderName = _myName.value))
    }

    private fun sendPingResponse() {
        publishPayload(CallMessagePayload(type = "PING", senderName = _myName.value))
    }

    fun sendCallRingSignal() {
        publishPayload(CallMessagePayload(type = "RING", senderName = _myName.value))
    }

    fun sendAcceptSignal() {
        publishPayload(CallMessagePayload(type = "ACCEPT", senderName = _myName.value))
    }

    fun sendHangupSignal() {
        publishPayload(CallMessagePayload(type = "HANGUP", senderName = _myName.value))
    }

    fun sendSpeechEvent(original: String, translated: String) {
        publishPayload(
            CallMessagePayload(
                type = "SPEECH",
                senderName = _myName.value,
                originalText = original,
                translatedText = translated,
                preferredVoice = myVoiceType,
                speechRate = speakerRate,
                speechPitch = speakerPitch
            )
        )
    }
}

/**
 * Payload model class for real-time room communication
 */
data class CallMessagePayload(
    val type: String,               // "PING", "RING", "ACCEPT", "HANGUP", "SPEECH"
    val senderName: String,
    val originalText: String? = null,
    val translatedText: String? = null,
    val preferredVoice: String? = null,
    val speechRate: Float = 1.0f,
    val speechPitch: Float = 1.0f
)
