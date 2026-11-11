package com.cocwidget.ui

import android.content.Context
import android.content.res.Configuration
import com.cocwidget.R
import com.cocwidget.store.Store

// The look of the app's own screens. The widget keeps its brown panel whichever is chosen.
class Skin(
    val key: String,
    val label: Int,
    // A light background, so the system bar icons and ripples go dark.
    val light: Boolean,
    val bg: Int,
    val card: Int,
    // A border, and a solid drop below cards and buttons for the Game look (0 for none).
    val border: Int,
    val borderDp: Float,
    val drop: Int,
    val divider: Int,
    val text: Int,
    val dim: Int,
    val title: Int,
    val accent: Int,
    val green: Int,
    val track: Int,
    val chip: Int,
    // Behind a group's opened items.
    val sub: Int,
    val radiusDp: Float,
    val barDp: Float,
    // Bars fade from the light colour at the top to the dark one (the same colour for a flat bar).
    val goldBar: IntArray,
    val greenBar: IntArray,
    val headingFont: Int?,
    val headingShadow: Int,
    val primaryBg: Int,
    val primaryText: Int,
    val primaryShade: Int,
    val secondaryBg: Int,
    val secondaryText: Int,
    val secondaryOutline: Int,
    val secondaryShade: Int,
    val runningBg: Int,
    val runningText: Int,
    val maxedBg: Int,
    val swatch: IntArray,
    // An icon or word drawn on an accent fill.
    val onAccent: Int,
    // Idle builders, an out-of-date export, a ready helper and the Goblin.
    val idle: Int,
    val helper: Int,
    val goblin: Int,
) {
    // Alert dialogs match the skin. The plain device dialog is always dark.
    val dialogTheme: Int
        get() = if (light) android.R.style.Theme_DeviceDefault_Light_Dialog_Alert else android.R.style.Theme_DeviceDefault_Dialog_Alert

    companion object {
        const val DEFAULT = "clean"

        val WIDGET = Skin(
            key = "widget", label = R.string.skin_widget, light = false,
            bg = 0xFF2A1D14.toInt(), card = 0xFF3D2B1F.toInt(), border = 0xFF5C4232.toInt(), borderDp = 1f, drop = 0,
            divider = 0xFF5C4232.toInt(), text = 0xFFFFFFFF.toInt(), dim = 0xFFE6D6C3.toInt(), title = 0xFFFFC93C.toInt(),
            accent = 0xFFFFC93C.toInt(), green = 0xFF8EE04A.toInt(), track = 0xFF24170F.toInt(), chip = 0xFF2A1D14.toInt(),
            sub = 0xFF33241A.toInt(), radiusDp = 12f, barDp = 6f,
            goldBar = intArrayOf(0xFFFFC93C.toInt(), 0xFFFFC93C.toInt()), greenBar = intArrayOf(0xFF8EE04A.toInt(), 0xFF8EE04A.toInt()),
            headingFont = null, headingShadow = 0,
            primaryBg = 0xFFFFC93C.toInt(), primaryText = 0xFF2A1D14.toInt(), primaryShade = 0,
            secondaryBg = 0, secondaryText = 0xFFFFC93C.toInt(), secondaryOutline = 0xFFFFC93C.toInt(), secondaryShade = 0,
            runningBg = 0xFF24170F.toInt(), runningText = 0xFF8EE04A.toInt(), maxedBg = 0xFF24170F.toInt(),
            swatch = intArrayOf(0xFF2A1D14.toInt(), 0xFF3D2B1F.toInt(), 0xFFFFC93C.toInt()),
            onAccent = 0xFF2A1D14.toInt(), idle = 0xFFFFA640.toInt(), helper = 0xFF6CCBFF.toInt(), goblin = 0xFF8EE04A.toInt(),
        )

        val CLEAN = Skin(
            key = "clean", label = R.string.skin_clean, light = false,
            bg = 0xFF121417.toInt(), card = 0xFF1C1F24.toInt(), border = 0, borderDp = 0f, drop = 0,
            divider = 0xFF2A2F37.toInt(), text = 0xFFF3F4F6.toInt(), dim = 0xFFAEB4BE.toInt(), title = 0xFFF3F4F6.toInt(),
            accent = 0xFFFFC93C.toInt(), green = 0xFF7BD144.toInt(), track = 0xFF2A2F37.toInt(), chip = 0xFF262A31.toInt(),
            sub = 0xFF181B1F.toInt(), radiusDp = 16f, barDp = 4f,
            goldBar = intArrayOf(0xFFFFC93C.toInt(), 0xFFFFC93C.toInt()), greenBar = intArrayOf(0xFF7BD144.toInt(), 0xFF7BD144.toInt()),
            headingFont = null, headingShadow = 0,
            primaryBg = 0xFFFFC93C.toInt(), primaryText = 0xFF121417.toInt(), primaryShade = 0,
            secondaryBg = 0xFF262A31.toInt(), secondaryText = 0xFFF3F4F6.toInt(), secondaryOutline = 0, secondaryShade = 0,
            runningBg = 0xFF22301A.toInt(), runningText = 0xFF7BD144.toInt(), maxedBg = 0xFF22301A.toInt(),
            swatch = intArrayOf(0xFF121417.toInt(), 0xFF1C1F24.toInt(), 0xFFFFC93C.toInt()),
            onAccent = 0xFF121417.toInt(), idle = 0xFFFFA640.toInt(), helper = 0xFF6CCBFF.toInt(), goblin = 0xFF8EE04A.toInt(),
        )

        val GAME = Skin(
            key = "game", label = R.string.skin_game, light = false,
            bg = 0xFF3D2B1F.toInt(), card = 0xFF5C4232.toInt(), border = 0xFF24170F.toInt(), borderDp = 2f, drop = 0xFF24170F.toInt(),
            divider = 0xFF4A3426.toInt(), text = 0xFFFFFFFF.toInt(), dim = 0xFFE6D6C3.toInt(), title = 0xFFFFC93C.toInt(),
            accent = 0xFFFFC93C.toInt(), green = 0xFF8EE04A.toInt(), track = 0xFF24170F.toInt(), chip = 0xFF4A3426.toInt(),
            sub = 0xFF4F3929.toInt(), radiusDp = 14f, barDp = 12f,
            goldBar = intArrayOf(0xFFFFE08A.toInt(), 0xFFE8A912.toInt()), greenBar = intArrayOf(0xFF8EE04A.toInt(), 0xFF4C9E22.toInt()),
            headingFont = R.font.lilita_one, headingShadow = 0xFF24170F.toInt(),
            primaryBg = 0xFF4E9A26.toInt(), primaryText = 0xFFFFFFFF.toInt(), primaryShade = 0xFF2F6414.toInt(),
            secondaryBg = 0xFFFFC93C.toInt(), secondaryText = 0xFF3D2B1F.toInt(), secondaryOutline = 0, secondaryShade = 0xFFC98F14.toInt(),
            runningBg = 0xFF4E9A26.toInt(), runningText = 0xFFFFFFFF.toInt(), maxedBg = 0xFF33461F.toInt(),
            swatch = intArrayOf(0xFF3D2B1F.toInt(), 0xFF5C4232.toInt(), 0xFF4E9A26.toInt()),
            onAccent = 0xFF2A1D14.toInt(), idle = 0xFFFFA640.toInt(), helper = 0xFF6CCBFF.toInt(), goblin = 0xFF8EE04A.toInt(),
        )

        // Warm off-white with dark gold ink. Status colours are darker than the dark skins' so
        // they still read on white and on the chips.
        val LIGHT = Skin(
            key = "light", label = R.string.skin_light, light = true,
            bg = 0xFFF6F3EE.toInt(), card = 0xFFFFFFFF.toInt(), border = 0xFFE6DFD5.toInt(), borderDp = 1f, drop = 0,
            divider = 0xFFECE5DA.toInt(), text = 0xFF1E1A16.toInt(), dim = 0xFF645B53.toInt(), title = 0xFF1E1A16.toInt(),
            accent = 0xFF7F5300.toInt(), green = 0xFF1A6B2D.toInt(), track = 0xFFE6DFD5.toInt(), chip = 0xFFEFE9E0.toInt(),
            sub = 0xFFF6F3EE.toInt(), radiusDp = 16f, barDp = 4f,
            goldBar = intArrayOf(0xFFE0A100.toInt(), 0xFFE0A100.toInt()), greenBar = intArrayOf(0xFF2E9E44.toInt(), 0xFF2E9E44.toInt()),
            headingFont = null, headingShadow = 0,
            primaryBg = 0xFFFFC93C.toInt(), primaryText = 0xFF1E1A16.toInt(), primaryShade = 0,
            secondaryBg = 0xFFEFE9E0.toInt(), secondaryText = 0xFF1E1A16.toInt(), secondaryOutline = 0, secondaryShade = 0,
            runningBg = 0xFFE3F1E4.toInt(), runningText = 0xFF1A6B2D.toInt(), maxedBg = 0xFFE3F1E4.toInt(),
            swatch = intArrayOf(0xFFF6F3EE.toInt(), 0xFFFFFFFF.toInt(), 0xFFFFC93C.toInt()),
            onAccent = 0xFFFFFFFF.toInt(), idle = 0xFFA3480A.toInt(), helper = 0xFF1A619E.toInt(), goblin = 0xFF336B1A.toInt(),
        )

        // Not a skin of its own: Light, or Clean when the phone is in dark mode.
        const val SYSTEM = "system"

        val ALL = listOf(LIGHT, CLEAN, WIDGET, GAME)

        fun of(key: String?): Skin = ALL.firstOrNull { it.key == key } ?: ALL.first { it.key == DEFAULT }

        fun current(context: Context): Skin {
            val key = Store.skin(context)
            if (key != SYSTEM) return of(key)
            val night = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            return if (night == Configuration.UI_MODE_NIGHT_YES) CLEAN else LIGHT
        }
    }
}
