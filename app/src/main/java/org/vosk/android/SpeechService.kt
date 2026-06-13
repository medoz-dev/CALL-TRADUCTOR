package org.vosk.android

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

class SpeechService(
    private val recognizer: Recognizer,
    private val sampleRate: Float = 16000f
) {
    private val isListening = AtomicBoolean(false)
    private var recordingThread: Thread? = null
    private var listener: RecognitionListener? = null
    private var audioRecord: AudioRecord? = null

    @SuppressLint("MissingPermission")
    fun startListening(listener: RecognitionListener): Boolean {
        if (isListening.get()) {
            return false
        }
        this.listener = listener
        isListening.set(true)
        Log.d("VoskSpeechService", "Starting offline mic capture stream at ${sampleRate}Hz...")

        // Spin up AudioRecord thread to read raw microphone hardware buffers
        recordingThread = thread(start = true, name = "vosk-audio-capture") {
            val minBufSize = AudioRecord.getMinBufferSize(
                sampleRate.toInt(),
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            
            val finalBufSize = Math.max(minBufSize, 4096)
            
            try {
                audioRecord = AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate.toInt(),
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    finalBufSize
                )

                if (audioRecord?.state != AudioRecord.STATE_INITIALIZED) {
                    throw IllegalStateException("Failed to initialize AudioRecord.")
                }

                audioRecord?.startRecording()
                val buffer = ByteArray(2048)

                while (isListening.get()) {
                    val readBytes = audioRecord?.read(buffer, 0, buffer.size) ?: -1
                    if (readBytes > 0) {
                        val isSilenceBreak = recognizer.acceptWaveform(buffer, readBytes)
                        if (isSilenceBreak) {
                            val fullResult = recognizer.getResult()
                            thread {
                                this.listener?.onResult(fullResult)
                            }
                        } else {
                            val partialResult = recognizer.getPartialResult()
                            thread {
                                this.listener?.onPartialResult(partialResult)
                            }
                        }
                    } else if (readBytes < 0) {
                        Log.e("VoskSpeechService", "AudioRecord read error: $readBytes")
                        break
                    }
                }
            } catch (e: Exception) {
                Log.e("VoskSpeechService", "Exception in AudioRecord stream loop", e)
                this.listener?.onError(e)
            } finally {
                cleanupAudio()
            }
        }
        return true
    }

    fun stop(): Boolean {
        if (!isListening.get()) return false
        isListening.set(false)
        
        val finalResult = recognizer.getFinalResult()
        listener?.onFinalResult(finalResult)
        
        recordingThread?.join(500)
        return true
    }

    fun cancel(): Boolean {
        if (!isListening.get()) return false
        isListening.set(false)
        recordingThread?.join(500)
        return true
    }

    fun shutdown() {
        cancel()
        cleanupAudio()
    }

    private fun cleanupAudio() {
        try {
            if (audioRecord?.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                audioRecord?.stop()
            }
        } catch (e: Exception) {
            Log.e("VoskSpeechService", "Error stopping audioRecord", e)
        }
        try {
            audioRecord?.release()
        } catch (e: Exception) {
            // ignore
        }
        audioRecord = null
    }
}
