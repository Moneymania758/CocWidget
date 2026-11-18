package com.cocwidget.plan

import com.cocwidget.game.GameItem
import com.cocwidget.game.Section
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner.Group
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

// How far one import was towards max at its own Town Hall. [done] is the share of upgrade time
// done (see Planner.Plan.timeDone), [groups] each group's share as Time to max shows it, and
// [maxSeconds] the time to max with every builder busy, at full price. [since] is what happened
// after the import before it, null for the first.
data class ProgressPoint(
    val exportedAtMillis: Long,
    val townHall: Int,
    val done: Float,
    val groups: Map<Group, Float>,
    val maxSeconds: Long,
    val since: Between? = null,
)

// One week, Monday first: upgrade levels finished in each section, counted in the week of the
// import that first shows them, and idle time, shared over the weeks between two imports by how
// much of the gap fell in each. [covered] is false when no two imports span any of it.
class Week(val start: LocalDate, val levels: Map<Section, Int>, val idleSeconds: Map<Section, Long>, val covered: Boolean)

// What the Progress chart plots: the share done in percent, or the days to max.
enum class ProgressMeasure {
    DONE, MAX;

    fun value(point: ProgressPoint): Float = if (this == DONE) point.done * 100 else point.maxSeconds / DAY_SECONDS

    fun ticks(points: List<ProgressPoint>): List<Int> = if (this == DONE) {
        Progress.ticks(points.minOf { it.done }, points.maxOf { it.done })
    } else {
        Progress.dayTicks(points.minOf { it.maxSeconds }, points.maxOf { it.maxSeconds })
    }

    fun label(tick: Int) = if (this == DONE) "$tick%" else "${tick}d"

    private companion object {
        const val DAY_SECONDS = 86_400f
    }
}

// Progress over time, worked out from the imports ImportHistory keeps.
object Progress {
    // The card compares with the first import in this many days.
    const val COMPARE_DAYS = 30
    private const val DAY_MILLIS = 86_400_000L

    // Null when [json] isn't a village export. Always at full price, so imports from an event
    // and from outside one line up.
    fun point(json: String, items: Map<Int, GameItem>): ProgressPoint? {
        val village = VillageParser.parse(json) { items[it] } ?: return null
        val plan = Planner.plan(json, items, village.builders) ?: return null
        return of(plan, village.exportedAtMillis)
    }

    fun of(plan: Planner.Plan, exportedAtMillis: Long) = ProgressPoint(
        exportedAtMillis = exportedAtMillis,
        townHall = plan.townHall,
        done = plan.timeDone(),
        groups = plan.groups.associate { it.group to it.done },
        maxSeconds = plan.builderEstimateSeconds(),
    )

    // The earlier import to compare [latest] with: the first in the [COMPARE_DAYS] days before
    // it, or the one just before it when none is that recent. Null when there's no earlier one.
    fun compareWith(times: List<Long>, latest: Long): Long? {
        val earlier = times.filter { it < latest }.sorted()
        val cutoff = latest - COMPARE_DAYS * DAY_MILLIS
        return earlier.firstOrNull { it >= cutoff } ?: earlier.lastOrNull()
    }

    // The last import of each day, oldest first, so a day of several imports is one point.
    fun daily(points: List<ProgressPoint>, zone: ZoneId): List<ProgressPoint> =
        points.sortedBy { it.exportedAtMillis }
            .groupBy { Instant.ofEpochMilli(it.exportedAtMillis).atZone(zone).toLocalDate() }
            .map { it.value.last() }

    // Runs of points at one Town Hall, oldest first. A new Town Hall raises the target, so the
    // share drops: drawn apart, it doesn't read as going backwards.
    fun stretches(points: List<ProgressPoint>): List<List<ProgressPoint>> {
        val runs = mutableListOf<MutableList<ProgressPoint>>()
        for (point in points.sortedBy { it.exportedAtMillis }) {
            val run = runs.lastOrNull()
            if (run != null && run.last().townHall == point.townHall) run += point else runs += mutableListOf(point)
        }
        return runs
    }

