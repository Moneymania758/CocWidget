package com.cocwidget.events

import android.content.Context
import com.cocwidget.store.Store
import com.cocwidget.widget.Notifier

// Reminders are per phone, not per account, and need neither an account nor a widget.
object EventReminders {
    private const val RECHECK_MS = 24 * 60 * 60_000L

    // Posts what's due. Returns when to check next, or null with every event switched off. Wakes at
    // least daily so a date added to the file is picked up in time. Does network, so only call this
    // off the main thread. Two callers can't post the same reminder, hence the lock.
    @Synchronized
    fun check(context: Context, now: Long): Long? {
        Store.startEventReminders(context, now)
        if (EventKind.entries.none { Store.eventReminderOn(context, it) }) return null

        EventDates.refreshIfDue(context, now)
        val decision = EventSchedule.decide(
            EventSchedule.reminders(now, EventDates.current(context)),
            { Store.eventReminderOn(context, it) },
            { Store.eventReminderSince(context, it) },
            Store.notifiedReminders(context),
            now,
        )
        for (reminder in decision.due) Notifier.notifyEvent(context, reminder)
        Store.setNotifiedReminders(context, decision.notified)
        return minOf(decision.next ?: Long.MAX_VALUE, now + RECHECK_MS)
    }

    // When the next reminder for [kind] would go off, whether or not it's switched on.
    fun nextFor(context: Context, kind: EventKind, now: Long): Long? =
        EventSchedule.reminders(now, EventDates.current(context)).firstOrNull { it.kind == kind && it.at > now }?.at
}
