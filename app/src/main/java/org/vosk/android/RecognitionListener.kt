package org.vosk.android

interface RecognitionListener {
    fun onResult(hypothesis: String)
    fun onPartialResult(hypothesis: String)
    fun onFinalResult(hypothesis: String)
    fun onError(exception: Exception)
    fun onTimeout()
}
