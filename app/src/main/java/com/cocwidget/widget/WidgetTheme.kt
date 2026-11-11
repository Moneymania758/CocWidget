package com.cocwidget.widget

import com.cocwidget.R

// A widget's colours. Classic is the brown and gold every widget had before there was a choice.
// Material You takes its colours from the wallpaper and follows the phone's dark mode. Its
// backgrounds are resources the launcher reads, so they follow the wallpaper straight away. Its text
// colours are read when the widget is drawn, so they catch up at the next redraw.
// Backgrounds are drawables. Bars and icons are tinted, which widgets allow from Android 12 on. A
// text's side icon can't be tinted, so the account chip's switch arrow has a drawable per theme.
enum class WidgetTheme(
    val key: String,
    val label: Int,
    val panel: Int,
    val chip: Int,
    val chipOn: Int,
    val card: Int,
    val switchIcon: Int,
    val text: Int,
    val dim: Int,
    val accent: Int,
    val idle: Int,
    val ok: Int,
    val helper: Int,
    val track: Int,
    val bar: Int,
) {
    CLASSIC(
        "classic", R.string.widget_theme_classic,
        R.drawable.widget_bg, R.drawable.chip_view, R.drawable.chip_view_on, R.drawable.wcard_classic, R.drawable.ic_switch,
        R.color.text, R.color.text_dim, R.color.gold, R.color.idle, R.color.bar_top, R.color.helper, R.color.track, R.color.bar_top,
    ),
    DARK(
        "dark", R.string.widget_theme_dark,
        R.drawable.wpanel_dark, R.drawable.wchip_dark, R.drawable.wchipon_dark, R.drawable.wcard_dark, R.drawable.ic_switch,
        R.color.w_dark_text, R.color.w_dark_dim, R.color.w_dark_accent, R.color.w_dark_idle, R.color.w_dark_ok, R.color.w_dark_helper, R.color.w_dark_track, R.color.w_dark_bar,
    ),
    LIGHT(
        "light", R.string.widget_theme_light,
        R.drawable.wpanel_light, R.drawable.wchip_light, R.drawable.wchipon_light, R.drawable.wcard_light, R.drawable.ic_switch_light,
        R.color.w_light_text, R.color.w_light_dim, R.color.w_light_accent, R.color.w_light_idle, R.color.w_light_ok, R.color.w_light_helper, R.color.w_light_track, R.color.w_light_bar,
    ),
    YOU(
        "you", R.string.widget_theme_you,
        R.drawable.wpanel_you, R.drawable.wchip_you, R.drawable.wchipon_you, R.drawable.wcard_you, R.drawable.ic_switch_you,
        R.color.w_you_text, R.color.w_you_dim, R.color.w_you_accent, R.color.w_you_idle, R.color.w_you_ok, R.color.w_you_helper, R.color.w_you_track, R.color.w_you_bar,
    );

    companion object {
        fun of(key: String?): WidgetTheme? = entries.firstOrNull { it.key == key }

        // The ring round the Town Hall level, by the account's place in the list.
        private val RINGS = listOf(R.drawable.wring_0, R.drawable.wring_1, R.drawable.wring_2, R.drawable.wring_3, R.drawable.wring_4, R.drawable.wring_5)

        fun ring(accountIndex: Int): Int = RINGS[maxOf(0, accountIndex) % RINGS.size]
    }
}
