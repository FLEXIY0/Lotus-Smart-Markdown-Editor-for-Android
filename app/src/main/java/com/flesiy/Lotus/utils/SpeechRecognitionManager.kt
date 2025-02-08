package com.flesiy.Lotus.utils

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel

private const val TAG = "SpeechRecognitionManager"

class SpeechRecognitionManager(private val context: Context) {
    private var speechRecognizer: SpeechRecognizer? = null
    private var recognitionCallback: ((String, Boolean) -> Unit)? = null
    private var isRecognitionActive = false
    private var lastPartialResult = ""
    private var isRestarting = false
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    private var startTime: Long = 0
    private var timerJob: Job? = null
    private val scope = MainScope()

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            setupRecognitionListener()
        }
    }

    private fun setupRecognitionListener() {
        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                Log.d(TAG, "onReadyForSpeech")
                if (!isRestarting) {
                    _isListening.value = true
                    startTime = System.currentTimeMillis()
                    startTimer()
                }
                isRecognitionActive = true
                isRestarting = false
            }

            override fun onBeginningOfSpeech() {
                Log.d(TAG, "onBeginningOfSpeech")
            }

            override fun onRmsChanged(rmsdB: Float) {}

            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                Log.d(TAG, "onEndOfSpeech")
                if (isRecognitionActive && !isRestarting) {
                    isRestarting = true
                    restartListening()
                }
            }

            override fun onError(error: Int) {
                Log.e(TAG, "onError: $error")
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> {
                        if (isRecognitionActive && !isRestarting) {
                            Log.d(TAG, "No match error - restarting")
                            isRestarting = true
                            restartListening()
                        }
                    }
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                        Log.d(TAG, "Speech timeout - stopping")
                        finalizeRecognition()
                    }
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> {
                        Log.e(TAG, "Insufficient permissions")
                        finalizeRecognition()
                    }
                    SpeechRecognizer.ERROR_NETWORK,
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                        Log.e(TAG, "Network error")
                        finalizeRecognition()
                    }
                    SpeechRecognizer.ERROR_CLIENT,
                    SpeechRecognizer.ERROR_SERVER -> {
                        Log.e(TAG, "Client/Server error")
                        finalizeRecognition()
                    }
                    else -> {
                        Log.e(TAG, "Other error - stopping")
                        finalizeRecognition()
                    }
                }
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    if (text.isNotBlank() && text != lastPartialResult) {
                        lastPartialResult = text
                        Log.d(TAG, "Partial result: $text")
                        recognitionCallback?.invoke(text, false)
                    }
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    if (text.isNotBlank() && text != lastPartialResult) {
                        lastPartialResult = text
                        Log.d(TAG, "Final result: $text")
                        recognitionCallback?.invoke(text, true)
                    }
                }
                if (isRecognitionActive && !isRestarting) {
                    isRestarting = true
                    restartListening()
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = scope.launch {
            while (_isListening.value) {
                _elapsedTime.value = System.currentTimeMillis() - startTime
                delay(100)
            }
        }
    }

    private fun createRecognizerIntent() = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        
        // Минимальное время прослушивания - 5 секунд
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
        
        // Время ожидания тишины перед завершением - 20 секунд
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 20000)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 20000)
        
        // Отключаем звуки
        flags = Intent.FLAG_ACTIVITY_NO_HISTORY
    }

    private fun restartListening() {
        if (isRecognitionActive) {
            scope.launch {
                delay(100) // Небольшая задержка перед перезапуском
                try {
                    speechRecognizer?.startListening(createRecognizerIntent())
                } catch (e: Exception) {
                    Log.e(TAG, "Error restarting recognition", e)
                    finalizeRecognition()
                }
            }
        }
    }

    private fun finalizeRecognition() {
        isRecognitionActive = false
        isRestarting = false
        lastPartialResult = ""
        stopListening()
    }

    fun startListening(callback: (String, Boolean) -> Unit) {
        // Проверяем разрешение на запись аудио
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) 
            != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "No RECORD_AUDIO permission")
            return
        }

        if (isRecognitionActive) {
            Log.d(TAG, "Recognition already active")
            return
        }
        
        recognitionCallback = callback
        isRecognitionActive = true
        lastPartialResult = ""
        isRestarting = false
        
        try {
            speechRecognizer?.startListening(createRecognizerIntent())
        } catch (e: Exception) {
            Log.e(TAG, "Error starting recognition", e)
            stopListening()
        }
    }

    fun stopListening() {
        _isListening.value = false
        _elapsedTime.value = 0
        startTime = 0
        isRecognitionActive = false
        isRestarting = false
        lastPartialResult = ""
        timerJob?.cancel()
        try {
            speechRecognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition", e)
        }
    }

    fun destroy() {
        stopListening()
        scope.cancel()
        try {
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying recognizer", e)
        }
        speechRecognizer = null
    }
} 