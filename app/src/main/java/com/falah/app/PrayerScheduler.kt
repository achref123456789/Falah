package com.falah.app

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.util.*

object PrayerScheduler {
    fun schedule(context: Context) {
        val prefs = context.getSharedPreferences("falah", Context.MODE_PRIVATE)
        val lat = prefs.getString("lat", "36.8065")?.toDoubleOrNull() ?: 36.8065
        val lon = prefs.getString("lon", "10.1815")?.toDoubleOrNull() ?: 10.1815
        val cal = Calendar.getInstance()
        val prayers = PrayerEngine.calculate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH), lat, lon)
        val alarm = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        prayers.forEachIndexed { index, prayer ->
            val at = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, prayer.minutes / 60); set(Calendar.MINUTE, prayer.minutes % 60); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
            if (at.timeInMillis <= System.currentTimeMillis()) at.add(Calendar.DAY_OF_YEAR, 1)
            val adhan = Intent(context, PrayerAlarmReceiver::class.java).putExtra("prayer", prayer.name).putExtra("reminder", false)
            val pre = Intent(context, PrayerAlarmReceiver::class.java).putExtra("prayer", prayer.name).putExtra("reminder", true)
            val p1 = PendingIntent.getBroadcast(context, index*2, adhan, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val p2 = PendingIntent.getBroadcast(context, index*2+1, pre, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val reminderAt = at.timeInMillis - 10*60*1000L
            if (reminderAt > System.currentTimeMillis()) setAlarm(alarm, reminderAt, p2)
            setAlarm(alarm, at.timeInMillis, p1)
        }
    }
    private fun setAlarm(manager: AlarmManager, time: Long, pending: PendingIntent) {
        try { manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) }
        catch (_: SecurityException) { manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending) }
    }
}
