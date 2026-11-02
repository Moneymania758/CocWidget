# Clash Timers

Android home-screen widget showing Clash of Clans builder, lab, pet and Builder Base upgrade timers,
driven by the game's own village export.

## Install

1. Download `ClashTimers-<version>.apk` from the [latest release](../../releases/latest) on your phone and open it.
   Allow "Install unknown apps" for your browser or file manager when asked.
2. Settings > Apps > Clash Timers > Battery > **Unrestricted**, so the widget keeps refreshing.
3. Long-press the home screen > Widgets > Clash Timers.
4. In Clash: Settings > More Settings > Data Export > Copy. Then tap **Sync** on the widget.

Android 12 or later.

## How it works

- In Clash: Settings > More Settings > Data Export > Copy. This puts your village on the clipboard as JSON.
- Tap **Sync** on the widget. A tiny invisible screen reads the clipboard (Android only lets the
  focused app do that), saves the export and closes.
- Every upgrade with a `timer` becomes a row. `timer` is seconds left at the export's `timestamp`,
  `lvl` is the current level, `extra: true` is the Goblin's slot.
- Timers tick live (Android `Chronometer`). Whole days sit in a separate label because a
  Chronometer can't show them, and an alarm redraws the widget whenever a day rolls over or an
  upgrade finishes, and at least every 15 minutes.
- Progress bars use per-level upgrade times bundled in `app/src/main/assets/coc_data.json`.
- Icons download from ClashKing's asset host on first sync and are cached.

The export is a snapshot. Sync again after starting an upgrade, finishing one early or using a potion.

## Boosts

The export's `boosts` block gives seconds left on a builder boost and a pet boost, not which item
caused it. Sync asks whether the builder boost is a Builder Potion (10x) or a Builder Bite (2x,
community figure, unconfirmed). Pet boosts are treated as the Pet Potion (24x). Research Potions
aren't handled yet: the field name hasn't been seen in an export.

## What the export doesn't contain

- Your player name (set one in the app).
- Helper assignments and cooldowns (Apprentice, Lab Assistant).
- Builders working in the Clan Capital Forge.

## Building

Needs JDK 17 or later and the Android SDK (Android Studio bundles both). Built with AGP 9.5 and
Gradle 9.6, which AGP 9.5 requires.

- Android Studio: File > Open > this folder, plug the phone in with USB debugging on, Run.
- Command line: `gradlew assembleDebug`, then install `app/build/outputs/apk/debug/app-debug.apk`.

One UI: set the app's battery usage to **Unrestricted**, or Samsung may delay the refresh alarms.

## Refreshing game data

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
