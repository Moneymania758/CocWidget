package com.cocwidget.ui

import android.content.Intent
import android.view.Gravity
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.GuideUnitActivity
import com.cocwidget.R
import com.cocwidget.game.GameData
import com.cocwidget.game.GameItem
import com.cocwidget.game.Ids
import com.cocwidget.guide.Fighter
import com.cocwidget.guide.Guide
import com.cocwidget.store.Icons
import com.cocwidget.store.Store

// What the guide's screens share: the data, the synced village if there is one, and pictures.
abstract class GuideScreen : Screen() {
    protected val guide: Guide by lazy { Guide.load(this) }
    protected val items: Map<Int, GameItem> by lazy { GameData.all(this) }
    private val pictures by lazy { Pictures(this) }

    // The selected village's Town Hall, and its hero and pet levels. 0 and none before a sync.
    protected var myTownHall = 0
        private set
    protected var myLevels: Map<Int, Int> = emptyMap()
        private set

    final override fun build() {
        val current = Store.current(this)
        myTownHall = current?.village?.townHall ?: 0
        myLevels = current?.let { Guide.levels(it.json) }.orEmpty()
        pictures.clear()
        buildGuide()
        pictures.load()
    }

    protected abstract fun buildGuide()

    protected val maxTownHall: Int get() = items[Ids.TOWN_HALL]?.levelCount ?: 0

    protected fun picture(item: GameItem?, sizeDp: Int = 44): ImageView = ui.picture().apply {
        layoutParams = LinearLayout.LayoutParams(ui.dp(sizeDp), ui.dp(sizeDp))
        if (item != null) pictures.add(Icons.path(item, 1), this)
    }

    // A picture ClashKing doesn't have, bundled in assets/icons under its [path]'s name.
    protected fun picture(path: String, sizeDp: Int = 44): ImageView = ui.picture().apply {
        layoutParams = LinearLayout.LayoutParams(ui.dp(sizeDp), ui.dp(sizeDp))
        pictures.add(path, this)
    }

    // A small picture and a name, for lists of troops, spells and equipment.
    protected fun namedChip(item: GameItem?, name: String) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = ui.rounded(ui.skin.chip, 8f)
        setPadding(ui.dp(4), ui.dp(4), ui.dp(9), ui.dp(4))
        addView(picture(item, 24).apply {
            setPadding(0, 0, 0, 0)
            background = null
        })
        addView(ui.text(name, 13f), ui.wrap().apply { marginStart = ui.dp(5) })
    }

    // A picture beside a title and a line under it.
    protected fun titleRow(item: GameItem?, title: String, line: String, pictureDp: Int = 44) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        addView(picture(item, pictureDp))
        addView(LinearLayout(this@GuideScreen).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.heading(title, 17f))
            if (line.isNotEmpty()) addView(ui.text(line, 13f, ui.skin.dim), ui.matchWidth(1))
        }, ui.weighted(10))
    }

    protected fun sectionTitle(text: String) = ui.heading(text, 15f, ui.skin.accent)

    protected fun moves(fighter: Fighter) = getString(if (fighter.flying) R.string.guide_flying else R.string.guide_ground)

    protected fun hits(fighter: Fighter) = getString(
        when {
            fighter.hitsAir && fighter.hitsGround -> R.string.guide_hits_both
            fighter.hitsAir -> R.string.guide_hits_air
            else -> R.string.guide_hits_ground
        },
    )

    protected fun open(screen: Class<*>, extras: Intent.() -> Unit = {}) = startActivity(Intent(this, screen).apply(extras))

    protected fun openUnit(id: Int) = startActivity(Intent(this, GuideUnitActivity::class.java).putExtra(EXTRA_ID, id))

    protected fun itemNamed(fighters: List<Fighter>, name: String) = fighters.firstOrNull { it.name == name }?.let { items[it.id] }

    companion object {
        const val EXTRA_ID = "id"
        const val EXTRA_TOWN_HALL = "town_hall"
    }
}
