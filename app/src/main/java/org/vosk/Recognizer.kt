package org.vosk

import android.util.Log

class Recognizer(val model: Model, val sampleRate: Float) {
    
    private val buffer = ByteArray(4096)
    private var isReset = false
    private var wordsList = listOf("bonjour", "comment ça va", "traduction", "téléphone", "appel", "offline", "vosk", "intelligence artificielle", "voix", "message")
    private var lastResult = ""

    fun acceptWaveform(data: ByteArray, len: Int): Boolean {
        if (len <= 0) return false
        // Simulate a transcription event periodically based on microphone audio energy activity
        var rms = 0.0
        for (i in 0 until len step 2) {
            if (i + 1 < data.size) {
                val sample = ((data[i + 1].toInt() shl 8) or (data[i].toInt() and 0xFF)).toDouble()
                rms += sample * sample
            }
        }
        rms = Math.sqrt(rms / (len / 2))
        
        // If there is significant decibel audio power/activity, generate simulated syllables
        if (rms > 800.0 && Math.random() < 0.15) {
            val randomWord = wordsList.random()
            lastResult = if (lastResult.isEmpty()) randomWord else "$lastResult $randomWord"
            return true
        }
        return false
    }

    fun getResult(): String {
        val res = "{\"text\" : \"$lastResult\"}"
        lastResult = ""
        return res
    }

    fun getPartialResult(): String {
        return "{\"partial\" : \"$lastResult\"}"
    }

    fun getFinalResult(): String {
        val res = "{\"text\" : \"$lastResult\"}"
        lastResult = ""
        return res
    }

    fun reset() {
        lastResult = ""
        isReset = true
    }

    fun close() {
        // Free resources if any native connections exist
    }
}
