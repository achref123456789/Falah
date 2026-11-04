package com.falah.app

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.widget.*
import java.text.SimpleDateFormat
import java.util.*

class PrayerLockActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val prayer = intent.getStringExtra("prayer") ?: "Prayer"
        val green = Color.rgb(17, 105, 63)
        val root = LinearLayout(this).apply { orientation = 1; gravity = Gravity.CENTER; setPadding(28, 28, 28, 28); setBackgroundColor(green) }
        root.addView(TextView(this).apply { text = "🕌  FALAH"; textSize = 30f; setTextColor(Color.WHITE); gravity = Gravity.CENTER })
        root.addView(TextView(this).apply { text = "It's time for $prayer"; textSize = 25f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 30, 0, 12) })
        root.addView(TextView(this).apply { text = "Have you prayed?"; textSize = 19f; setTextColor(Color.WHITE); gravity = Gravity.CENTER; setPadding(0, 0, 0, 28) })
        root.addView(Button(this).apply { text = "YES — I PRAYED"; setOnClickListener {
            val key = SimpleDateFormat("yyyyMMdd", Locale.US).format(Date())
            getSharedPreferences("falah", MODE_PRIVATE).edit().putBoolean("done_${key}_$prayer", true).apply()
            Toast.makeText(this@PrayerLockActivity, "May Allah accept your prayer 🤲", Toast.LENGTH_LONG).show()
            finish()
        } })
        root.addView(Button(this).apply { text = "NO — NOT YET"; setOnClickListener { moveTaskToBack(true); finish() } })
        setContentView(root)
    }
    override fun onBackPressed() { moveTaskToBack(true) }
}
