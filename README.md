# Clash Timers

An Android home-screen widget that shows your Clash of Clans builder, lab, pet and Builder Base
upgrades counting down live, using the game's own village export.

Needs Android 12 or later. It is not available for iPhone.

## Install

1. On your phone, open the [latest release](../../releases/latest) and download the `.apk` file.
2. Open the downloaded file and tap **Install**. If your phone blocks it, allow
   "Install unknown apps" for your browser or file manager and try again.

To update, install the newer `.apk` the same way. Your synced village is kept.

## Set up (once)

1. **Add the widget.** Long-press an empty spot on your home screen, tap **Widgets**, find
   **Clash Timers** and drag it onto the screen. Make it as tall as you can. If it still doesn't
   all fit, tap a section title (for example **BUILDER BASE**) to fold that section to one line.
   Tap it again to open it.
2. **Add the sync tile (recommended).** Pull the notification shade all the way down, tap the
   edit button (a pencil, or ⋮ then Edit), and drag **Clash Sync** into your active tiles.
3. **Stop the phone pausing it.** Open your phone's Settings > Apps > Clash Timers > Battery and
   choose **Unrestricted** (on some phones: "Don't optimise" or "No restrictions"). Without this
   the widget can be slow to update.
4. **Optional: set your name.** Tap the name at the top of the widget to open the app, type the
   name you want shown and tap **Save name**.

## Sync your village

1. In Clash of Clans: **Settings > More Settings > Data Export > Copy**.
2. Pull down the notification shade and tap **Clash Sync**. You stay in the game.
   (Or go to your home screen and tap **Sync** on the widget.)
3. You'll see **Village synced**.

If a builder potion or Builder Bite is running, it asks which one the first time it sees that
boost. The export doesn't say which it is, and it changes how fast the timers run.

**Sync again whenever things change:** you start an upgrade, finish one early with gems or a
book, or use a potion. The widget only knows what was true when you last copied the export.

## Reading the widget

- **BUILDERS 6/6** is builders busy out of builders you have. An orange **idle** means a free
  builder (or a free lab or pet house).
- **+ Goblin** and the green **GOBLIN** tag mark the extra upgrade run by the event Goblin.
- **×10 34:44** is a boost running and how long it has left. Timers already account for it.
- The **green bar** is how far through the upgrade you are.
- The **timer** counts down live. **Done** means it has finished.
- **▾** after a section title means it's open. **▸ 4** means it's folded, with 4 upgrades hidden.
  Tap the title to switch.
- **Exported 13:44** at the bottom is when you last copied the export.

The game shows upgrade times as if no potion were running, so while a boost is active the widget
shows shorter times than the game does. The widget's times are when the upgrades really finish.

## If something's wrong

- **"The clipboard doesn't hold a village export"**: copy the export in Clash again, then sync
  straight away. Some phones clear the clipboard after about an hour.
- **A timer looks wrong**: sync again. The widget can't see upgrades started, sped up or
  finished since your last export.
- **The widget is slow to update**: check the battery setting in Set up, step 3.

## What it can't show

The export doesn't include these, so the widget can't either: gold, elixir and dark elixir
amounts, which helper (Apprentice, Lab Assistant) is assigned where, Clan Capital Forge builders,
and Research Potions.

---

## For developers

### How it works

- Sync reads the clipboard from a tiny invisible screen, because Android only lets the focused app
  read it. The widget button and the Quick Settings tile both open that screen.
- Every upgrade with a `timer` in the export becomes a row. `timer` is seconds left at the
  export's `timestamp`, `lvl` is the current level, `extra: true` is the Goblin's slot.
- Timers are Android `Chronometer`s. Rows are added to a plain `LinearLayout`, not a scrolling
  `ListView`: on One UI, Chronometers inside a widget `ListView` stop ticking between redraws.
  Sections fold instead of scrolling. Whole days sit in a separate label because a Chronometer
  can't show them, and an alarm redraws the widget when a day rolls over, an upgrade finishes or
  a boost ends, and at least every 15 minutes.
- Boosts: the export's `boosts` block gives seconds left on a builder and a pet boost. Builder
  Potion is 10x, Builder Bite is taken as 2x (community figure, unconfirmed), Pet Potion 24x.
  Work done during the boost runs at that rate and the rest at normal speed.
- Progress bars use per-level upgrade times bundled in `app/src/main/assets/coc_data.json`.
- Icons download from ClashKing's asset host on first sync and are cached.

### Building

Needs JDK 17 or later and the Android SDK (Android Studio bundles both). Built with AGP 9.5 and
Gradle 9.6, which AGP 9.5 requires.

- Android Studio: File > Open > this folder, plug the phone in with USB debugging on, Run.
- Command line: `gradlew assembleDebug`, then install `app/build/outputs/apk/debug/app-debug.apk`.

### Refreshing game data

After a game update adds buildings, troops or levels:

```
node tools/build-coc-data.js
```

## Credit

Game data and icons from [ClashKing](https://github.com/ClashKingInc/ClashKingAssets)
(`assets.clashk.ing`).

This material is unofficial and is not endorsed by Supercell. For more information see Supercell's
Fan Content Policy: www.supercell.com/fan-content-policy.

## Licence

GPL-3.0, matching the ClashKing data the app bundles. See [LICENSE](LICENSE).
