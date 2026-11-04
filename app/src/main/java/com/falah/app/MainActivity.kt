package com.falah.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.location.LocationListener
import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import android.os.Looper
import android.os.Bundle
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.accessibility.AccessibilityManager
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : Activity() {
    private val prefs by lazy { getSharedPreferences("falah", MODE_PRIVATE) }
    private val green = Color.rgb(20, 133, 76); private val dark = Color.rgb(12, 83, 49); private val pale = Color.rgb(237, 248, 240)
    private val prayers = listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
    private var lat = 36.8065; private var lon = 10.1815
    private lateinit var serviceStatus: TextView
    private val locationRequestCode = 4102
    private var locationManager: LocationManager? = null
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); lat = prefs.getString("lat", "36.8065")!!.toDoubleOrNull() ?: 36.8065; lon = prefs.getString("lon", "10.1815")!!.toDoubleOrNull() ?: 10.1815; draw(); schedulePrayerAlarms() }
    override fun onResume() { super.onResume(); schedulePrayerAlarms(); if (::serviceStatus.isInitialized) refreshStatus() }
    private fun schedulePrayerAlarms() {
        try { PrayerScheduler.schedule(this) }
        catch (error: Exception) { Log.e("MainActivity", "Could not schedule prayer alarms", error) }
    }
    private fun hasLocationPermission() = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED || checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
    private fun requestAutomaticLocation() {
        if (!hasLocationPermission()) {
            requestPermissions(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION), locationRequestCode)
            return
        }
        try {
            locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
            val providers = locationManager!!.getProviders(true)
            var best: Location? = null
            for (provider in providers) {
                val candidate = try { locationManager!!.getLastKnownLocation(provider) } catch (_: SecurityException) { null }
                if (candidate != null && (best == null || candidate.accuracy < best!!.accuracy)) best = candidate
            }
            if (best != null) saveLocation(best!!.latitude, best!!.longitude, "Current location updated")
            val listener = object : LocationListener {
                override fun onLocationChanged(location: Location) { saveLocation(location.latitude, location.longitude, "Prayer times updated for your location"); try { locationManager?.removeUpdates(this) } catch (_: Exception) {} }
                override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                override fun onProviderEnabled(provider: String) {}
                override fun onProviderDisabled(provider: String) {}
            }
            for (provider in providers) try { locationManager!!.requestLocationUpdates(provider, 0L, 0f, listener, Looper.getMainLooper()) } catch (_: Exception) {}
            if (best == null) Toast.makeText(this, "Getting location… keeping Tunis times until GPS is available", Toast.LENGTH_LONG).show()
        } catch (_: Exception) { Toast.makeText(this, "Could not get location. Tunis is still selected.", Toast.LENGTH_LONG).show() }
    }
    private fun saveLocation(latitude: Double, longitude: Double, message: String) {
        if (latitude !in -90.0..90.0 || longitude !in -180.0..180.0) return
        lat = latitude; lon = longitude
        prefs.edit().putString("lat", lat.toString()).putString("lon", lon.toString()).apply()
        if (::serviceStatus.isInitialized) draw()
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationRequestCode) { if (grantResults.any { it == PackageManager.PERMISSION_GRANTED }) requestAutomaticLocation() else Toast.makeText(this, "Location denied. Prayer times will use Tunis coordinates.", Toast.LENGTH_LONG).show() }
    }
    private fun dayKey(): String = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
    private fun today(): List<Prayer> { val c=Calendar.getInstance(); return PrayerEngine.calculate(c.get(Calendar.YEAR),c.get(Calendar.MONTH)+1,c.get(Calendar.DAY_OF_MONTH),lat,lon) }
    private fun isDone(name:String)=prefs.getBoolean("done_${dayKey()}_$name",false)
    private fun setDone(name:String, value:Boolean)=prefs.edit().putBoolean("done_${dayKey()}_$name",value).apply()
    private fun currentPrayer(): Prayer? { val now=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60+Calendar.getInstance().get(Calendar.MINUTE); return today().lastOrNull { it.minutes <= now } }
    private fun draw() {
        val scroll=ScrollView(this).apply{setBackgroundColor(pale)}; val root=LinearLayout(this).apply{orientation=1;setPadding(18,20,18,28)}; scroll.addView(root)
        root.addView(TextView(this).apply{text="☾  FALAH";textSize=30f;gravity=Gravity.CENTER;setTextColor(Color.WHITE);setPadding(16,22,16,22);setBackgroundColor(green);typeface=android.graphics.Typeface.DEFAULT_BOLD})
        val date=SimpleDateFormat("EEEE, d MMMM yyyy",Locale.getDefault()).format(Date()); section(root,"Assalamu alaikum",date+"\nYour daily prayer companion")
        val schedule=today(); val now=Calendar.getInstance().get(Calendar.HOUR_OF_DAY)*60+Calendar.getInstance().get(Calendar.MINUTE); val next=schedule.firstOrNull{it.minutes>now} ?: schedule.first()
        section(root,"Next prayer", "${next.name}  •  ${next.display()}\nLocation: ${"%.4f".format(lat)}, ${"%.4f".format(lon)}")
        title(root,"Today's prayers")
        schedule.forEach { p ->
            val row=LinearLayout(this).apply{orientation=0;gravity=Gravity.CENTER_VERTICAL;setPadding(12,9,12,9);setBackgroundColor(Color.WHITE)}
            row.addView(TextView(this).apply{text=p.name+"   ${p.display()}";textSize=17f;setTextColor(dark)},LinearLayout.LayoutParams(0,-2,1f))
            val done=CheckBox(this).apply{text="Prayed";isChecked=isDone(p.name);setOnCheckedChangeListener{_,v->setDone(p.name,v);draw()}}
            row.addView(done); val lp=LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=6;root.addView(row,lp)
        }
        val completed=schedule.count{isDone(it.name)}; section(root,"90-day journey","Day ${journeyDay()} of 90  •  $completed / 5 prayers completed today")
        val progress=ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal).apply{max=5;this.progress=completed};root.addView(progress)
        title(root,"Apps to restrict during prayer")
        root.addView(TextView(this).apply{text="Choose distracting apps. When a prayer time begins, Falah sends selected apps to the home screen until you mark that prayer as completed. Enable the accessibility service below.";textSize=14f;setTextColor(Color.DKGRAY);setPadding(0,0,0,8)})
        val pm=packageManager
        val apps=try{pm.getInstalledApplications(PackageManager.GET_META_DATA).filter{it.packageName!=packageName && pm.getLaunchIntentForPackage(it.packageName)!=null}.sortedBy{pm.getApplicationLabel(it).toString().lowercase(Locale.getDefault())}}catch(_:Exception){emptyList()}
        apps.forEach{app->val pkg=app.packageName;root.addView(CheckBox(this).apply{text=pm.getApplicationLabel(app).toString();textSize=15f;setTextColor(Color.BLACK);isChecked=prefs.getBoolean("app_$pkg",false);setOnCheckedChangeListener{_,v->prefs.edit().putBoolean("app_$pkg",v).apply()}})}
        title(root,"Blocking permission")
        serviceStatus=TextView(this).apply{textSize=14f;setTextColor(dark);setPadding(0,4,0,8)};root.addView(serviceStatus)
        root.addView(Button(this).apply{text="Enable app blocking";setOnClickListener{startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))}})
        title(root,"Location & calculation")
        root.addView(TextView(this).apply{text="Prayer times recalculate automatically each day using your saved coordinates. Enable location once to update them for where you are in Tunisia (or elsewhere).";textSize=13f;setTextColor(Color.DKGRAY);setPadding(0,0,0,8)})
        root.addView(Button(this).apply{text="Use my current location automatically";setOnClickListener{requestAutomaticLocation()}})
        val latInput=EditText(this).apply{hint="Latitude (e.g. 36.8065)";setSingleLine();setText(lat.toString());inputType= android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED};root.addView(latInput)
        val lonInput=EditText(this).apply{hint="Longitude (e.g. 10.1815)";setSingleLine();setText(lon.toString());inputType= android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_FLAG_DECIMAL or android.text.InputType.TYPE_NUMBER_FLAG_SIGNED};root.addView(lonInput)
        root.addView(Button(this).apply{text="Save location";setOnClickListener{val a=latInput.text.toString().toDoubleOrNull();val b=lonInput.text.toString().toDoubleOrNull();if(a==null||b==null||a !in -90.0..90.0||b !in -180.0..180.0)Toast.makeText(this@MainActivity,"Enter valid coordinates",Toast.LENGTH_SHORT).show() else {lat=a;lon=b;prefs.edit().putString("lat",lat.toString()).putString("lon",lon.toString()).apply();draw()}}})
        root.addView(TextView(this).apply{text="Default location is Tunis, Tunisia (36.8065, 10.1815). Prayer times are calculated on-device each day using an approximate solar method (Fajr 18°, Isha 17°, standard Asr); GPS updates the coordinates when permission is granted. Compare with the Tunisian Ministry of Religious Affairs or your local mosque, since official timetables may differ by a few minutes. App blocking depends on Android Accessibility permission and can be disabled in system settings; it is not tamper-proof.";textSize=12f;setTextColor(Color.DKGRAY);setPadding(4,18,4,0)})
        setContentView(scroll);refreshStatus()
    }
    private fun journeyDay():Int {val start=prefs.getLong("journey_start",0L);if(start==0L){prefs.edit().putLong("journey_start",System.currentTimeMillis()).apply();return 1};return (((System.currentTimeMillis()-start)/86400000L).toInt()+1).coerceIn(1,90)}
    private fun title(root:LinearLayout,s:String){root.addView(TextView(this).apply{text=s;textSize=21f;setTextColor(dark);typeface=android.graphics.Typeface.DEFAULT_BOLD;setPadding(2,20,2,8)})}
    private fun section(root:LinearLayout,h:String,b:String){val box=LinearLayout(this).apply{orientation=1;setPadding(16,14,16,14);setBackgroundColor(Color.WHITE)};box.addView(TextView(this).apply{text=h;textSize=19f;setTextColor(dark);typeface=android.graphics.Typeface.DEFAULT_BOLD});box.addView(TextView(this).apply{text=b;textSize=15f;setTextColor(Color.DKGRAY);setPadding(0,6,0,0)});val lp=LinearLayout.LayoutParams(-1,-2);lp.bottomMargin=9;root.addView(box,lp)}
    private fun refreshStatus(){if(!::serviceStatus.isInitialized)return;val m=getSystemService(ACCESSIBILITY_SERVICE) as AccessibilityManager;val on=m.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK).any{it.resolveInfo.serviceInfo.packageName==packageName&&it.resolveInfo.serviceInfo.name.endsWith("AppBlockAccessibilityService")};serviceStatus.text=if(on)"✓ Accessibility service enabled" else "Not enabled — tap the button and enable Falah"}
}
