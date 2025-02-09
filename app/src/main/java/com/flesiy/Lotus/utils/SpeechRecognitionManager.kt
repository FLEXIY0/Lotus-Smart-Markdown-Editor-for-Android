package com.flesiy.Lotus.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.speech.RecognizerIntent
import android.widget.Toast
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale

private const val TAG = "SPEECH_MANAGER_DEBUG"
private const val SPEECH_REQUEST_CODE = 102

class SpeechRecognitionManager(private val context: Context) {
    private var recognitionCallback: ((String, Boolean) -> Unit)? = null
    private var activity: Activity? = null
    
    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening

    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime

    fun setActivity(activity: Activity) {
        Log.d(TAG, "👉 setActivity вызван")
        this.activity = activity
    }

    fun startListening(callback: (String, Boolean) -> Unit) {
        Log.d(TAG, "👉 startListening вызван")
        recognitionCallback = callback
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Говорите...")
        }
        
        try {
            activity?.let { act ->
                Log.d(TAG, "🎤 Запуск распознавания речи")
                act.startActivityForResult(intent, SPEECH_REQUEST_CODE)
                _isListening.value = true
            } ?: run {
                Log.e(TAG, "❌ Activity не установлена")
                Toast.makeText(context, "Ошибка запуска распознавания речи", Toast.LENGTH_SHORT).show()
                _isListening.value = false
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 Ошибка при запуске распознавания", e)
            Toast.makeText(context, "Распознавание речи недоступно", Toast.LENGTH_SHORT).show()
            _isListening.value = false
        }
    }

    fun stopListening() {
        Log.d(TAG, "🛑 stopListening вызван")
        _isListening.value = false
        _elapsedTime.value = 0
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        Log.d(TAG, "📝 handleActivityResult вызван: requestCode=$requestCode, resultCode=$resultCode")
        if (requestCode == SPEECH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val results = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            Log.d(TAG, "📝 Результаты распознавания: $results")
            if (!results.isNullOrEmpty()) {
                val recognizedText = results[0]
                Log.d(TAG, "✅ Распознанный текст: $recognizedText")
                recognitionCallback?.invoke(recognizedText, true)
            } else {
                Log.e(TAG, "❌ Пустой результат распознавания")
            }
        } else {
            Log.e(TAG, "❌ Ошибка распознавания: requestCode=$requestCode, resultCode=$resultCode")
        }
        stopListening()
    }

    fun destroy() {
        Log.d(TAG, "🗑️ destroy вызван")
        stopListening()
        recognitionCallback = null
        activity = null
    }
} 