package com.falah.app

import android.app.*
import android.content.*
import android.media.RingtoneManager
import android.os.Build
import android.speech.tts.TextToSpeech
import java.util.*

object PrayerAlarm {
    const val CHANNEL = "falah_prayer_alerts"
    fun schedule(context: Context, lat: Double, lon: Double) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        for (offset in 0..1) {
            val date = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, offset) }
            val prayers = PrayerEngine.calculate(date.get(Calendar.YEAR), date.get(Calendar.MONTH)+1, date.get(Calendar.DAY_OF_MONTH), lat, lon)
            prayers.forEachIndexed { index, prayer ->
                val at = Calendar.getInstance().apply { set(date.get(Calendar.YEAR), date.get(Calendar.MONTH), date.get(Calendar.DAY_OF_MONTH), prayer.minutes/60, prayer.minutes%60, 0); set(Calendar.MILLISECOND,0) }.timeInMillis
                listOf(true, false).forEach { reminder ->
                    val trigger = at - if (reminder) 10*60*1000L else 0L
                    if (trigger <= now) return@forEach
                    val id = (date.get(Calendar.DAY_OF_YEAR)*1000 + index*10 + if(reminder)1 else 2)
                    val intent = Intent(context, PrayerAlarmReceiver::class.java).putExtra("prayer", prayer.name).putExtra("reminder", reminder)
                    val pi = PendingIntent.getBroadcast(context, id, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
                    if (Build.VERSION.SDK_INT >= 23) am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, trigger, pi) else am.set(AlarmManager.RTC_WAKEUP, trigger, pi)
                }
            }
        }
    }
    fun channel(context: Context) {
        if (Build.VERSION.SDK_INT >= 26) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val ch = NotificationChannel(CHANNEL, "Prayer reminders and Adhan alerts", NotificationManager.IMPORTANCE_HIGH)
            ch.description = "Falah prayer-time reminders"
            manager.createNotificationChannel(ch)
        }
    }
}

class PrayerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        PrayerAlarm.channel(context)
        val prayer = intent.getStringExtra("prayer") ?: "Prayer"
        val reminder = intent.getBooleanExtra("reminder", false)
        val title = if (reminder) "$prayer in 10 minutes" else "$prayer prayer time"
        val body = if (reminder) "Prepare for prayer. May Allah make it easy for you." else "It is time for $prayer. May Allah accept your prayer."
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = launch?.let { PendingIntent.getActivity(context, 0, it, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(context, PrayerAlarm.CHANNEL) else Notification.Builder(context)
        val notification = builder.setSmallIcon(android.R.drawable.ic_lock_idle_alarm).setContentTitle(title).setContentText(body).setAutoCancel(true).setContentIntent(pi).setCategory(Notification.CATEGORY_ALARM).build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(prayer.hashCode() + if(reminder)1 else 2, notification)
        // Spoken reminder uses the device's installed text-to-speech engine.
        if (reminder && context.getSharedPreferences("falah", Context.MODE_PRIVATE).getBoolean("voice_reminders", true)) {
            val pending = goAsync()
            lateinit var engine: TextToSpeech
            engine = TextToSpeech(context) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    engine.language = Locale.getDefault()
                    engine.speak("In ten minutes, it will be time for $prayer prayer. Please prepare.", TextToSpeech.QUEUE_FLUSH, null, "falah_$prayer")
                    android.os.Handler(context.mainLooper).postDelayed({ engine.shutdown(); pending.finish() }, 6000)
                } else pending.finish()
            }
        }
    }

}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_TIME_CHANGED || intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            val p = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
            PrayerAlarm.schedule(context, p.getString("lat", "36.8065")!!.toDoubleOrNull() ?: 36.8065, p.getString("lon", "10.1815")!!.toDoubleOrNull() ?: 10.1815)
        }
    }
}
