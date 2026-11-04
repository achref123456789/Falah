package com.falah.app

import android.app.*
import android.content.*
import android.media.AudioAttributes
import android.net.Uri
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.*

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val prayer = intent.getStringExtra("prayer") ?: return
        PrayerScheduler.schedule(context)
        val isReminder = intent.getBooleanExtra("reminder", false)
        val channelId = if (isReminder) "falah_reminders" else "falah_adhan"
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= 26) {
            val channel = NotificationChannel(channelId, if (isReminder) "Prayer reminders" else "Prayer time", NotificationManager.IMPORTANCE_HIGH)
            nm.createNotificationChannel(channel)
        }
        val text = if (isReminder) "$prayer is in 10 minutes" else "It is time for $prayer"
        val notification = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, channelId) else Notification.Builder(context)
        notification.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle("Falah • $prayer").setContentText(text).setAutoCancel(true)
        nm.notify((prayer + isReminder).hashCode(), notification.build())
        if (isReminder) {
            val pending = goAsync()
            Thread {
                var tts: TextToSpeech? = null
                try {
                    val latch = java.util.concurrent.CountDownLatch(1)
                    tts = TextToSpeech(context) { status -> if (status == TextToSpeech.SUCCESS) { tts?.language = Locale.UK }; latch.countDown() }
                    latch.await(4, java.util.concurrent.TimeUnit.SECONDS)
                    tts?.speak("Assalamu alaikum. $prayer prayer is in ten minutes.", TextToSpeech.QUEUE_FLUSH, null, "falah_$prayer")
                    Thread.sleep(5000)
                } catch (_: Exception) { } finally { tts?.shutdown(); pending.finish() }
            }.start()
        }
    }
}
