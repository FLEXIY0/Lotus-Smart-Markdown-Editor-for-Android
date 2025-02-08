package com.flesiy.Lotus.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

private const val TAG = "SpeechRecognitionManager"
private const val SPEECH_REQUEST_CODE = 102

class SpeechRecognitionManager(private val context: Context) {
    private var recognitionCallback: ((String, Boolean) -> Unit)? = null
    private var activity: Activity? = null
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    fun setActivity(activity: Activity) {
        this.activity = activity
    }

    fun startListening(callback: (String, Boolean) -> Unit) {
        recognitionCallback = callback
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
        }
        
        try {
            activity?.let { act ->
                act.startActivityForResult(intent, SPEECH_REQUEST_CODE)
                _isListening.value = true
            } ?: run {
                Toast.makeText(context, "Ошибка запуска распознавания речи", Toast.LENGTH_SHORT).show()
                _isListening.value = false
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Распознавание речи недоступно", Toast.LENGTH_SHORT).show()
            _isListening.value = false
        }
    }

    fun stopListening() {
        _isListening.value = false
        _elapsedTime.value = 0
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!results.isNullOrEmpty()) {
                val recognizedText = results[0]
                recognitionCallback?.invoke(recognizedText, true)
            }
        }
        stopListening()
    }

    fun destroy() {
        stopListening()
        recognitionCallback = null
        activity = null
    }
} 