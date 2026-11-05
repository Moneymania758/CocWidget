package com.cocwidget.guide

import android.content.Context
import com.cocwidget.game.GameItem
import com.cocwidget.game.Ids
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale

// A hero or pet: how it moves and hits, and its stats at its top level.
class Fighter(
    val id: Int,
    val name: String,
    val flying: Boolean,
    val hitsAir: Boolean,
    val hitsGround: Boolean,
    val range: Int,
    val hitpoints: Int,
    val dps: Int,
)

class Equipment(val id: Int, val name: String, val info: String)

// A troop, siege machine or spell and the Town Hall it opens at.
class ArmyUnit(val id: Int, val name: String, val kind: String, val townHall: Int)

// Generated from the game data by tools/build-coc-data.js.
class GuideFacts(
    val heroes: List<Fighter>,
    val pets: List<Fighter>,
    val equipment: List<Equipment>,
    val army: List<ArmyUnit>,
)

class HeroSetup(val hero: String, val pet: String, val gear: List<String>)

class Strategy(
    val name: String,
    val level: String,
    val about: String,
    val steps: List<String>,
    val army: List<String>,
    val heroes: List<HeroSetup>,
    val sources: List<String>,
)

class TownHallGuide(val townHall: Int, val summary: String, val note: String, val strategies: List<Strategy>)

class PetPick(val pet: String, val why: String)

class HeroGuide(
    val hero: String,
    val role: String,
    val gear: List<String>,
    val split: String,
    val picks: List<PetPick>,
    val avoid: List<PetPick>,
)

class Source(val key: String, val name: String, val date: String, val url: String)

// What something is for in an attack, how to use it and what stops it. Written by hand, in place of
// the game's own text. Heroes and pets keep their role elsewhere, so theirs is empty here. [sources]
// is empty for advice from general play that no guide backs.
class Tactics(
    val role: String,
    val use: List<String>,
    val beware: List<String>,
    val partners: List<String>,
    val sources: List<String>,
)

// Written by hand in assets/guide_content.json. These picks are opinion, so they carry their sources
// and the date they were checked. GuideTest checks them against the game facts.
class GuideContent(
    val checked: String,
    val sources: List<Source>,
    // In the order the pet set hands out pets.
    val heroes: List<HeroGuide>,
    val pets: Map<String, String>,
    val townHalls: List<TownHallGuide>,
    val unitTactics: Map<String, Tactics>,
    val defenceTactics: Map<String, Tactics>,
    val heroTactics: Map<String, Tactics>,
    val petTactics: Map<String, Tactics>,
    val glossary: Glossary,
)

// One thing a Town Hall adds. [first] is false for more copies of something an earlier one added.
class TownHallAddition(val item: GameItem, val quantity: Int, val first: Boolean)

class Guide(val content: GuideContent, val facts: GuideFacts) {
    val glossary: Glossary get() = content.glossary

    private val unitsByName = facts.army.associateBy { it.name }

    fun unit(name: String): ArmyUnit? = unitsByName[name]

    fun heroGuide(name: String): HeroGuide? = content.heroes.firstOrNull { it.hero == name }

    fun townHall(townHall: Int): TownHallGuide? = content.townHalls.firstOrNull { it.townHall == townHall }

    fun unitsOpeningAt(townHall: Int): List<ArmyUnit> = facts.army.filter { it.townHall == townHall }

    // Which heroes like this pet, for the Pets screen.
    fun heroesFor(pet: String): List<String> = content.heroes.filter { hero -> hero.picks.any { it.pet == pet } }.map { it.hero }

    // A pet for each hero at [townHall], since a pet can only go with one hero at a time. Each hero
    // in turn takes its first pick still free. A hero left with none maps to null.
    fun petSet(townHall: Int, items: Map<Int, GameItem>): Map<String, String?> {
        val open = facts.pets.filter { (items[it.id]?.maxLevelAt(townHall) ?: 0) > 0 }.map { it.name }.toMutableSet()
        val heroesOpen = facts.heroes.filter { (items[it.id]?.maxLevelAt(townHall) ?: 0) > 0 }.map { it.name }.toSet()
        val set = LinkedHashMap<String, String?>()
        for (hero in content.heroes) {
            if (hero.hero !in heroesOpen) continue
            val pet = hero.picks.firstOrNull { it.pet in open }?.pet
            if (pet != null) open -= pet
            set[hero.hero] = pet
        }
        return set
    }

