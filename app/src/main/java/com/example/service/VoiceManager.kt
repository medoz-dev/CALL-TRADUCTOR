package com.example.service

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import java.util.*

class VoiceManager(
    private val context: Context,
    private val onSpeechResult: (String) -> Unit,
    private val onSpeechPartialResult: (String) -> Unit = {},
    private val onSpeechStateChange: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var isTtsInitialized = false
    private var isListening = false
    private var currentTtsLocale = Locale.FRENCH

    init {
        // Initialize TTS
        tts = TextToSpeech(context, this)
        
        // Initialize SpeechRecognizer if available
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(createRecognitionListener())
            }
        } else {
            Log.w("VoiceManager", "Speech recognition not available on this device/emulator.")
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
            tts?.language = Locale.FRENCH
            Log.d("VoiceManager", "TTS Initialized successfully.")
        } else {
            Log.e("VoiceManager", "Failed to initialize TTS.")
        }
    }

    /**
     * Speaks the translated text using TextToSpeech.
     */
    fun speak(text: String, langCode: String, voiceGender: String = "FEMALE", speechRate: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isTtsInitialized) {
            Log.w("VoiceManager", "TTS not initialized yet.")
            return
        }

        val locale = getLocaleFromCode(langCode)
        currentTtsLocale = locale
        tts?.language = locale
        tts?.setSpeechRate(speechRate)
        tts?.setPitch(pitch)

        // Try to filter voice based on gender
        try {
            val voices = tts?.voices
            if (voices != null && voices.isNotEmpty()) {
                val matchedVoice = voices.firstOrNull { voice ->
                    val name = voice.name.lowercase()
                    val matchesLang = voice.locale.language.equals(locale.language, ignoreCase = true)
                    val matchesGender = if (voiceGender.uppercase() == "FEMALE") {
                        name.contains("female") || name.contains("fem") || name.contains("f-")
                    } else {
                        name.contains("male") || name.contains("masc") || name.contains("m-")
                    }
                    matchesLang && matchesGender
                } ?: voices.firstOrNull { it.locale.language.equals(locale.language, ignoreCase = true) }

                if (matchedVoice != null) {
                    tts?.voice = matchedVoice
                }
            }
        } catch (e: Exception) {
            Log.e("VoiceManager", "Voice selection failed: ${e.message}")
        }

        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "VoxBridge_SpeechID_${System.currentTimeMillis()}")
    }

    /**
     * Starts native audio recording and transcribing for the given language.
     */
    fun startListening(langCode: String, preferOffline: Boolean = false) {
        if (speechRecognizer == null) {
            Log.e("VoiceManager", "SpeechRecognizer not initialized.")
            return
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, langCode)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, langCode)
            putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            if (preferOffline) {
                putExtra("android.speech.extra.PREFER_OFFLINE", true)
            }
        }

        try {
            isListening = true
            onSpeechStateChange(true)
            speechRecognizer?.startListening(intent)
        } catch (e: Exception) {
            Log.e("VoiceManager", "Failed to start listening: ${e.message}")
            isListening = false
            onSpeechStateChange(false)
        }
    }

    /**
     * Stops current voice parsing.
     */
    fun stopListening() {
        speechRecognizer?.stopListening()
        isListening = false
        onSpeechStateChange(false)
    }

    fun destroy() {
        tts?.stop()
        tts?.shutdown()
        speechRecognizer?.destroy()
    }

    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d("VoiceManager", "Ready for speech")
        }

        override fun onBeginningOfSpeech() {
            Log.d("VoiceManager", "Beginning speech")
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Can be used for audio wave visualizations
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            isListening = false
            onSpeechStateChange(false)
        }

        override fun onError(error: Int) {
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions manquantes"
                SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Réseau expiré"
                SpeechRecognizer.ERROR_NO_MATCH -> "Aucune voix détectée"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Détecteur occupé"
                SpeechRecognizer.ERROR_SERVER -> "Erreur serveur de reconnaissance"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Expiration de silence"
                else -> "Erreur inconnue ($error)"
            }
            Log.w("VoiceManager", "Speech Recognition Error: $errorMessage")
            isListening = false
            onSpeechStateChange(false)
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onSpeechResult(text)
            }
            isListening = false
            onSpeechStateChange(false)
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val text = matches?.firstOrNull()
            if (!text.isNullOrBlank()) {
                onSpeechPartialResult(text)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun getLocaleFromCode(code: String): Locale {
        return when (code.lowercase()) {
            "fr" -> Locale.FRENCH
            "en" -> Locale.US
            "es" -> Locale("es", "ES")
            "de" -> Locale.GERMAN
            "it" -> Locale.ITALIAN
            "pt" -> Locale("pt", "PT")
            "fon" -> Locale.FRENCH // Fallback for TTS synthesized voices
            "yo" -> Locale.US // Fallback
            "wo" -> Locale.FRENCH // Fallback
            else -> Locale.getDefault()
        }
    }
}
