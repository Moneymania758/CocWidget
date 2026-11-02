package com.cocwidget

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import java.text.DateFormat
import kotlin.concurrent.thread

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
        findViewById<Button>(R.id.update).setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UpdateChecker.RELEASES_PAGE)))
        }
        val event = findViewById<Switch>(R.id.event)
        event.isChecked = Store.eventOn(this)
        event.setOnCheckedChangeListener { _, on -> Store.setEventOn(this, on) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_REQUEST)
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

        showUpdate()
        thread {
            UpdateChecker.checkIfDue(applicationContext, UpdateChecker.OPEN_APP_INTERVAL_MS)
            runOnUiThread { if (!isDestroyed) showUpdate() }
        }
    }

    private fun showUpdate() {
        val button = findViewById<Button>(R.id.update)
        val update = UpdateChecker.availableUpdate(this)
        button.visibility = if (update != null) View.VISIBLE else View.GONE
        if (update != null) button.text = getString(R.string.update_available_long, update)
    }

    private companion object {
        const val NOTIFICATION_REQUEST = 1
    }
}