    companion object {
        const val FIRST_TOWN_HALL = 3

        @Volatile
        private var loaded: Guide? = null

        fun load(context: Context): Guide = loaded ?: parse(
            context.assets.open("guide_content.json").bufferedReader().use { it.readText() },
            context.assets.open("guide_facts.json").bufferedReader().use { it.readText() },
        ).also { loaded = it }

        fun parse(content: String, facts: String) = Guide(parseContent(JSONObject(content)), parseFacts(JSONObject(facts)))

        // The lowest Town Hall [item] can be used at, or 0 if none.
        fun firstTownHall(item: GameItem): Int = (1..100).firstOrNull { item.maxLevelAt(it) > 0 } ?: 0

        // What the Town Hall upgrade to [townHall] adds, brand new things first.
        fun additions(townHall: Int, items: Map<Int, GameItem>): List<TownHallAddition> {
            val unlocks = items[Ids.TOWN_HALL]?.unlocks.orEmpty()
            val earlier = unlocks.take(townHall - 1).flatMap { it.keys }.toSet()
            return unlocks.getOrNull(townHall - 1).orEmpty().mapNotNull { (id, quantity) ->
                items[id]?.let { TownHallAddition(it, quantity, id !in earlier) }
            }.sortedByDescending { it.first }
        }

        // The current level of every hero, pet, troop, spell, siege machine, defence and trap in an export,
        // by id. With several copies of a defence, it's the highest.
        fun levels(export: String): Map<Int, Int> {
            val root = JSONObject(export)
            val levels = HashMap<Int, Int>()
            for (section in listOf("heroes", "pets", "units", "spells", "siege_machines", "buildings", "traps")) {
                val entries = root.optJSONArray(section) ?: continue
                for (index in 0 until entries.length()) {
                    val entry = entries.optJSONObject(index) ?: continue
                    levels.merge(entry.optInt("data"), entry.optInt("lvl"), ::maxOf)
                }
            }
            return levels
        }

        // The game's ranges are in hundredths of a tile: 450 is "4.5".
        fun tiles(range: Int): String = String.format(Locale.ROOT, "%.2f", range / 100.0).trimEnd('0').trimEnd('.')

        private fun parseFacts(root: JSONObject) = GuideFacts(
            heroes = root.getJSONArray("heroes").objects().map(::fighter),
            pets = root.getJSONArray("pets").objects().map(::fighter),
            equipment = root.getJSONArray("equipment").objects().map { Equipment(it.getInt("id"), it.getString("n"), it.getString("i")) },
            army = root.getJSONArray("army").objects().map { ArmyUnit(it.getInt("id"), it.getString("n"), it.getString("k"), it.getInt("h")) },
        )

        private fun fighter(json: JSONObject) = Fighter(
            id = json.getInt("id"),
            name = json.getString("n"),
            flying = json.getInt("f") == 1,
            hitsAir = json.getInt("a") == 1,
            hitsGround = json.getInt("g") == 1,
            range = json.getInt("r"),
            hitpoints = json.getInt("hp"),
            dps = json.getInt("d"),
        )

        private fun parseContent(root: JSONObject): GuideContent {
            val sources = root.getJSONObject("sources")
            val pets = root.getJSONObject("pets")
            val tactics = root.getJSONObject("tactics")
            return GuideContent(
                checked = root.getString("checked"),
                sources = sources.keys().asSequence().map { key ->
                    val source = sources.getJSONObject(key)
                    Source(key, source.getString("name"), source.getString("date"), source.getString("url"))
                }.toList().sortedBy { it.name },
                heroes = root.getJSONArray("heroes").objects().map { hero ->
                    HeroGuide(
                        hero = hero.getString("hero"),
                        role = hero.getString("role"),
                        gear = hero.getJSONArray("gear").strings(),
                        split = hero.getString("split"),
                        picks = hero.getJSONArray("picks").objects().map(::pick),
                        avoid = hero.getJSONArray("avoid").objects().map(::pick),
                    )
                },
                pets = pets.keys().asSequence().associateWith { pets.getString(it) },
                townHalls = root.getJSONArray("townHalls").objects().map { townHall ->
                    TownHallGuide(
                        townHall = townHall.getInt("th"),
                        summary = townHall.getString("summary"),
                        note = townHall.optString("note"),
                        strategies = townHall.getJSONArray("strategies").objects().map(::strategy),
                    )
                },
                unitTactics = tacticsByName(tactics.getJSONObject("units")),
                defenceTactics = tacticsByName(tactics.getJSONObject("defences")),
                heroTactics = tacticsByName(tactics.getJSONObject("heroes")),
                petTactics = tacticsByName(tactics.getJSONObject("pets")),
                glossary = Glossary(root.getJSONArray("glossary").objects().map(::glossaryTerm)),
            )
        }

        private fun glossaryTerm(json: JSONObject) = GlossaryTerm(
            term = json.getString("term"),
            group = json.getString("group"),
            short = json.getString("short"),
            means = json.getJSONArray("means").strings(),
            example = json.optString("example"),
            aliases = json.getJSONArray("aliases").strings(),
            see = json.optJSONArray("see")?.strings().orEmpty(),
            units = json.optJSONArray("units")?.strings().orEmpty(),
            sources = json.getJSONArray("src").strings(),
        )

        private fun tacticsByName(json: JSONObject): Map<String, Tactics> =
            json.keys().asSequence().associateWith { name ->
                val entry = json.getJSONObject(name)
                Tactics(
                    role = entry.optString("role"),
                    use = entry.getJSONArray("use").strings(),
                    beware = entry.getJSONArray("beware").strings(),
                    partners = entry.optJSONArray("with")?.strings().orEmpty(),
                    sources = entry.getJSONArray("src").strings(),
                )
            }

        private fun pick(json: JSONObject) = PetPick(json.getString("pet"), json.getString("why"))

        private fun strategy(json: JSONObject) = Strategy(
            name = json.getString("name"),
            level = json.getString("level"),
            about = json.getString("about"),
            steps = json.getJSONArray("steps").strings(),
            army = json.getJSONArray("army").strings(),
            heroes = json.optJSONArray("heroes")?.objects().orEmpty().map {
                HeroSetup(it.getString("hero"), it.optString("pet"), it.optJSONArray("gear")?.strings().orEmpty())
            },
            sources = json.getJSONArray("sources").strings(),
        )

        private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { getJSONObject(it) }

        private fun JSONArray.strings(): List<String> = (0 until length()).map { getString(it) }
    }
}
