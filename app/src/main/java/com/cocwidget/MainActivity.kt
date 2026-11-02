package com.cocwidget

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat

class MainActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val name = findViewById<EditText>(R.id.name)
        name.setText(Store.displayName(this))
        findViewById<Button>(R.id.save_name).setOnClickListener {
            Store.setDisplayName(this, name.text.toString().trim())
            VillageWidget.requestRefresh(this)
            Toast.makeText(this, R.string.name_saved, Toast.LENGTH_SHORT).show()
        }
        findViewById<Button>(R.id.sync).setOnClickListener {
            startActivity(Intent(this, SyncActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        val village = Store.village(this)
        val now = System.currentTimeMillis()
        findViewById<TextView>(R.id.status).text = if (village == null) {
            getString(R.string.status_none)
        } else {
            val exported = DateUtils.formatSameDayTime(village.exportedAtMillis, now, DateFormat.MEDIUM, DateFormat.SHORT)
            val running = village.upgrades.count { village.endMillis(it) > now }
            getString(R.string.status_synced, exported, running)
        }
    }
}
