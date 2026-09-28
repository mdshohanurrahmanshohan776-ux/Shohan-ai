package com.example.data.device

import android.content.Context
import android.content.SharedPreferences
import java.util.Locale

enum class AppLanguage(
    val code: String,
    val speechTag: String,
    val displayName: String,
    val nativeName: String,
    val sampleGreeting: String
) {
    BENGALI("bn", "bn-BD", "Bengali", "বাংলা", "আসসালামু আলাইকুম! আমি সোহান।"),
    ENGLISH("en", "en-US", "English", "English", "Hello! I am Shohan, your personal AI assistant."),
    HINDI("hi", "hi-IN", "Hindi", "हिन्दी", "नमस्ते! मैं शोहन हूँ, आपका व्यक्तिगत एआई सहायक।"),
    SPANISH("es", "es-ES", "Spanish", "Español", "¡Hola! Soy Shohan, tu asistente personal de IA.");

    fun getLocale(): Locale {
        return when (this) {
            BENGALI -> Locale("bn", "BD")
            ENGLISH -> Locale.US
            HINDI -> Locale("hi", "IN")
            SPANISH -> Locale("es", "ES")
        }
    }
}

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("shohan_ai_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_CUSTOM_WAKE_WORD = "custom_wake_word"
        private const val KEY_SELECTED_LANGUAGE = "selected_language"
        private const val KEY_TTS_MUTED = "tts_muted"
        private const val KEY_CUSTOM_API_KEY = "custom_api_key"
        private const val KEY_PROACTIVE_ENABLED = "proactive_enabled"

        const val DEFAULT_WAKE_WORD = "Hi Shohan"
    }

    var customWakeWord: String
        get() = prefs.getString(KEY_CUSTOM_WAKE_WORD, DEFAULT_WAKE_WORD) ?: DEFAULT_WAKE_WORD
        set(value) = prefs.edit().putString(KEY_CUSTOM_WAKE_WORD, value.trim()).apply()

    var selectedLanguage: AppLanguage
        get() {
            val code = prefs.getString(KEY_SELECTED_LANGUAGE, AppLanguage.BENGALI.code)
            return AppLanguage.values().firstOrNull { it.code == code } ?: AppLanguage.BENGALI
        }
        set(value) = prefs.edit().putString(KEY_SELECTED_LANGUAGE, value.code).apply()

    var isTtsMuted: Boolean
        get() = prefs.getBoolean(KEY_TTS_MUTED, false)
        set(value) = prefs.edit().putBoolean(KEY_TTS_MUTED, value).apply()

    var customApiKey: String
        get() = prefs.getString(KEY_CUSTOM_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_API_KEY, value).apply()

    var isProactiveEnabled: Boolean
        get() = prefs.getBoolean(KEY_PROACTIVE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_PROACTIVE_ENABLED, value).apply()
}
