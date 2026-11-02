package com.cocwidget

import android.app.Activity
import android.graphics.Typeface
import android.os.Bundle
import android.text.format.DateUtils
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import com.cocwidget.Planner.Group
import java.text.DateFormat

// Time and resources left to max the current Town Hall, like Clash Ninja's "Remaining" panel.
class PlannerActivity : Activity() {
    private val open = mutableSetOf<Group>()
    private lateinit var content: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_planner)
        content = findViewById(R.id.content)
    }

    // Recalculated on every visit, so a sync made meanwhile shows.
    override fun onResume() {
        super.onResume()
        show()
    }

    private fun show() {
        content.removeAllViews()
        val village = Store.village(this)
        val json = Store.exportJson(this)
        val eventPercent = if (Store.eventOn(this)) EventDiscount.PERCENT else 0
        val plan = if (village != null && json != null) {
            Planner.plan(json, GameData.all(this), village.builders, eventPercent)
        } else {
            null
        }
        if (village == null || plan == null) {
            content.addView(text(getString(R.string.planner_none)))
            return
        }

        content.addView(text(getString(R.string.planner_title, plan.townHall), 22f, bold = true))
        val exported = DateUtils.formatSameDayTime(village.exportedAtMillis, System.currentTimeMillis(), DateFormat.MEDIUM, DateFormat.SHORT)
        content.addView(text(getString(R.string.planner_source, exported), 13f, dim = true))
        if (eventPercent > 0) content.addView(text(getString(R.string.planner_event, eventPercent), 13f, dim = true))

        content.addView(text(resources.getQuantityString(R.plurals.planner_builders, plan.builders, PlanFormat.duration(plan.builderEstimateSeconds()), plan.builders), 17f, bold = true, top = 20))
        content.addView(text(getString(R.string.planner_builders_detail, PlanFormat.duration(plan.builderSeconds)), 13f, dim = true))
        plan.group(Group.LAB)?.let { content.addView(text(getString(R.string.planner_lab, PlanFormat.duration(it.seconds)), 17f, bold = true, top = 12)) }
        plan.group(Group.PETS)?.let { content.addView(text(getString(R.string.planner_pets, PlanFormat.duration(it.seconds)), 17f, bold = true, top = 12)) }

        content.addView(text(getString(R.string.planner_tap), 13f, dim = true, top = 20))
        for (group in plan.groups) addGroup(group)
        content.addView(text(getString(R.string.planner_notes), 12f, dim = true, top = 24))
    }

    private fun addGroup(group: Planner.GroupPlan) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(14), 0, dp(4))
        }
        val percent = (group.done * 100).toInt()
        val title = text("", 16f, bold = true)
        header.addView(title)
        header.addView(ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 1000
            progress = (group.done * 1000).toInt()
        })
        val left = if (group.seconds == 0L && group.costs.isEmpty()) {
            getString(R.string.planner_group_done)
        } else {
            listOf(PlanFormat.duration(group.seconds).takeIf { group.seconds > 0 }, PlanFormat.costs(group.costs).takeIf { it.isNotEmpty() })
                .filterNotNull()
                .joinToString(" · ")
        }
        header.addView(text(left, 14f))
        content.addView(header)

        // Folding shows or hides the group's items in place. Rebuilding the whole screen instead
        // could move the scroll position.
        val details = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        content.addView(details)
        fun refresh() {
            val isOpen = group.group in open
            title.text = getString(R.string.planner_group, if (isOpen) "▾" else "▸", getString(titleOf(group.group)), percent)
            details.visibility = if (isOpen) View.VISIBLE else View.GONE
        }
        header.setOnClickListener {
            if (!open.remove(group.group)) open += group.group
            refresh()
        }
        refresh()

        for (item in group.items) {
            if (item.seconds == 0L && item.costs.isEmpty()) continue
            val name = if (item.levels.size > 1) getString(R.string.planner_item, item.name, item.levels.size) else item.name
            val levels = getString(R.string.planner_item_levels, PlanFormat.levels(item.levels), item.maxLevel)
            details.addView(text("$name   $levels", 14f, bold = true, top = 8, start = 16))
            val detail = listOf(PlanFormat.duration(item.seconds).takeIf { item.seconds > 0 }, PlanFormat.costs(item.costs).takeIf { it.isNotEmpty() })
                .filterNotNull()
                .joinToString(" · ")
            details.addView(text(detail, 13f, dim = true, start = 16))
        }
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

    private fun text(value: String, size: Float = 14f, bold: Boolean = false, dim: Boolean = false, top: Int = 0, start: Int = 0) =
        TextView(this).apply {
            text = value
            textSize = size
            if (bold) typeface = Typeface.DEFAULT_BOLD
            if (dim) alpha = 0.7f
            setPadding(dp(start), dp(top), 0, 0)
        }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
