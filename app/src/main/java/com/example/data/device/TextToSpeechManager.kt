package com.example.data.device

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class TextToSpeechManager(
    private val context: Context,
    private var currentLanguage: AppLanguage = AppLanguage.BENGALI
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        tts = TextToSpeech(context.applicationContext, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            applyLanguage(currentLanguage)
            tts?.setPitch(1.02f)
            tts?.setSpeechRate(0.96f)

            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isInitialized = true
        } else {
            Log.e("TextToSpeechManager", "TTS initialization failed")
        }
    }

    fun setLanguage(language: AppLanguage) {
        currentLanguage = language
        if (isInitialized) {
            applyLanguage(language)
        }
    }

    private fun applyLanguage(language: AppLanguage) {
        val targetLocale = language.getLocale()
        val available = tts?.isLanguageAvailable(targetLocale) ?: TextToSpeech.LANG_NOT_SUPPORTED
        if (available >= TextToSpeech.LANG_AVAILABLE) {
            tts?.language = targetLocale
        } else {
            val fallback = when (language) {
                AppLanguage.BENGALI -> Locale("bn")
                AppLanguage.HINDI -> Locale("hi")
                AppLanguage.SPANISH -> Locale("es")
                AppLanguage.ENGLISH -> Locale.ENGLISH
            }
            if ((tts?.isLanguageAvailable(fallback) ?: TextToSpeech.LANG_NOT_SUPPORTED) >= TextToSpeech.LANG_AVAILABLE) {
                tts?.language = fallback
            } else {
                tts?.language = Locale.getDefault()
            }
        }
    }

    fun speak(text: String, utteranceId: String = "ShohanResponse") {
        if (!isInitialized) return
        stop()

        // Clean out any raw markdown formatting or action tags for clean natural speech
        val cleanedText = text
            .replace(Regex("\\[ACTION:[^\\]]+\\]"), "")
            .replace(Regex("[*#`_]"), "")
            .trim()

        if (cleanedText.isBlank()) return

        _isSpeaking.value = true
        tts?.speak(cleanedText, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun stop() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        isInitialized = false
    }
}
