package com.example.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.device.AppLanguage
import com.example.ui.components.AddReminderDialog
import com.example.ui.components.AiOrbVisualizer
import com.example.ui.components.AudioTranscriberCard
import com.example.ui.components.LatestPhotoCard
import com.example.ui.components.MessageBubble
import com.example.ui.components.PendingActionCard
import com.example.ui.components.ProactiveSuggestionBanner
import com.example.ui.components.QuickCommandsRow
import com.example.ui.components.SettingsDialog
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShohanMainScreen(
    viewModel: ShohanViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val messages by viewModel.chatMessages.collectAsState()
    val assistantStatus by viewModel.assistantStatus.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()
    val soundLevel by viewModel.soundLevel.collectAsState()
    val liveTranscript by viewModel.liveTranscript.collectAsState()
    val latestPhoto by viewModel.latestPhoto.collectAsState()
    val photoAnalysis by viewModel.photoAnalysis.collectAsState()
    val isAnalyzingPhoto by viewModel.isAnalyzingPhoto.collectAsState()
    val pendingAction by viewModel.pendingAction.collectAsState()
    val isVoiceWakeActive by viewModel.isVoiceWakeActive.collectAsState()
    val isOverlayActive by viewModel.isOverlayActive.collectAsState()
    val isTtsMuted by viewModel.isTtsMuted.collectAsState()
    val customApiKey by viewModel.customApiKey.collectAsState()
    val customWakeWord by viewModel.customWakeWord.collectAsState()
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val isProactiveEnabled by viewModel.isProactiveEnabled.collectAsState()
    val reminders by viewModel.reminders.collectAsState()
    val ambientContext by viewModel.ambientContext.collectAsState()
    val proactiveSuggestions by viewModel.proactiveSuggestions.collectAsState()
    val proactiveAiBriefing by viewModel.proactiveAiBriefing.collectAsState()
    val isRefreshingBriefing by viewModel.isRefreshingBriefing.collectAsState()
    val transcribedResult by viewModel.transcribedAudioText.collectAsState()
    val isTranscribingAudio by viewModel.isTranscribingAudio.collectAsState()

    var textInput by remember { mutableStateOf("") }
    var showSettings by remember { mutableStateOf(false) }
    var showAudioCard by remember { mutableStateOf(false) }
    var showAddReminderDialog by remember { mutableStateOf(false) }
    var showLanguageMenu by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()

    // Auto-scroll on new message
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    // Permission launcher for microphone and media
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val recordAudioGranted = permissions[Manifest.permission.RECORD_AUDIO] ?: false
        if (recordAudioGranted) {
            viewModel.initSpeechRecognizer(context)
        } else {
            Toast.makeText(context, "মাইক্রোফোন পারমিশন প্রয়োজন", Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_IMAGES)
            permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val missing = permissionsToRequest.filter {
            ContextCompat.checkSelfPermission(context, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missing.isNotEmpty()) {
            permissionLauncher.launch(missing.toTypedArray())
        } else {
            viewModel.initSpeechRecognizer(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Shohan AI",
                            style = MaterialTheme.typography.titleLarge,
                            color = NeonCyan,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {
                                viewModel.toggleVoiceWakeService(context)
                            },
                            label = {
                                Text(
                                    text = if (isVoiceWakeActive) "$customWakeWord: অন" else "$customWakeWord: অফ",
                                    fontSize = 11.sp,
                                    color = if (isVoiceWakeActive) EmeraldGlow else TextMuted
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isVoiceWakeActive) Icons.Default.GraphicEq else Icons.Default.MicOff,
                                    contentDescription = null,
                                    tint = if (isVoiceWakeActive) EmeraldGlow else TextMuted,
                                    modifier = Modifier.size(13.dp)
                                )
                            },
                            colors = AssistChipDefaults.assistChipColors(
                                containerColor = DarkCardSurface
                            ),
                            border = AssistChipDefaults.assistChipBorder(
                                enabled = true,
                                borderColor = if (isVoiceWakeActive) EmeraldGlow.copy(alpha = 0.5f) else DarkSurfaceVariant,
                                borderWidth = 1.dp
                            ),
                            modifier = Modifier.height(28.dp).testTag("wake_word_status_chip")
                        )
                    }
                },
                actions = {
                    // Quick Language Switcher Dropdown
                    Box {
                        IconButton(
                            onClick = { showLanguageMenu = true },
                            modifier = Modifier.testTag("language_switch_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = "Language",
                                tint = NeonCyan
                            )
                        }

                        DropdownMenu(
                            expanded = showLanguageMenu,
                            onDismissRequest = { showLanguageMenu = false },
                            modifier = Modifier.background(DarkCardSurface)
                        ) {
                            AppLanguage.values().forEach { lang ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = "${lang.nativeName} (${lang.displayName})",
                                            color = if (selectedLanguage == lang) NeonCyan else TextPrimary,
                                            fontWeight = if (selectedLanguage == lang) FontWeight.Bold else FontWeight.Normal
                                        )
                                    },
                                    onClick = {
                                        viewModel.setSelectedLanguage(lang)
                                        showLanguageMenu = false
                                    }
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = { viewModel.toggleTtsMute() },
                        modifier = Modifier.testTag("tts_mute_button")
                    ) {
                        Icon(
                            imageVector = if (isTtsMuted) Icons.Default.VolumeOff else Icons.Default.VolumeUp,
                            contentDescription = "TTS Mute Toggle",
                            tint = if (isTtsMuted) TextMuted else NeonCyan
                        )
                    }

                    IconButton(
                        onClick = { showSettings = true },
                        modifier = Modifier.testTag("settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Interactive Neural Orb & Visualizer Section
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AiOrbVisualizer(
                        status = assistantStatus,
                        soundLevel = soundLevel,
                        onClick = {
                            viewModel.toggleTapToTalk()
                        }
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = if (liveTranscript.isNotBlank()) liveTranscript else statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (liveTranscript.isNotBlank()) NeonCyan else TextSecondary,
                        fontWeight = if (liveTranscript.isNotBlank()) FontWeight.Bold else FontWeight.Normal,
                        fontSize = 12.sp,
                        maxLines = 2,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }
            }

            // Proactive Contextual Suggestions & Calendar Schedule Banner
            if (isProactiveEnabled) {
                ProactiveSuggestionBanner(
                    ambient = ambientContext,
                    suggestions = proactiveSuggestions,
                    reminders = reminders,
                    language = selectedLanguage,
                    aiBriefing = proactiveAiBriefing,
                    isRefreshingBriefing = isRefreshingBriefing,
                    onRefreshBriefing = { viewModel.refreshProactiveBriefing() },
                    onSuggestionClick = { prompt ->
                        viewModel.handleUserPrompt(prompt)
                    },
                    onToggleReminder = { id, done ->
                        viewModel.toggleReminderCompletion(id, done)
                    },
                    onDeleteReminder = { id ->
                        viewModel.deleteReminder(id)
                    },
                    onOpenAddReminder = {
                        showAddReminderDialog = true
                    }
                )
            }

            // Language-Aware Quick Commands Row
            QuickCommandsRow(
                language = selectedLanguage,
                onCommandClick = { prompt ->
                    if (prompt.contains("অডিও") || prompt.contains("audio") || prompt.contains("ऑडियो")) {
                        showAudioCard = true
                    }
                    viewModel.handleUserPrompt(prompt)
                }
            )

            // Dynamic Action Cards Area (Photo, Pending Action, Audio)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp)
            ) {
                latestPhoto?.let { photo ->
                    Spacer(modifier = Modifier.height(6.dp))
                    LatestPhotoCard(
                        photo = photo,
                        isAnalyzing = isAnalyzingPhoto,
                        analysisResult = photoAnalysis,
                        onAnalyzeClick = { viewModel.analyzeLatestPhotoWithGemini() },
                        onDismiss = { /* keep */ }
                    )
                }

                pendingAction?.let { action ->
                    Spacer(modifier = Modifier.height(6.dp))
                    PendingActionCard(
                        action = action,
                        onConfirm = { viewModel.executePendingAction(context) },
                        onDismiss = { viewModel.dismissPendingAction() }
                    )
                }

                AnimatedVisibility(visible = showAudioCard) {
                    Spacer(modifier = Modifier.height(6.dp))
                    AudioTranscriberCard(
                        isTranscribing = isTranscribingAudio,
                        transcribedResult = transcribedResult,
                        onPickAudio = { uri ->
                            viewModel.transcribeAudioUri(uri)
                        }
                    )
                }
            }

            // Chat Messages Timeline
            LazyColumn(
                state = listState,
                contentPadding = PaddingValues(top = 6.dp, bottom = 6.dp),
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(messages, key = { it.id }) { msg ->
                    MessageBubble(
                        message = msg,
                        onSpeakClick = { text -> viewModel.speak(text) }
                    )
                }
            }

            // Bottom Input Bar
            Surface(
                color = DarkSurface,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    val inputPlaceholder = when (selectedLanguage) {
                        AppLanguage.BENGALI -> "\"$customWakeWord\" বলুন বা লিখুন..."
                        AppLanguage.ENGLISH -> "Say \"$customWakeWord\" or type..."
                        AppLanguage.HINDI -> "\"$customWakeWord\" बोलें या टाइप करें..."
                        AppLanguage.SPANISH -> "Di \"$customWakeWord\" o escribe..."
                    }

                    OutlinedTextField(
                        value = textInput,
                        onValueChange = { textInput = it },
                        placeholder = {
                            Text(
                                inputPlaceholder,
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = NeonCyan,
                            unfocusedBorderColor = DarkSurfaceVariant,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = DarkCardSurface,
                            unfocusedContainerColor = DarkCardSurface
                        ),
                        shape = RoundedCornerShape(24.dp),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_input_field")
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    if (textInput.isNotBlank()) {
                        IconButton(
                            onClick = {
                                val text = textInput
                                textInput = ""
                                viewModel.handleUserPrompt(text)
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .background(NeonCyan, CircleShape)
                                .testTag("send_message_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = "Send",
                                tint = DarkBackground,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        IconButton(
                            onClick = { viewModel.toggleTapToTalk() },
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    if (assistantStatus == AssistantStatus.LISTENING) Color.Red else ElectricBlue,
                                    CircleShape
                                )
                                .testTag("mic_talk_button")
                        ) {
                            Icon(
                                imageVector = if (assistantStatus == AssistantStatus.LISTENING) Icons.Default.Mic else Icons.Default.MicOff,
                                contentDescription = "Voice Input",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSettings) {
        SettingsDialog(
            isVoiceWakeActive = isVoiceWakeActive,
            isOverlayActive = isOverlayActive,
            isTtsMuted = isTtsMuted,
            customApiKey = customApiKey,
            customWakeWord = customWakeWord,
            selectedLanguage = selectedLanguage,
            onToggleVoiceWake = { viewModel.toggleVoiceWakeService(context) },
            onToggleOverlay = { viewModel.toggleOverlayService(context) },
            onToggleTts = { viewModel.toggleTtsMute() },
            onSaveApiKey = { key -> viewModel.setCustomApiKey(key) },
            onSaveWakeWord = { newWord -> viewModel.setCustomWakeWord(newWord) },
            onSelectLanguage = { lang -> viewModel.setSelectedLanguage(lang) },
            onClearChat = { viewModel.clearChatHistory() },
            onDismiss = { showSettings = false }
        )
    }

    if (showAddReminderDialog) {
        AddReminderDialog(
            onAdd = { title, time, cat ->
                viewModel.addReminder(title, time, cat)
            },
            onDismiss = { showAddReminderDialog = false }
        )
    }
}
