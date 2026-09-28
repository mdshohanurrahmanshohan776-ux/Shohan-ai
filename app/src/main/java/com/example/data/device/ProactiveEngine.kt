package com.example.data.device

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import com.example.data.local.ProactiveReminderEntity
import java.util.Calendar

data class DeviceAmbientContext(
    val timeOfDay: String, // "MORNING", "AFTERNOON", "EVENING", "NIGHT"
    val timeGreetingBn: String,
    val timeGreetingEn: String,
    val batteryPercent: Int,
    val isCharging: Boolean,
    val pendingRemindersCount: Int,
    val formattedCurrentTime: String
)

data class ProactiveSuggestion(
    val id: String,
    val title: String,
    val description: String,
    val actionPrompt: String,
    val iconType: String, // "SUN", "BATTERY", "CALENDAR", "PHOTO", "AUDIO", "MESSAGE", "HEALTH"
    val badge: String? = null
)

object ProactiveEngine {

    fun getDeviceAmbientContext(context: Context, pendingReminders: List<ProactiveReminderEntity>): DeviceAmbientContext {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val (timePeriod, greetingBn, greetingEn) = when (hour) {
            in 5..11 -> Triple("MORNING", "শুভ সকাল", "Good Morning")
            in 12..16 -> Triple("AFTERNOON", "শুভ দুপুর", "Good Afternoon")
            in 17..20 -> Triple("EVENING", "শুভ সন্ধ্যা", "Good Evening")
            else -> Triple("NIGHT", "শুভ রাত্রি", "Good Night")
        }

        // Battery Info
        val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 75
        val status = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL

        val timeString = String.format("%02d:%02d", hour, minute)

        return DeviceAmbientContext(
            timeOfDay = timePeriod,
            timeGreetingBn = greetingBn,
            timeGreetingEn = greetingEn,
            batteryPercent = batteryPct,
            isCharging = isCharging,
            pendingRemindersCount = pendingReminders.size,
            formattedCurrentTime = timeString
        )
    }

