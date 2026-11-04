package com.falah.app

import java.util.Calendar
import java.util.Locale
import kotlin.math.*

data class Prayer(val name: String, val minutes: Int) { fun display(): String = String.format(Locale.US, "%02d:%02d", minutes / 60, minutes % 60) }

/** Lightweight solar-position prayer calculator. Angles are configurable; times use the device time zone. */
object PrayerEngine {
    fun calculate(year: Int, month: Int, day: Int, latitude: Double, longitude: Double, fajrAngle: Double = 18.0, ishaAngle: Double = 17.0, asrFactor: Double = 1.0): List<Prayer> {
        val cal = Calendar.getInstance().apply { set(year, month - 1, day, 12, 0, 0); set(Calendar.MILLISECOND, 0) }
        val n = cal.get(Calendar.DAY_OF_YEAR).toDouble()
        val gamma = 2.0 * Math.PI / 365.0 * (n - 1.0)
        val eq = 229.18 * (0.000075 + 0.001868*cos(gamma) - 0.032077*sin(gamma) - 0.014615*cos(2*gamma) - 0.040849*sin(2*gamma))
        val decl = 0.006918 - 0.399912*cos(gamma) + 0.070257*sin(gamma) - 0.006758*cos(2*gamma) + 0.000907*sin(2*gamma) - 0.002697*cos(3*gamma) + 0.00148*sin(3*gamma)
        val tz = cal.timeZone.rawOffset / 3600000.0 + cal.timeZone.dstSavings / 3600000.0
        val noon = 720.0 - 4.0 * longitude - eq + tz * 60.0
        fun angleTime(angle: Double, morning: Boolean): Double {
            val lat = Math.toRadians(latitude); val a = Math.toRadians(angle)
            val c = (sin(a) - sin(lat)*sin(decl)) / (cos(lat)*cos(decl))
            if (c !in -1.0..1.0) return Double.NaN
            val delta = Math.toDegrees(acos(c)) * 4.0
            return noon + if (morning) -delta else delta
        }
        val sunrise = angleTime(-0.833, true)
        val sunset = angleTime(-0.833, false)
        val fajr = angleTime(-fajrAngle, true)
        val isha = angleTime(-ishaAngle, false)
        val asrAltitude = atan(1.0 / (asrFactor + tan(abs(Math.toRadians(latitude) - decl))))
        val asrCos = (sin(asrAltitude) - sin(Math.toRadians(latitude))*sin(decl)) / (cos(Math.toRadians(latitude))*cos(decl))
        val asr = if (asrCos in -1.0..1.0) noon + Math.toDegrees(acos(asrCos))*4 else noon + 180
        fun safe(v: Double, fallback: Double) = (if (v.isFinite()) v else fallback).roundToInt().coerceIn(0, 1439)
        return listOf(Prayer("Fajr", safe(fajr, 300.0)), Prayer("Dhuhr", safe(noon + 2.0, 720.0)), Prayer("Asr", safe(asr, 900.0)), Prayer("Maghrib", safe(sunset, 1080.0)), Prayer("Isha", safe(isha, 1140.0)))
    }
}
