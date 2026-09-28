package com.example.ui

import android.app.Application
import android.content.Context
import android.net.Uri
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.device.ActionDispatcher
import com.example.data.device.AppLanguage
import com.example.data.device.AppPreferences
import com.example.data.device.DeviceAmbientContext
import com.example.data.device.GalleryHelper
import com.example.data.device.GalleryPhoto
import com.example.data.device.ProactiveEngine
import com.example.data.device.ProactiveSuggestion
import com.example.data.device.ShohanSpeechRecognizer
import com.example.data.device.TextToSpeechManager
import com.example.data.gemini.AssistantResponse
import com.example.data.gemini.GeminiRepository
import com.example.data.local.ChatMessageEntity
import com.example.data.local.ProactiveReminderEntity
import com.example.data.local.ShohanDatabase
import com.example.service.ShohanOverlayService
import com.example.service.ShohanVoiceService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AssistantStatus {
    IDLE,
    LISTENING,
    PROCESSING,
    EXECUTING,
    SPEAKING
}

data class PendingAction(
    val type: String, // "SEND_MESSAGE", "MAKE_CALL", "POST_SOCIAL", "TOGGLE_TORCH", "OPEN_APP", "ADD_REMINDER"
    val target: String = "",
    val content: String = "",
    val previewTitle: String = "",
    val executed: Boolean = false
)

class ShohanViewModel(application: Application) : AndroidViewModel(application) {

    private val db = ShohanDatabase.getInstance(application)
    private val dao = db.shohanDao()
    private val geminiRepo = GeminiRepository()
    val appPreferences = AppPreferences(application)
    private val ttsManager = TextToSpeechManager(application, appPreferences.selectedLanguage)

    val chatMessages: StateFlow<List<ChatMessageEntity>> = dao.getAllMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val reminders: StateFlow<List<ProactiveReminderEntity>> = dao.getAllReminders()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _assistantStatus = MutableStateFlow(AssistantStatus.IDLE)
    val assistantStatus: StateFlow<AssistantStatus> = _assistantStatus.asStateFlow()

    private val _statusMessage = MutableStateFlow("প্রস্তুত")
    val statusMessage: StateFlow<String> = _statusMessage.asStateFlow()

    private val _soundLevel = MutableStateFlow(0f)
    val soundLevel: StateFlow<Float> = _soundLevel.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript: StateFlow<String> = _liveTranscript.asStateFlow()

    private val _latestPhoto = MutableStateFlow<GalleryPhoto?>(null)
    val latestPhoto: StateFlow<GalleryPhoto?> = _latestPhoto.asStateFlow()

    private val _photoAnalysis = MutableStateFlow<String?>(null)
    val photoAnalysis: StateFlow<String?> = _photoAnalysis.asStateFlow()

    private val _isAnalyzingPhoto = MutableStateFlow(false)
    val isAnalyzingPhoto: StateFlow<Boolean> = _isAnalyzingPhoto.asStateFlow()

    private val _pendingAction = MutableStateFlow<PendingAction?>(null)
    val pendingAction: StateFlow<PendingAction?> = _pendingAction.asStateFlow()

    private val _isVoiceWakeActive = MutableStateFlow(false)
    val isVoiceWakeActive: StateFlow<Boolean> = _isVoiceWakeActive.asStateFlow()

    private val _isOverlayActive = MutableStateFlow(false)
    val isOverlayActive: StateFlow<Boolean> = _isOverlayActive.asStateFlow()

    private val _isTtsMuted = MutableStateFlow(appPreferences.isTtsMuted)
    val isTtsMuted: StateFlow<Boolean> = _isTtsMuted.asStateFlow()

    private val _customApiKey = MutableStateFlow(appPreferences.customApiKey)
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    private val _customWakeWord = MutableStateFlow(appPreferences.customWakeWord)
    val customWakeWord: StateFlow<String> = _customWakeWord.asStateFlow()

