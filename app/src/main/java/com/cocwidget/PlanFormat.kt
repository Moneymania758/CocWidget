package com.cocwidget

import java.util.Locale

// Text for the planner screen, kept free of Android types so unit tests can run it.
object PlanFormat {
    // Resources in the order the planner lists them.
    private val RESOURCE_ORDER = listOf("Gold", "Elixir", "Dark Elixir", "Gold or Elixir", "Shiny Ore", "Glowy Ore", "Starry Ore")

    // "88d 6h", "6h 30m" or "45m": the two largest units, rounded down.
    fun duration(seconds: Long): String {
        val days = seconds / 86_400
        val hours = seconds % 86_400 / 3_600
        val minutes = seconds % 3_600 / 60
        return when {
            days > 0 -> if (hours > 0) "${days}d ${hours}h" else "${days}d"
            hours > 0 -> if (minutes > 0) "${hours}h ${minutes}m" else "${hours}h"
            else -> "${maxOf(minutes, if (seconds > 0) 1 else 0)}m"
        }
    }

    // "5.78M", "56.5K", "40M", as Clash Ninja shows them.
    fun amount(value: Long): String = when {
        value >= 1_000_000_000 -> decimal(value / 1e9) + "B"
        value >= 1_000_000 -> decimal(value / 1e6) + "M"
        value >= 1_000 -> decimal(value / 1e3) + "K"
        else -> value.toString()
    }

    fun costs(costs: Map<String, Long>): String = ordered(costs).joinToString(" · ") { "${it.key} ${amount(it.value)}" }

    fun ordered(costs: Map<String, Long>): List<Map.Entry<String, Long>> = costs.entries
        .sortedBy { RESOURCE_ORDER.indexOf(it.key).let { index -> if (index < 0) RESOURCE_ORDER.size else index } }

    // Copies on the same level, shown as one chip: "Lv 2 ×3". Running copies get their own chip.
    class LevelChip(val level: Int, val count: Int, val running: Boolean, val maxed: Boolean)

    // Lowest level first, so what's furthest behind reads first. [running] holds the levels of the
    // copies being upgraded, which are also in [levels].
    fun levelChips(levels: List<Int>, running: List<Int>, max: Int): List<LevelChip> {
        val busy = running.groupingBy { it }.eachCount()
        return levels.groupingBy { it }.eachCount().toSortedMap().flatMap { (level, count) ->
            val upgrading = minOf(busy[level] ?: 0, count)
            listOfNotNull(
                LevelChip(level, count - upgrading, running = false, maxed = level >= max).takeIf { it.count > 0 },
                LevelChip(level, upgrading, running = true, maxed = false).takeIf { it.count > 0 },
            )
        }
    }

    private fun decimal(value: Double) = String.format(Locale.ROOT, "%.2f", value).trimEnd('0').trimEnd('.')
}
