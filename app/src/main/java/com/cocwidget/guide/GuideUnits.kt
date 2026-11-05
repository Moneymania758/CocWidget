package com.cocwidget.guide

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

// One line on a chart. A value is null at a level the data doesn't cover.
class UnitSeries(val name: String, val values: List<Double?>)

// Lines on one chart share its one axis, so they always share a unit.
class UnitChart(val title: String, val unit: String, val series: List<UnitSeries>)

class UnitStat(val name: String, val value: String)

// How a unit behaves, whatever its level. Any of these can be missing.
class UnitTraits(
    val housing: Int,
    val speed: Double,
    val attackSpeed: Double,
    val range: Double,
    val damageType: String,
    val targets: String,
)

// How many of a defence a Town Hall allows, and the highest level it allows them.
class PerTownHall(val townHall: Int, val count: Int, val cap: Int)

// Something a unit makes, like a Ruin Witch's Ruin Knights. Its charts follow the unit's levels.
class SubUnit(
    val name: String,
    val note: String,
    val icon: String,
    val traits: UnitTraits?,
    val charts: List<UnitChart>,
    val stats: List<UnitStat>,
    // Part of the unit rather than something it makes, like a defence's supercharges.
    val partOfUnit: Boolean,
)

class GuideUnit(
    val id: Int,
    val name: String,
    val kind: String,
    // Which list it's in: elixir, dark, super, siege, spell, defence or trap.
    val group: String,
    val townHall: Int,
    val levels: IntArray,
    val townHalls: IntArray,
    val charts: List<UnitChart>,
    val stats: List<UnitStat>,
    val subs: List<SubUnit>,
    val traits: UnitTraits,
    // Spells and defences don't move, so this and flying are false for them.
    val isTroop: Boolean,
    val flying: Boolean,
    val hitsAir: Boolean,
    val hitsGround: Boolean,
    val preferredTarget: String,
    val ability: String,
    // Super Troops only: the troop they come from, the level it needs, and the boost.
    val baseId: Int,
    val baseLevel: Int,
    val boostCost: Long,
    val boostResource: String,
    val boostSeconds: Long,
    // Research to reach each level. Super Troops are boosted, not researched, so these are empty.
    val resource: String,
    val costs: LongArray,
    val times: LongArray,
    // Defences and traps only. Each entry is a Town Hall where the count or cap changes.
    val perTownHall: List<PerTownHall>,
) {
    // The position of the highest level [townHall] allows, or -1 if it hasn't opened yet.
    fun capIndex(townHall: Int): Int = townHalls.indexOfLast { it <= townHall }

    fun indexOf(level: Int): Int = levels.indexOf(level)
}

object GuideUnits {
    @Volatile
    private var loaded: Map<Int, GuideUnit>? = null

    fun load(context: Context): Map<Int, GuideUnit> = loaded ?: parse(
        context.assets.open("guide_units.json").bufferedReader().use { it.readText() },
    ).also { loaded = it }

    fun parse(text: String): Map<Int, GuideUnit> {
        val units = JSONArray(text)
        val map = LinkedHashMap<Int, GuideUnit>(units.length())
        for (index in 0 until units.length()) {
            val entry = units.getJSONObject(index)
            val base = entry.optJSONObject("base")
            val boost = entry.optJSONObject("boost")
            map[entry.getInt("id")] = GuideUnit(
                id = entry.getInt("id"),
                name = entry.getString("n"),
                kind = entry.getString("k"),
                group = entry.getString("g"),
                townHall = entry.getInt("h"),
                levels = ints(entry.getJSONArray("lv")),
                townHalls = ints(entry.getJSONArray("th")),
                charts = charts(entry.optJSONArray("charts")),
                stats = stats(entry.optJSONArray("stats")),
                subs = objects(entry.optJSONArray("subs")).map(::subUnit),
                traits = traits(entry, "as", "tt"),
                isTroop = entry.getString("k") in setOf("troop", "super", "siege"),
                flying = entry.optInt("f") == 1,
                hitsAir = entry.optInt("a") == 1,
                hitsGround = entry.optInt("gr") == 1,
                preferredTarget = entry.optString("pt"),
                ability = entry.optString("ab"),
                baseId = base?.optInt("id") ?: 0,
                baseLevel = base?.optInt("l") ?: 0,
                boostCost = boost?.optLong("c") ?: 0,
                boostResource = boost?.optString("r").orEmpty(),
                boostSeconds = boost?.optLong("t") ?: 0,
                resource = entry.optString("r"),
                costs = longs(entry.optJSONArray("c")),
                times = longs(entry.optJSONArray("tm")),
                perTownHall = objects(entry.optJSONArray("pt")).map { PerTownHall(it.getInt("h"), it.getInt("n"), it.getInt("l")) },
            )
        }
        return map
    }

