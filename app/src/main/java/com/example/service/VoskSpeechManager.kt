package com.example.service

import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import org.json.JSONObject
import java.io.IOException

class VoskSpeechManager(
    private val context: Context,
    private val onTranscriptResult: (String) -> Unit = {},
    private val onPartialResult: (String) -> Unit = {}
) : RecognitionListener {

    private var model: Model? = null
    private var speechService: SpeechService? = null
    
    private val _isInitializing = MutableStateFlow(false)
    val isInitializing: StateFlow<Boolean> = _isInitializing

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _transcriptFlow = MutableStateFlow("")
    val transcriptFlow: StateFlow<String> = _transcriptFlow

    private val _errorFlow = MutableStateFlow<String?>(null)
    val errorFlow: StateFlow<String?> = _errorFlow

    init {
        initVoskModel()
    }

    /**
     * Initializes the Vosk model from embedded assets asynchronously
     */
    fun initVoskModel() {
        if (model != null) return
        
        _isInitializing.value = true
        Log.i("VoskSpeechManager", "Unpacking local Vosk offline language model assets...")
        
        StorageService.unpack(
            context,
            "vosk-model-small-fr",
            "model",
            { modelPath ->
                try {
                    model = Model(modelPath)
                    _isInitializing.value = false
                    _errorFlow.value = null
                    Log.i("VoskSpeechManager", "Vosk offline model initialized successfully from: $modelPath")
                } catch (e: Exception) {
                    _isInitializing.value = false
                    _errorFlow.value = "Failed to load unpacked Vosk model: ${e.message}"
                    Log.e("VoskSpeechManager", "Failed to load model", e)
                }
            },
            { error ->
                _isInitializing.value = false
                _errorFlow.value = "Failed to unpack offline Vosk model: ${error.message}"
                Log.e("VoskSpeechManager", "Failed to unpack asset model", error)
            }
        )
    }

    /**
     * Starts listening to audio streaming from the device hardware microphone
     */
    fun startListening(): Boolean {
        val currentModel = model
        if (currentModel == null) {
            Log.w("VoskSpeechManager", "Cannot start listening: Model is not initialized yet.")
            initVoskModel()
            return false
        }

        try {
            if (speechService != null) {
                speechService?.stop()
                speechService = null
            }

            val recognizer = Recognizer(currentModel, 16000f)
            speechService = SpeechService(recognizer, 16000f)
            
            val isSuccess = speechService?.startListening(this) ?: false
            if (isSuccess) {
                _isListening.value = true
                _errorFlow.value = null
                Log.i("VoskSpeechManager", "Vosk microphone stream listening started.")
            }
            return isSuccess
        } catch (e: Exception) {
            _errorFlow.value = "Failed to start speech service: ${e.message}"
            Log.e("VoskSpeechManager", "Error starting listening", e)
            return false
        }
    }

    /**
     * Stops listening to the microphone audio capture stream
     */
    fun stopListening() {
        speechService?.let {
            it.stop()
            speechService = null
        }
        _isListening.value = false
        Log.i("VoskSpeechManager", "Vosk microphone stream listening stopped.")
    }

    /**
     * Cancels current active microphone recognition session
     */
    fun cancelListening() {
        speechService?.let {
            it.cancel()
            speechService = null
        }
        _isListening.value = false
    }

    /**
     * Cleans up running resources
     */
    fun destroy() {
        speechService?.let {
            it.shutdown()
            speechService = null
        }
    }

    // Helper to parse text out of JSON formats: {"text" : "bonjour"} or {"partial" : "bon"}
    private fun extractTextFromJson(json: String, key: String): String {
        return try {
            val jsonObject = JSONObject(json)
            jsonObject.optString(key, "")
        } catch (e: Exception) {
            ""
        }
    }

    // --- Vosk RecognitionListener interface overrides ---

    override fun onResult(hypothesis: String) {
        val recognizedText = extractTextFromJson(hypothesis, "text")
        if (recognizedText.isNotBlank()) {
            _transcriptFlow.value = recognizedText
            onTranscriptResult(recognizedText)
            Log.d("VoskSpeechManager", "Recognized full result: $recognizedText")
        }
    }

    override fun onPartialResult(hypothesis: String) {
        val partialText = extractTextFromJson(hypothesis, "partial")
        if (partialText.isNotBlank()) {
            onPartialResult(partialText)
        }
    }

    override fun onFinalResult(hypothesis: String) {
        val finalResultText = extractTextFromJson(hypothesis, "text")
        if (finalResultText.isNotBlank()) {
            _transcriptFlow.value = finalResultText
            onTranscriptResult(finalResultText)
            Log.d("VoskSpeechManager", "Recognized final result: $finalResultText")
        }
    }

    override fun onError(exception: Exception) {
        _errorFlow.value = "Vosk stream error: ${exception.message}"
        Log.e("VoskSpeechManager", "Vosk callback error", exception)
    }

    override fun onTimeout() {
        Log.w("VoskSpeechManager", "Vosk input listening session timed out.")
    }
}
