package com.example.data.device

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class ShohanSpeechRecognizer(
    private val context: Context,
    private var customWakeWord: String = AppPreferences(context).customWakeWord,
    private var currentLanguage: AppLanguage = AppPreferences(context).selectedLanguage,
    private val onWakeWordDetected: (spokenCommand: String?) -> Unit,
    private val onFinalCommand: (command: String) -> Unit,
    private val onErrorOccurred: (errorMsg: String) -> Unit
) {
    private var speechRecognizer: SpeechRecognizer? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isContinuousWakeMode = MutableStateFlow(false)
    val isContinuousWakeMode: StateFlow<Boolean> = _isContinuousWakeMode.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val defaultWakeWords = listOf(
        "hi shohan", "hey shohan", "hello shohan", "shohan",
        "হাই সোহান", "হে সোহান", "হ্যালো সোহান", "সোহান", "শোহান",
        "হায় সোহান", "হায় সোহান", "नमस्ते शोहन", "शोहन", "hola shohan"
    )

    fun updateCustomWakeWord(newWakeWord: String) {
        customWakeWord = newWakeWord.trim()
    }

    fun updateLanguage(newLanguage: AppLanguage) {
        currentLanguage = newLanguage
    }

    private fun getActiveWakeWords(): List<String> {
        val list = mutableListOf<String>()
        if (customWakeWord.isNotBlank()) {
            list.add(customWakeWord.lowercase())
        }
        list.addAll(defaultWakeWords)
        return list.distinct()
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            _isListening.value = true
        }

        override fun onBeginningOfSpeech() {}

        override fun onRmsChanged(rmsdB: Float) {
            val normalized = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            _soundLevel.value = normalized
        }

        override fun onBufferReceived(buffer: ByteArray?) {}

        override fun onEndOfSpeech() {
            _isListening.value = false
            _soundLevel.value = 0f
        }

        override fun onError(error: Int) {
            _isListening.value = false
            _soundLevel.value = 0f

            if (_isContinuousWakeMode.value) {
                mainHandler.postDelayed({
                    if (_isContinuousWakeMode.value) {
                        startListeningInternal()
                    }
                }, 1000)
            } else {
                val errorMsg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> when (currentLanguage) {
                        AppLanguage.BENGALI -> "কথা বুঝতে পারিনি, আবার বলুন"
                        AppLanguage.ENGLISH -> "Could not understand, please try again"
                        AppLanguage.HINDI -> "समझ नहीं आया, कृपया पुनः प्रयास करें"
                        AppLanguage.SPANISH -> "No se entendió, intente de nuevo"
                    }
                    SpeechRecognizer.ERROR_NETWORK -> when (currentLanguage) {
                        AppLanguage.BENGALI -> "নেটওয়ার্ক সমস্যা"
                        AppLanguage.ENGLISH -> "Network issue"
                        AppLanguage.HINDI -> "नेटवर्क समस्या"
                        AppLanguage.SPANISH -> "Problema de red"
                    }
                    SpeechRecognizer.ERROR_AUDIO -> when (currentLanguage) {
                        AppLanguage.BENGALI -> "মাইক্রোফোন ত্রুটি"
                        AppLanguage.ENGLISH -> "Microphone error"
                        AppLanguage.HINDI -> "माइक्रोफोन त्रुटि"
                        AppLanguage.SPANISH -> "Error de micrófono"
                    }
                    else -> when (currentLanguage) {
                        AppLanguage.BENGALI -> "মাইক্রোফোন প্রস্তুত"
                        AppLanguage.ENGLISH -> "Microphone ready"
                        AppLanguage.HINDI -> "माइक्रोफोन तैयार"
                        AppLanguage.SPANISH -> "Micrófono listo"
                    }
                }
                onErrorOccurred(errorMsg)
            }
        }

        override fun onResults(results: Bundle?) {
            _isListening.value = false
            _soundLevel.value = 0f

            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            val topResult = matches?.firstOrNull()?.trim() ?: ""
            _liveTranscript.value = topResult

            if (topResult.isNotBlank()) {
                handleRecognizedSpeech(topResult)
            }

            if (_isContinuousWakeMode.value) {
                mainHandler.postDelayed({
                    if (_isContinuousWakeMode.value) {
                        startListeningInternal()
                    }
                }, 500)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull() ?: ""
            _liveTranscript.value = partial

            if (_isContinuousWakeMode.value) {
                val lower = partial.lowercase()
                for (wake in getActiveWakeWords()) {
                    if (lower.contains(wake)) {
                        triggerHapticFeedback()
                        break
                    }
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    private fun handleRecognizedSpeech(spokenText: String) {
        val lower = spokenText.lowercase()
        var matchedWake: String? = null

        for (wake in getActiveWakeWords()) {
            if (lower.contains(wake)) {
                matchedWake = wake
                break
            }
        }

        if (matchedWake != null) {
            triggerHapticFeedback()
            val index = lower.indexOf(matchedWake)
            val commandAfterWake = spokenText.substring(index + matchedWake.length)
                .trimStart(',', ' ', '!', '.', '।', ':', ';', '-')
                .trim()

            onWakeWordDetected(if (commandAfterWake.isNotBlank()) commandAfterWake else null)
            if (commandAfterWake.isNotBlank()) {
                onFinalCommand(commandAfterWake)
            }
        } else {
            onFinalCommand(spokenText)
        }
    }

    fun startListening(continuous: Boolean = false) {
        _isContinuousWakeMode.value = continuous
        mainHandler.post {
            startListeningInternal()
        }
    }

    private fun startListeningInternal() {
        try {
            stopListeningInternal()

            if (!SpeechRecognizer.isRecognitionAvailable(context)) {
                onErrorOccurred("ভয়েস রিকগনিশন ডিভাইসটিতে সমর্থিত নয়")
                return
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(recognitionListener)
            }

            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, currentLanguage.speechTag)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, currentLanguage.speechTag)
                putExtra(RecognizerIntent.EXTRA_ONLY_RETURN_LANGUAGE_PREFERENCE, false)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            }

            speechRecognizer?.startListening(intent)
            _isListening.value = true
        } catch (e: Exception) {
            Log.e("ShohanSpeechRecognizer", "Failed to start listening", e)
            _isListening.value = false
        }
    }

    fun stopListening() {
        _isContinuousWakeMode.value = false
        mainHandler.post {
            stopListeningInternal()
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e("ShohanSpeechRecognizer", "Error stopping recognizer", e)
        } finally {
            speechRecognizer = null
            _isListening.value = false
            _soundLevel.value = 0f
        }
    }

    fun triggerHapticFeedback() {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
            } else {
                @Suppress("DEPRECATION")
                val v = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    v?.vibrate(VibrationEffect.createOneShot(80, VibrationEffect.DEFAULT_AMPLITUDE))
                } else {
                    @Suppress("DEPRECATION")
                    v?.vibrate(80)
                }
            }
        } catch (e: Exception) {
            Log.e("ShohanSpeechRecognizer", "Vibrator exception", e)
        }
    }

    fun destroy() {
        stopListening()
    }
}
