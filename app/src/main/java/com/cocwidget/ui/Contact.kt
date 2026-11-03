package com.cocwidget.ui

import java.net.URLEncoder

// Where people can reach the developer. Reports open already filled in with the app version and
// phone, so nobody has to be asked for them. An empty address hides its row on the main screen.
object Contact {
    const val ISSUES = "https://github.com/Moneymania758/CocWidget/issues/new"
    const val EMAIL = "clashtimers@proton.me"
    const val DISCORD = "https://discord.gg/XZ6bA7wCbr"

    fun device(version: String, android: String, maker: String, model: String) =
        "Clash Timers $version, Android $android, ${phone(maker, model)}"

    // Some phones put the maker in the model name and some don't, so add it only when it's missing.
    private fun phone(maker: String, model: String) =
        if (model.startsWith(maker, ignoreCase = true)) model else "${maker.replaceFirstChar { it.uppercase() }} $model"

    // Opens the bug form with its phone field filled. GitHub fills a form field from the query
    // parameter with the same id.
    fun issueUrl(device: String) = "$ISSUES?template=bug.yml&device=${encode(device)}"

    fun emailUri(device: String) =
        "mailto:$EMAIL?subject=${encode("Clash Timers problem")}&body=${encode("\n\n$device")}"

    fun exportSubject(townHall: Int?) =
        if (townHall == null) "Clash Timers export" else "Clash Timers export: Town Hall $townHall"

    // What the export can't say for itself, then the export. [export] must already have the
    // player tag taken out, and the account's name stays out for the same reason.
    fun exportBody(device: String, builderBoost: Int, labBoost: Int, hammerJam: Boolean, export: String) =
        "Here's my village export.\n\n$device\nBuilder boost set to ${builderBoost}x\n" +
            "Lab boost set to ${labBoost}x\nHammer Jam switch ${if (hammerJam) "on" else "off"}\n\n$export"

    // URLEncoder writes a space as +, which mail apps show as a plus sign. %20 works everywhere.
    private fun encode(text: String) = URLEncoder.encode(text, "UTF-8").replace("+", "%20")
}
