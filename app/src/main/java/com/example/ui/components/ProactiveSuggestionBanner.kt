package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Nightlight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.AppLanguage
import com.example.data.device.DeviceAmbientContext
import com.example.data.device.ProactiveSuggestion
import com.example.data.local.ProactiveReminderEntity
import com.example.ui.theme.CyberPurple
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.EmeraldGlow
import com.example.ui.theme.GlowingPink
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun ProactiveSuggestionBanner(
    ambient: DeviceAmbientContext,
    suggestions: List<ProactiveSuggestion>,
    reminders: List<ProactiveReminderEntity>,
    language: AppLanguage,
    aiBriefing: String?,
    isRefreshingBriefing: Boolean,
    onRefreshBriefing: () -> Unit,
    onSuggestionClick: (String) -> Unit,
    onToggleReminder: (Long, Boolean) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onOpenAddReminder: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpandedSchedule by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .testTag("proactive_suggestion_banner")
    ) {
        // Context Header Bar
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = DarkCardSurface,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonCyan.copy(alpha = 0.25f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val periodIcon = when (ambient.timeOfDay) {
                            "MORNING" -> Icons.Default.WbSunny
                            "AFTERNOON" -> Icons.Default.LightMode
                            "EVENING" -> Icons.Default.WbSunny
                            else -> Icons.Default.Nightlight
                        }
                        Icon(
                            imageVector = periodIcon,
                            contentDescription = null,
                            tint = NeonCyan,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (language) {
                                AppLanguage.BENGALI -> "${ambient.timeGreetingBn} • ${ambient.formattedCurrentTime}"
                                AppLanguage.ENGLISH -> "${ambient.timeGreetingEn} • ${ambient.formattedCurrentTime}"
                                AppLanguage.HINDI -> "${ambient.timeGreetingEn} • ${ambient.formattedCurrentTime}"
                                AppLanguage.SPANISH -> "${ambient.timeGreetingEn} • ${ambient.formattedCurrentTime}"
                            },
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (ambient.batteryPercent < 25) Color(0xFFEF4444).copy(alpha = 0.3f) else EmeraldGlow.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "⚡ ${ambient.batteryPercent}%",
                                color = if (ambient.batteryPercent < 25) Color(0xFFFCA5A5) else EmeraldGlow,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    // Proactive AI Smart Briefing Trigger
                    Button(
                        onClick = onRefreshBriefing,
                        enabled = !isRefreshingBriefing,
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(32.dp).testTag("proactive_briefing_button")
                    ) {
                        if (isRefreshingBriefing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(14.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = when (language) {
                                    AppLanguage.BENGALI -> "এআই ব্রিফিং"
                                    AppLanguage.ENGLISH -> "AI Briefing"
                                    AppLanguage.HINDI -> "एआई सारांश"
                                    AppLanguage.SPANISH -> "Resumen IA"
                                },
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // If AI briefing text is loaded
                AnimatedVisibility(visible = !aiBriefing.isNullOrBlank()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
                            .padding(10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = NeonCyan,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (language) {
                                    AppLanguage.BENGALI -> "সোহানের প্রোঅ্যাক্টিভ ব্রিফিং:"
                                    AppLanguage.ENGLISH -> "Shohan Proactive Briefing:"
                                    AppLanguage.HINDI -> "शोहन प्रोएक्टिव सारांश:"
                                    AppLanguage.SPANISH -> "Resumen proactivo de Shohan:"
                                },
                                color = NeonCyan,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = aiBriefing ?: "",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            lineHeight = 17.sp
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Horizontal Carousel of Proactive Suggestion Cards
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
        ) {
            suggestions.forEach { item ->
                ProactiveCardItem(
                    suggestion = item,
                    onClick = { onSuggestionClick(item.actionPrompt) }
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Routine / Calendar Quick Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp)
        ) {
            val pendingCount = reminders.count { !it.isCompleted }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { isExpandedSchedule = !isExpandedSchedule }
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = CyberPurple,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = when (language) {
                        AppLanguage.BENGALI -> "ক্যালেন্ডার ও দৈনন্দিন রুটিন ($pendingCount টি সক্রিয়)"
                        AppLanguage.ENGLISH -> "Calendar & Routines ($pendingCount active)"
                        AppLanguage.HINDI -> "कैलेंडर और दिनचर्या ($pendingCount सक्रिय)"
                        AppLanguage.SPANISH -> "Calendario y rutinas ($pendingCount activas)"
                    },
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            IconButton(
                onClick = onOpenAddReminder,
                modifier = Modifier.size(28.dp).testTag("add_reminder_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Reminder",
                    tint = NeonCyan,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        // Expandable list of reminders & scheduled tasks
        AnimatedVisibility(visible = isExpandedSchedule) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp)
                    .background(DarkCardSurface, RoundedCornerShape(12.dp))
                    .border(1.dp, DarkSurfaceVariant, RoundedCornerShape(12.dp))
                    .padding(8.dp)
            ) {
                if (reminders.isEmpty()) {
                    Text(
                        text = when (language) {
                            AppLanguage.BENGALI -> "কোনো নির্ধারিত রিমাইন্ডার নেই। + বাটনে চাপুন বা মুখে বলুন।"
                            AppLanguage.ENGLISH -> "No scheduled reminders. Tap + or speak to Shohan."
                            AppLanguage.HINDI -> "कोई निर्धारित कार्य नहीं है। + दबाएं या बोलें।"
                            AppLanguage.SPANISH -> "Sin recordatorios programados. Toca + o habla con Shohan."
                        },
                        color = TextMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(8.dp)
                    )
                } else {
                    reminders.forEach { reminder ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp, horizontal = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                IconButton(
                                    onClick = { onToggleReminder(reminder.id, !reminder.isCompleted) },
                                    modifier = Modifier.size(26.dp)
                                ) {
                                    Icon(
                                        imageVector = if (reminder.isCompleted) Icons.Default.CheckCircle else Icons.Default.Check,
                                        contentDescription = "Toggle Complete",
                                        tint = if (reminder.isCompleted) EmeraldGlow else TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = reminder.title,
                                        color = if (reminder.isCompleted) TextMuted else TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${reminder.timeLabel} • ${reminder.category}",
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onDeleteReminder(reminder.id) },
                                modifier = Modifier.size(26.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProactiveCardItem(
    suggestion: ProactiveSuggestion,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val icon = getSuggestionIcon(suggestion.iconType)
    val accentColor = when (suggestion.iconType) {
        "BATTERY" -> Color(0xFFEF4444)
        "SUN" -> Color(0xFFF59E0B)
        "CALENDAR" -> CyberPurple
        "PHOTO" -> NeonCyan
        "MESSAGE" -> ElectricBlue
        "HEALTH" -> EmeraldGlow
        else -> NeonCyan
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = DarkCardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.35f)),
        modifier = modifier
            .widthIn(min = 220.dp, max = 260.dp)
            .clickable(onClick = onClick)
            .testTag("proactive_card_${suggestion.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.15f))
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = suggestion.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                suggestion.badge?.let { badge ->
                    Text(
                        text = badge,
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = suggestion.description,
                color = TextSecondary,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(
                    text = "চালু করুন",
                    color = NeonCyan,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    tint = NeonCyan,
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

private fun getSuggestionIcon(type: String): ImageVector {
    return when (type) {
        "BATTERY" -> Icons.Default.BatteryAlert
        "SUN" -> Icons.Default.WbSunny
        "CALENDAR" -> Icons.Default.CalendarMonth
        "PHOTO" -> Icons.Default.Image
        "MESSAGE" -> Icons.Default.Message
        "HEALTH" -> Icons.Default.Favorite
        else -> Icons.Default.AutoAwesome
    }
}
