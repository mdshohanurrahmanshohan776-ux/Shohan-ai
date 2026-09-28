package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.device.AppLanguage
import com.example.ui.theme.DarkCardSurface
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TextPrimary

data class QuickCommand(
    val title: String,
    val icon: ImageVector,
    val prompt: String
)

@Composable
fun QuickCommandsRow(
    language: AppLanguage,
    onCommandClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val commands = when (language) {
        AppLanguage.BENGALI -> listOf(
            QuickCommand("সর্বশেষ ছবি", Icons.Default.Image, "আমাকে গ্যালারি থেকে সর্বশেষ ছবিটি খুঁজে বের করে দাও"),
            QuickCommand("অডিও লিখুন", Icons.Default.Mic, "আমাকে অডিও ফাইলটা লিখে দাও"),
            QuickCommand("রিমাইন্ডার", Icons.Default.Alarm, "আজকের জন্য একটি গুরুত্বপূর্ণ রিমাইন্ডার সেট করে দাও"),
            QuickCommand("মেসেজ পাঠান", Icons.Default.Send, "কাউকে দ্রুত মেসেজ পাঠিয়ে দাও"),
            QuickCommand("কল করুন", Icons.Default.Call, "কল করার অপশন দাও"),
            QuickCommand("সোশ্যাল পোস্ট", Icons.Default.Share, "সোশ্যাল মিডিয়ায় সরাসরি পোস্ট করার অপশন দাও"),
            QuickCommand("ফ্ল্যাশলাইট", Icons.Default.FlashlightOn, "ফ্ল্যাশলাইট চালু করো")
        )
        AppLanguage.ENGLISH -> listOf(
            QuickCommand("Latest Photo", Icons.Default.Image, "Find my latest gallery photo"),
            QuickCommand("Transcribe Audio", Icons.Default.Mic, "Transcribe this audio file for me"),
            QuickCommand("Set Reminder", Icons.Default.Alarm, "Set a priority reminder for today"),
            QuickCommand("Send Message", Icons.Default.Send, "Help me draft a quick message"),
            QuickCommand("Phone Call", Icons.Default.Call, "Open dialer to make a call"),
            QuickCommand("Social Post", Icons.Default.Share, "Prepare a post for social media"),
            QuickCommand("Flashlight", Icons.Default.FlashlightOn, "Toggle flashlight")
        )
        AppLanguage.HINDI -> listOf(
            QuickCommand("नवीनतम फोटो", Icons.Default.Image, "गैलरी से नवीनतम फोटो ढूंढें"),
            QuickCommand("ऑडियो लिखें", Icons.Default.Mic, "इस ऑडियो फाइल को टेक्स्ट में लिखें"),
            QuickCommand("रिमाइंडर", Icons.Default.Alarm, "आज के लिए एक रिमाइंडर सेट करें"),
            QuickCommand("संदेश भेजें", Icons.Default.Send, "संदेश भेजने में सहायता करें"),
            QuickCommand("कॉल करें", Icons.Default.Call, "कॉल करने का विकल्प दें"),
            QuickCommand("सोशल पोस्ट", Icons.Default.Share, "सोशल मीडिया पोस्ट तैयार करें"),
            QuickCommand("टॉर्च", Icons.Default.FlashlightOn, "टॉर्च चालू करें")
        )
        AppLanguage.SPANISH -> listOf(
            QuickCommand("Última foto", Icons.Default.Image, "Busca la última foto de mi galería"),
            QuickCommand("Transcribir audio", Icons.Default.Mic, "Transcribe este audio a texto"),
            QuickCommand("Recordatorio", Icons.Default.Alarm, "Crea un recordatorio para hoy"),
            QuickCommand("Enviar mensaje", Icons.Default.Send, "Ayúdame a enviar un mensaje rápido"),
            QuickCommand("Llamar", Icons.Default.Call, "Abrir marcador para llamar"),
            QuickCommand("Publicar", Icons.Default.Share, "Publicar en redes sociales"),
            QuickCommand("Linterna", Icons.Default.FlashlightOn, "Encender la linterna")
        )
    }

    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .testTag("quick_commands_row")
    ) {
        commands.forEach { cmd ->
            FilterChip(
                selected = false,
                onClick = { onCommandClick(cmd.prompt) },
                label = {
                    Text(
                        text = cmd.title,
                        fontSize = 12.sp,
                        color = TextPrimary
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = cmd.icon,
                        contentDescription = null,
                        tint = NeonCyan,
                        modifier = Modifier.size(16.dp)
                    )
                },
                shape = RoundedCornerShape(20.dp),
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkCardSurface
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = false,
                    borderColor = NeonCyan.copy(alpha = 0.3f),
                    borderWidth = 1.dp
                ),
                modifier = Modifier.height(34.dp)
            )
        }
    }
}
