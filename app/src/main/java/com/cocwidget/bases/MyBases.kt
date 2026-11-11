package com.cocwidget.bases

import com.cocwidget.R
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

// What a saved base is for, picked by the player. Optional.
enum class BaseKind(val key: String, val label: Int) {
    WAR("war", R.string.base_kind_war),
    CWL("cwl", R.string.base_kind_cwl),
    FARMING("farming", R.string.base_kind_farming),
    LEGENDS("legends", R.string.base_kind_legends);

    companion object {
        fun of(key: String?) = entries.firstOrNull { it.key == key }
    }
}

// A layout link the player saved, with their own name for it. [account] is the player tag it was
// saved for, empty when saved before any sync. [opened] is when it last went to the game, 0 if never.
data class SavedBase(
    val id: String,
    val account: String,
    val link: String,
    val name: String,
    val kind: BaseKind?,
    val added: Long,
    val opened: Long = 0,
) {
    val townHall: Int get() = BaseLinks.townHall(link)
}

// The saved bases as kept in settings: one list for every account, newest first.
object MyBases {
    fun parse(text: String?): List<SavedBase> {
        if (text.isNullOrEmpty()) return emptyList()
        val array = try {
            JSONArray(text)
        } catch (_: JSONException) {
            return emptyList()
        }
        return (0 until array.length()).mapNotNull { index ->
            val json = array.optJSONObject(index) ?: return@mapNotNull null
            val id = json.optString("id")
            val link = json.optString("link")
            if (id.isEmpty() || link.isEmpty()) return@mapNotNull null
            SavedBase(
                id = id,
                account = json.optString("account"),
                link = link,
                name = json.optString("name"),
                kind = BaseKind.of(json.optString("kind")),
                added = json.optLong("added"),
                opened = json.optLong("opened"),
            )
        }
    }

    fun toJson(bases: List<SavedBase>): String = JSONArray().apply {
        for (base in bases) {
            put(JSONObject().apply {
                put("id", base.id)
                put("account", base.account)
                put("link", base.link)
                put("name", base.name)
                base.kind?.let { put("kind", it.key) }
                put("added", base.added)
                if (base.opened > 0) put("opened", base.opened)
            })
        }
    }.toString()

    // An account's own bases, and any saved before there was an account to save them to.
    fun forAccount(bases: List<SavedBase>, account: String?): List<SavedBase> =
        bases.filter { it.account.isEmpty() || it.account == account }

    // A base already saved for this account with this link, so saving it again updates that one.
    fun matching(bases: List<SavedBase>, account: String, link: String): SavedBase? =
        bases.firstOrNull { it.account == account && it.link == link }

    // Replaces the base with the same id where it is, or adds it at the top.
    fun put(bases: List<SavedBase>, base: SavedBase): List<SavedBase> =
        if (bases.any { it.id == base.id }) bases.map { if (it.id == base.id) base else it } else listOf(base) + bases
}
