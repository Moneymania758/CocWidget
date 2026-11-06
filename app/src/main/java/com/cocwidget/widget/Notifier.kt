package com.cocwidget.widget

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.cocwidget.MainActivity
import com.cocwidget.R
import com.cocwidget.game.Section
import com.cocwidget.game.Upgrade
import com.cocwidget.game.Village
import com.cocwidget.store.Store

object Notifier {
    private const val CHANNEL_FINISHED = "finished"
    private const val CLASH_PACKAGE = "com.supercell.clashofclans"
    // Anything that finished longer ago than this (phone off, app just installed) stays quiet.
    private const val MAX_LATENESS_MS = 2 * 60 * 60_000L

    // Posts one notification for each upgrade that has finished since the last check. [account]
    // names the village when there's more than one, so people know which one to log into.
    fun notifyFinished(context: Context, tag: String, village: Village, now: Long, account: String?) {
        val decision = decide(village, Store.notifiedUpgrades(context, tag), now)
        if (decision.toNotify.isNotEmpty()) {
            val manager = context.getSystemService(NotificationManager::class.java)
            createChannel(context, manager)
            for ((key, upgrade) in decision.toNotify) {
                manager.notify("$tag:$key".hashCode(), build(context, upgrade, village, account))
            }
        }
        Store.setNotifiedUpgrades(context, tag, decision.notified)
    }

    class Decision(val toNotify: List<Pair<String, Upgrade>>, val notified: Set<String>)

    // Which upgrades to announce now, and the keys to remember as already handled.
    fun decide(village: Village, notified: Set<String>, now: Long): Decision {
        val current = village.upgrades.associateBy { key(village, it) }
        val finished = current.filterValues { village.endMillis(it) <= now }
        val toNotify = finished.filter { (key, upgrade) ->
            key !in notified && now - village.endMillis(upgrade) <= MAX_LATENESS_MS
        }
        // Only keys still in the export are kept, so the set doesn't grow forever.
        return Decision(toNotify.toList(), (notified intersect current.keys) + finished.keys)
    }

    private fun build(context: Context, upgrade: Upgrade, village: Village, account: String?): Notification {
        val title = if (upgrade.supercharge > 0) {
            context.getString(R.string.notify_supercharge_finished_title, upgrade.name, upgrade.supercharge)
        } else {
            context.getString(R.string.notify_finished_title, upgrade.name, upgrade.toLevel)
        }
        val freed = context.getString(
            when {
                upgrade.section == Section.BUILDER_BASE -> R.string.notify_builder_base
                upgrade.extra -> R.string.notify_goblin_free
                upgrade.section == Section.BUILDERS -> R.string.notify_builder_free
                upgrade.section == Section.LAB -> R.string.notify_lab_free
                else -> R.string.notify_pet_house_free
            },
        )
        // In the main text, as sub-text is small and easy to miss. The Town Hall is 0 if the export
        // didn't list one.
        val text = when {
            account == null -> freed
            village.townHall > 0 -> context.getString(R.string.notify_account, account, village.townHall, freed)
            else -> context.getString(R.string.notify_account_no_town_hall, account, freed)
        }
        val open = context.packageManager.getLaunchIntentForPackage(CLASH_PACKAGE)
            ?: Intent(context, MainActivity::class.java)
        val tap = PendingIntent.getActivity(context, 2, open, PendingIntent.FLAG_IMMUTABLE)
        return Notification.Builder(context, CHANNEL_FINISHED)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setWhen(village.endMillis(upgrade))
            .setShowWhen(true)
            .setContentIntent(tap)
            .setAutoCancel(true)
            .build()
    }

    private fun createChannel(context: Context, manager: NotificationManager) {
        val channel = NotificationChannel(
            CHANNEL_FINISHED,
            context.getString(R.string.channel_finished),
            NotificationManager.IMPORTANCE_DEFAULT,
        )
        manager.createNotificationChannel(channel)
    }

    // workSec tells apart two copies of the same building at the same level.
    private fun key(village: Village, upgrade: Upgrade) =
        "${village.exportedAtMillis}:${upgrade.section}:${upgrade.name}:${upgrade.toLevel}:${upgrade.extra}:${upgrade.workSec}"
}
