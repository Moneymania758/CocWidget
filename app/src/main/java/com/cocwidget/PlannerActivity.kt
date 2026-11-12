package com.cocwidget

import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import com.cocwidget.game.GameData
import com.cocwidget.game.GameItem
import com.cocwidget.plan.PlanFormat
import com.cocwidget.plan.Planner
import com.cocwidget.plan.Planner.Group
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.ui.Pictures
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Tab
import com.cocwidget.ui.TabChrome
import com.cocwidget.ui.TimeText

// Time and resources left to max the current Town Hall, like Clash Ninja's "Remaining" panel.
class PlannerActivity : Screen() {
    override val tab = Tab.PLAN

    private val open = mutableSetOf<Group>()
    private val pictures = Pictures(this)

    override fun build() {
        pictures.clear()
        val current = Store.current(this)
        val items = GameData.all(this)
        val eventPercent = Store.eventPercent(this)
        val plan = current?.let { Planner.plan(it.json, items, it.village.builders, eventPercent) }

        addTabTop(TabChrome.PLAN)
        if (current == null || plan == null) {
            addNothingSynced()
            return
        }
        content.addView(summary(plan, current.village.exportedAtMillis, eventPercent), ui.matchWidth(16))
        addNote(getString(R.string.planner_tap), 13f)
        content.addView(ui.card(0).apply {
            for ((index, group) in plan.groups.withIndex()) {
                if (index > 0) addView(ui.divider())
                addGroup(this, group, items)
            }
        }, ui.matchWidth(8))
        var notes = getString(R.string.planner_notes)
        if (plan.unknownEpics > 0) notes += " " + resources.getQuantityString(R.plurals.planner_unknown_epics, plan.unknownEpics, plan.unknownEpics)
        addNote(notes)
        pictures.load()
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

        val exported = TimeText.exported(exportedAt, System.currentTimeMillis())
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

        val picture = ui.picture()
        pictures.add(game?.let { Icons.path(it, item.levels.maxOrNull() ?: 1) }, picture)
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
                if (item.supercharges > 0) {
                    addView(ui.chip(resources.getQuantityString(R.plurals.chip_supercharges, item.supercharges, item.supercharges), ui.skin.runningBg, ui.skin.runningText))
                }
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
        Group.EQUIPMENT -> R.string.group_equipment
    }
}
