# Clash Timers

An Android home-screen widget that shows your Clash of Clans builder, lab, pet and Builder Base
upgrades counting down live, using the game's own village export.

Needs Android 12 or later. It is not available for iPhone.

## Install

1. On your phone, open the [latest release](../../releases/latest) and download the `.apk` file.
2. Open the downloaded file and tap **Install**. If your phone blocks it, allow
   "Install unknown apps" for your browser or file manager and try again.

To update, install the newer `.apk` the same way. Your synced village is kept. From 1.3 on, the
widget shows **Update 2.1 ›** (or whichever is newest) at the bottom when a new version is out.
Tap it to go to the download. From 1.7 on, opening the app after an update shows what it added,
and **What's new** further down lists every version.

## Set up (once)

1. **Add the widget.** Long-press an empty spot on your home screen, tap **Widgets**, find
   **Clash Timers** and drag it onto the screen. Make it as tall as you can. If it still doesn't
   all fit, tap a section title (for example **BUILDER BASE**) to fold that section to one line.
   Tap it again to open it. For just the next upgrade to finish, there's also a small
   one-line widget called **Next to finish**.
2. **Add the sync tile (recommended).** Pull the notification shade all the way down, tap the
   edit button (a pencil, or ⋮ then Edit), and drag **Clash Sync** into your active tiles.
3. **Stop the phone pausing it.** Open your phone's Settings > Apps > Clash Timers > Battery and
   choose **Unrestricted** (on some phones: "Don't optimise" or "No restrictions"). Without this
   the widget can be slow to update.
4. **Allow notifications** when asked (on your first sync, or when you open the app). You'll get
   a notification each time an upgrade finishes, such as "Bomb Tower → 11 finished · A builder is
   free". Tap it to open Clash.
5. **Optional: set your name.** Tap the name at the top of the widget to open the app, tap the
   gear at the top right, type the name you want shown under **Name on the widget** and tap
   **Save name**. Each account has its
   own name.

## Sync your village

1. In Clash of Clans: **Settings > More Settings > Data Export > Copy**.
2. Pull down the notification shade and tap **Clash Sync**. You stay in the game.
   (Or go to your home screen and tap **Sync** on the widget.)
3. You'll see **Village synced**.

**Sync again whenever things change:** you start an upgrade, finish one early with gems or a
book, or use a potion. The widget only knows what was true when you last copied the export.

## More than one account

Export and sync each account the same way: switch account in Clash, copy its Data Export, tap
**Clash Sync**. The app reads the player tag in the export, so each account is added by itself,
with its own timers, boost answers and notifications. Once there's more than one, sync says
which account it was, such as **Main synced** (or the tag, until you name it).

- **Widgets:** add one widget per account. When you place a widget it asks which account to
  show (only if you have more than one). On some phones you can long-press a widget and tap
  **Settings** to switch it later.
- **In the app:** tap an account's chip at the top of the main screen to see it. Time to max,
  the name and **Remove this account** in Settings all follow the chip you picked. Tapping a widget's name
  opens the app on that widget's account.
- **Notifications** carry the account's name when you have more than one.

## During Hammer Jam (or any event that halves upgrade times)

Open the app and turn on **Event: 50% off upgrade times** when the event starts, and turn it off
when it ends. Upgrades you start while it's on then get the right progress bar. Without it,
their bar starts half full, because the widget expects the normal upgrade time.

Timers and notifications are right either way: they use the real time left from your export.
Leaving the switch on by mistake does no harm, because a full-price upgrade has more time left
than a discounted one could, so it's never halved.

## Reading the widget

- **BUILDERS 6/6** is builders busy out of builders you have. An orange **idle** means a free
  builder (or a free lab or pet house).
- **+ Goblin** and the green **GOBLIN** tag mark the extra upgrade run by the event Goblin.
- **×10 34:44** is a boost running and how long it has left. Timers already account for it.
- The blue **HELPER** tag marks the upgrade your Builder's Apprentice or Lab Assistant is
  working on. Timers already account for the hour they're working.
- **Helpers ready in 19:56:32** at the bottom counts down to your helpers' next work day. It
  turns orange as **Helpers ready** when you can give them a new job.
- The **green bar** is how far through the upgrade you are.
- The **timer** counts down live. **Done** means it has finished.
- **+4 more ›** under a section means its last 4 upgrades don't fit. Tap it to show only that section, or make
  the widget taller. The soonest to finish always show first.