    private val _selectedLanguage = MutableStateFlow(appPreferences.selectedLanguage)
    val selectedLanguage: StateFlow<AppLanguage> = _selectedLanguage.asStateFlow()

    private val _isProactiveEnabled = MutableStateFlow(appPreferences.isProactiveEnabled)
    val isProactiveEnabled: StateFlow<Boolean> = _isProactiveEnabled.asStateFlow()

    private val _transcribedAudioText = MutableStateFlow<String?>(null)
    val transcribedAudioText: StateFlow<String?> = _transcribedAudioText.asStateFlow()

    private val _isTranscribingAudio = MutableStateFlow(false)
    val isTranscribingAudio: StateFlow<Boolean> = _isTranscribingAudio.asStateFlow()

    // Proactive Context & Suggestions
    private val _ambientContext = MutableStateFlow(
        ProactiveEngine.getDeviceAmbientContext(application, emptyList())
    )
    val ambientContext: StateFlow<DeviceAmbientContext> = _ambientContext.asStateFlow()

    private val _proactiveSuggestions = MutableStateFlow<List<ProactiveSuggestion>>(emptyList())
    val proactiveSuggestions: StateFlow<List<ProactiveSuggestion>> = _proactiveSuggestions.asStateFlow()

    private val _proactiveAiBriefing = MutableStateFlow<String?>(null)
    val proactiveAiBriefing: StateFlow<String?> = _proactiveAiBriefing.asStateFlow()

    private val _isRefreshingBriefing = MutableStateFlow(false)
    val isRefreshingBriefing: StateFlow<Boolean> = _isRefreshingBriefing.asStateFlow()

    private var speechRecognizer: ShohanSpeechRecognizer? = null

    init {
        // Collect TTS status
        viewModelScope.launch {
            ttsManager.isSpeaking.collect { speaking ->
                if (speaking && _assistantStatus.value != AssistantStatus.LISTENING) {
                    _assistantStatus.value = AssistantStatus.SPEAKING
                } else if (!speaking && _assistantStatus.value == AssistantStatus.SPEAKING) {
                    _assistantStatus.value = AssistantStatus.IDLE
                }
            }
        }

        // Welcome greeting if chat is empty
        viewModelScope.launch {
            chatMessages.collect { list ->
                if (list.isEmpty()) {
                    val welcomeMsg = when (appPreferences.selectedLanguage) {
                        AppLanguage.BENGALI -> "আসসালামু আলাইকুম! আমি সোহান (Shohan), আপনার স্মার্ট ব্যক্তিগত এআই সহকারী।\n\nআমাকে যা বলবেন আমি তাই করতে প্রস্তুত:\n• \"Hi Shohan আমাকে গ্যালারি থেকে ছবি বের করে দাও\"\n• \"কাউকে মেসেজ পাঠাও বা কল করো\"\n• \"অডিও ফাইল লিখে দাও\"\n• \"সোশ্যাল মিডিয়ায় পোস্ট করো\"\n• \"কাল সকাল ১০ টায় মিটিং মনে করিয়ে দাও\""
                        AppLanguage.ENGLISH -> "Hello! I am Shohan, your personal AI assistant.\n\nReady for anything you need:\n• \"Hi Shohan, find my latest gallery photo\"\n• \"Send a message or place a call\"\n• \"Transcribe this audio file into text\"\n• \"Post to social media\"\n• \"Remind me of my meeting at 10 AM\""
                        AppLanguage.HINDI -> "नमस्ते! मैं शोहन हूँ, आपका व्यक्तिगत एआई सहायक।\n\nमैं आपकी सहायता के लिए तैयार हूँ:\n• \"शोहन, गैलरी से नवीनतम फोटो दिखाओ\"\n• \"संदेश भेजें या कॉल करें\"\n• \"ऑडियो फाइल को टेक्स्ट में बदलें\"\n• \"कैलेंडर और कार्यों का प्रबंधन करें\""
                        AppLanguage.SPANISH -> "¡Hola! Soy Shohan, tu asistente personal de IA.\n\nListo para ayudarte:\n• \"Shohan, busca la última foto de mi galería\"\n• \"Enviar un mensaje o hacer una llamada\"\n• \"Transcribir un archivo de audio a texto\"\n• \"Crear recordatorios y organizar tu día\""
                    }

                    dao.insertMessage(
                        ChatMessageEntity(
                            role = "assistant",
                            text = welcomeMsg
                        )
                    )
                }
            }
        }

        // Seed initial smart routines & reminders if none exist
        viewModelScope.launch {
            reminders.collect { list ->
                if (list.isEmpty()) {
                    dao.insertReminder(
                        ProactiveReminderEntity(
                            title = "সকালের অগ্রাধিকার ও কাজের পরিকল্পনা",
                            timeLabel = "09:30 AM",
                            category = "ROUTINE",
                            isCompleted = false
                        )
                    )
                    dao.insertReminder(
                        ProactiveReminderEntity(
                            title = "পানি পানের বিরতি ও স্বাস্থ্য সচেতনতা",
                            timeLabel = "02:00 PM",
                            category = "HEALTH",
                            isCompleted = false
                        )
                    )
                    dao.insertReminder(
                        ProactiveReminderEntity(
                            title = "আজকের কাজের অগ্রগতি পর্যালোচনা",
                            timeLabel = "07:30 PM",
                            category = "TASK",
                            isCompleted = false
                        )
                    )
                }
                updateProactiveState(list)
            }
        }

        _isVoiceWakeActive.value = ShohanVoiceService.isServiceRunning
        _isOverlayActive.value = ShohanOverlayService.isOverlayRunning
    }

