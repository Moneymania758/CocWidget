package com.cocwidget.bases

import java.net.URI
import java.net.URLDecoder

// The base layout sites the Bases screen opens. Each one is shown as it is, inside the app, and
// nothing is copied from it: their terms forbid harvesting their lists and pictures, so the
// pictures, ads and words stay theirs.
enum class BaseSite(val label: String, val host: String, val firstTownHall: Int, private val path: String) {
    CLASH_CODES("ClashCodes", "clashcodes.com", 2, "/bases/th%d"),
    LAYOUTS("CoC Layouts", "clashofclans-layouts.com", 3, "/plans/th_%d/");

    // The site's list of bases for a Town Hall. Below the site's first one, its first one.
    fun page(townHall: Int) = "https://$host" + path.format(townHall.coerceAtLeast(firstTownHall))

    fun owns(host: String?) = host != null && (host.equals(this.host, true) || host.endsWith(".${this.host}", true))

    companion object {
        fun named(name: String?) = entries.firstOrNull { it.name == name } ?: CLASH_CODES
    }
}

// Where a tapped link goes from inside a site.
enum class Route {
    // Opens the layout in Clash of Clans.
    LAYOUT,

    // Another page of the same site, so it stays in the app.
    STAY,

    // A web page somewhere else, such as an advert, so it opens in the phone's browser.
    BROWSER,

    // Anything else (intent:, market:, javascript:), which is dropped.
    DROP,
}

object BaseLinks {
    const val GAME_PACKAGE = "com.supercell.clashofclans"
    private const val LINK_HOST = "link.clashofclans.com"
    private val TOWN_HALL_ID = Regex("""^TH(\d{1,2}):""", RegexOption.IGNORE_CASE)
    private val WEB_LINK = Regex("""https?://\S+""", RegexOption.IGNORE_CASE)

    // The first layout link in some shared or pasted text, such as a message with the link at its end.
    fun find(text: CharSequence?): String? = text?.let { words ->
        WEB_LINK.findAll(words).map { it.value.trimEnd('.', ',', ')', '>', '"', '\'') }.firstOrNull(::isLayout)
    }

    fun route(url: String, site: BaseSite): Route {
        val uri = parse(url) ?: return Route.DROP
        val scheme = uri.scheme?.lowercase()
        if (scheme != "https" && scheme != "http") return Route.DROP
        return when {
            isLayout(uri) -> Route.LAYOUT
            site.owns(uri.host) -> Route.STAY
            else -> Route.BROWSER
        }
    }

    // A link from the game's Share button, such as
    // https://link.clashofclans.com/en/?action=OpenLayout&id=TH18%3AWB%3AAAAA...
    fun isLayout(url: String) = parse(url)?.let(::isLayout) ?: false

    private fun isLayout(uri: URI) =
        uri.host.equals(LINK_HOST, true) && query(uri)["action"].equals("OpenLayout", true) && !query(uri)["id"].isNullOrEmpty()

    // The Town Hall a layout is for, from the start of its id ("TH18:WB:..."). 0 if it doesn't say.
    fun townHall(url: String): Int {
        val id = parse(url)?.let { query(it)["id"] } ?: return 0
        return TOWN_HALL_ID.find(id)?.groupValues?.get(1)?.toIntOrNull() ?: 0
    }

    private fun parse(url: String): URI? = try {
        URI(url.trim())
    } catch (_: Exception) {
        null
    }

    private fun query(uri: URI): Map<String, String> = uri.rawQuery.orEmpty().split('&').mapNotNull { pair ->
        val key = pair.substringBefore('=')
        if (key.isEmpty()) null else key to decode(pair.substringAfter('=', ""))
    }.toMap()

    private fun decode(value: String) = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (_: Exception) {
        value
    }
}