- **▾** after a section title means it's open. **▸ 4** means it's folded, with 4 upgrades hidden.
  Tap the title to switch.
- **HOME**, **BUILDER BASE** and **ALL** under the title fold several sections in one tap: HOME
  shows builders, lab and pets, BUILDER BASE shows only the Builder Base, and ALL opens
  everything. The one in use has a gold outline. They appear whenever you have a Builder Base.
- **Update 2.1 ›** in small grey text at the bottom means a new version is out. Tap it to
  download.
- **Exported 13:44** at the bottom is when you last copied the export. It turns **orange** when
  an upgrade has finished since then, or the export is over a day old. That's your cue to sync
  again, because you've probably started something new that the widget can't see yet.

The small **Next to finish** widget shows the one upgrade that finishes first, from any section,
with its countdown. Under it, in orange, anything idle (**2 builders idle · Lab idle**), or in
gold **All 6 builders busy** when nothing is. The Builder Base never counts as idle there. Tap it
to open the app.

The game shows upgrade times as if no potion were running, so while a boost is active the widget
shows shorter times than the game does. The widget's times are when the upgrades really finish.

## Time to max

Open the app and tap **Time to max** to see what's left before everything your Town Hall allows
is maxed. It uses your last sync, so sync first for an up-to-date answer.

- **Builders** is the soonest your builders could finish it all, if none of them is ever idle.
  It can't be shorter than the longest run of upgrades on one thing, such as a hero's levels.
- **Lab** and **Pets** are the research and pet upgrades left, one after another.
- Each group (Defences, Heroes, Walls and so on) shows how much is done, the time left and what
  it costs. Tap a group to see each building or troop in it, with its picture.
- Under each one, chips show how many copies sit on each level: **Lv 2 ×3** means three of them
  are on level 2. Maxed copies get a tick, and one being upgraded shows where it's going, such
  as **Lv 10 → 11**. **Max 6** on the right is the highest level your Town Hall allows.
- Times are at normal speed, without potions, helpers or the Goblin. An upgrade that's already
  running counts the time it has left, and not its cost, which you've paid.
- With the event switch on, upgrades you haven't started are counted at half time and half cost.
- Buildings that merge (two Cannons into a Ricochet Cannon, for example) count as the merged
  building. The copies still to be merged only need the level the merge asks for.

- At the highest Town Hall (TH18 now), buildings on their final level also count the
  supercharges left, shown as a chip such as **+2 supercharges**. Supercharges only open at the
  highest Town Hall, so below it they aren't counted.
- **Hero equipment** lists the Shiny, Glowy and Starry Ore left to max each piece. Equipment
  upgrades are instant, so it adds no time, and events don't discount it. The export lists
  pieces you don't own at level 1. Common pieces come from the Blacksmith, so those count once
  your Blacksmith and the hero are there. Epic pieces at level 1 are left out, since the app
  can't tell whether you own them, and the screen says how many.

Not counted yet: the Builder Base, Crafted Defences, and the gear-ups some TH17 and TH18 merges
need.

## Bedtime planner

Open the app and tap **Bedtime planner** to see which upgrade to start so it finishes while
you're awake, and no builder sits idle overnight.

- Set when you sleep at the top (11pm to 7am to begin with). Tap a time to change it.
- **Builders**, **Lab** and **Pets** each say when they're next free. If that's while you're
  asleep, the planner works from when you wake up.
- Below that are the upgrades you could start, soonest finish first, with how long each takes,
  when it would be done and what it costs. Ones that would finish while you're asleep are left
  out and just counted.
- Like Time to max, it works from your last sync at normal speed, without potions, helpers or the
  Goblin. It only lists levels your Lab, Pet House and Hero Hall are high enough for.

## Potions

Open the app and tap **Potions** to see whether now is a good time for a Builder Potion, Builder
Bite, Research Potion, Study Soup or Pet Potion. Each lasts an hour and speeds up every slot it
covers, so it's fully used when every slot stays busy for the whole hour.

- **Use it now** means nothing goes to waste. **Mostly used** means at least three quarters of
  the possible saving. **Better to wait** means a lot would be wasted.
- It says why: an idle builder (start something first), or an upgrade that would finish partway
  through the hour, leaving its slot idle unless you start something new straight away.
- If a boost is already running, it says how long is left instead.
- It works from your last sync, so sync first. Helpers and the event Goblin aren't counted.

## Themes

Open the app, tap the gear at the top right and pick a look under **Theme**:

