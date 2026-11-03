package com.cocwidget

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import android.text.format.DateUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import com.cocwidget.Planner.Group
import java.text.DateFormat
import kotlin.concurrent.thread

// Time and resources left to max the current Town Hall, like Clash Ninja's "Remaining" panel.
class PlannerActivity : Activity() {
    private val open = mutableSetOf<Group>()
    private lateinit var content: LinearLayout
    private lateinit var ui: Ui
    // Each item's picture, filled in once the icons have downloaded.
    private val pictures = mutableMapOf<String, MutableList<ImageView>>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_screen)
        content = findViewById(R.id.content)
    }

    // Recalculated on every visit, so a sync made meanwhile shows.
    override fun onResume() {
        super.onResume()
        show()
    }

    private fun show() {
        ui = Ui(this, Skin.current(this))
        ui.applyWindow(findViewById<ScrollView>(R.id.scroll))
        content.removeAllViews()
        pictures.clear()

        val tag = Store.selected(this)
        val village = tag?.let { Store.village(this, it) }
        val json = tag?.let { Store.exportJson(this, it) }
        val items = GameData.all(this)
        val eventPercent = if (Store.eventOn(this)) EventDiscount.PERCENT else 0
        val plan = if (village != null && json != null) Planner.plan(json, items, village.builders, eventPercent) else null

        content.addView(header(plan?.townHall))
        if (village == null || plan == null) {
            content.addView(ui.card().apply { addView(ui.text(getString(R.string.planner_none), 15f)) }, ui.matchWidth(16))
            return
        }
        content.addView(summary(plan, village.exportedAtMillis, eventPercent), ui.matchWidth(16))
        content.addView(ui.text(getString(R.string.planner_tap), 13f, ui.skin.dim), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
        content.addView(ui.card(0).apply {
            for ((index, group) in plan.groups.withIndex()) {
                if (index > 0) addView(ui.divider())
                addGroup(this, group, items)
            }
        }, ui.matchWidth(8))
        content.addView(ui.text(getString(R.string.planner_notes), 12f, ui.skin.dim), ui.matchWidth(16).apply { marginStart = ui.dp(4) })
        loadPictures()
    }

    private fun header(townHall: Int?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(LinearLayout(this@PlannerActivity).apply {
            gravity = Gravity.CENTER
            contentDescription = getString(R.string.back)
            addView(ui.icon(R.drawable.ic_back, ui.skin.text, 22))
            ui.pressable(this, 22f) { finish() }
        }, LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
        addView(ui.heading(getString(R.string.planner_open), 22f, ui.skin.title), ui.weighted(4))
        if (townHall != null) {
            addView(ui.chip(getString(R.string.town_hall_chip, townHall), colour = ui.skin.accent).apply { setTypeface(typeface, android.graphics.Typeface.BOLD) })
        }
    }

    private fun summary(plan: Planner.Plan, exportedAt: Long, eventPercent: Int) = ui.card().apply {
        val estimate = plan.builderEstimateSeconds()
        addView(ui.text(getString(R.string.planner_could_finish), 13f, ui.skin.dim))
        addView(ui.heading(PlanFormat.duration(estimate), 36f), ui.matchWidth(2))
        var detail = resources.getQuantityString(R.plurals.planner_builders_detail, plan.builders, plan.builders, PlanFormat.duration(plan.builderSeconds))
        if (plan.longestName.isNotEmpty() && estimate == plan.longestSeconds) detail += " " + getString(R.string.planner_pace, plan.longestName)
        addView(ui.text(detail, 13f, ui.skin.dim), ui.matchWidth(4))

        val tiles = listOfNotNull(
            plan.group(Group.LAB)?.let { R.string.group_lab to it.seconds },
            plan.group(Group.PETS)?.let { R.string.group_pets to it.seconds },
        )
        if (tiles.isNotEmpty()) {
            addView(LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                for ((index, tile) in tiles.withIndex()) {
                    addView(LinearLayout(this@PlannerActivity).apply {
                        orientation = LinearLayout.VERTICAL
                        background = ui.rounded(ui.skin.chip, 12f)
                        setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
                        addView(ui.text(getString(tile.first), 13f, ui.skin.dim))
                        addView(ui.heading(PlanFormat.duration(tile.second), 20f), ui.matchWidth(2))
                    }, ui.weighted(if (index == 0) 0 else 10))
                }
            }, ui.matchWidth(14))
        }

        val exported = DateUtils.formatSameDayTime(exportedAt, System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT)
        addView(ui.text(getString(R.string.planner_source, exported), 12f, ui.skin.dim), ui.matchWidth(14))
        if (eventPercent > 0) addView(ui.text(getString(R.string.planner_event, eventPercent), 12f, ui.skin.dim), ui.matchWidth(2))
    }

    private fun addGroup(card: LinearLayout, group: Planner.GroupPlan, items: Map<Int, GameItem>) {
        val done = group.seconds == 0L && group.costs.isEmpty()
        val percent = (group.done * 100).toInt()
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(16), ui.dp(14), ui.dp(16), ui.dp(14))
            addView(LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(ui.heading(getString(titleOf(group.group)), if (ui.skin.headingFont != null) 18f else 15f), ui.weighted())
                val left = if (done) getString(R.string.planner_group_done) else PlanFormat.duration(group.seconds).takeIf { group.seconds > 0 }
                if (left != null) addView(ui.text(left, 14f, ui.skin.dim))
                addView(ui.text(getString(R.string.planner_percent, percent), 14f, if (group.done >= 1f) ui.skin.green else ui.skin.accent, bold = true).apply {
                    gravity = Gravity.END
                }, LinearLayout.LayoutParams(ui.dp(52), LinearLayout.LayoutParams.WRAP_CONTENT))
            })
            addView(ui.bar(group.done).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(8) })
            if (group.costs.isNotEmpty()) addView(costs(group.costs), ui.matchWidth(8))
        }
        card.addView(header)

        // Folding shows or hides the group's items in place, so the scroll position doesn't move.
        val details = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(ui.skin.sub)
            setPadding(ui.dp(16), 0, ui.dp(16), ui.dp(4))
        }
        card.addView(details)
        fun refresh() {
            details.visibility = if (group.group in open) View.VISIBLE else View.GONE
        }
        ui.pressable(header, 0f) {
            if (!open.remove(group.group)) open += group.group
            refresh()
        }
        refresh()

        val shown = group.items.filter { it.seconds > 0 || it.costs.isNotEmpty() }
        for ((index, item) in shown.withIndex()) {
            if (index > 0) details.addView(ui.divider())
            details.addView(itemRow(item, items[item.id]))
        }
    }

    private fun itemRow(item: Planner.Item, game: GameItem?) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        setPadding(0, ui.dp(12), 0, ui.dp(12))

        val picture = ImageView(this@PlannerActivity).apply {
            background = ui.rounded(ui.skin.chip, 8f)
            setPadding(ui.dp(3), ui.dp(3), ui.dp(3), ui.dp(3))
            scaleType = ImageView.ScaleType.FIT_CENTER
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        game?.let { Icons.path(it, item.levels.maxOrNull() ?: 1) }?.let { pictures.getOrPut(it) { mutableListOf() } += picture }
        addView(picture, LinearLayout.LayoutParams(ui.dp(40), ui.dp(40)))

        addView(LinearLayout(this@PlannerActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(LinearLayout(this@PlannerActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                addView(ui.text(item.name, 15f, bold = true), ui.wrap())
                if (item.levels.size > 1) {
                    addView(ui.text(getString(R.string.planner_count, item.levels.size), 15f, ui.skin.dim), ui.wrap().apply { marginStart = ui.dp(6) })
                }
                addView(View(this@PlannerActivity), ui.weighted())
                addView(ui.text(getString(R.string.planner_max, item.maxLevel), 13f, ui.skin.dim))
            })
            addView(ui.flow().apply {
                for (chip in PlanFormat.levelChips(item.levels, item.running, item.maxLevel)) addView(levelChip(chip))
            }, ui.matchWidth(8))
            val time = PlanFormat.duration(item.seconds).takeIf { item.seconds > 0 }
            if (item.costs.isEmpty() && time != null) {
                addView(ui.text(getString(R.string.planner_running_left, time), 13f, ui.skin.dim), ui.matchWidth(8))
            } else {
                addView(ui.flow().apply {
                    if (time != null) addView(ui.text(time, 13f, ui.skin.dim))
                    for ((resource, cost) in PlanFormat.ordered(item.costs)) addView(ui.costChip(resource, PlanFormat.amount(cost)))
                }, ui.matchWidth(8))
            }
        }, ui.weighted(12))
    }

    private fun costs(costs: Map<String, Long>) = ui.flow().apply {
        for ((resource, cost) in PlanFormat.ordered(costs)) addView(ui.costChip(resource, PlanFormat.amount(cost)))
    }

    private fun levelChip(chip: PlanFormat.LevelChip) = when {
        chip.running -> ui.chip(
            if (chip.count > 1) getString(R.string.chip_running_count, chip.level, chip.level + 1, chip.count) else getString(R.string.chip_running, chip.level, chip.level + 1),
            ui.skin.runningBg, ui.skin.runningText,
        )
        chip.level == 0 -> ui.chip(if (chip.count > 1) getString(R.string.chip_unbuilt_count, chip.count) else getString(R.string.chip_unbuilt))
        else -> ui.chip(
            if (chip.count > 1) getString(R.string.chip_level_count, chip.level, chip.count) else getString(R.string.chip_level, chip.level),
            if (chip.maxed) ui.skin.maxedBg else ui.skin.chip,
            if (chip.maxed) ui.skin.green else ui.skin.text,
            if (chip.maxed) R.drawable.ic_check else null,
        )
    }

    // Pictures already cached show straight away. Missing ones download in the background, and
    // the rows fill in when they arrive.
    private fun loadPictures() {
        val wanted = pictures.toMap()
        thread {
            fun decode() = wanted.keys.mapNotNull { path -> Icons.load(this, path)?.let { path to it } }.toMap()
            post(decode(), wanted)
            if (Icons.fetchPaths(applicationContext, wanted.keys.toList())) post(decode(), wanted)
        }
    }

    private fun post(bitmaps: Map<String, Bitmap>, views: Map<String, List<ImageView>>) = runOnUiThread {
        if (isDestroyed) return@runOnUiThread
        for ((path, bitmap) in bitmaps) views[path]?.forEach { it.setImageBitmap(bitmap) }
    }

    private fun titleOf(group: Group): Int = when (group) {
        Group.DEFENCES -> R.string.group_defences
        Group.TRAPS -> R.string.group_traps
        Group.ARMY -> R.string.group_army
        Group.RESOURCES -> R.string.group_resources
        Group.WALLS -> R.string.group_walls
        Group.HEROES -> R.string.group_heroes
        Group.GUARDIANS -> R.string.group_guardians
        Group.LAB -> R.string.group_lab
        Group.PETS -> R.string.group_pets
    }
}
