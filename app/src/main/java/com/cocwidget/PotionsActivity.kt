package com.cocwidget

import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.widget.ImageView
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
import com.cocwidget.store.Icons
import com.cocwidget.store.Store
import com.cocwidget.ui.Pictures
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Tab
import com.cocwidget.ui.TabChrome
import com.cocwidget.widget.WidgetRenderer

// Whether now is a good time for a Builder, Research or Pet potion: does every slot stay busy
// for the whole hour?
class PotionsActivity : Screen() {
    override val tab = Tab.PLAN

    private val pictures = Pictures(this)

    private class Item(val name: Int, val multiplier: Int, val icon: Int)

    // [idle] says how many slots are idle.
    private class Queue(val section: Section, val title: Int, val slots: Int, val idle: (Int) -> String, val items: List<Item>)

    override fun build() {
        pictures.clear()
        addTabTop(TabChrome.PLAN)
        val village = Store.current(this)?.village
        if (village == null) {
            addNothingSynced()
            return
        }
        addNote(getString(R.string.potions_explain), 13f)
        val builderPotion = WidgetRenderer.potionIcon(Section.BUILDERS)
        val queues = listOfNotNull(
            Queue(
                Section.BUILDERS, R.string.bedtime_builders, village.builders, { resources.getQuantityString(R.plurals.potions_idle_builders, it, it) },
                listOf(
                    Item(R.string.potion_builder, BUILDER_POTION_MULTIPLIER, builderPotion),
                    Item(R.string.potion_bite, BUILDER_BITE_MULTIPLIER, WidgetRenderer.snackIcon(Section.BUILDERS)),
                ),
            ),
            Queue(
                Section.LAB, R.string.group_lab, 1, { getString(R.string.potions_idle_lab) },
                listOf(
                    Item(R.string.potion_research, RESEARCH_POTION_MULTIPLIER, WidgetRenderer.potionIcon(Section.LAB)),
                    Item(R.string.potion_soup, STUDY_SOUP_MULTIPLIER, WidgetRenderer.snackIcon(Section.LAB)),
                ),
            ),
            Queue(
                Section.PETS, R.string.group_pets, 1, { getString(R.string.potions_idle_pets) },
                listOf(Item(R.string.potion_pet, PET_POTION_MULTIPLIER, WidgetRenderer.potionIcon(Section.PETS))),
            ).takeIf { village.hasPetHouse },
        )
        val now = System.currentTimeMillis()
        for (queue in queues) content.addView(queueCard(queue, village, now), ui.matchWidth(16))
        addNote(getString(R.string.potions_notes))
        pictures.load()
    }

    private fun queueCard(queue: Queue, village: Village, now: Long) = ui.card().apply {
        addView(ui.heading(getString(queue.title), if (ui.skin.headingFont != null) 18f else 15f))
        val advice = queue.items.map { PotionAdvisor.advise(village, queue.section, queue.slots, it.multiplier, now) }
        val running = advice.first().boostLeftSec
        if (running > 0) {
            // The potion if it's the one running, otherwise the snack.
            val potion = village.boostEndMillis(village.boostFor(queue.section)) > now
            val icon = if (potion) WidgetRenderer.potionIcon(queue.section) else WidgetRenderer.snackIcon(queue.section)
            addView(reasonRow(potionIcon(icon), getString(R.string.potions_running, PlanFormat.duration(running)), ui.skin.text), ui.matchWidth(10))
            return@apply
        }
        val idle = advice.first().idle
        if (idle > 0) {
            addView(reasonRow(picture(WidgetRenderer.idlePicture(queue.section)), queue.idle(idle), ui.skin.idle), ui.matchWidth(10))
        }
        for ((index, item) in queue.items.withIndex()) {
            if (index > 0) addView(ui.divider(), ui.matchWidth(12))
            addView(itemRow(item, advice[index]), ui.matchWidth(12))
        }
    }

    private fun itemRow(item: Item, advice: PotionAdvisor.Advice) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        val share = if (advice.possibleSec > 0) advice.savedSec.toFloat() / advice.possibleSec else 0f
        val (verdict, colour) = when {
            advice.full -> R.string.potions_now to ui.skin.green
            share >= MOSTLY -> R.string.potions_mostly to ui.skin.accent
            else -> R.string.potions_wait to ui.skin.idle
        }
        addView(LinearLayout(this@PotionsActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(potionIcon(item.icon), LinearLayout.LayoutParams(ui.dp(44), ui.dp(44)))
            addView(LinearLayout(this@PotionsActivity).apply {
                orientation = LinearLayout.VERTICAL
                addView(LinearLayout(this@PotionsActivity).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    addView(ui.text(getString(item.name, item.multiplier), 15f, bold = true), ui.weighted())
                    // A pill tinted with the verdict's own colour.
                    addView(ui.chip(getString(verdict), (colour and 0x00FFFFFF) or 0x26000000, colour).apply {
                        setTypeface(typeface, android.graphics.Typeface.BOLD)
                    })
                })
                addView(ui.text(getString(R.string.potions_saves, PlanFormat.duration(advice.savedSec), PlanFormat.duration(advice.possibleSec)), 13f, ui.skin.dim), ui.matchWidth(3))
                addView(ui.bar(share).apply { (layoutParams as LinearLayout.LayoutParams).topMargin = ui.dp(6) })
            }, ui.weighted(12))
        })
        // What cuts the hour short: each upgrade that would finish inside it, with its picture.
        for ((upgrade, seconds) in advice.finishing) {
            // A template rather than a format, so the name keeps its smaller level.
            val line = TextUtils.expandTemplate(getText(R.string.potions_finishing), WidgetRenderer.upgradeName(upgrade), PlanFormat.duration(seconds))
            addView(reasonRow(picture(Icons.path(upgrade)), line, ui.skin.dim), ui.matchWidth(8).apply { marginStart = ui.dp(56) })
        }
    }

    // A small picture beside a line of text.
    private fun reasonRow(picture: View, text: CharSequence, colour: Int) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(picture, LinearLayout.LayoutParams(ui.dp(28), ui.dp(28)))
        addView(ui.text(text, 13f, colour), ui.weighted(10))
    }

    private fun picture(path: String?) = ui.picture().also { view -> path?.let { pictures.add(it, view) } }

    private fun potionIcon(icon: Int) = ImageView(this).apply {
        setImageResource(icon)
        background = ui.rounded(ui.skin.chip, 8f)
        setPadding(ui.dp(5), ui.dp(5), ui.dp(5), ui.dp(5))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    companion object {
        // Below this share of the hour's possible saving, it's better to wait.
        private const val MOSTLY = 0.75f
    }
}