    private fun updateProactiveState(currentReminders: List<ProactiveReminderEntity>) {
        val ambient = ProactiveEngine.getDeviceAmbientContext(getApplication(), currentReminders.filter { !it.isCompleted })
        _ambientContext.value = ambient
        _proactiveSuggestions.value = ProactiveEngine.generateContextualSuggestions(
            ambient = ambient,
            language = _selectedLanguage.value,
            pendingReminders = currentReminders.filter { !it.isCompleted }
        )
    }

    fun initSpeechRecognizer(context: Context) {
        if (speechRecognizer == null) {
            speechRecognizer = ShohanSpeechRecognizer(
                context = context,
                customWakeWord = _customWakeWord.value,
                currentLanguage = _selectedLanguage.value,
                onWakeWordDetected = { commandAfterWake ->
                    _statusMessage.value = "${_customWakeWord.value} শনাক্ত হয়েছে!"
                    speechRecognizer?.triggerHapticFeedback()
                    if (commandAfterWake.isNullOrBlank()) {
                        val wakeAck = when (_selectedLanguage.value) {
                            AppLanguage.BENGALI -> "হ্যাঁ বলুন, আমি শুনছি!"
                            AppLanguage.ENGLISH -> "Yes, I am listening!"
                            AppLanguage.HINDI -> "हाँ, मैं सुन रहा हूँ!"
                            AppLanguage.SPANISH -> "¡Sí, te escucho!"
                        }
                        speak(wakeAck)
                    }
                },
                onFinalCommand = { command ->
                    if (command.isNotBlank()) {
                        handleUserPrompt(command)
                    }
                },
                onErrorOccurred = { msg ->
                    _statusMessage.value = msg
                    _assistantStatus.value = AssistantStatus.IDLE
                }
            )

            viewModelScope.launch {
                speechRecognizer?.soundLevel?.collect { level ->
                    _soundLevel.value = level
                }
            }
            viewModelScope.launch {
                speechRecognizer?.liveTranscript?.collect { text ->
                    _liveTranscript.value = text
                }
            }
            viewModelScope.launch {
                speechRecognizer?.isListening?.collect { listening ->
                    if (listening) {
                        _assistantStatus.value = AssistantStatus.LISTENING
                        _statusMessage.value = when (_selectedLanguage.value) {
                            AppLanguage.BENGALI -> "শুনছি... বলুন..."
                            AppLanguage.ENGLISH -> "Listening... please speak..."
                            AppLanguage.HINDI -> "सुन रहा हूँ... बोलिए..."
                            AppLanguage.SPANISH -> "Escuchando... habla..."
                        }
                    } else if (_assistantStatus.value == AssistantStatus.LISTENING) {
                        _assistantStatus.value = AssistantStatus.IDLE
                        _statusMessage.value = "প্রস্তুত"
                    }
                }
            }
        }
    }