    private fun subUnit(json: JSONObject) = SubUnit(
        name = json.getString("n"),
        note = json.optString("note"),
        icon = json.optString("icon"),
        traits = json.optJSONObject("facts")?.let { traits(it, "as", "tt") },
        charts = charts(json.optJSONArray("charts")),
        stats = stats(json.optJSONArray("stats")),
        partOfUnit = json.optInt("plain") == 1,
    )

    private fun traits(json: JSONObject, attack: String, targets: String) = UnitTraits(
        housing = json.optInt("hs"),
        speed = json.optDouble("sp", 0.0),
        attackSpeed = json.optDouble(attack, 0.0),
        range = json.optDouble("rg", 0.0),
        damageType = json.optString("dt"),
        targets = json.optString(targets),
    )

    private fun charts(array: JSONArray?) = objects(array).map { chart ->
        UnitChart(
            title = chart.getString("t"),
            unit = chart.optString("u"),
            series = objects(chart.getJSONArray("s")).map { series ->
                val values = series.getJSONArray("v")
                UnitSeries(series.getString("n"), (0 until values.length()).map { if (values.isNull(it)) null else values.getDouble(it) })
            },
        )
    }

    private fun stats(array: JSONArray?) = objects(array).map { UnitStat(it.getString("n"), it.getString("v")) }

    private fun objects(array: JSONArray?): List<JSONObject> = if (array == null) emptyList() else (0 until array.length()).map { array.getJSONObject(it) }

    private fun ints(array: JSONArray) = IntArray(array.length()) { array.getInt(it) }

    private fun longs(array: JSONArray?) = if (array == null) LongArray(0) else LongArray(array.length()) { array.getLong(it) }
}

// The arithmetic of a chart, kept free of Android drawing so tests can run it.
object ChartScale {
    // Round tick values from 0 up past [max]: 0, 200, 400, 600 for a max of 530.
    fun ticks(max: Double, count: Int = 3): List<Double> {
        if (max <= 0) return listOf(0.0, 1.0)
        val rough = max / count
        val magnitude = 10.0.pow(floor(log10(rough)))
        val step = listOf(1.0, 2.0, 2.5, 5.0, 10.0).map { it * magnitude }.first { it >= rough }
        val steps = ceil(max / step - 1e-9).toInt().coerceAtLeast(1)
        return (0..steps).map { it * step }
    }

    // Full numbers under 10,000 and short ones from there, one style per axis.
    fun axisLabel(value: Double, axisMax: Double, unit: String): String {
        val text = if (axisMax >= 10_000 && value != 0.0) trim(value / 1000) + "K" else trim(value)
        return if (unit.isBlank() || unit.startsWith(" ")) text else text + unit
    }

    fun value(value: Double, unit: String): String = number(value) + unit

    fun number(value: Double): String = NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 2 }.format(value)

    // Which of [count] evenly spaced points is nearest to [x], between [left] and [right].
    fun nearest(x: Float, left: Float, right: Float, count: Int): Int {
        if (count <= 1 || right <= left) return 0
        val share = ((x - left) / (right - left)).coerceIn(0f, 1f)
        return Math.round(share * (count - 1))
    }

    // How many levels apart to label the bottom axis so labels fit. The last level always gets one.
    fun labelStep(count: Int, width: Float, labelWidth: Float): Int {
        if (count <= 1 || labelWidth <= 0f) return 1
        val fit = (width / labelWidth).toInt().coerceAtLeast(2)
        return ceil((count - 1).toDouble() / (fit - 1)).toInt().coerceAtLeast(1)
    }

    // Whether every line's value label can sit above its dot without touching another label or
    // another line's dot. [points] are the dots' heights on screen, where down is larger.
    fun endLabelsClear(points: List<Float>, labelHeight: Float, gap: Float): Boolean {
        for ((index, point) in points.withIndex()) {
            val bottom = point - gap
            val top = bottom - labelHeight
            for ((other, otherPoint) in points.withIndex()) {
                if (other == index) continue
                val otherBottom = otherPoint - gap
                val otherTop = otherBottom - labelHeight
                if (otherPoint in top..point) return false
                if (top <= otherBottom && otherTop <= bottom) return false
            }
        }
        return true
    }

    private fun trim(value: Double) = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}

// Tells a tap and a sideways drag from a scroll. Nothing is picked as a finger lands, as a scroll
// that happens to start on a chart mustn't change the level.
class ChartTouch(private val slop: Float) {
    private var downX = 0f
    private var downY = 0f
    private var dragging = false

    fun down(x: Float, y: Float) {
        downX = x
        downY = y
        dragging = false
    }

    // True while this is a sideways drag, which picks levels as it goes.
    fun move(x: Float, y: Float): Boolean {
        val sideways = abs(x - downX)
        if (!dragging && sideways > slop && sideways > abs(y - downY)) dragging = true
        return dragging
    }

    // True if this was a tap, which picks the level it's on.
    fun up(x: Float, y: Float): Boolean = !dragging && abs(x - downX) <= slop && abs(y - downY) <= slop
}
