package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import com.example.data.device.AppLanguage
import com.example.data.device.DeviceAmbientContext
import com.example.data.local.ProactiveReminderEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

data class AssistantResponse(
    val replyText: String,
    val actionType: String? = null,
    val actionPayload: String? = null,
    val rawActionTag: String? = null
)

class GeminiRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private fun buildSystemInstruction(preferredLanguage: AppLanguage): String {
        val langHint = when (preferredLanguage) {
            AppLanguage.BENGALI -> "Default to Bengali (বাংলা) unless user asks in another language."
            AppLanguage.ENGLISH -> "Default to English unless user asks in another language."
            AppLanguage.HINDI -> "Default to Hindi (हिन्दी) unless user asks in another language."
            AppLanguage.SPANISH -> "Default to Spanish (Español) unless user asks in another language."
        }

        return """
            You are "Shohan" (সোহান / शोहन), a proactive, intelligent, and versatile personal AI assistant living inside an Android device.
            You are natively and fluently multilingual with exceptional command over:
            1. Bengali (বাংলা)
            2. English
            3. Hindi (हिन्दी)
            4. Spanish (Español)
            $langHint
            Always respond warmly, concisely, and helpfully in the user's spoken language.
            
            You have direct capability to execute phone actions. When a command requires an action, include a special tag at the very end of your response:
            - Latest Gallery Photo: [ACTION:FIND_LATEST_PHOTO]
            - Transcribe Audio: [ACTION:TRANSCRIBE_AUDIO]
            - Send Message: [ACTION:SEND_MESSAGE|recipient|message]
            - Make Phone Call: [ACTION:MAKE_CALL|recipient_or_number]
            - Social Media Post: [ACTION:POST_SOCIAL|platform|text_content] (platform can be 'facebook', 'twitter', 'whatsapp', or 'share')
            - Flashlight / Torch: [ACTION:TOGGLE_TORCH|on] or [ACTION:TOGGLE_TORCH|off]
            - Add Reminder / Calendar Schedule: [ACTION:ADD_REMINDER|title|time]
            - Open App: [ACTION:OPEN_APP|app_name]
            
            Examples:
            - Bengali: "আমাকে গ্যালারি থেকে সর্বশেষ ছবিটি খুঁজে বের করে দাও" -> "অবশ্যই! আমি আপনার গ্যালারির সর্বশেষ ছবিটি নিয়ে আসছি। [ACTION:FIND_LATEST_PHOTO]"
            - English: "Find my latest photo from the gallery" -> "Sure! Fetching the latest photo from your gallery now. [ACTION:FIND_LATEST_PHOTO]"
            - Hindi: "मुझे गैलरी से नवीनतम फोटो ढूंढ कर दो" -> "बिल्कुल! मैं आपकी गैलरी से नवीनतम फोटो ला रहा हूँ। [ACTION:FIND_LATEST_PHOTO]"
            - Spanish: "Encuentra la última foto de mi galería" -> "¡Por supuesto! Obteniendo la última foto de tu galería ahora mismo. [ACTION:FIND_LATEST_PHOTO]"
            
            - Reminder in any language:
            User: "Remind me of meeting tomorrow at 10 AM" -> "I have set a reminder for your meeting tomorrow at 10 AM. [ACTION:ADD_REMINDER|Meeting|10:00 AM]"
            User: "কাল সকাল ১০ টায় ডাক্তারের কাছে যেতে মনে করিয়ে দাও" -> "আমি ডাক্তারের সাক্ষাতের রিমাইন্ডার সেট করে দিয়েছি। [ACTION:ADD_REMINDER|ডাক্তারের সাক্ষাৎ|সকাল ১০:০০]"
            
            For general questions, knowledge, coding, advice, or ideas, answer thoroughly, naturally, and intelligently in the user's language without action tags.
        """.trimIndent()
    }

    suspend fun generateAssistantReply(
        userMessage: String,
        conversationHistory: List<Pair<String, String>> = emptyList(),
        customApiKey: String? = null,
        language: AppLanguage = AppLanguage.BENGALI
    ): AssistantResponse = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext parseActionFromText(
                generateLocalFallbackReply(userMessage, language)
            )
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val contentsArray = JSONArray()

            // System prompt + history
            conversationHistory.takeLast(6).forEach { (role, text) ->
                val turnObj = JSONObject()
                turnObj.put("role", if (role == "user") "user" else "model")
                val partsArray = JSONArray()
                partsArray.put(JSONObject().put("text", text))
                turnObj.put("parts", partsArray)
                contentsArray.put(turnObj)
            }

            // Current prompt
            val currentTurn = JSONObject()
            currentTurn.put("role", "user")
            val currentParts = JSONArray()
            currentParts.put(JSONObject().put("text", userMessage))
            currentTurn.put("parts", currentParts)
            contentsArray.put(currentTurn)

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)

            // Multilingual system instruction
            val systemObj = JSONObject()
            val sysParts = JSONArray()
            sysParts.put(JSONObject().put("text", buildSystemInstruction(language)))
            systemObj.put("parts", sysParts)
            requestJson.put("systemInstruction", systemObj)

            // Generation config
            val genConfig = JSONObject()
            genConfig.put("temperature", 0.7)
            genConfig.put("maxOutputTokens", 1024)
            requestJson.put("generationConfig", genConfig)

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder()
                .url(endpoint)
                .post(body)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                Log.e("GeminiRepository", "Error: ${response.code} -> $responseString")
                return@withContext parseActionFromText(generateLocalFallbackReply(userMessage, language))
            }

            val rootJson = JSONObject(responseString)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            val fallbackMsg = when (language) {
                AppLanguage.BENGALI -> "কোনো উত্তর পাওয়া যায়নি।"
                AppLanguage.ENGLISH -> "No response received."
                AppLanguage.HINDI -> "कोई उत्तर प्राप्त नहीं हुआ।"
                AppLanguage.SPANISH -> "No se recibió respuesta."
            }
            val responseText = parts?.optJSONObject(0)?.optString("text")?.trim() ?: fallbackMsg

            return@withContext parseActionFromText(responseText)
        } catch (e: Exception) {
            Log.e("GeminiRepository", "Exception calling Gemini", e)
            return@withContext parseActionFromText(generateLocalFallbackReply(userMessage, language))
        }
    }

    /**
     * Multimodal analysis of an image (e.g. latest gallery photo)
     */
    suspend fun analyzeImage(
        bitmap: Bitmap,
        prompt: String? = null,
        customApiKey: String? = null,
        language: AppLanguage = AppLanguage.BENGALI
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext when (language) {
                AppLanguage.BENGALI -> "ছবিটি সফলভাবে খুঁজে পাওয়া গেছে! জেমিনাই এআই ভিশন বিশ্লেষণের জন্য আপনার জেমিনাই এপিআই কি প্রয়োজন।"
                AppLanguage.ENGLISH -> "Photo retrieved! A Gemini API key is needed to run AI vision analysis."
                AppLanguage.HINDI -> "फोटो मिल गई है! विश्लेषण के लिए जेमिनी एपीआई की आवश्यकता है।"
                AppLanguage.SPANISH -> "¡Foto obtenida! Se necesita una clave API de Gemini para el análisis de visión."
            }
        }

        val effectivePrompt = prompt ?: when (language) {
            AppLanguage.BENGALI -> "এই ছবিটিতে কি কি দেখা যাচ্ছে তা বিস্তারিত এবং সুন্দর বাংলায় বর্ণনা করুন।"
            AppLanguage.ENGLISH -> "Describe what you see in this photo in clear and engaging detail."
            AppLanguage.HINDI -> "इस तस्वीर में क्या-क्या दिखाई दे रहा है, विस्तार से बताएं।"
            AppLanguage.SPANISH -> "Describe lo que ves en esta imagen con detalles claros y atractivos."
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"

            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
            val base64Data = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)

            val contentsArray = JSONArray()
            val turnObj = JSONObject()
            turnObj.put("role", "user")

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", effectivePrompt))

            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", "image/jpeg")
            inlineDataObj.put("data", base64Data)
            partsArray.put(JSONObject().put("inlineData", inlineDataObj))

            turnObj.put("parts", partsArray)
            contentsArray.put(turnObj)

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(endpoint).post(body).build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Error: (${response.code})"
            }

            val rootJson = JSONObject(responseString)
            val candidates = rootJson.optJSONArray("candidates")
            val firstCandidate = candidates?.optJSONObject(0)
            val contentObj = firstCandidate?.optJSONObject("content")
            val parts = contentObj?.optJSONArray("parts")
            return@withContext parts?.optJSONObject(0)?.optString("text")?.trim() ?: "Completed."
        } catch (e: Exception) {
            return@withContext "Analysis error: ${e.message}"
        }
    }

    /**
     * Transcribe or process audio with Gemini
     */
    suspend fun transcribeAudioBytes(
        audioBytes: ByteArray,
        mimeType: String = "audio/mp3",
        customApiKey: String? = null,
        language: AppLanguage = AppLanguage.BENGALI
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext when (language) {
                AppLanguage.BENGALI -> "অডিও ফাইলটি লোড হয়েছে। জেমিনাই এআই দিয়ে নির্ভুল ট্রান্সক্রিপশন পেতে এপিআই কি প্রয়োজন।"
                AppLanguage.ENGLISH -> "Audio loaded. Provide an API key for accurate Gemini AI transcription."
                AppLanguage.HINDI -> "ऑडियो लोड हो गया है। सटीक ट्रांसक्रिप्शन के लिए जेमिनी एपीआई की आवश्यकता है।"
                AppLanguage.SPANISH -> "Audio cargado. Proporcione una clave API para la transcripción con Gemini."
            }
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)

            val contentsArray = JSONArray()
            val turnObj = JSONObject()
            turnObj.put("role", "user")

            val promptText = "Please listen to this audio and transcribe every spoken word accurately into text. Format nicely preserving the original language (Bengali, English, Hindi, Spanish, etc.)."

            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", promptText))

            val inlineDataObj = JSONObject()
            inlineDataObj.put("mimeType", mimeType)
            inlineDataObj.put("data", base64Audio)
            partsArray.put(JSONObject().put("inlineData", inlineDataObj))

            turnObj.put("parts", partsArray)
            contentsArray.put(turnObj)

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(endpoint).post(body).build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (!response.isSuccessful) {
                return@withContext "Error (${response.code})"
            }

            val rootJson = JSONObject(responseString)
            val candidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
            val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
            return@withContext text?.trim() ?: "Completed."
        } catch (e: Exception) {
            return@withContext "Transcription error: ${e.message}"
        }
    }

    /**
     * Proactive briefing generation analyzing time, battery, and scheduled tasks
     */
    suspend fun generateProactiveBriefing(
        ambient: DeviceAmbientContext,
        reminders: List<ProactiveReminderEntity>,
        language: AppLanguage,
        customApiKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getEffectiveApiKey(customApiKey)
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext generateLocalProactiveBriefing(ambient, reminders, language)
        }

        try {
            val endpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3.5-flash:generateContent?key=$apiKey"
            val pendingTitles = reminders.filter { !it.isCompleted }.joinToString(", ") { "${it.title} at ${it.timeLabel}" }

            val prompt = """
                You are Shohan AI Assistant. Generate a warm, concise, proactive 2-3 sentence personalized smart briefing and 1 actionable suggestion for the user based on their current phone & schedule context:
                - Time of Day: ${ambient.timeOfDay} (${ambient.formattedCurrentTime})
                - Battery Status: ${ambient.batteryPercent}% (${if (ambient.isCharging) "Charging" else "On battery"})
                - Pending Reminders & Schedule: ${if (pendingTitles.isNotBlank()) pendingTitles else "No pending tasks recorded"}
                
                Language to reply in: ${language.displayName} (${language.nativeName})
                Keep it motivating, proactive, and directly actionable!
            """.trimIndent()

            val contentsArray = JSONArray()
            val turnObj = JSONObject()
            turnObj.put("role", "user")
            val partsArray = JSONArray()
            partsArray.put(JSONObject().put("text", prompt))
            turnObj.put("parts", partsArray)
            contentsArray.put(turnObj)

            val requestJson = JSONObject()
            requestJson.put("contents", contentsArray)

            val body = requestJson.toString().toRequestBody(jsonMediaType)
            val request = Request.Builder().url(endpoint).post(body).build()
            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val rootJson = JSONObject(responseString)
                val candidate = rootJson.optJSONArray("candidates")?.optJSONObject(0)
                val text = candidate?.optJSONObject("content")?.optJSONArray("parts")?.optJSONObject(0)?.optString("text")
                if (!text.isNullOrBlank()) {
                    return@withContext text.trim()
                }
            }
            return@withContext generateLocalProactiveBriefing(ambient, reminders, language)
        } catch (e: Exception) {
            return@withContext generateLocalProactiveBriefing(ambient, reminders, language)
        }
    }

    private fun generateLocalProactiveBriefing(
        ambient: DeviceAmbientContext,
        reminders: List<ProactiveReminderEntity>,
        language: AppLanguage
    ): String {
        val pendingCount = reminders.count { !it.isCompleted }
        return when (language) {
            AppLanguage.BENGALI -> {
                val greeting = ambient.timeGreetingBn
                val batteryNote = if (ambient.batteryPercent < 20) " আপনার ফোনের ব্যাটারি ${ambient.batteryPercent}%।" else ""
                val taskNote = if (pendingCount > 0) " আপনার $pendingCount টি কাজ নির্ধারিত আছে।" else " আজকের সূচী বেশ পরিষ্কার।"
                "$greeting! সময় এখন ${ambient.formattedCurrentTime}।$batteryNote$taskNote আমি আপনাকে সারাদিনের যেকোনো কাজে সহায়তা করতে প্রস্তুত।"
            }
            AppLanguage.ENGLISH -> {
                val greeting = ambient.timeGreetingEn
                val batteryNote = if (ambient.batteryPercent < 20) " Your phone battery is at ${ambient.batteryPercent}%." else ""
                val taskNote = if (pendingCount > 0) " You have $pendingCount pending items on your schedule." else " Your schedule looks clear."
                "$greeting! The time is ${ambient.formattedCurrentTime}.$batteryNote$taskNote I am ready to help you with photos, audio notes, messaging, or calls."
            }
            AppLanguage.HINDI -> {
                val greeting = when (ambient.timeOfDay) {
                    "MORNING" -> "शुभ प्रभात"
                    "AFTERNOON" -> "शुभ दोपहर"
                    "EVENING" -> "शुभ संध्या"
                    else -> "शुभ रात्रि"
                }
                "$greeting! समय ${ambient.formattedCurrentTime} है। बैटरी ${ambient.batteryPercent}% है। मैं आपके कार्यों, संदेशों और कॉल में सहायता के लिए पूरी तरह तैयार हूँ।"
            }
            AppLanguage.SPANISH -> {
                val greeting = when (ambient.timeOfDay) {
                    "MORNING" -> "¡Buenos días!"
                    "AFTERNOON" -> "¡Buenas tardes!"
                    "EVENING" -> "¡Buenas tardes!"
                    else -> "¡Buenas noches!"
                }
                "$greeting Son las ${ambient.formattedCurrentTime}. Batería al ${ambient.batteryPercent}%. Estoy listo para asistirte con tareas, mensajes y llamadas."
            }
        }
    }

    private fun getEffectiveApiKey(customKey: String?): String {
        return when {
            !customKey.isNullOrBlank() -> customKey
            try { BuildConfig.GEMINI_API_KEY.isNotBlank() } catch (_: Exception) { false } -> BuildConfig.GEMINI_API_KEY
            else -> ""
        }
    }

    fun parseActionFromText(rawText: String): AssistantResponse {
        val actionRegex = Regex("\\[ACTION:([^\\]]+)\\]")
        val match = actionRegex.find(rawText)

        if (match != null) {
            val fullAction = match.groupValues[1]
            val cleanReply = rawText.replace(match.value, "").trim()
            val parts = fullAction.split("|")
            val actionType = parts[0]
            val actionPayload = if (parts.size > 1) parts.subList(1, parts.size).joinToString("|") else null
            return AssistantResponse(
                replyText = cleanReply,
                actionType = actionType,
                actionPayload = actionPayload,
                rawActionTag = match.value
            )
        }

        return AssistantResponse(replyText = rawText)
    }

    private fun generateLocalFallbackReply(userMessage: String, language: AppLanguage): String {
        val lower = userMessage.lowercase()
        return when {
            lower.contains("ছবি") || lower.contains("গ্যালারি") || lower.contains("gallery") || lower.contains("photo") || lower.contains("तस्वीर") || lower.contains("foto") -> {
                when (language) {
                    AppLanguage.BENGALI -> "অবশ্যই! আপনার গ্যালারির সর্বশেষ ছবিটি নিয়ে আসছি। [ACTION:FIND_LATEST_PHOTO]"
                    AppLanguage.ENGLISH -> "Sure! Fetching the latest photo from your gallery. [ACTION:FIND_LATEST_PHOTO]"
                    AppLanguage.HINDI -> "अवश्य! मैं आपकी गैलरी से नवीनतम तस्वीर ला रहा हूँ। [ACTION:FIND_LATEST_PHOTO]"
                    AppLanguage.SPANISH -> "¡Por supuesto! Obteniendo la foto más reciente de tu galería. [ACTION:FIND_LATEST_PHOTO]"
                }
            }
            lower.contains("অডিও") || lower.contains("লিখে দাও") || lower.contains("transcribe") || lower.contains("audio") || lower.contains("ऑडियो") -> {
                when (language) {
                    AppLanguage.BENGALI -> "ঠিক আছে! আমি আপনার অডিও ফাইলটি লিখে দেওয়ার জন্য প্রস্তুত। [ACTION:TRANSCRIBE_AUDIO]"
                    AppLanguage.ENGLISH -> "Got it! Audio transcriber is ready. [ACTION:TRANSCRIBE_AUDIO]"
                    AppLanguage.HINDI -> "ठीक है! मैं ऑडियो फाइल को टेक्स्ट में बदलने के लिए तैयार हूँ। [ACTION:TRANSCRIBE_AUDIO]"
                    AppLanguage.SPANISH -> "¡Entendido! El transcriptor de audio está listo. [ACTION:TRANSCRIBE_AUDIO]"
                }
            }
            lower.contains("মেসেজ") || lower.contains("message") || lower.contains("sms") || lower.contains("संदेश") || lower.contains("mensaje") -> {
                when (language) {
                    AppLanguage.BENGALI -> "আমি মেসেজ পাঠানোর উইন্ডো প্রস্তুত করেছি। [ACTION:SEND_MESSAGE|পরিচিতি|হ্যালো!]"
                    AppLanguage.ENGLISH -> "Prepared a message draft for you. [ACTION:SEND_MESSAGE|Contact|Hello!]"
                    AppLanguage.HINDI -> "मैंने संदेश भेजने की विंडो तैयार की है। [ACTION:SEND_MESSAGE|संपर्क|नमस्ते!]"
                    AppLanguage.SPANISH -> "He preparado la ventana de envío de mensaje. [ACTION:SEND_MESSAGE|Contacto|¡Hola!]"
                }
            }
            lower.contains("কল") || lower.contains("call") || lower.contains("ফোন") || lower.contains("कॉल") || lower.contains("llamar") -> {
                when (language) {
                    AppLanguage.BENGALI -> "আমি ডায়ালার খুলে দিচ্ছি কল করার জন্য। [ACTION:MAKE_CALL|]"
                    AppLanguage.ENGLISH -> "Opening the dialer to place a call. [ACTION:MAKE_CALL|]"
                    AppLanguage.HINDI -> "कॉल करने के लिए डायलर खोला जा रहा है। [ACTION:MAKE_CALL|]"
                    AppLanguage.SPANISH -> "Abriendo el marcador para realizar una llamada. [ACTION:MAKE_CALL|]"
                }
            }
            lower.contains("সোশ্যাল") || lower.contains("পোস্ট") || lower.contains("facebook") || lower.contains("post") || lower.contains("शेयर") || lower.contains("publicar") -> {
                when (language) {
                    AppLanguage.BENGALI -> "সোশ্যাল মিডিয়ায় শেয়ার করার জন্য পোস্ট প্রস্তুত করেছি। [ACTION:POST_SOCIAL|share|হ্যালো বন্ধুরা!]"
                    AppLanguage.ENGLISH -> "Prepared a social media post for you. [ACTION:POST_SOCIAL|share|Hello world!]"
                    AppLanguage.HINDI -> "सोशल मीडिया पर साझा करने के लिए पोस्ट तैयार है। [ACTION:POST_SOCIAL|share|नमस्ते दोस्तों!]"
                    AppLanguage.SPANISH -> "Publicación en redes sociales preparada. [ACTION:POST_SOCIAL|share|¡Hola a todos!]"
                }
            }
            lower.contains("ফ্ল্যাশ") || lower.contains("লাইট") || lower.contains("torch") || lower.contains("फ्लैश") || lower.contains("linterna") -> {
                "[ACTION:TOGGLE_TORCH|toggle]"
            }
            lower.contains("রিমাইন্ডার") || lower.contains("মনে করিয়ে") || lower.contains("reminder") || lower.contains("रिमाइंडर") || lower.contains("recordatorio") -> {
                when (language) {
                    AppLanguage.BENGALI -> "আমি আপনার রিমাইন্ডারটি ক্যালেন্ডারে যুক্ত করেছি। [ACTION:ADD_REMINDER|গুরুত্বপূর্ণ কাজ|আজ]"
                    AppLanguage.ENGLISH -> "I have added this reminder to your schedule. [ACTION:ADD_REMINDER|Task|Today]"
                    AppLanguage.HINDI -> "मैंने यह रिमाइंडर आपकी कार्यसूची में जोड़ दिया है। [ACTION:ADD_REMINDER|कार्य|आज]"
                    AppLanguage.SPANISH -> "He añadido este recordatorio a tu calendario. [ACTION:ADD_REMINDER|Tarea|Hoy]"
                }
            }
            else -> {
                when (language) {
                    AppLanguage.BENGALI -> "হ্যালো! আমি সোহান, আপনার ব্যক্তিগত এআই সহকারী। বলুন আমি আপনার জন্য কি করতে পারি? গ্যালারির ছবি খোঁজা, অডিও রূপান্তর, মেসেজ বা কল - যেকোনো নির্দেশ দিতে পারেন।"
                    AppLanguage.ENGLISH -> "Hello! I am Shohan, your personal AI assistant. How can I help you today? Ask me to find photos, transcribe audio, send messages, or place calls."
                    AppLanguage.HINDI -> "नमस्ते! मैं शोहन हूँ, आपका व्यक्तिगत एआई सहायक। मैं आपकी क्या मदद कर सकता हूँ? फोटो ढूंढना, ऑडियो ट्रांसक्रिप्शन, संदेश या कॉल - आप कुछ भी कह सकते हैं।"
                    AppLanguage.SPANISH -> "¡Hola! Soy Shohan, tu asistente personal de IA. ¿En qué te puedo ayudar hoy? Buscar fotos, transcribir audio, enviar mensajes o llamadas."
                }
            }
        }
    }
}