    fun startListening(continuous: Boolean = false) {
        ttsManager.stop()
        speechRecognizer?.startListening(continuous)
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _assistantStatus.value = AssistantStatus.IDLE
        _soundLevel.value = 0f
    }

    fun toggleTapToTalk() {
        if (_assistantStatus.value == AssistantStatus.LISTENING) {
            stopListening()
        } else {
            startListening(continuous = false)
        }
    }

    fun handleUserPrompt(promptText: String) {
        val trimmed = promptText.trim()
        if (trimmed.isBlank()) return

        stopListening()
        _liveTranscript.value = ""

        viewModelScope.launch {
            dao.insertMessage(
                ChatMessageEntity(
                    role = "user",
                    text = trimmed
                )
            )

            _assistantStatus.value = AssistantStatus.PROCESSING
            _statusMessage.value = when (_selectedLanguage.value) {
                AppLanguage.BENGALI -> "সোহান ভাবছে..."
                AppLanguage.ENGLISH -> "Shohan is thinking..."
                AppLanguage.HINDI -> "शोहन सोच रहा है..."
                AppLanguage.SPANISH -> "Shohan está pensando..."
            }

            val history = chatMessages.value.takeLast(6).map { it.role to it.text }

            val response: AssistantResponse = geminiRepo.generateAssistantReply(
                userMessage = trimmed,
                conversationHistory = history,
                customApiKey = _customApiKey.value.ifBlank { null },
                language = _selectedLanguage.value
            )

            _assistantStatus.value = AssistantStatus.IDLE
            _statusMessage.value = "উত্তর সম্পন্ন"

            dao.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    text = response.replyText,
                    actionType = response.actionType,
                    actionPayload = response.actionPayload
                )
            )

            if (!_isTtsMuted.value) {
                ttsManager.speak(response.replyText)
            }

