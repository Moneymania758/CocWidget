package com.cocwidget

import android.content.Context
import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.view.Gravity
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.bases.BasePictures
import com.cocwidget.bases.LayoutOpener
import com.cocwidget.bases.SavedBase
import com.cocwidget.store.Store
import com.cocwidget.ui.Screen
import com.cocwidget.ui.Tab
import com.cocwidget.ui.TabChrome
import java.io.File
import kotlin.concurrent.thread

// The player's own saved base links, for the selected account. Each one opens in the game.
class MyBasesActivity : Screen() {
    override val tab = Tab.BASES

    override fun build() {
        val current = Store.current(this)
        val myTownHall = current?.village?.townHall ?: 0
        addTabTop(TabChrome.BASES)
        val bases = Store.myBases(this, Store.selected(this))
        if (bases.isEmpty()) {
            content.addView(ui.card().apply { addView(ui.text(getString(R.string.my_bases_empty), 14f, ui.skin.dim)) }, ui.matchWidth(4))
        } else {
            Store.selected(this)?.let { addNote(getString(R.string.my_bases_for, Store.label(this, it)), 13f) }
        }
        val pictures = mutableMapOf<ImageView, File>()
        for ((index, base) in bases.withIndex()) content.addView(baseCard(base, myTownHall, pictures), ui.matchWidth(if (index == 0) 10 else 8))
        content.addView(addButton(), ui.matchWidth(12))
        loadPictures(pictures)
    }

    // Dashed, in the accent colour, so it reads as a place for one more.
    private fun addButton() = ui.text(getString(R.string.my_bases_add_plus), 15f, ui.skin.accent, bold = true).apply {
        gravity = Gravity.CENTER
        minHeight = ui.dp(52)
        background = GradientDrawable().apply {
            cornerRadius = ui.dp(16).toFloat()
            setStroke(ui.dp(1.5f), ui.skin.accent, ui.dp(6).toFloat(), ui.dp(4).toFloat())
        }
        ui.pressable(this, 16f) { startActivity(Intent(this@MyBasesActivity, BaseEditActivity::class.java)) }
    }

    // With a screenshot, the picture across the top and the rest under it. A tap on the picture
    // shows it whole. Without one, the bases icon on the left keeps the card small.
    private fun baseCard(base: SavedBase, myTownHall: Int, pictures: MutableMap<ImageView, File>) = LinearLayout(this).apply {
        background = ui.rounded(ui.skin.card, 18f, ui.skin.divider, 1f)
        setPadding(ui.dp(12), ui.dp(12), ui.dp(12), ui.dp(12))
        val name = nameOf(this@MyBasesActivity, base)
        val file = BasePictures.file(this@MyBasesActivity, base.id)
        val details = details(base, name, myTownHall)
        if (file.exists()) {
            orientation = LinearLayout.VERTICAL
            addView(CoverView(this@MyBasesActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = getString(R.string.base_picture_of, name)
                background = ui.rounded(ui.skin.chip, 12f)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                setOnClickListener {
                    startActivity(Intent(this@MyBasesActivity, BasePictureActivity::class.java).putExtra(BasePictureActivity.EXTRA_BASE, base.id))
                }
                pictures[this] = file
            }, ui.matchWidth(0))
            addView(details, ui.matchWidth(10))
        } else {
            orientation = LinearLayout.HORIZONTAL
            addView(LinearLayout(this@MyBasesActivity).apply {
                gravity = Gravity.CENTER
                background = ui.rounded(ui.skin.chip, 12f)
                addView(ui.icon(R.drawable.ic_bases, ui.skin.dim, 28))
            }, LinearLayout.LayoutParams(ui.dp(84), ui.dp(84)))
            addView(details, ui.weighted(12))
        }
    }

    // The name, chips and buttons.
    private fun details(base: SavedBase, name: String, myTownHall: Int) =
        LinearLayout(this@MyBasesActivity).apply {
            orientation = LinearLayout.VERTICAL
            addView(ui.heading(name, 17f, ui.skin.title).apply {
                maxLines = 2
                ellipsize = TextUtils.TruncateAt.END
            })
            val chips = buildList {
                base.kind?.let { add(getString(it.label)) }
                if (base.townHall > 0) add(getString(R.string.town_hall_chip, base.townHall))
            }
            if (chips.isNotEmpty()) {
                addView(ui.flow().apply { chips.forEach { addView(ui.chip(it)) } }, ui.matchWidth(6))
            }
            addView(LinearLayout(this@MyBasesActivity).apply {
                orientation = LinearLayout.HORIZONTAL
                addView(ui.button(getString(R.string.my_bases_open_game), null, primary = true) {
                    LayoutOpener.open(this@MyBasesActivity, base.link, myTownHall) {
                        Store.saveBase(this@MyBasesActivity, base.copy(opened = System.currentTimeMillis()))
                    }
                }, ui.weighted())
                addView(ui.button(getString(R.string.my_bases_edit), null, primary = false) {
                    startActivity(Intent(this@MyBasesActivity, BaseEditActivity::class.java).putExtra(BaseEditActivity.EXTRA_BASE, base.id))
                }, ui.wrap().apply { marginStart = ui.dp(8) })
            }, ui.matchWidth(10))
        }

    // Read off the main thread, as a few screenshots take a moment to decode.
    private fun loadPictures(pictures: Map<ImageView, File>) {
        if (pictures.isEmpty()) return
        // About as wide as a card and cut to its shape, so a full-size screenshot isn't held for each.
        val width = resources.displayMetrics.widthPixels
        thread {
            val loaded = pictures.mapNotNull { (view, file) -> BasePictures.cover(file, width)?.let { view to it } }
            runOnUiThread {
                if (!isDestroyed) for ((view, bitmap) in loaded) view.setImageBitmap(bitmap)
            }
        }
    }

    // As wide as the card and 9/16 of that tall, the shape of a game screenshot near enough.
    private class CoverView(context: Context) : ImageView(context) {
        override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
            val width = MeasureSpec.getSize(widthMeasureSpec)
            setMeasuredDimension(width, width * 9 / 16)
        }
    }

    companion object {
        // The player's name for it, or which Town Hall it's for when they gave none.
        fun nameOf(context: Context, base: SavedBase): String = base.name.ifBlank {
            if (base.townHall > 0) context.getString(R.string.my_bases_unnamed, base.townHall) else context.getString(R.string.my_bases_unnamed_plain)
        }
    }
}