- **Widget**: brown and gold, like the widget.
- **Clean**: dark grey with gold, the default.
- **Game**: chunky buttons, thick bars and a game-style font.

It only changes the app's own screens. The widget keeps its look.

## If something's wrong

- **"The clipboard doesn't hold a village export"**: copy the export in Clash again, then sync
  straight away. Some phones clear the clipboard after about an hour.
- **A timer looks wrong**: sync again. The widget can't see upgrades started, sped up or
  finished since your last export.
- **The widget is slow to update, or notifications arrive late**: check the battery setting in
  Set up, step 3.
- **No notifications**: Settings > Apps > Clash Timers > Notifications, and switch them on.

Still stuck? Tap the gear at the top right of the app, then **Report a problem** under **Get help**. It opens a
short form on GitHub with your app version and phone already filled in (you need a free GitHub
account). Or [report it here](https://github.com/Moneymania758/CocWidget/issues/new?template=bug.yml).

No GitHub account? Email **clashtimers@proton.me**, or tap **Email the developer** in the app,
which fills in your version and phone the same way.

Want to help test against more villages? Use **Share your export** in the app's Settings (the gear at the top right). Your email
app opens with the selected account's export and details filled in, so you just press send. Your player
tag is taken out first. It's kept private and only used for testing.

For updates, questions and chat with other players, join the
[Clash Timers Discord](https://discord.gg/XZ6bA7wCbr).

## What it can't show

The export doesn't include these, so the widget can't either: gold, elixir and dark elixir
amounts, and Clan Capital Forge builders.

If you've ticked "Keep assigned until upgrade is complete" for a helper, the widget counts only
the hour they're working at your export, not the ones on later days. Sync after each new work day
starts to pick up the next hour.

---

## For developers

### How it works

- Sync reads the clipboard from a tiny invisible screen, because Android only lets the focused app
  read it. The widget button and the Quick Settings tile both open that screen.
- Every entry with a `timer` anywhere in the export becomes a row. `timer` is seconds left at the
  export's `timestamp`, `lvl` is the current level, `extra: true` is the Goblin's slot. Known
  sections (`buildings`, `units`, `heroes`, `pets`, the `2` Builder Base variants and so on) map
  to their widget section. Anything in a section the parser doesn't know, or nested inside a
  building, is placed by the game data's kind: troops and spells to Lab, pets to Pets, the rest
  to Builders. Guardians (TH18) were missed before this.
- Timers are Android `Chronometer`s. Rows are added to a plain `LinearLayout`, not a scrolling
  `ListView`: on One UI, Chronometers inside a widget `ListView` stop ticking between redraws.
  Sections fold instead of scrolling. Whole days sit in a separate label because a Chronometer
  can't show them, and an alarm redraws the widget when a day rolls over, an upgrade finishes or
  a boost ends, and at least every 15 minutes.
- Boosts: the export's `boosts` block gives seconds left. `builder_boost`, `lab_boost` and
  `pet_boost` are the potions (Builder 10x, Research 24x, Pet 24x). `builder_consumable` is a
  Builder Bite (2x) and `lab_consumable` a Study Soup (4x, for the lab and the Pet House). Work
  done during a boost runs at that rate and the rest at normal speed. A potion and a snack run on
  their own clocks and add up (10x plus 2x makes 11x, 24x plus 4x makes 27x), with a chip each
  on the widget showing the item's picture (bundled from ClashKing).
- Helpers: `helpers[]` gives each helper's level and `helper_cooldown`, the seconds left in the
  23-hour work day they share (missing when none is running). `helper_timer` on an upgrade is
  the real seconds left of a helper's one-hour shift on it, and `helper_recurrent` marks "Keep
  assigned". During a shift the timer runs at the boost's rate (or 1x) plus the helper's level:
  the game's own text says potions don't affect helpers, and its data gives the Apprentice and
  Lab Assistant a `boost_multiplier` equal to their level. A level 12 Assistant during a
  Research Potion is 24 + 12 = 36x. Later shifts from "Keep assigned" aren't counted, because
  whether they start by themselves at the reset isn't known yet.
- Notifications: a separate wake-up alarm fires at the next finish time (the redraw alarm doesn't
  wake the phone). Upgrades that finished more than two hours ago, for example while the phone
  was off, stay quiet.
- Update check: at most every 12 hours from the widget, or every minute when the app is opened,
  the app reads GitHub's `releases/latest`, which skips pre-releases, and compares its tag with
  the installed `versionName`.
- Event discount: each upgrade gets an id that carries over between syncs, matched by predicting
  its time left from the previous export (time left only ever goes down). Unmatched upgrades are
  new, and with the switch on their progress bar total is halved, unless they have more time
  left than half the total.
- Progress bars use per-level upgrade times bundled in `app/src/main/assets/coc_data.json`.
- App screens (not the widget) are built in code by `Ui` in the chosen `Skin`, because the three
  looks differ in borders, raised edges and fonts, not only colours. The Game look uses the Lilita
  One font (SIL Open Font License 1.1, see `licenses/`). Planner pictures come from the same
  ClashKing icon cache as the widget, fetched in the background when the planner opens.
- Planner: the same bundled data also holds per-level costs, each Town Hall's unlocks (how many
  of each building it allows) and merge requirements. Missing copies count from level 1. When
  buildings merge, the lowest copies are the ones merged, so they only need the merge level.
  The builder estimate is the larger of the longest single item and total builder time divided
  by builders.
- Icons download from ClashKing's asset host on first sync and are cached.
- Accounts: each export is filed under its `tag` in its own preferences file
  (`account_<tag>`), holding the export, boost answers, name, folded sections and notification
  record. Theme, event switch and update check stay app-wide in `village`. Each widget's account
  is stored by widget id, and a widget with none (or a removed one) shows the first account
  synced. Up to 1.9.1 the single account lived in `village`. It's moved on first use after the
  update (`Store.migrate`, tested in `AccountsTest`).

### Code layout

Under `app/src/main/java/com/cocwidget/`, with tests in matching folders:

| Package | What's in it |
|---|---|
| `com.cocwidget` | Everything named in the manifest: activities, the widget, the Quick Settings tile |
| `game` | The export parser, village model, boost and helper pace, bundled game data, event discount |
| `plan` | The time-to-max planner, bedtime planner and their text formatting |
| `store` | Saved accounts and settings, the icon cache, the update check |
| `widget` | Widget drawing, its quick views, countdown refresh times, finish notifications |
| `ui` | The in-code view toolkit, themes, shared screen base, Settings screen cards, contact details |

Manifest classes stay in `com.cocwidget`. Android remembers placed widgets, Quick Settings tiles
and launcher icons by class name, so moving one would remove it from people's phones on update.
`game` and `plan` hold no Android views and are where most tests point.

### Building

Needs JDK 17 or later and the Android SDK (Android Studio bundles both). Built with AGP 9.5 and
Gradle 9.6, which AGP 9.5 requires.

- Android Studio: File > Open > this folder, plug the phone in with USB debugging on, Run.
- Command line: `gradlew assembleDebug`, then install `app/build/outputs/apk/debug/app-debug.apk`.

### Tests

```
gradlew testDebugUnitTest
```

Run before every release. The tests cover export parsing against two real exports, TH16 and
TH18 (`app/src/test/resources/village_export*.json`, player tags replaced), potion and helper
maths, day rollover, notification decisions, version comparison and the time-to-max planner
(checked against Clash Ninja's figures). One test also fails if the widget
layouts gain a `ListView` or other collection view, where countdowns stop ticking on One UI.
Widget rendering itself can only be checked on a phone.

### Release signing

Releases are built with `gradlew assembleRelease` and signed by a key that is not in this repo.
Gradle reads it from `keystore.properties` in the repo root (gitignored):

```
storeFile=C:/path/to/clash-timers.jks
storePassword=...
keyAlias=clash-timers
keyPassword=...
```

Every release must be signed with this same key, or phones refuse to install it over the
previous version and people have to uninstall first. Losing the key means that for everyone.

### Refreshing game data

After a game update adds buildings, troops or levels:

```
node tools/build-coc-data.js
```

## Credit

Game data and icons from [ClashKing](https://github.com/ClashKingInc/ClashKingAssets)
(`assets.clashk.ing`).

The Game look's font is [Lilita One](https://fonts.google.com/specimen/Lilita+One) by Juan
Montoreano, under the SIL Open Font License 1.1 ([licences/LilitaOne-OFL.txt](licenses/LilitaOne-OFL.txt)).

This material is unofficial and is not endorsed by Supercell. For more information see Supercell's
Fan Content Policy: www.supercell.com/fan-content-policy.

## Licence

GPL-3.0, matching the ClashKing data the app bundles. See [LICENSE](LICENSE).
