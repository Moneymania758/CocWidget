package com.cocwidget

import android.view.Gravity
import android.widget.LinearLayout
import com.cocwidget.game.BUILDER_BITE_MULTIPLIER
import com.cocwidget.game.BUILDER_POTION_MULTIPLIER
import com.cocwidget.game.PET_POTION_MULTIPLIER
import com.cocwidget.game.RESEARCH_POTION_MULTIPLIER
import com.cocwidget.game.STUDY_SOUP_MULTIPLIER
import com.cocwidget.game.Section
import com.cocwidget.game.Village
import com.cocwidget.plan.PlanFormat
import com.cocwidget.plan.PotionAdvisor
import com.cocwidget.store.Store
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Tab

// Whether now is a good time for a Builder, Research or Pet potion: does every slot stay busy
// for the whole hour?
class PotionsActivity : Screen() {
    override val tab = Tab.PLAN

    private class Item(val name: Int, val multiplier: Int)

    // [idle] says how many slots are idle.
    private class Queue(val section: Section, val title: Int, val slots: Int, val idle: (Int) -> String, val items: List<Item>)

    override fun build() {
        content.addView(tabHeader(R.string.tab_plan))
        content.addView(planSwitch(PotionsActivity::class.java), ui.matchWidth(12))
        val village = Store.current(this)?.village
        if (village == null) {
            addNothingSynced()
            return
        }
        addNote(getString(R.string.potions_explain), 13f)
        val queues = listOfNotNull(
            Queue(
                Section.BUILDERS, R.string.bedtime_builders, village.builders, { resources.getQuantityString(R.plurals.potions_idle_builders, it, it) },
                listOf(Item(R.string.potion_builder, BUILDER_POTION_MULTIPLIER), Item(R.string.potion_bite, BUILDER_BITE_MULTIPLIER)),
            ),
            Queue(
                Section.LAB, R.string.group_lab, 1, { getString(R.string.potions_idle_lab) },
                listOf(Item(R.string.potion_research, RESEARCH_POTION_MULTIPLIER), Item(R.string.potion_soup, STUDY_SOUP_MULTIPLIER)),
            ),
            Queue(
                Section.PETS, R.string.group_pets, 1, { getString(R.string.potions_idle_pets) },
                listOf(Item(R.string.potion_pet, PET_POTION_MULTIPLIER)),
            ).takeIf { village.hasPetHouse },
        )
        val now = System.currentTimeMillis()
        for (queue in queues) content.addView(queueCard(queue, village, now), ui.matchWidth(16))
        addNote(getString(R.string.potions_notes))
    }

    private fun queueCard(queue: Queue, village: Village, now: Long) = ui.card().apply {
        addView(ui.heading(getString(queue.title), if (ui.skin.headingFont != null) 18f else 15f))
        for ((index, item) in queue.items.withIndex()) {
            val advice = PotionAdvisor.advise(village, queue.section, queue.slots, item.multiplier, now)
            if (advice.boostLeftSec > 0) {
                addView(ui.text(getString(R.string.potions_running, PlanFormat.duration(advice.boostLeftSec)), 14f, ui.skin.dim), ui.matchWidth(6))
                return@apply
            }
            addView(itemRow(item, advice), ui.matchWidth(if (index == 0) 10 else 14))
            if (index == 0 && advice.idle > 0) {
                addView(reason(queue.idle(advice.idle)))
            }
            for ((upgrade, seconds) in advice.finishing) {
                addView(reason(getString(R.string.potions_finishing, upgrade.name, PlanFormat.duration(seconds))))
            }
        }
    }

    private fun itemRow(item: Item, advice: PotionAdvisor.Advice) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val share = if (advice.possibleSec > 0) advice.savedSec.toFloat() / advice.possibleSec else 0f
        val (verdict, colour) = when {
            advice.full -> R.string.potions_now to ui.skin.green
            share >= MOSTLY -> R.string.potions_mostly to ui.skin.accent
            else -> R.string.potions_wait to ui.skin.dim
        }
        addView(LinearLayout(this@PotionsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(ui.text(getString(item.name, item.multiplier), 15f, bold = true), ui.weighted())
            addView(ui.text(getString(verdict), 14f, colour, bold = true))
        })
        addView(ui.text(getString(R.string.potions_saves, PlanFormat.duration(advice.savedSec), PlanFormat.duration(advice.possibleSec)), 13f, ui.skin.dim), ui.matchWidth(3))
        addView(ui.bar(share).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(6) })
    }

    private fun reason(text: String) = ui.text("• $text", 13f, ui.skin.dim).apply { setPadding(0, ui.dp(4), 0, 0) }

    companion object {
        // Below this share of the hour's possible saving, it's better to wait.
        private const val MOSTLY = 0.75f
    }
}
