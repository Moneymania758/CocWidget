package com.cocwidget

import android.app.Activity
import android.app.TimePickerDialog
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.Planner.Group
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

// What to start when a builder, the lab or the pet house frees up, so it finishes while you're
// awake rather than overnight.
class BedtimeActivity : Activity() {
    private lateinit var content: LinearLayout
    private lateinit var ui: Ui
    private val pictures = Pictures(this)
    // Queues showing every option, not just the first few.
    private val expanded = mutableSetOf<Section>()

    private class Queue(val section: Section, val title: Int, val slots: Int, val groups: Set<Group>)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)
        content = findViewById(R.id.content)
    }

    override fun onResume() {
        super.onResume()
        show()
    }

    private fun show() {
        ui = Ui(this, Skin.current(this))
        ui.applyWindow(findViewById<ScrollView>(R.id.scroll))
        content.removeAllViews()
        pictures.clear()
        content.addView(header())

        val tag = Store.selected(this)
        val village = tag?.let { Store.village(this, it) }
        val json = tag?.let { Store.exportJson(this, it) }
        val items = GameData.all(this)
        val eventPercent = if (Store.eventOn(this)) EventDiscount.PERCENT else 0
        val steps = json?.let { Planner.nextSteps(it, items, eventPercent) }
        if (village == null || steps == null) {
            content.addView(ui.card().apply { addView(ui.text(getString(R.string.planner_none), 15f)) }, ui.matchWidth(16))
            return
        }

        val sleep = Store.sleep(this)
        content.addView(sleepCard(sleep), ui.matchWidth(16))
        val queues = listOfNotNull(
            Queue(Section.BUILDERS, R.string.bedtime_builders, village.builders, Group.entries.filter { it.usesBuilders }.toSet()),
            Queue(Section.LAB, R.string.group_lab, 1, setOf(Group.LAB)),
            Queue(Section.PETS, R.string.group_pets, 1, setOf(Group.PETS)).takeIf { village.hasPetHouse },
        )
        val now = System.currentTimeMillis()
        for (queue in queues) {
            content.addView(queueCard(queue, village, steps.filter { it.group in queue.groups }, sleep, items, now), ui.matchWidth(16))
        }
        var notes = getString(R.string.bedtime_notes)
        if (eventPercent > 0) notes += " " + getString(R.string.planner_event, eventPercent)
        content.addView(ui.text(notes, 12f, ui.skin.dim), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
        pictures.load()
    }

    private fun header() = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(this@BedtimeActivity).apply {
            gravity = Gravity.CENTER
            contentDescription = getString(R.string.back)
            addView(ui.icon(R.drawable.ic_back, ui.skin.text, 22))
            ui.pressable(this, 22f) { finish() }
        }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
        addView(ui.heading(getString(R.string.bedtime_open), 22f, ui.skin.title), ui.weighted(4))
    }

    private fun sleepCard(sleep: Bedtime.Sleep) = ui.card().apply {
        addView(ui.text(getString(R.string.bedtime_sleep), 13f, ui.skin.dim))
        addView(LinearLayout(this@BedtimeActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(timeChip(sleep.bedMinute, R.string.bedtime_bed) { Store.setSleep(this@BedtimeActivity, Bedtime.Sleep(it, sleep.wakeMinute)) })
            addView(ui.text(getString(R.string.bedtime_to), 15f, ui.skin.dim), ui.wrap().apply { marginStart = ui.dp(10); marginEnd = ui.dp(10) })
            addView(timeChip(sleep.wakeMinute, R.string.bedtime_wake) { Store.setSleep(this@BedtimeActivity, Bedtime.Sleep(sleep.bedMinute, it)) })
        }, ui.matchWidth(8))
        addView(ui.text(getString(R.string.bedtime_explain), 13f, ui.skin.dim), ui.matchWidth(10))
    }

    private fun timeChip(minute: Int, label: Int, save: (Int) -> Unit) = ui.chip(clock(minute), colour = ui.skin.accent).apply {
        textSize = 18f
        setTypeface(typeface, android.graphics.Typeface.BOLD)
        minHeight = ui.dp(44)
        gravity = Gravity.CENTER_VERTICAL
        contentDescription = getString(label, clock(minute))
        ui.pressable(this, 8f) {
            val is24 = android.text.format.DateFormat.is24HourFormat(this@BedtimeActivity)
            TimePickerDialog(this@BedtimeActivity, { _, hour, picked ->
                save(hour * 60 + picked)
                show()
            }, minute / 60, minute % 60, is24).show()
        }
    }

    private fun queueCard(
        queue: Queue,
        village: Village,
        steps: List<Planner.Step>,
        sleep: Bedtime.Sleep,
        items: Map<Int, GameItem>,
        now: Long,
    ) = ui.card(0).apply {
        val zone = ZoneId.systemDefault()
        val free = Bedtime.freeAt(village, queue.section, queue.slots, now)
        val start = Bedtime.startAt(free, sleep, zone)
        val freeText = when {
            free <= now -> getString(R.string.bedtime_free_now)
            start != free -> getString(R.string.bedtime_free_asleep, moment(free, now), moment(start, now))
            else -> getString(R.string.bedtime_free_at, moment(free, now))
        }
        addView(LinearLayout(this@BedtimeActivity).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(10))
            addView(ui.heading(getString(queue.title), if (ui.skin.headingFont != null) 18f else 15f))
            addView(ui.text(freeText, 13f, ui.skin.dim), ui.matchWidth(3))
        })

        val options = Bedtime.options(steps, start, sleep, zone)
        val awake = options.filter { it.awake }
        val message = when {
            options.isEmpty() -> getString(R.string.bedtime_nothing_left)
            awake.isEmpty() -> getString(R.string.bedtime_none_awake)
            else -> null
        }
        if (message != null) {
            addView(ui.text(message, 14f, ui.skin.dim).apply { setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(14)) })
            return@apply
        }

        val all = queue.section in expanded
        val shown = if (all) awake else awake.take(SHORT_LIST)
        for (option in shown) {
            addView(ui.divider())
            addView(optionRow(option, items[option.step.id], now))
        }
        if (awake.size > shown.size) {
            addView(ui.divider())
            addView(ui.text(getString(R.string.bedtime_show_all, awake.size), 14f, ui.skin.accent, bold = true).apply {
                minHeight = ui.dp(48)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(ui.dp(16), 0, ui.dp(16), 0)
                ui.pressable(this, 0f) {
                    expanded += queue.section
                    show()
                }
            }, ui.matchWidth())
        }
        val asleep = options.size - awake.size
        if (asleep > 0) {
            addView(ui.text(resources.getQuantityString(R.plurals.bedtime_asleep_count, asleep, asleep), 12f, ui.skin.dim).apply {
                setPadding(ui.dp(16), ui.dp(4), ui.dp(16), ui.dp(14))
            })
        }
    }

    private fun optionRow(option: Bedtime.Option, game: GameItem?, now: Long) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        setPadding(ui.dp(16), ui.dp(10), ui.dp(16), ui.dp(10))
        val step = option.step

        val picture = ImageView(this@BedtimeActivity).apply {
            background = ui.rounded(ui.skin.chip, 8f)
            setPadding(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3))
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        pictures.add(game?.let { Icons.path(it, step.toLevel) }, picture)
        addView(picture, LinearLayout.LayoutParams(ui.dp(36), ui.dp(36)))

        addView(LinearLayout(this@BedtimeActivity).apply {
            orientation = LinearLayout.VERTICAL
            var name = if (step.supercharge > 0) {
                getString(R.string.bedtime_supercharge, step.name, step.supercharge)
            } else {
                getString(R.string.bedtime_step, step.name, step.toLevel)
            }
            if (step.count > 1) name += " " + getString(R.string.planner_count, step.count)
            addView(ui.text(name, 15f, bold = true))
            addView(ui.flow().apply {
                addView(ui.text(PlanFormat.duration(step.seconds), 13f, ui.skin.dim))
                addView(ui.text(getString(R.string.bedtime_done_at, moment(option.finishMillis, now)), 13f, ui.skin.green, bold = true))
                for ((resource, cost) in PlanFormat.ordered(step.costs)) addView(ui.costChip(resource, PlanFormat.amount(cost)))
            }, ui.matchWidth(4))
        }, ui.weighted(12))
    }

    private fun clock(minute: Int): String =
        LocalTime.of(minute / 60, minute % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT))

    // "21:40" today, "Wed 07:15" this week, "12 Nov 14:00" further out.
    private fun moment(millis: Long, now: Long): String {
        var flags = DateUtils.FORMAT_SHOW_TIME
        if (!DateUtils.isToday(millis)) {
            flags = flags or if (millis - now < 6 * DateUtils.DAY_IN_MILLIS) {
                DateUtils.FORMAT_SHOW_WEEKDAY or DateUtils.FORMAT_ABBREV_WEEKDAY
            } else {
                DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH or DateUtils.FORMAT_NO_YEAR
            }
        }
        return DateUtils.formatDateTime(this, millis, flags)
    }

    companion object {
        private const val SHORT_LIST = 8
    }
}
