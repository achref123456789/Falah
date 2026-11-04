package com.falah.app

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.widget.Toast
import android.content.Intent
import java.util.Calendar

/** Uses Accessibility events to return selected apps to Home during an unconfirmed prayer window. */
class AppBlockAccessibilityService : AccessibilityService() {
    private var lastBlockedPackage = ""
    private var lastToastAt = 0L
    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == "com.android.systemui") return
        val prefs = getSharedPreferences("falah", MODE_PRIVATE)
        if (!prefs.getBoolean("app_$pkg", false)) return
        val cal = Calendar.getInstance()
        val today = PrayerEngine.calculate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH)+1, cal.get(Calendar.DAY_OF_MONTH),
            prefs.getString("lat", "36.8065")!!.toDoubleOrNull() ?: 36.8065,
            prefs.getString("lon", "10.1815")!!.toDoubleOrNull() ?: 10.1815)
        val now = cal.get(Calendar.HOUR_OF_DAY)*60 + cal.get(Calendar.MINUTE)
        // The most recent prayer time defines the active prayer period. It remains active until confirmation.
        val active = today.lastOrNull { it.minutes <= now }
        val blocked = active != null && !prefs.getBoolean("done_${java.text.SimpleDateFormat("yyyyMMdd", java.util.Locale.US).format(cal.time)}_${active.name}", false)
        if (blocked) {
            val lock = Intent(this, PrayerLockActivity::class.java).putExtra("prayer", active!!.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            startActivity(lock)
            performGlobalAction(GLOBAL_ACTION_HOME)
            lastBlockedPackage = pkg; lastToastAt = System.currentTimeMillis()
        }
    }
    override fun onInterrupt() {}
}
