package com.example.data.device

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.util.Log
import android.widget.Toast

object ActionDispatcher {

    private var isTorchOn = false

    fun sendSms(context: Context, phoneNumber: String, message: String) {
        try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("smsto:$cleanPhone")
                putExtra("sms_body", message)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ActionDispatcher", "Failed to launch SMS intent", e)
            fallbackGenericShare(context, message, "মেসেজ পাঠান")
        }
    }

    fun sendWhatsApp(context: Context, phoneNumber: String = "", message: String) {
        try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val url = if (cleanPhone.isNotBlank()) {
                "https://api.whatsapp.com/send?phone=$cleanPhone&text=${Uri.encode(message)}"
            } else {
                "https://api.whatsapp.com/send?text=${Uri.encode(message)}"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ActionDispatcher", "Failed to launch WhatsApp", e)
            fallbackGenericShare(context, message, "WhatsApp মেসেজ")
        }
    }

    fun makeCall(context: Context, phoneNumber: String) {
        try {
            val cleanPhone = phoneNumber.filter { it.isDigit() || it == '+' }
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:${cleanPhone.ifBlank { "" }}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ActionDispatcher", "Failed to launch dialer", e)
            Toast.makeText(context, "কল করা সম্ভব হচ্ছে না", Toast.LENGTH_SHORT).show()
        }
    }

    fun postSocial(context: Context, platform: String, content: String) {
        try {
            val intent = when (platform.lowercase()) {
                "twitter", "x" -> {
                    val url = "https://twitter.com/intent/tweet?text=${Uri.encode(content)}"
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
                "facebook", "fb" -> {
                    // Facebook direct share or generic share
                    val fbIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                        setPackage("com.facebook.katana")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    if (fbIntent.resolveActivity(context.packageManager) != null) {
                        fbIntent
                    } else {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, content)
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        Intent.createChooser(shareIntent, "ফেসবুকে শেয়ার করুন").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                    }
                }
                else -> {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, content)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    Intent.createChooser(shareIntent, "সোশ্যাল মিডিয়ায় পোস্ট করুন").apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                }
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Log.e("ActionDispatcher", "Failed to share post", e)
            fallbackGenericShare(context, content, "শেয়ার করুন")
        }
    }

    fun toggleTorch(context: Context, forceState: Boolean? = null): Boolean {
        try {
            val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                ?: return false
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                val characteristics = cameraManager.getCameraCharacteristics(id)
                characteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            } ?: return false

            val newState = forceState ?: !isTorchOn
            cameraManager.setTorchMode(cameraId, newState)
            isTorchOn = newState
            return isTorchOn
        } catch (e: CameraAccessException) {
            Log.e("ActionDispatcher", "Torch access exception", e)
            return false
        } catch (e: Exception) {
            Log.e("ActionDispatcher", "Torch error", e)
            return false
        }
    }

    fun openApp(context: Context, appName: String): Boolean {
        return try {
            val pm = context.packageManager
            val packages = pm.getInstalledApplications(0)
            val cleanTarget = appName.lowercase().trim()

            val matchedApp = packages.firstOrNull {
                val label = pm.getApplicationLabel(it).toString().lowercase()
                label.contains(cleanTarget) || it.packageName.lowercase().contains(cleanTarget)
            }

            if (matchedApp != null) {
                val launchIntent = pm.getLaunchIntentForPackage(matchedApp.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return true
                }
            }
            false
        } catch (e: Exception) {
            Log.e("ActionDispatcher", "Failed to open app $appName", e)
            false
        }
    }

    private fun fallbackGenericShare(context: Context, text: String, title: String) {
        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            putExtra(Intent.EXTRA_TEXT, text)
            type = "text/plain"
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val chooser = Intent.createChooser(sendIntent, title).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
