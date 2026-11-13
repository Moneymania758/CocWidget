package com.cocwidget.plan

import com.cocwidget.game.GameItem
import com.cocwidget.game.VillageParser
import com.cocwidget.plan.Planner.Group
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import kotlin.math.ceil
import kotlin.math.floor

// How far one import was towards max at its own Town Hall. [done] is the share of upgrade time
// done (see Planner.Plan.timeDone), [groups] each group's share as Time to max shows it, and
// [maxSeconds] the time to max with every builder busy, at full price.
data class ProgressPoint(
    val exportedAtMillis: Long,
    val townHall: Int,
    val done: Float,
    val groups: Map<Group, Float>,
    val maxSeconds: Long,
)

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
                    .put("groups", JSONObject().apply { for ((group, done) in point.groups) put(group.key, done.toDouble()) }))
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
                )
            }.associateBy { it.exportedAtMillis }
        }
    } catch (_: JSONException) {
        emptyMap()
    }
}
