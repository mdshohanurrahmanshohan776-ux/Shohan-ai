package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.AppLanguage
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun SettingsDialog(
    isVoiceWakeActive: Boolean,
    isOverlayActive: Boolean,
    isTtsMuted: Boolean,
    customApiKey: String,
    customWakeWord: String,
    selectedLanguage: AppLanguage,
    onToggleVoiceWake: () -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleTts: () -> Unit,
    onSaveApiKey: (String) -> Unit,
    onSaveWakeWord: (String) -> Unit,
    onSelectLanguage: (AppLanguage) -> Unit,
    onClearChat: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var apiKeyInput by remember { mutableStateOf(customApiKey) }
    var wakeWordInput by remember { mutableStateOf(customWakeWord) }

    val suggestedWakeWords = listOf(
        "Hi Shohan",
        "Hey Jarvis",
        "শুনুন",
        "সোহান ভাই",
        "नमस्ते शोहन",
        "Hola Shohan"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    onSaveApiKey(apiKeyInput)
                    if (wakeWordInput.isNotBlank()) {
                        onSaveWakeWord(wakeWordInput)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_settings_button")
            ) {
                Text("সংরক্ষণ করুন")
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("বন্ধ করুন", color = TextSecondary)
            }
        },
        title = {
            Text(
                text = "Shohan AI সেটিংস",
                color = NeonCyan,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                // SECTION 1: CUSTOM WAKE WORD (কাস্টমাইজযোগ্য অ্যাক্টিভেশন শব্দ)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.RecordVoiceOver,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "কাস্টমাইজযোগ্য অ্যাক্টিভেশন শব্দ",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "আপনি যে শব্দ বা বাক্য দিয়ে ডাকলে অ্যাসিস্ট্যান্ট চালু হবে:",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = wakeWordInput,
                    onValueChange = { wakeWordInput = it },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        focusedContainerColor = DarkSurfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("custom_wake_word_input")
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Quick Suggestion Chips for wake words
                Text(
                    text = "পছন্দের শব্দ বেছে নিন:",
                    color = TextSecondary,
                    fontSize = 10.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestedWakeWords.take(3).forEach { phrase ->
                        FilterChip(
                            selected = wakeWordInput.equals(phrase, ignoreCase = true),
                            onClick = { wakeWordInput = phrase },
                            label = { Text(phrase, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue,
                                containerColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    suggestedWakeWords.drop(3).forEach { phrase ->
                        FilterChip(
                            selected = wakeWordInput.equals(phrase, ignoreCase = true),
                            onClick = { wakeWordInput = phrase },
                            label = { Text(phrase, fontSize = 10.sp) },
                            shape = RoundedCornerShape(8.dp),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = ElectricBlue,
                                containerColor = DarkSurfaceVariant
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 2: MULTI-LANGUAGE SELECTION (একাধিক ভাষার সমর্থন)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = CyberPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ভাষা নির্বাচন (Multi-Language)",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "ভয়েস কমান্ড ও কথা বলার ভাষা নির্ধারণ করুন:",
                    color = TextMuted,
                    fontSize = 11.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    AppLanguage.values().forEach { lang ->
                        val isSelected = selectedLanguage == lang
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) ElectricBlue.copy(alpha = 0.35f) else DarkSurfaceVariant.copy(alpha = 0.4f))
                                .border(
                                    1.dp,
                                    if (isSelected) NeonCyan else Color.Transparent,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { onSelectLanguage(lang) }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                                .testTag("language_option_${lang.code}")
                        ) {
                            Column {
                                Text(
                                    text = "${lang.nativeName} (${lang.displayName})",
                                    color = if (isSelected) NeonCyan else TextPrimary,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = lang.sampleGreeting,
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                            }
                            if (isSelected) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(NeonCyan)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 3: Background Voice Wake & Overlay Controls
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ভয়েস ওয়েক সর্বত্র সক্রিয়",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "যেকোনো অ্যাপে থাকা অবস্থায় '$wakeWordInput' শুনবে",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isVoiceWakeActive,
                        onCheckedChange = { onToggleVoiceWake() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = ElectricBlue
                        ),
                        modifier = Modifier.testTag("wake_word_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 4: Floating Assistant Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Layers,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ফ্লোটিং অ্যাসিস্ট্যান্ট বাটন",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "স্ক্রিনে ভাসমান সোহান বাবল প্রদর্শন করবে",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = isOverlayActive,
                        onCheckedChange = {
                            if (!Settings.canDrawOverlays(context)) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                )
                                context.startActivity(intent)
                            } else {
                                onToggleOverlay()
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = ElectricBlue
                        ),
                        modifier = Modifier.testTag("overlay_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 5: Spoken Voice Response
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VolumeUp,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ভয়েস প্রতিক্রিয়া (TTS)",
                                color = TextPrimary,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = if (isTtsMuted) "শব্দ বন্ধ (Muted)" else "সোহান ভয়েসে উত্তর শোনাবে",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = !isTtsMuted,
                        onCheckedChange = { onToggleTts() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = NeonCyan,
                            checkedTrackColor = ElectricBlue
                        ),
                        modifier = Modifier.testTag("tts_switch")
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = DarkSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 6: Gemini API Key Override
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Gemini API Key (ঐচ্ছিক)",
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                OutlinedTextField(
                    value = apiKeyInput,
                    onValueChange = { apiKeyInput = it },
                    placeholder = { Text("AIzaSy...", color = TextMuted, fontSize = 12.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NeonCyan,
                        unfocusedBorderColor = DarkSurfaceVariant,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("api_key_input")
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = DarkSurfaceVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // SECTION 7: Clear Chat History
                Button(
                    onClick = onClearChat,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444).copy(alpha = 0.8f)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth().testTag("clear_history_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteSweep,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("চ্যাট হিস্টোরি মুছে ফেলুন", fontSize = 12.sp)
                }
            }
        },
        containerColor = DarkCardSurface,
        shape = RoundedCornerShape(20.dp)
    )
}
