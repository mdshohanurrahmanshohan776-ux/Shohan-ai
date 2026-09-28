package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.device.AppPreferences
import com.example.data.device.ShohanSpeechRecognizer

class ShohanVoiceService : Service() {

    private var speechRecognizer: ShohanSpeechRecognizer? = null
    private lateinit var appPreferences: AppPreferences

    companion object {
        const val CHANNEL_ID = "shohan_voice_channel"
        const val NOTIFICATION_ID = 1001
        const val ACTION_START_LISTENING = "ACTION_START_LISTENING"
        const val ACTION_STOP_LISTENING = "ACTION_STOP_LISTENING"
        const val EXTRA_WAKE_WORD_TRIGGERED = "EXTRA_WAKE_WORD_TRIGGERED"
        const val EXTRA_SPOKEN_COMMAND = "EXTRA_SPOKEN_COMMAND"

        var isServiceRunning = false
            private set

        fun start(context: Context) {
            val intent = Intent(context, ShohanVoiceService::class.java).apply {
                action = ACTION_START_LISTENING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, ShohanVoiceService::class.java).apply {
                action = ACTION_STOP_LISTENING
            }
            context.startService(intent)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LISTENING -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                isServiceRunning = true
                initAndStartListening()
            }
            ACTION_STOP_LISTENING -> {
                isServiceRunning = false
                stopListening()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
        }
        return START_STICKY
    }

    private fun initAndStartListening() {
        speechRecognizer?.destroy()
        speechRecognizer = ShohanSpeechRecognizer(
            context = applicationContext,
            customWakeWord = appPreferences.customWakeWord,
            currentLanguage = appPreferences.selectedLanguage,
            onWakeWordDetected = { spokenCommand ->
                val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra(EXTRA_WAKE_WORD_TRIGGERED, true)
                    if (!spokenCommand.isNullOrBlank()) {
                        putExtra(EXTRA_SPOKEN_COMMAND, spokenCommand)
                    }
                }
                startActivity(openIntent)
            },
            onFinalCommand = { command ->
                val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                    putExtra(EXTRA_WAKE_WORD_TRIGGERED, true)
                    putExtra(EXTRA_SPOKEN_COMMAND, command)
                }
                startActivity(openIntent)
            },
            onErrorOccurred = { /* Keep background recognizer alive */ }
        )

        speechRecognizer?.startListening(continuous = true)
    }

    private fun stopListening() {
        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Shohan AI ভয়েস লিসেনার",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "ভয়েস কমান্ড শোনার জন্য ব্যাকগ্রাউন্ড সার্ভিস"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val stopIntent = Intent(this, ShohanVoiceService::class.java).apply {
            action = ACTION_STOP_LISTENING
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_IMMUTABLE
        )

        val wakeWordDisplay = appPreferences.customWakeWord

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shohan AI প্রস্তুত")
            .setContentText("বলুন \"$wakeWordDisplay\" — যেকোনো জায়গা থেকেই চালু হবে")
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .addAction(0, "বন্ধ করুন", stopPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        isServiceRunning = false
        stopListening()
        super.onDestroy()
    }
}
