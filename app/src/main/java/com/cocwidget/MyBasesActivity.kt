package com.cocwidget

import android.content.Context
import android.content.Intent
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import com.cocwidget.bases.BasePictures
import com.cocwidget.bases.LayoutOpener
import com.cocwidget.bases.SavedBase
import com.cocwidget.store.Store
import com.cocwidget.ui.Screen
import java.io.File
import kotlin.concurrent.thread

// The player's own saved base links, for the selected account. Each one opens in the game.
class MyBasesActivity : Screen() {
    override fun build() {
        val current = Store.current(this)
        val myTownHall = current?.village?.townHall ?: 0
        content.addView(backHeader(R.string.my_bases_title))
        current?.let { addNote(getString(R.string.my_bases_for, Store.label(this, it.tag)), 13f) }
        content.addView(ui.button(getString(R.string.my_bases_add), null, primary = true) {
            startActivity(Intent(this, BaseEditActivity::class.java))
        }, ui.matchWidth(14))
        content.addView(ui.button(getString(R.string.bases_open), null, primary = false) {
            startActivity(Intent(this, BasesActivity::class.java))
        }, ui.matchWidth(10))
        val bases = Store.myBases(this, current?.tag)
        if (bases.isEmpty()) {
            content.addView(ui.card().apply { addView(ui.text(getString(R.string.my_bases_empty), 14f, ui.skin.dim)) }, ui.matchWidth(16))
        }
        val pictures = mutableMapOf<ImageView, File>()
        for (base in bases) content.addView(baseCard(base, myTownHall, pictures), ui.matchWidth(16))
        loadPictures(pictures)
    }

    private fun baseCard(base: SavedBase, myTownHall: Int, pictures: MutableMap<ImageView, File>) = ui.card().apply {
        val name = nameOf(this@MyBasesActivity, base)
        val file = BasePictures.file(this@MyBasesActivity, base.id)
        if (file.exists()) {
            addView(ImageView(this@MyBasesActivity).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                contentDescription = getString(R.string.base_picture_of, name)
                background = ui.rounded(ui.skin.chip, 10f)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                clipToOutline = true
                pictures[this] = file
            }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ui.dp(180)).apply { bottomMargin = ui.dp(12) })
        }
        addView(ui.heading(name, 17f, ui.skin.title))
        val chips = buildList {
            if (base.townHall > 0) add(getString(R.string.town_hall_chip, base.townHall))
            base.kind?.let { add(getString(it.label)) }
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
            }, ui.wrap().apply { marginStart = ui.dp(10) })
        }, ui.matchWidth(12))
    }

    // Read off the main thread, as a few screenshots take a moment to decode.
    private fun loadPictures(pictures: Map<ImageView, File>) {
        if (pictures.isEmpty()) return
        val width = resources.displayMetrics.widthPixels
        thread {
            val loaded = pictures.mapNotNull { (view, file) -> BasePictures.load(file, width)?.let { view to it } }
            runOnUiThread {
                if (!isDestroyed) for ((view, bitmap) in loaded) view.setImageBitmap(bitmap)
            }
        }
    }

    companion object {
        // The player's name for it, or which Town Hall it's for when they gave none.
        fun nameOf(context: Context, base: SavedBase): String = base.name.ifBlank {
            if (base.townHall > 0) context.getString(R.string.my_bases_unnamed, base.townHall) else context.getString(R.string.my_bases_unnamed_plain)
        }
    }
}