    fun generateContextualSuggestions(
        ambient: DeviceAmbientContext,
        language: AppLanguage,
        pendingReminders: List<ProactiveReminderEntity>
    ): List<ProactiveSuggestion> {
        val suggestions = mutableListOf<ProactiveSuggestion>()

        // 1. Calendar / Upcoming reminder proactive suggestion
        if (pendingReminders.isNotEmpty()) {
            val nextReminder = pendingReminders.first()
            val (title, desc) = when (language) {
                AppLanguage.BENGALI -> "আসন্ন কাজ: ${nextReminder.title}" to "নির্ধারিত সময়: ${nextReminder.timeLabel} (${nextReminder.category})"
                AppLanguage.ENGLISH -> "Upcoming: ${nextReminder.title}" to "Scheduled at: ${nextReminder.timeLabel} (${nextReminder.category})"
                AppLanguage.HINDI -> "आगामी कार्य: ${nextReminder.title}" to "निर्धारित समय: ${nextReminder.timeLabel}"
                AppLanguage.SPANISH -> "Próxima tarea: ${nextReminder.title}" to "Hora: ${nextReminder.timeLabel}"
            }
            suggestions.add(
                ProactiveSuggestion(
                    id = "reminder_alert",
                    title = title,
                    description = desc,
                    actionPrompt = when (language) {
                        AppLanguage.BENGALI -> "আমার রিমাইন্ডার এবং সময়সূচী বিশ্লেষণ করে দাও"
                        AppLanguage.ENGLISH -> "Review and summarize my upcoming reminders"
                        AppLanguage.HINDI -> "मेरे आगामी कार्यों की समीक्षा करें"
                        AppLanguage.SPANISH -> "Revisa y resume mis recordatorios pendientes"
                    },
                    iconType = "CALENDAR",
                    badge = "সূচী"
                )
            )
        }

        // 2. Battery health proactive suggestion
        if (ambient.batteryPercent in 1..25 && !ambient.isCharging) {
            val (title, desc) = when (language) {
                AppLanguage.BENGALI -> "ব্যাটারি কম (${ambient.batteryPercent}%)" to "ফোন চার্জে দিন অথবা অতিরিক্ত ব্যাকগ্রাউন্ড কাজ কমানোর নির্দেশ দিন।"
                AppLanguage.ENGLISH -> "Low Battery (${ambient.batteryPercent}%)" to "Consider plugging in or enabling power saving tips."
                AppLanguage.HINDI -> "कम बैटरी (${ambient.batteryPercent}%)" to "कृपया फोन चार्ज करें या बिजली बचत मोड चालू करें।"
                AppLanguage.SPANISH -> "Batería baja (${ambient.batteryPercent}%)" to "Conecte el cargador o active el ahorro de energía."
            }
            suggestions.add(
                ProactiveSuggestion(
                    id = "battery_alert",
                    title = title,
                    description = desc,
                    actionPrompt = when (language) {
                        AppLanguage.BENGALI -> "ব্যাটারি সেভ করার সেরা উপায় কি?"
                        AppLanguage.ENGLISH -> "How can I maximize my phone battery life right now?"
                        AppLanguage.HINDI -> "फोन की बैटरी बचाने के तरीके बताइए"
                        AppLanguage.SPANISH -> "¿Cómo puedo ahorrar batería en mi teléfono ahora?"
                    },
                    iconType = "BATTERY",
                    badge = "সতর্কতা"
                )
            )
        }

        // 3. Time-of-day Habit Suggestions
        when (ambient.timeOfDay) {
            "MORNING" -> {
                val (title, desc) = when (language) {
                    AppLanguage.BENGALI -> "সকালের ব্রিফিং ও পরিকল্পনা" to "আজকের দিনের আবহাওয়া, রুটিন এবং অনুপ্রেরণামূলক পরামর্শ শুনুন।"
                    AppLanguage.ENGLISH -> "Morning Briefing & Schedule" to "Review your plan for the day, weather, and top priorities."
                    AppLanguage.HINDI -> "सुबह का सारांश और योजना" to "आज की दिनचर्या, मौसम और प्राथमिकताओं की समीक्षा करें।"
                    AppLanguage.SPANISH -> "Resumen matutino y plan del día" to "Consulta el plan de hoy, clima y prioridades."
                }
                suggestions.add(
                    ProactiveSuggestion(
                        id = "morning_brief",
                        title = title,
                        description = desc,
                        actionPrompt = when (language) {
                            AppLanguage.BENGALI -> "আজকের জন্য সকালের বিস্তারিত ব্রিফিং ও পরিকল্পনা দাও"
                            AppLanguage.ENGLISH -> "Give me a complete morning briefing and plan for today"
                            AppLanguage.HINDI -> "आज के लिए सुबह का संक्षिप्त विवरण और योजना दीजिए"
                            AppLanguage.SPANISH -> "Dame un resumen completo de la mañana y el plan para hoy"
                        },
                        iconType = "SUN",
                        badge = "সকাল"
                    )
                )
            }
            "AFTERNOON" -> {
                val (title, desc) = when (language) {
                    AppLanguage.BENGALI -> "দুপুরের রিফ্রেশমেন্ট ও হাইড্রেসন" to "পানি পানের বিরতি নিন এবং অসমাপ্ত কাজের আপডেট দেখুন।"
                    AppLanguage.ENGLISH -> "Midday Refresh & Hydration" to "Take a hydration break and check progress on pending tasks."
                    AppLanguage.HINDI -> "दोपहर का विश्राम और जलपान" to "पानी पिएं और शेष कार्यों की प्रगति देखें।"
                    AppLanguage.SPANISH -> "Descanso del mediodía e hidratación" to "Toma agua y revisa el progreso de tus tareas pendientes."
                }
                suggestions.add(
                    ProactiveSuggestion(
                        id = "afternoon_break",
                        title = title,
                        description = desc,
                        actionPrompt = when (language) {
                            AppLanguage.BENGALI -> "দুপুরের কাজের জন্য আমাকে একটি দ্রুত ফোকাস টিপ দাও"
                            AppLanguage.ENGLISH -> "Give me a quick productivity tip for this afternoon"
                            AppLanguage.HINDI -> "दोपहर की उत्पादकता के लिए एक त्वरित सुझाव दीजिए"
                            AppLanguage.SPANISH -> "Dame un consejo rápido de productividad para esta tarde"
                        },
                        iconType = "HEALTH",
                        badge = "দুপুর"
                    )
                )
            }
            "EVENING" -> {
                val (title, desc) = when (language) {
                    AppLanguage.BENGALI -> "সন্ধ্যার পারিবারিক যোগাযোগ ও পর্যালোচনা" to "কাউকে মেসেজ/কল করতে চান বা দিনের সারসংক্ষেপ দেখতে চান?"
                    AppLanguage.ENGLISH -> "Evening Recap & Connect" to "Send a quick check-in message to family or review today's achievements."
                    AppLanguage.HINDI -> "शाम की समीक्षा और परिवार से संपर्क" to "परिवार को संदेश भेजें या आज की उपलब्धियों की समीक्षा करें।"
                    AppLanguage.SPANISH -> "Resumen de la tarde y conectar" to "Envía un mensaje rápido a la familia o revisa el día."
                }
                suggestions.add(
                    ProactiveSuggestion(
                        id = "evening_recap",
                        title = title,
                        description = desc,
                        actionPrompt = when (language) {
                            AppLanguage.BENGALI -> "আজকের দিনের কাজের অগ্রগতি পর্যালোচনা করে দাও"
                            AppLanguage.ENGLISH -> "Review my progress today and suggest tomorrow's priorities"
                            AppLanguage.HINDI -> "आज की प्रगति की समीक्षा करें और कल की योजना बनाएं"
                            AppLanguage.SPANISH -> "Revisa mi progreso de hoy y sugiere prioridades para mañana"
                        },
                        iconType = "MESSAGE",
                        badge = "সন্ধ্যা"
                    )
                )
            }
            else -> { // NIGHT
                val (title, desc) = when (language) {
                    AppLanguage.BENGALI -> "রাত্রিকালীন শিথিলতা ও নোট" to "ঘুমানোর আগে স্ক্রিনের আলো কমান এবং কালকের জন্য রিমাইন্ডার সেট করুন।"
                    AppLanguage.ENGLISH -> "Night Wind-Down & Reminders" to "Set tomorrow's wake-up alarm and note down remaining thoughts."
                    AppLanguage.HINDI -> "रात्रि विश्राम और आगामी रिमाइंडर" to "कल के लिए अलार्म सेट करें और अपने विचार संजोएं।"
                    AppLanguage.SPANISH -> "Descanso nocturno y notas" to "Configura alarmas y anota recordatorios para mañana."
                }
                suggestions.add(
                    ProactiveSuggestion(
                        id = "night_wind_down",
                        title = title,
                        description = desc,
                        actionPrompt = when (language) {
                            AppLanguage.BENGALI -> "কালকের জন্য একটি ভালো রুটিন ও রিমাইন্ডার তৈরি করে দাও"
                            AppLanguage.ENGLISH -> "Help me plan a solid morning routine for tomorrow"
                            AppLanguage.HINDI -> "कल के लिए एक अच्छी सुबह की दिनचर्या तैयार करें"
                            AppLanguage.SPANISH -> "Ayúdame a planificar una rutina matutina para mañana"
                        },
                        iconType = "HEALTH",
                        badge = "রাত্রি"
                    )
                )
            }
        }

        // 4. Photo & Media habit suggestion
        val (photoTitle, photoDesc) = when (language) {
            AppLanguage.BENGALI -> "গ্যালারির সর্বশেষ ছবি বিশ্লেষণ" to "সর্বশেষ তোলা ছবি খুঁজে এআই ভিশন দিয়ে বর্ণনা ও বিশদ জানুন।"
            AppLanguage.ENGLISH -> "Analyze Latest Gallery Photo" to "Find the newest photo and let Gemini AI Vision describe it."
            AppLanguage.HINDI -> "नवीनतम गैलरी फोटो विश्लेषण" to "हालिया फोटो ढूंढें और एआई विजन से उसका विवरण जानें।"
            AppLanguage.SPANISH -> "Analizar la foto más reciente" to "Encuentra la última foto y analízala con Gemini Vision."
        }
        suggestions.add(
            ProactiveSuggestion(
                id = "photo_insight",
                title = photoTitle,
                description = photoDesc,
                actionPrompt = when (language) {
                    AppLanguage.BENGALI -> "আমাকে গ্যালারি থেকে সর্বশেষ ছবিটি খুঁজে বের করে দাও"
                    AppLanguage.ENGLISH -> "Find my latest gallery photo and describe what you see"
                    AppLanguage.HINDI -> "गैलरी से नवीनतम फोटो ढूंढकर मुझे दिखाएं"
                    AppLanguage.SPANISH -> "Búscame la última foto de la galería y descríbela"
                },
                iconType = "PHOTO",
                badge = "মিডিয়া"
            )
        )

        return suggestions
    }
}
