package com.cocwidget.acceptance

import android.view.View
import android.widget.FrameLayout
import android.widget.TextView
import com.cocwidget.R
import com.cocwidget.TestData.upgrade
import com.cocwidget.TestData.villageOf
import com.cocwidget.game.Section
import com.cocwidget.widget.WidgetRenderer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

// Every potion due shares one chip, as three chips cut the last one off on a narrow widget.
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PotionChipWidgetTest {
    private val app get() = RuntimeEnvironment.getApplication()
    private val now = 1_800_000_000_000L
    private val day = 86_400L

    // Every builder, the lab and the pet house busy all day.
    private val allBusy = villageOf(
        now,
        *Array(6) { upgrade(day, name = "Building $it") },
        upgrade(day, Section.LAB, name = "Archer"),
        upgrade(day, Section.PETS, name = "L.A.S.S.I"),
    )

    @Test
    fun oneChipNamesEveryPotionDue() {
        assertEquals("Builder Potion now", WidgetRenderer.potionChipText(app, listOf(Section.BUILDERS)))
        assertEquals("Research and Pet Potions now", WidgetRenderer.potionChipText(app, listOf(Section.LAB, Section.PETS)))
        assertEquals("Builder, Research and Pet Potions now", WidgetRenderer.potionChipText(app, listOf(Section.BUILDERS, Section.LAB, Section.PETS)))
        assertNull(WidgetRenderer.potionChipText(app, emptyList()))
    }

    // About 358 dp, the narrow big widget Android Claude saw cut.
    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun onANarrowWidgetTheChipFitsWhole() {
        assertEquals(listOf(Section.BUILDERS, Section.LAB, Section.PETS), WidgetRenderer.potionNow(allBusy, now))
        val root = WidgetRenderer.build(app, 9_003, allBusy.tag, allBusy, now).apply(app, FrameLayout(app))
        val density = app.resources.displayMetrics.density
        root.measure(
            View.MeasureSpec.makeMeasureSpec((358 * density).toInt(), View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        root.layout(0, 0, root.measuredWidth, root.measuredHeight)

        val chips = listOf(R.id.chip_1, R.id.chip_2, R.id.chip_3)
            .map { root.findViewById<TextView>(it) }
            .filter { it.visibility == View.VISIBLE }
        assertEquals(listOf("Builder, Research and Pet Potions now"), chips.map { it.text.toString() })
        for (chip in chips) {
            val needed = chip.paint.measureText(chip.text.toString()) + chip.paddingStart + chip.paddingEnd
            assertTrue("${chip.text}: ${chip.width} of $needed", chip.width >= needed - 1)
        }
    }
}