    // The [count] weeks up to the latest import's, oldest first.
    fun weekly(points: List<ProgressPoint>, zone: ZoneId, count: Int = WEEKS): List<Week> {
        val latest = points.maxOfOrNull { it.exportedAtMillis } ?: return emptyList()
        val last = weekOf(latest, zone)
        val starts = (count - 1 downTo 0).map { last.minusWeeks(it.toLong()) }
        val levels = starts.associateWith { HashMap<Section, Int>() }
        val idle = starts.associateWith { HashMap<Section, Double>() }
        val covered = HashSet<LocalDate>()
        for (point in points) {
            val since = point.since ?: continue
            levels[weekOf(point.exportedAtMillis, zone)]?.let { week -> for ((section, n) in since.levels) week.merge(section, n, Int::plus) }
            val span = (point.exportedAtMillis - since.previousAt).toDouble()
            if (span <= 0) continue
            for (start in starts) {
                val from = start.atStartOfDay(zone).toInstant().toEpochMilli()
                val to = start.plusWeeks(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val overlap = minOf(to, point.exportedAtMillis) - maxOf(from, since.previousAt)
                if (overlap <= 0) continue
                covered += start
                val week = idle.getValue(start)
                for ((section, seconds) in since.idleSeconds) week.merge(section, seconds * overlap / span, Double::plus)
            }
        }
        return starts.map { start ->
            Week(start, levels.getValue(start), idle.getValue(start).mapValues { Math.round(it.value) }, start in covered)
        }
    }

    private fun weekOf(millis: Long, zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().with(DayOfWeek.MONDAY)

    // Nice round lines for a bar chart starting at 0: at most five steps up to just over [high].
    fun barTicks(high: Float): List<Int> {
        val step = BAR_STEPS.firstOrNull { high / it <= 4 } ?: BAR_STEPS.last()
        val top = maxOf(step, (ceil(high / step) * step).toInt())
        return (0..top step step).toList()
    }

    private val BAR_STEPS = listOf(1, 2, 5, 10, 20, 25, 50, 100, 200, 500, 1000)
    const val WEEKS = 8

    // The chart's percent lines: a round step from just under the lowest share to just over the
    // highest, within 0 to 100. A line that barely moves still fills the chart.
    fun ticks(low: Float, high: Float): List<Int> {
        val bottomPercent = low * 100
        val topPercent = high * 100
        val span = topPercent - bottomPercent
        val step = when {
            span <= 8 -> 2
            span <= 20 -> 5
            span <= 40 -> 10
            else -> 20
        }
        var bottom = (floor(bottomPercent / step) * step).toInt().coerceAtLeast(0)
        var top = (ceil(topPercent / step) * step).toInt().coerceAtMost(100)
        if (top <= bottom) {
            if (top + step <= 100) top += step else bottom = top - step
        }
        return (bottom..top step step).toList()
    }

    // The chart's day lines for time to max: a round step giving at most six lines, from just
    // under the shortest to just over the longest.
    fun dayTicks(lowSeconds: Long, highSeconds: Long): List<Int> {
        val low = lowSeconds / 86_400f
        val high = highSeconds / 86_400f
        val step = DAY_STEPS.firstOrNull { (high - low) / it <= 5 } ?: DAY_STEPS.last()
        val bottom = (floor(low / step) * step).toInt().coerceAtLeast(0)
        var top = (ceil(high / step) * step).toInt()
        if (top <= bottom) top = bottom + step
        return (bottom..top step step).toList()
    }

    private val DAY_STEPS = listOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000)

    // Kept beside the imports, so each is worked out once. [version] is the app's, as the game
    // data, and so every figure, can change with it.
    fun toJson(points: Collection<ProgressPoint>, version: Long): String = JSONObject()
        .put("version", version)
        .put("points", JSONArray().apply {
            for (point in points.sortedBy { it.exportedAtMillis }) {
                put(JSONObject()
                    .put("at", point.exportedAtMillis)
                    .put("th", point.townHall)
                    .put("done", point.done.toDouble())
                    .put("max", point.maxSeconds)
                    .put("groups", JSONObject().apply { for ((group, done) in point.groups) put(group.key, done.toDouble()) })
                    .apply {
                        val since = point.since ?: return@apply
                        put("since", JSONObject()
                            .put("prev", since.previousAt)
                            .put("levels", JSONObject().apply { for ((section, n) in since.levels) put(section.name, n) })
                            .put("idle", JSONObject().apply { for ((section, seconds) in since.idleSeconds) put(section.name, seconds) }))
                    })
            }
        })
        .toString()

    // By export time. Empty when the text is missing, broken or from another version.
    fun parse(text: String?, version: Long): Map<Long, ProgressPoint> = try {
        val root = JSONObject(text ?: "")
        if (root.optLong("version", -1) != version) {
            emptyMap()
        } else {
            val list = root.getJSONArray("points")
            (0 until list.length()).map { index ->
                val entry = list.getJSONObject(index)
                val groups = entry.getJSONObject("groups")
                ProgressPoint(
                    exportedAtMillis = entry.getLong("at"),
                    townHall = entry.getInt("th"),
                    done = entry.getDouble("done").toFloat(),
                    groups = groups.keys().asSequence().mapNotNull { key -> Group.of(key)?.let { it to groups.getDouble(key).toFloat() } }.toMap(),
                    maxSeconds = entry.getLong("max"),
                    since = entry.optJSONObject("since")?.let { since ->
                        val levels = since.getJSONObject("levels")
                        val idle = since.getJSONObject("idle")
                        Between(
                            previousAt = since.getLong("prev"),
                            levels = levels.keys().asSequence().mapNotNull { key -> sectionOf(key)?.let { it to levels.getInt(key) } }.toMap(),
                            idleSeconds = idle.keys().asSequence().mapNotNull { key -> sectionOf(key)?.let { it to idle.getLong(key) } }.toMap(),
                        )
                    },
                )
            }.associateBy { it.exportedAtMillis }
        }
    } catch (_: JSONException) {
        emptyMap()
    }

    private fun sectionOf(name: String) = Section.entries.firstOrNull { it.name == name }
}
