package com.cocwidget.events

import android.app.NotificationManager
import android.content.Context
import com.cocwidget.store.Store
import com.cocwidget.widget.Notifier

// Reminders are per phone, not per account, and need neither an account nor a widget.
object EventReminders {
    private const val RECHECK_MS = 24 * 60 * 60_000L

    // Posts what's due. Returns when to check next, or null with every event switched off. Wakes at
    // least daily so a date added to the file is picked up in time. Two callers can't post the same
    // reminder, hence the lock.
    @Synchronized
    fun check(context: Context, now: Long): Long? {
        Store.startEventReminders(context, now)
        if (!anyOn(context)) return null

        val decision = EventSchedule.decide(
            EventSchedule.reminders(now, EventDates.current(context)),
            { Store.eventReminderOn(context, it) },
            { Store.eventReminderSince(context, it) },
            Store.notifiedReminders(context),
            now,
        )
        // With notifications refused, nothing counts as sent, so turning them on catches up.
        if (notificationsAllowed(context)) {
            for (reminder in decision.due) Notifier.notifyEvent(context, reminder)
            Store.setNotifiedReminders(context, decision.notified)
        }
        return minOf(decision.next ?: Long.MAX_VALUE, now + RECHECK_MS)
    }

    // True if a new dates file arrived. [download] is network, so only call this off the main thread.
    // [widgetPlaced] means an Events widget is on the home screen, which wants dates even with every
    // reminder off.
    fun refreshDates(context: Context, now: Long, download: () -> String?, widgetPlaced: Boolean = false): Boolean =
        (widgetPlaced || anyOn(context)) && EventDates.refreshIfDue(context, now, download)

    fun notificationsAllowed(context: Context): Boolean =
        context.getSystemService(NotificationManager::class.java).areNotificationsEnabled()

    private fun anyOn(context: Context) = EventKind.entries.any { Store.eventReminderOn(context, it) }

    // When the next reminder for [kind] would go off, whether or not it's switched on.
    fun nextFor(context: Context, kind: EventKind, now: Long): Long? =
        EventSchedule.reminders(now, EventDates.current(context)).firstOrNull { it.kind == kind && it.at > now }?.at
}
