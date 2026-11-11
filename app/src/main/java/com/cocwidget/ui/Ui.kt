package com.cocwidget.ui

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.view.WindowInsetsController
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.store.Icons
import kotlin.math.roundToInt

// Builds the app's screens in the chosen skin. Views are made in code because the skins
// differ in borders, drops and fonts, not only colours.
class Ui(private val activity: Activity, val skin: Skin) {
    private val density = activity.resources.displayMetrics.density
    val headingFace: Typeface = skin.headingFont?.let { activity.resources.getFont(it) } ?: Typeface.DEFAULT_BOLD

    fun dp(value: Float): Int = (value * density).roundToInt()
    fun dp(value: Int): Int = dp(value.toFloat())

    // The system bar icons follow the skin, not the phone's own theme: dark icons on Light.
    fun applyWindow(root: View) {
        root.setBackgroundColor(skin.bg)
        val window = activity.window
        window.decorView.setBackgroundColor(skin.bg)
        @Suppress("DEPRECATION")
        window.statusBarColor = skin.bg
        @Suppress("DEPRECATION")
        window.navigationBarColor = skin.bg
        val lightBars = WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS or WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
        window.insetsController?.setSystemBarsAppearance(if (skin.light) lightBars else 0, lightBars)
    }

    fun text(value: CharSequence, sizeSp: Float = 14f, colour: Int = skin.text, bold: Boolean = false) = TextView(activity).apply {
        text = value
        textSize = sizeSp
        setTextColor(colour)
        if (bold) typeface = Typeface.DEFAULT_BOLD
        setLineSpacing(0f, 1.15f)
    }

    // Titles and big numbers: the Game skin's display font with a dark drop under the letters.
    fun heading(value: CharSequence, sizeSp: Float, colour: Int = skin.text) = text(value, sizeSp, colour).apply {
        typeface = headingFace
        if (skin.headingShadow != 0) {
            setShadowLayer(0.01f, 0f, dp(2).toFloat(), skin.headingShadow)
            // The shadow reaches below the letters, and a view can't draw outside its own bounds.
            // Without this room, a descender's shadow is cut off in a straight line.
            setPadding(0, dp(2), 0, dp(2))
        }
    }

    fun rounded(fill: Int, radiusDp: Float, stroke: Int = 0, strokeDp: Float = 0f) = GradientDrawable().apply {
        setColor(fill)
        cornerRadius = dp(radiusDp).toFloat()
        if (stroke != 0 && strokeDp > 0) setStroke(dp(strokeDp), stroke)
    }

    // A face over a solid drop, with a dark shade along its bottom edge and a light line along
    // its top, so it looks raised. Used by the Game skin.
    private fun raised(face: Int, radiusDp: Float, shade: Int, shadeDp: Int, highlight: Int): Drawable {
        val border = dp(skin.borderDp)
        val drop = dp(3)
        val layers = mutableListOf<Drawable>(rounded(skin.drop, radiusDp), rounded(face, radiusDp - skin.borderDp))
        if (shade != 0) layers += rounded(shade, radiusDp - skin.borderDp)
        layers += rounded(highlight, 1f)
        return LayerDrawable(layers.toTypedArray()).apply {
            setLayerInset(1, border, border, border, border + drop)
            var top = 2
            if (shade != 0) {
                setLayerGravity(2, Gravity.BOTTOM or Gravity.FILL_HORIZONTAL)
                setLayerHeight(2, dp(shadeDp))
                setLayerInset(2, border, 0, border, border + drop)
                top = 3
            }
            setLayerGravity(top, Gravity.TOP or Gravity.FILL_HORIZONTAL)
            setLayerHeight(top, dp(2))
            setLayerInset(top, dp(radiusDp / 2), border, dp(radiusDp / 2), 0)
        }
    }

    private val raisedDrop get() = if (skin.drop != 0) dp(3) else 0