            if (response.actionType != null) {
                handleActionTag(response.actionType, response.actionPayload)
            }
        }
    }

    private fun handleActionTag(actionType: String, payload: String?) {
        when (actionType.uppercase()) {
            "FIND_LATEST_PHOTO" -> {
                fetchLatestGalleryPhoto()
            }
            "TRANSCRIBE_AUDIO" -> {
                _statusMessage.value = "অডিও ট্রান্সক্রিপশন মোড প্রস্তুত"
            }
            "SEND_MESSAGE" -> {
                val parts = payload?.split("|") ?: emptyList()
                val recipient = parts.getOrNull(0)?.trim() ?: ""
                val msg = parts.getOrNull(1)?.trim() ?: "হ্যালো!"
                _pendingAction.value = PendingAction(
                    type = "SEND_MESSAGE",
                    target = recipient,
                    content = msg,
                    previewTitle = when (_selectedLanguage.value) {
                        AppLanguage.BENGALI -> "মেসেজ পাঠানোর প্রস্তুতি"
                        AppLanguage.ENGLISH -> "Draft Message"
                        AppLanguage.HINDI -> "संदेश भेजने की तैयारी"
                        AppLanguage.SPANISH -> "Borrador de mensaje"
                    }
                )
            }
            "MAKE_CALL" -> {
                val target = payload?.trim() ?: ""
                _pendingAction.value = PendingAction(
                    type = "MAKE_CALL",
                    target = target,
                    content = "কল করা হবে: $target",
                    previewTitle = when (_selectedLanguage.value) {
                        AppLanguage.BENGALI -> "ফোন কল প্রস্তুতি"
                        AppLanguage.ENGLISH -> "Outgoing Call"
                        AppLanguage.HINDI -> "फोन कॉल तैयारी"
                        AppLanguage.SPANISH -> "Llamada telefónica"
                    }
                )
            }
            "POST_SOCIAL" -> {
                val parts = payload?.split("|") ?: emptyList()
                val platform = parts.getOrNull(0)?.trim() ?: "share"
                val text = parts.getOrNull(1)?.trim() ?: ""
                _pendingAction.value = PendingAction(
                    type = "POST_SOCIAL",
                    target = platform,
                    content = text,
                    previewTitle = when (_selectedLanguage.value) {
                        AppLanguage.BENGALI -> "সোশ্যাল মিডিয়ায় পোস্ট"
                        AppLanguage.ENGLISH -> "Social Media Post"
                        AppLanguage.HINDI -> "सोशल मीडिया पोस्ट"
                        AppLanguage.SPANISH -> "Publicar en redes sociales"
                    }
                )
            }
            "TOGGLE_TORCH" -> {
                val newState = ActionDispatcher.toggleTorch(getApplication())
                val stateText = when (_selectedLanguage.value) {
                    AppLanguage.BENGALI -> if (newState) "ফ্ল্যাশলাইট চালু হয়েছে" else "ফ্ল্যাশলাইট বন্ধ হয়েছে"
                    AppLanguage.ENGLISH -> if (newState) "Flashlight turned on" else "Flashlight turned off"
                    AppLanguage.HINDI -> if (newState) "टॉर्च चालू हो गई" else "टॉर्च बंद हो गई"
                    AppLanguage.SPANISH -> if (newState) "Linterna encendida" else "Linterna apagada"
                }
                speak(stateText)
            }
            "ADD_REMINDER" -> {
                val parts = payload?.split("|") ?: emptyList()
                val title = parts.getOrNull(0)?.trim() ?: "রিমাইন্ডার"
                val time = parts.getOrNull(1)?.trim() ?: "আজ"
                addReminder(title, time, "CALENDAR")
            }
            "OPEN_APP" -> {
                val app = payload?.trim() ?: ""
                val opened = ActionDispatcher.openApp(getApplication(), app)
                if (opened) {
                    speak("$app ওপেন করছি")
                }
            }
        }
    }

    fun fetchLatestGalleryPhoto() {
        val photo = GalleryHelper.getLatestPhoto(getApplication())
        _latestPhoto.value = photo
        _photoAnalysis.value = null
        if (photo != null) {
            val msg = when (_selectedLanguage.value) {
                AppLanguage.BENGALI -> "আপনার গ্যালারির সর্বশেষ ছবিটি পাওয়া গেছে: ${photo.displayName}"
                AppLanguage.ENGLISH -> "Found your latest gallery photo: ${photo.displayName}"
                AppLanguage.HINDI -> "आपकी गैलरी की नवीनतम तस्वीर मिली: ${photo.displayName}"
                AppLanguage.SPANISH -> "Se encontró la foto más reciente: ${photo.displayName}"
            }
            speak(msg)
        } else {
            val msg = when (_selectedLanguage.value) {
                AppLanguage.BENGALI -> "গ্যালারিতে কোনো ছবি পাওয়া যায়নি অথবা পারমিশন প্রয়োজন।"
                AppLanguage.ENGLISH -> "No photos found or storage permission is required."
                AppLanguage.HINDI -> "कोई तस्वीर नहीं मिली या अनुमति आवश्यक है।"
                AppLanguage.SPANISH -> "No se encontraron fotos o se requiere permiso."
            }
            speak(msg)
        }
    }

    fun analyzeLatestPhotoWithGemini() {
        val photo = _latestPhoto.value ?: return
        val context = getApplication<Application>()
        val bitmap = GalleryHelper.loadBitmapFromUri(context, photo.uri)

        if (bitmap == null) {
            _photoAnalysis.value = "ছবিটি লোড করা যায়নি।"
            return
        }

        viewModelScope.launch {
            _isAnalyzingPhoto.value = true
            _assistantStatus.value = AssistantStatus.PROCESSING
            _statusMessage.value = "ছবিটি বিশ্লেষণ করা হচ্ছে..."

            val analysis = geminiRepo.analyzeImage(
                bitmap = bitmap,
                customApiKey = _customApiKey.value.ifBlank { null },
                language = _selectedLanguage.value
            )

            _isAnalyzingPhoto.value = false
            _assistantStatus.value = AssistantStatus.IDLE
            _photoAnalysis.value = analysis

            if (!_isTtsMuted.value) {
                ttsManager.speak(analysis)
            }
        }
    }

    fun transcribeAudioUri(uri: Uri) {
        val context = getApplication<Application>()
        viewModelScope.launch {
            _isTranscribingAudio.value = true
            _statusMessage.value = "অডিও ট্রান্সক্রাইব করা হচ্ছে..."

            val bytes = try {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            } catch (e: Exception) {
                null
            }

            if (bytes == null || bytes.isEmpty()) {
                _transcribedAudioText.value = "অডিও ফাইল পড়া যায়নি।"
                _isTranscribingAudio.value = false
                return@launch
            }

            val textResult = geminiRepo.transcribeAudioBytes(
                audioBytes = bytes,
                customApiKey = _customApiKey.value.ifBlank { null },
                language = _selectedLanguage.value
            )

            _transcribedAudioText.value = textResult
            _isTranscribingAudio.value = false
            _statusMessage.value = "অডিও রূপান্তর সম্পন্ন"

            dao.insertMessage(
                ChatMessageEntity(
                    role = "assistant",
                    text = "🎙️ অডিও ট্রান্সক্রিপশন:\n\n$textResult",
                    actionType = "AUDIO_TRANSCRIBE",
                    actionPayload = textResult
                )
            )

            if (!_isTtsMuted.value) {
                val confirmVoice = when (_selectedLanguage.value) {
                    AppLanguage.BENGALI -> "অডিওটি সফলভাবে টেক্সটে রূপান্তর করা হয়েছে।"
                    AppLanguage.ENGLISH -> "Your audio has been transcribed into text."
                    AppLanguage.HINDI -> "ऑडियो को सफलतापूर्वक टेक्स्ट में बदल दिया गया है।"
                    AppLanguage.SPANISH -> "Tu audio ha sido transcrito a texto."
                }
                ttsManager.speak(confirmVoice)
            }
        }
    }

    fun refreshProactiveBriefing() {
        viewModelScope.launch {
            _isRefreshingBriefing.value = true
            val briefing = geminiRepo.generateProactiveBriefing(
                ambient = _ambientContext.value,
                reminders = reminders.value,
                language = _selectedLanguage.value,
                customApiKey = _customApiKey.value.ifBlank { null }
            )
            _proactiveAiBriefing.value = briefing
            _isRefreshingBriefing.value = false
            if (!_isTtsMuted.value) {
                speak(briefing)
            }
        }
    }

    fun setCustomWakeWord(newWakeWord: String) {
        val trimmed = newWakeWord.trim().ifBlank { AppPreferences.DEFAULT_WAKE_WORD }
        appPreferences.customWakeWord = trimmed
        _customWakeWord.value = trimmed
        speechRecognizer?.updateCustomWakeWord(trimmed)

        // If voice wake service is running, restart it to load new wake word
        if (_isVoiceWakeActive.value) {
            val ctx = getApplication<Application>()
            ShohanVoiceService.stop(ctx)
            ShohanVoiceService.start(ctx)
        }
    }

    fun setSelectedLanguage(language: AppLanguage) {
        appPreferences.selectedLanguage = language
        _selectedLanguage.value = language
        speechRecognizer?.updateLanguage(language)
        ttsManager.setLanguage(language)

        updateProactiveState(reminders.value)

        // Speak language confirmation in the new language
        speak(language.sampleGreeting)
    }

    fun addReminder(title: String, timeLabel: String, category: String = "TASK") {
        viewModelScope.launch {
            dao.insertReminder(
                ProactiveReminderEntity(
                    title = title,
                    timeLabel = timeLabel,
                    category = category
                )
            )
            val msg = when (_selectedLanguage.value) {
                AppLanguage.BENGALI -> "\"$title\" ($timeLabel) রিমাইন্ডার যুক্ত হয়েছে।"
                AppLanguage.ENGLISH -> "Added reminder: \"$title\" ($timeLabel)."
                AppLanguage.HINDI -> "रिमाइंडर जोड़ा गया: \"$title\" ($timeLabel)"
                AppLanguage.SPANISH -> "Recordatorio añadido: \"$title\" ($timeLabel)"
            }
            _statusMessage.value = msg
            speak(msg)
        }
    }

    fun toggleReminderCompletion(id: Long, isCompleted: Boolean) {
        viewModelScope.launch {
            dao.updateReminderStatus(id, isCompleted)
        }
    }

    fun deleteReminder(id: Long) {
        viewModelScope.launch {
            dao.deleteReminder(id)
        }
    }

    fun executePendingAction(context: Context) {
        val action = _pendingAction.value ?: return
        when (action.type) {
            "SEND_MESSAGE" -> {
                ActionDispatcher.sendSms(context, action.target, action.content)
            }
            "MAKE_CALL" -> {
                ActionDispatcher.makeCall(context, action.target)
            }
            "POST_SOCIAL" -> {
                ActionDispatcher.postSocial(context, action.target, action.content)
            }
        }
        _pendingAction.value = null
    }

    fun dismissPendingAction() {
        _pendingAction.value = null
    }

    fun toggleVoiceWakeService(context: Context) {
        val newState = !_isVoiceWakeActive.value
        _isVoiceWakeActive.value = newState
        if (newState) {
            ShohanVoiceService.start(context)
            _statusMessage.value = "\"${_customWakeWord.value}\" সর্বত্র প্রস্তুত"
        } else {
            ShohanVoiceService.stop(context)
            _statusMessage.value = "ভয়েস ওয়েক বন্ধ করা হয়েছে"
        }
    }

    fun toggleOverlayService(context: Context) {
        if (!Settings.canDrawOverlays(context)) {
            _isOverlayActive.value = false
            return
        }
        val newState = !_isOverlayActive.value
        _isOverlayActive.value = newState
        if (newState) {
            ShohanOverlayService.start(context)
        } else {
            ShohanOverlayService.stop(context)
        }
    }

    fun toggleTtsMute() {
        val muted = !_isTtsMuted.value
        appPreferences.isTtsMuted = muted
        _isTtsMuted.value = muted
        if (muted) {
            ttsManager.stop()
        }
    }

    fun setCustomApiKey(key: String) {
        val trimmed = key.trim()
        appPreferences.customApiKey = trimmed
        _customApiKey.value = trimmed
    }

    fun clearChatHistory() {
        viewModelScope.launch {
            dao.clearAll()
        }
    }

    fun speak(text: String) {
        if (!_isTtsMuted.value) {
            ttsManager.speak(text)
        }
    }

    override fun onCleared() {
        super.onCleared()
        speechRecognizer?.destroy()
        ttsManager.shutdown()
    }
}