    fun card(padding: Int = 16) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        background = if (skin.drop != 0) {
            raised(skin.card, skin.radiusDp, 0, 0, 0x1AFFFFFF)
        } else {
            rounded(skin.card, skin.radiusDp, skin.border, skin.borderDp)
        }
        setPadding(dp(padding), dp(padding), dp(padding), dp(padding) + raisedDrop)
    }

    // A tappable row or tile that screen readers announce as a button.
    fun pressable(view: View, radiusDp: Float, onClick: () -> Unit) {
        view.isClickable = true
        view.isFocusable = true
        val ripple = if (skin.light) 0x1F000000 else 0x33FFFFFF
        view.foreground = RippleDrawable(ColorStateList.valueOf(ripple), null, rounded(0xFFFFFFFF.toInt(), radiusDp))
        view.setOnClickListener { onClick() }
        view.accessibilityDelegate = object : View.AccessibilityDelegate() {
            override fun onInitializeAccessibilityNodeInfo(host: View, info: AccessibilityNodeInfo) {
                super.onInitializeAccessibilityNodeInfo(host, info)
                info.className = Button::class.java.name
            }
        }
    }

    private fun buttonFace(primary: Boolean): Drawable {
        val fill = if (primary) skin.primaryBg else skin.secondaryBg
        val shade = if (primary) skin.primaryShade else skin.secondaryShade
        return when {
            skin.drop != 0 -> raised(fill, 12f, shade, 4, if (primary) 0x38FFFFFF else 0x59FFFFFF)
            !primary && skin.secondaryOutline != 0 -> rounded(fill, 12f, skin.secondaryOutline, 1.5f)
            else -> rounded(fill, 12f)
        }
    }

    fun button(label: CharSequence, icon: Int?, primary: Boolean, onClick: () -> Unit): LinearLayout {
        val colour = if (primary) skin.primaryText else skin.secondaryText
        return LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            minimumHeight = dp(52) + raisedDrop
            background = buttonFace(primary)
            setPadding(dp(12), dp(6), dp(12), dp(6) + raisedDrop)
            if (icon != null) {
                addView(ImageView(activity).apply {
                    setImageResource(icon)
                    imageTintList = ColorStateList.valueOf(colour)
                }, LinearLayout.LayoutParams(dp(20), dp(20)).apply { marginEnd = dp(8) })
            }
            val size = if (skin.headingFont != null) 18f else 15f
            addView(heading(label, size, colour).apply {
                gravity = Gravity.CENTER
                if (skin.headingFont == null) typeface = Typeface.DEFAULT_BOLD
                if (primary && skin.primaryShade != 0) setShadowLayer(0.01f, 0f, dp(2).toFloat(), skin.primaryShade) else setShadowLayer(0f, 0f, 0f, 0)
            })
            pressable(this, 12f, onClick)
        }
    }

    // A secondary button with its icon above the label, for a row of tools.
    fun tile(label: CharSequence, icon: Int, onClick: () -> Unit) = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        // From the top, so icons and first lines stay level when one label wraps.
        gravity = Gravity.CENTER_HORIZONTAL
        minimumHeight = dp(76) + raisedDrop
        background = buttonFace(primary = false)
        setPadding(dp(6), dp(12), dp(6), dp(12) + raisedDrop)
        addView(ImageView(activity).apply {
            setImageResource(icon)
            imageTintList = ColorStateList.valueOf(skin.secondaryText)
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }, LinearLayout.LayoutParams(dp(24), dp(24)))
        val size = if (skin.headingFont != null) 15f else 13f
        addView(heading(label, size, skin.secondaryText).apply {
            gravity = Gravity.CENTER
            if (skin.headingFont == null) typeface = Typeface.DEFAULT_BOLD
            setShadowLayer(0f, 0f, 0f, 0)
        }, matchWidth(6))
        pressable(this, 12f, onClick)
    }

    fun chip(value: CharSequence, background: Int = skin.chip, colour: Int = skin.text, icon: Int? = null) = text(value, 13f, colour).apply {
        this.background = rounded(background, 8f)
        setPadding(dp(9), dp(4), dp(9), dp(4))
        if (icon != null) {
            val drawable = activity.getDrawable(icon)!!.mutate().apply {
                setTint(colour)
                setBounds(0, 0, dp(12), dp(12))
            }
            setCompoundDrawablesRelative(drawable, null, null, null)
            compoundDrawablePadding = dp(4)
        }
    }

    // A chip with the resource's icons and the amount: "Gold or Elixir" shows both icons.
    fun costChip(resource: String, amount: String) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        background = rounded(skin.chip, 8f)
        setPadding(dp(8), dp(4), dp(9), dp(4))
        contentDescription = "$resource $amount"
        for (name in resource.split(" or ")) {
            val icon = when (name) {
                "Gold" -> R.drawable.ic_res_gold
                "Elixir" -> R.drawable.ic_res_elixir
                "Dark Elixir" -> R.drawable.ic_res_dark
                else -> null
            }
            if (icon != null) {
                addView(ImageView(activity).apply { setImageResource(icon) }, LinearLayout.LayoutParams(dp(13), dp(13)).apply { marginEnd = dp(4) })
            } else {
                addView(text("$name ", 13f))
            }
        }
        addView(text(amount, 13f).apply { importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO })
    }

    // A progress bar in the skin: gold while there's work left, green when done.
    fun bar(fraction: Float) = LinearLayout(activity).apply {
        orientation = LinearLayout.HORIZONTAL
        background = rounded(skin.track, skin.barDp / 2)
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        val done = fraction >= 1f
        val share = fraction.coerceIn(0f, 1f)
        addView(View(activity).apply {
            background = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, if (done) skin.greenBar else skin.goldBar).apply {
                cornerRadius = dp(skin.barDp / 2).toFloat()
            }
        }, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, share))
        addView(View(activity), LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f - share))
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(skin.barDp))
    }

    // A padded row inside a card, for a setting or a link.
    fun row() = LinearLayout(activity).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(14), dp(16), dp(14))
    }

    fun divider() = View(activity).apply {
        setBackgroundColor(skin.divider)
        layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(1))
    }

    fun icon(resource: Int, colour: Int, sizeDp: Int) = ImageView(activity).apply {
        setImageResource(resource)
        imageTintList = ColorStateList.valueOf(colour)
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    // An item's picture on a chip-coloured tile, filled in later by Pictures.
    fun picture() = ImageView(activity).apply {
        background = rounded(skin.chip, 8f)
        setPadding(dp(3), dp(3), dp(3), dp(3))
        scaleType = ImageView.ScaleType.FIT_CENTER
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    // A character's face in a coloured ring, for the helpers and the Goblin.
    fun face(resource: Int, sizeDp: Int, ring: Int, description: CharSequence) = ImageView(activity).apply {
        setImageResource(resource)
        scaleType = ImageView.ScaleType.CENTER_CROP
        contentDescription = description
        background = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(skin.chip)
            setStroke(dp(2), ring)
        }
        setPadding(dp(2), dp(2), dp(2), dp(2))
        outlineProvider = ViewOutlineProvider.BACKGROUND
        clipToOutline = true
        layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
    }

    // A Town Hall large, on a soft glow. Straight away if it's bundled, so it doesn't blink each time
    // a screen is rebuilt.
    fun townHall(level: Int, pictures: Pictures) = ImageView(activity).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        contentDescription = activity.getString(R.string.home_town_hall, level)
        val glow = skin.accent and 0x00FFFFFF
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(glow or 0x40000000, glow)).apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = dp(58).toFloat()
        }
        val path = Icons.townHallPath(level)
        val bitmap = Icons.load(activity, path)
        if (bitmap != null) setImageBitmap(bitmap) else pictures.add(path, this)
    }

    // A Town Hall's picture in a ring, for an account. Its number when the picture isn't bundled,
    // and a question mark with no village.
    fun townHallBadge(level: Int, ring: Int, sizeDp: Int): View {
        val ringed = GradientDrawable().apply {
            shape = GradientDrawable.OVAL
            setColor(skin.chip)
            setStroke(dp(2), ring)
        }
        val size = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
        val bitmap = if (level > 0) Icons.load(activity, Icons.townHallPath(level)) else null
        if (bitmap == null) {
            return text(if (level > 0) level.toString() else "?", 13f, bold = true).apply {
                gravity = Gravity.CENTER
                background = ringed
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                layoutParams = size
            }
        }
        return ImageView(activity).apply {
            setImageBitmap(bitmap)
            scaleType = ImageView.ScaleType.FIT_CENTER
            background = ringed
            setPadding(dp(5), dp(5), dp(5), dp(5))
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
            layoutParams = size
        }
    }

    fun flow(gapDp: Int = 6) = FlowLayout(activity, dp(gapDp), dp(gapDp))

    fun matchWidth(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(top)
    }

    fun wrap(top: Int = 0) = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT).apply {
        topMargin = dp(top)
    }

    fun weighted(start: Int = 0) = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
        marginStart = dp(start)
    }
}
