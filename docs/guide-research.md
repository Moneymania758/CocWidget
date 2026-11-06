# In-app guide: research

Researched 2026-11-05, against the game at Town Hall 18. Input for building the guide section.

How far to trust each part:

- **Game facts** come from ClashKing's `static_data.json`, the source the app already uses. Current
  as of this date: Fire Heart regeneration 140, Frozen Arrow damage 175 and Portal Pendant HP 1,200
  all match Supercell's October post. Hero caps match Clash Ninja at every level from TH7 to TH18.
- **Usage and win figures** come from Supercell's balance posts. They cover TH18 against TH18 in
  wars and CWL only. Nothing official exists for lower Town Halls.
- **Army and pet picks are opinion.** Most guide sites sell base or army links or boosting, and they
  disagree. No source gave pet pick rates. Show these as recommendations with a checked date, not as
  measured "best".
- **Not read:** the Fandom wiki (HTTP 402) and Reddit (nothing came up in search). Video guides were
  not watched, so high-end picks come from pages that summarise them.

## Town Hall 19 changes the timing

- Supercell, 1 Nov: "Town Hall 19 lands mid-month." No date given. Secondary site: Sneak Peeks
  13 to 15 Nov. Its 19 Nov release date is a guess.
- Reported by Base Drop (10 Aug), not found on Supercell's site: Guardians leave the Town Hall and a
  new TH19 weapon arrives, older merged defences get weaker, old troops are meant to return to the
  meta.
- **Pets moving into the Laboratory with a second researcher: not confirmed.** It traces to an
  interview, apparently Judo Sloth Talks with Fernando Ferri (not watched). Base Drop calls it both
  "confirmed" (10 Aug) and "being looked at, not announced" (1 Sep). Nothing from Supercell found.
  If it ships, it affects the widget's Pet House rows, Study Soup and pet boost handling, and the
  planner, not just the guide. Worth checking in the Sneak Peeks.
- TH16 to TH18 picks are the most likely to go stale. TH3 to TH12 picks should hold.

## Town Halls (game data)

Cost is Gold. Days are full build time without events.

| TH | Cost | Days | Hero Hall | Pet House | Lab | Blacksmith | Hero first available | Main new defences and buildings |
|---|---|---|---|---|---|---|---|---|
| 3 | 4K | under 0.1 | - | - | 1 | - | | Mortar, Laboratory |
| 4 | 25K | 0.1 | 1 | - | 2 | - | Barbarian King (level 1 only) | Air Defense, Hero Hall |
| 5 | 150K | 0.3 | 1 | - | 3 | - | | Wizard Tower, Spell Factory |
| 6 | 500K | 0.5 | 1 | - | 4 | - | | Air Sweeper, Giant Bomb |
| 7 | 1M | 0.8 | 1 | - | 5 | - | King levels open up | Hidden Tesla, Dark Barracks, Drill |
| 8 | 2M | 1 | 2 | - | 6 | 1 | Archer Queen | Bomb Tower, Dark Spell Factory, Blacksmith |
| 9 | 2.5M | 2 | 3 | - | 7 | 2 | Minion Prince | X-Bow, Helper Hut |
| 10 | 3.5M | 3 | 4 | - | 8 | 3 | | Inferno Tower |
| 11 | 4M | 5 | 5 | - | 9 | 4 | Grand Warden | Eagle Artillery |
| 12 | 6M | 6 | 6 | - | 10 | 5 | | Workshop |
| 13 | 9M | 7 | 7 | - | 11 | 6 | Royal Champion | Scattershot |
| 14 | 12M | 7.5 | 8 | 4 | 12 | 7 | | Pet House |
| 15 | 13M | 8 | 9 | 8 | 13 | 8 | Dragon Duke | Spell Tower, Monolith |
| 16 | 15M | 9 | 10 | 10 | 14 | 9 | | Multi-Archer Tower, Ricochet Cannon |
| 17 | 16M | 10 | 11 | 11 | 15 | 10 | | Firespitter, Multi-Gear Tower, Giga Bomb |
| 18 | 25M | 12 | 12 | 12 | 16 | 10 | | Super Wizard Tower, Revenge Tower, Crafting Station |

TH1 and TH2 are left out. TH2 costs 1,000 Gold and neither has a real attack plan. Sources start
useful advice at TH2 or TH3.

### Hero level caps (game data, matches Clash Ninja)

| TH | King | Queen | Prince | Warden | Champion | Duke |
|---|---|---|---|---|---|---|
| 4 to 6 | 1 | | | | | |
| 7 | 10 | | | | | |
| 8 | 20 | 10 | | | | |
| 9 | 30 | 30 | 10 | | | |
| 10 | 40 | 40 | 20 | | | |
| 11 | 50 | 50 | 30 | 20 | | |
| 12 | 65 | 65 | 40 | 40 | | |
| 13 | 75 | 75 | 50 | 50 | 25 | |
| 14 | 85 | 85 | 60 | 60 | 30 | |
| 15 | 90 | 90 | 70 | 65 | 40 | 10 |
| 16 | 95 | 95 | 80 | 70 | 45 | 15 |
| 17 | 100 | 100 | 90 | 75 | 50 | 20 |
| 18 | 110 | 110 | 95 | 85 | 55 | 25 |

## Heroes (game data)

| Hero | Unlock | Upgrades with | Moves | Hits | Range (data units) | Max HP / DPS |
|---|---|---|---|---|---|---|
| Barbarian King | TH4, Hall 1 | Dark Elixir | Ground | Ground | 100 | 13,350 / 713 |
| Archer Queen | TH8, Hall 2 | Dark Elixir | Ground | Both | 500 | 3,576 / 810 |
| Minion Prince | TH9, Hall 3 | Dark Elixir | Flying | Both | 450 | 4,510 / 765 |
| Grand Warden | TH11, Hall 5 | Elixir | Ground (walks or flies over walls) | Both | 700 | 2,669 / 354 |
| Royal Champion | TH13, Hall 7 | Dark Elixir | Ground (over walls) | Both | 300 | 4,298 / 575 |
| Dragon Duke | TH15, Hall 9 | Dark Elixir | Flying | Both | 125 | 10,900 / 400 |

Dragon Duke gets stronger when separated from friendly flying units (game text). This explains the
warning below about Sneezy.

### Equipment by hero (game data)

The data has every piece with its rarity and ability text. Common and epic as listed:

| Hero | Common | Epic |
|---|---|---|
| Barbarian King | Barbarian Puppet, Rage Vial, Earthquake Boots, Vampstache | Giant Gauntlet, Spiky Ball, Snake Bracelet, Stick Horse |
| Archer Queen | Archer Puppet, Invisibility Vial, Giant Arrow, Healer Puppet | Frozen Arrow, Monolith Arrow, Magic Mirror, Action Figure |
| Grand Warden | Eternal Tome, Life Gem, Rage Gem, Healing Tome | Heroic Torch, Fireball, Lavaloon Puppet |
| Royal Champion | Seeking Shield, Royal Gem, Hog Rider Puppet, Haste Vial | Rocket Spear, Electro Boots, Frost Flake |
| Minion Prince | Henchmen Puppet, Dark Orb, Metal Pants, Noble Iron | Dark Crown, Meteor Staff, Portal Pendant |
| Dragon Duke | Fire Heart, Stun Blaster, Flame Blower, Electro Fangs | Rocket Backpack, Revenge Deck |

## Pets (game data, level caps from Clash Ninja)

| Pet | Unlock | Pet House | Max | Moves | Hits | Does |
|---|---|---|---|---|---|---|
| L.A.S.S.I | TH14 | 1 | 15 | Ground | Ground | Fast melee chaser |
| Electro Owl | TH14 | 2 | 15 | Flying | Both | Long range zaps, fragile |
| Mighty Yak | TH14 | 3 | 15 | Ground | Ground | Breaks walls, rages if the hero falls |
| Unicorn | TH14 | 4 | 15 | Ground | Both | Heals the hero |
| Frosty | TH15 | 5 | 15 | Ground | Both | Slows defences, Frostmites |
| Diggy | TH15 | 6 | 15 | Ground | Ground | Burrows, surfaces to attack buildings |
| Poison Lizard | TH15 | 7 | 15 | Ground | Both | Fast, slowing poison |
| Phoenix | TH15 | 8 | 10 | Flying | Both | Egg, hatches to make the hero briefly invulnerable when it would fall |
| Spirit Fox | TH16 | 9 | 10 | Ground | Ground | Makes the hero invisible at times |
| Angry Jelly | TH16 | 10 | 10 | Flying | Both | Turns the hero onto defences, then splits |
| Sneezy | TH17 | 11 | 10 | Flying | Both | Sneezes tanky air Boogers, attacks when the hero falls |
| Greedy Raven | TH18 | 12 | 10 | Flying | Both | Extra damage to resource buildings, farming pick |

## Which pet with which hero (opinion)

Five sources: AllClash (Mar 2026), Base Drop (Oct 2026), GameRant (Feb 2026, predates Dragon Duke), Clash Ninja
(modified Apr 2026, covers all heroes and pets), and the pets actually used in 1337wiki's four TH18 armies (Oct 2026).
Clash Ninja was added after the first draft, and `guide_content.json` holds the current picks and warnings.

| Hero | Suggested first | Also good | Where they disagree |
|---|---|---|---|
| Barbarian King | Frosty | Phoenix | Base Drop has Spirit Fox first. AllClash has Phoenix first. |
| Archer Queen | Unicorn for Queen Charge | Frosty otherwise | 1337wiki never uses Unicorn at TH18: Frosty three times, Sneezy once. |
| Grand Warden | Sneezy (TH17 up) | Electro Owl below TH17, Poison Lizard for ground | AllClash prefers Owl and Lizard over Sneezy. |
| Royal Champion | Spirit Fox | Phoenix, Diggy | AllClash calls Diggy the best pairing in the game. Base Drop has Phoenix first. |
| Minion Prince | Angry Jelly | Phoenix, Electro Owl | Base Drop has Owl first. |
| Dragon Duke | Angry Jelly | Spirit Fox, Phoenix | Spirit Fox is in 1337wiki's two Duke Charge armies. |

Worth showing as warnings or advice:

- Avoid Sneezy on Dragon Duke. AllClash says the Booger stops his enrage. It fits the game text, but
  was not tested.
- Swap Greedy Raven out before war (Base Drop). AllClash rates it B+ on Queen and Warden, depending
  on the layout.
- Pets conflict. Angry Jelly is the top pick for two heroes and every 1337wiki army uses each pet
  once. So the guide should offer full sets for an attack, as well as per-hero picks. Pet guides in a later
  search agree that a pet can only be assigned to one hero at a time.
- Below TH16 only the listed unlocks exist, so picks must be filtered by Town Hall.

## Attack strategies by Town Hall (opinion)

Picks are ordered by how many sources agree. Source key: **B** Blueprint CoC, **T** Trophy Coach,
**S** Skycoach, **E** EZ Arena, **W** 1337wiki, **D** Base Drop.

| TH | Most agreed | Also listed | Easy start |
|---|---|---|---|
| 3 | Giants, Archers, Wall Breakers (S, E, W) | | Same |
| 4 | Balloons and Archers (S, E, W) | | Same |
| 5 | Giants, Wizards, Balloons (S, E, W) | | Same |
| 6 | Giant and Wizard ground push with Healing (S, W, E) | S warns air is inconsistent here | Same |
| 7 | Mass Dragons / Zap Dragons (S, E, W, T) | Hog Riders (W) | Mass Dragons |
| 8 | GoHo (S, W) | Golem and Valkyrie push (E), Mass Dragons (T), GoWiPe (W) | Mass Dragons |
| 9 | Witch Slap / Mass Witches (S, W, E, B) | Zap Lalo, Zap Dragons (B), Hog Riders, GoWiPe (T) | Mass Witches |
| 10 | Zap Dragons (S, E, W) | Queen Charge Hybrid (T, W), Zap Lalo, Falcon, Penta Lalo (B) | Zap Dragons |
| 11 | Zap Witches (S, E, W, B) | Queen Charge Hybrid (B, E, T), Electro Dragons (B, T), LaLoon (T) | Zap Witches |
| 12 | Zap Dragons (B, E) | Queen Charge Hybrid (B, T), DragBat (S, W), Super Witch Fireball (B), Yeti Smash (T) | Zap Dragons |
| 13 | Yeti Smash (S, W, T), Yeti Super Archer Spam (B, E) | Super Archer Clone Hydra (B, E), QC Dragon Rider (B) | Yeti Super Archer Spam |
| 14 | Mass or Super Dragons with Blimp (S, W, B, E) | Super Archer Blimp Dragons, Fireball Super Witches, RC Walk Dragons (B), Sui Lalo, DragBat (E) | Super Dragons |
| 15 | Root Rider with RC Charge (B, T), Warden Charge Super Yetis (B, E) | Hydra (S, W), Fireball Rocket Loons, Fireball Super Witches, RC Charge Dragons (B) | Root Rider Smash |
| 16 | Fireball Super Yeti (B, E, T), RC Walk Root Riders (B, E, T) | Root Riders and Valkyries (S, W), RC Walk Dragons (B), Fireball Super Witch (B) | Fireball Super Yeti |
| 17 | Fireball Super Yeti (B, E, T), RC Walk Dragons (B, E, and S and W as Invisibility Dragons) | RC Walk Root Riders, Fireball Rocket Loons (B), Hydra (T) | Fireball Super Yeti |
| 18 | Root Rider Yeti (T, D, W), Dragon Duke Charge Hydra (W, B), Rocket Backpack Throwers (W, B), Super Yeti with RC Charge (T, B) | Duke Charge Super Bowlers (W, B), Ruin Witch (T), Super Bowler Smash (E), RC Walk Meteor Golems (W), Lalo (D), Air Slice (S) | Super Yeti with RC Charge |

The "Easy start" column is a reading of what sources called forgiving or simple. It needs checking
by real players, especially at TH17 and TH18.

Example armies for the app come from Skycoach (TH2 to TH18 with counts) and 1337wiki (TH18 with full
equipment and pets). Other sites hide the army behind a link. Rewrite and check any counts before use.

### What Supercell's own numbers say (TH18 against TH18, wars and CWL)

- 9 Jul 2026: Dragon armies (Duke and Dragon Riders) in about 20% of attacks, the most used. Throwers
  with Healers second. Together about 40%.
- 17 Oct 2026: average three-star rate 53%. Ruin Witch about 76%, highest of any troop with meaningful
  use. Duke with Fire Heart about 66%, without about 56%. Fire Heart in almost 40% of attacks.
  Frozen Arrow in about 7% at about 31% three-star. Hog Rider, Miner and P.E.K.K.A each under 3% of
  attacks at 20% to 35%.
- The October post nerfs Fire Heart and Ruin Knights and buffs Hog Rider, Miner, P.E.K.K.A, Meteor
  Golem and Frozen Arrow. Those figures predate the changes. The post gives no go-live date, but the
  Fire Heart, Frozen Arrow and Pendant values are already in the data. Ruin Knights could not be checked:
  they aren't listed as a troop.

### Where sources are wrong or unsafe

- 1337wiki lists Root Rider Smash "from TH14", but the data unlocks Root Riders at TH15.
- 1337wiki gives Electro Dragon and Yeti Smash as TH10 to TH13. The data unlocks them at TH11 and TH12.
- Blueprint's main army page has no TH18 section. Its TH18 page has names only. Its FAQ names armies
  absent from the article.
- AllClash's TH18 page is from Nov 2025 and predates Dragon Duke.

## Rules for what we can publish

Supercell's Fan Content Policy (last updated 27 Sep 2023):

- Non-commercial guide apps are allowed. The app's existing "unofficial, not endorsed" notice covers
  the required wording. Show it on or near the guide too.
- **Nothing unreleased.** Leaks break the policy, so TH19 content only after Supercell makes it public.
  No "leaked" YouTube content, including theme or troop rumours.
- Supercell assets can be displayed but not modified.
- Guide sites' text is their copyright. Write our own wording and credit them as sources.

## Gaps in the data, for the build

- **Troop unlocks** can be derived from `production_building_level`, but not for everything:
  - Super troops land on the base troop's Barracks level, which is too early. The app uses the latest of
    TH11 (guides), the base troop's unlock and the Town Hall of the base level in `super_troop`.
  - Level 1 `required_townhall` on troops is unreliable (Super Yeti says 3).
  - About 20 event troops have no Barracks level. Leave them out.
  - Meteor Golem appears twice.
  - Ruin Witch derives to TH16. Not verified elsewhere, and Supercell's figures are TH18 only.
- **Equipment:** `required_townhall` says TH8 for most epics, which doesn't tell you when you can get
  them. Show rarity, not an unlock level.
- **Army capacity, spell capacity and Clan Castle size** aren't in the data. Leave them out or add
  them by hand.
- `info` text holds a literal `\n` to clean up.

## Tactics text (added 2026-11-05)

Replaces the game's own descriptions on troop, spell, siege, hero and pet pages. It lives in
`tactics` in `guide_content.json`, with sources per entry. The game text is gone from the guide files
and the build scripts, except equipment text, which was never shown.

How far to trust it:

- **Guides backed the newer units best:** Ruin Witch, Thrower, Meteor Golem, Overgrowth, Ice Block, Totem,
  Dragon Duke and Super Troops all had a dedicated page read for this.
- **No source at all, written from general knowledge of the game:** Goblin, Electro Titan, Minion, Valkyrie,
  Bowler, Rocket Balloon, Super Bowler, Jump Spell. Entries with sources can also hold a line of general
  knowledge, so a source doesn't vouch for every line.
- **Old sources:** AllClash Super Troops (Dec 2020), AllClash Siege Machines (Dec 2022), Blueprint on Recall
  (Oct 2022). Used for how things behave more than for rankings.
- **Thin:** Sky Wagon and Troop Launcher have little written about them. Angry Spell's guides were written
  before release, so it leans on the game text, the data and one Blueprint pairing. Revive's timing advice
  comes from a search excerpt of Sportskeeda, as the page refused to load.
- **Disagreements settled in favour of the data or the newer source:** Ruin Knights cap at 10 and lost their
  decay (Base Drop, Sep 2026, and the data), not 8 (Blueprint, Jun 2026). A Log Launcher opens at TH13 in the
  data, so COC Base Guide's TH12 plan isn't repeated. Base Drop's pre-release Ruin Witch page was wrong and is
  not used. Battle Drill goes by the game text (nearest defence), not AllClash's older description.
- TH19 is due mid-November, so meta claims, especially at TH16 to TH18, may go stale.

Sources added:
- [Base Drop: Thrower](https://cocbasedrop.com/blog/thrower-troop-guide-coc-2026-healers), [Meteor Golem](https://cocbasedrop.com/blog/meteor-golem-guide-coc-2026-biggest-buff), [Ruin Knights](https://cocbasedrop.com/blog/ruin-witch-ruin-knights-buff-coc-2026), [Overgrowth](https://cocbasedrop.com/blog/overgrowth-spell-guide-coc-2026-how-to-use), [Minion Prince](https://cocbasedrop.com/blog/minion-prince-best-equipment-coc-2026), [Super Troops](https://cocbasedrop.com/blog/best-super-troops-guide-coc-2026)
- [Blueprint: Ruin Witch](https://blueprintcoc.com/blogs/town-hall-18/best-th18-ruin-witch-army-links), [Rocket Backpack](https://blueprintcoc.com/blogs/clash-of-clans-guides/rocket-backpack-equipment-guide), [Recall](https://blueprintcoc.com/blogs/new-clash-of-clans-update/th15-sneak-peek-4)
- [Trophy Coach: equipment](https://trophycoach.com/clash-of-clans/guides/hero-equipment-guide), [Dragon Duke](https://trophycoach.com/clash-of-clans/guides/dragon-duke-guide)
- [COC Base Guide: spells](https://coc-base-guide.vercel.app/articles/coc-spell-guide-2026), [war troops](https://coc-base-guide.vercel.app/articles/best-war-troops-2026)
- [Mobile Matters: Totem](https://mobilematters.gg/guides/clash-of-clans/totem-spell-overview), [Ice Block](https://mobilematters.gg/guides/clash-of-clans/ice-block-spell-overview), [Meteor Golem](https://mobilematters.gg/guides/clash-of-clans/meteor-golem-overview), [Siege Machines](https://mobilematters.gg/clash-of-clans/all-siege-machines-in-clash-of-clans-explained)
- [AllClash: Siege Machines](https://www.allclash.com/siege-machine-guide/), [Super Troops](https://www.allclash.com/supertroops-guide-all-supertroops/)
- [Clash Ninja: April update](https://www.clash.ninja/guides/april-2026-update-is-live), [SportsDunia: Totem](https://www.sportsdunia.com/gaming/clash-of-clans-totem-spell), [Sportskeeda: Furnace](https://sportskeeda.com/mobile-games/furnace-troop-clash-clans-skills-stats), [Touch, Tap, Play: Headhunter](https://www.touchtapplay.com/how-to-use-headhunter-in-clash-of-clans/)

## Defence tips and the glossary (added 2026-11-05)

`tactics.defences` and `glossary` in `guide_content.json`. Each entry lists its sources. All pages were read through a small fetch model, not line by line.

How far to trust them:

- **Defence tips lean on one fan wiki, 1337wiki.** Its values matched the data and Supercell where compared, except the Monolith's unlock, but its counter advice mostly has no second source. Its army pages have known errors (see "Where sources are wrong or unsafe").
- **Glossary: 37 terms have no source** and are standard game knowledge: No source read, standard game knowledge: Tank, Draw fire, Lock on, Push, Carry, Spam, Mass, Dive, Clean-up, Lure, Snipe, Stall, Distraction, Mid-base, Time out, Clan Castle, Housing space, Splash, Single target, Multi-target, Chain, Aura, Melee and ranged, Tile, Minimum range, Spawned troops, Enrage, CWL, Siege Machine, Air mode and ground mode, Spell Tower effects, Inferno, Hogs, Loot, Farming, Friendly challenge, Meta..
- Army names are used loosely in the community, and our text mixes walk and charge. The pages say so.
- Trophy pushing was dropped: nothing in our text uses it, and it may not fit the 2026 Ranked changes (not checked).
- Guardian: Clash Ninja's April 2026 notes (Poison works at reduced damage, no waking while Frozen) replace a 2025 line saying Poison does nothing.
- The Monolith hits one target, hardest on high health. Earlier lines saying clustered troops suffer from it were wrong and are fixed.

ClashKing flags X-Bow and Skeleton Trap ground only and gives Builder's Hut no flag, though clash-of-clans-data has all three as both. `HITS_NOTES` in `tools/build-guide-units.js` gives those pages, and the Air Sweeper's, a chip in words.

Defence writer's conflicts:

- Monolith unlock: guide_units.json says TH14 (h and th). Every source and guide-research.md say TH15. No Town Hall appears in the text, but the data looks wrong and is worth checking.
- Monolith splash: Base Drop (Apr 2026) calls it heavy area damage. The data says single target and 1337wiki agrees, so I wrote single target. The finished Root Rider and Super Yeti entries say clustered armies suffer from the Monolith, which implies splash. Those lines were not touched and may need changing.
- Revenge Tower stages: Clash Ninja, Blueprint and Mobile Matters (Nov 2025) give 5, 20 and 40 buildings. Supercell (28 Jan 2026) moved the later two to 25 and 50, and the data's 0.35s speed matches that post. The text gives no numbers, so it will not go stale.
- Revenge Tower while dormant: Mobile Matters (Nov 2025) says it cannot be destroyed, 1337wiki (Oct 2026) says it can. Unresolved. The text avoids the claim, and its advice holds either way.
- Giga Bomb trigger: the data says 'Needs a troop of 18 housing', which reads as one troop. Clash Ninja and 1337wiki say total housing nearby. The text says 18 housing close by, which fits both readings.
- Giant Bomb radius: Base Drop (Jun 2026) says it scales with nearby troop count. The data's chart and 1337wiki tie it to level. Went with the data.
- Eagle Artillery wake-up: AllClash (2016) and a Base Drop search excerpt say 150 housing. The data says 200, and 1337wiki agrees.
- Eagle Artillery spells: heroes and spells counting comes from 1337wiki (Oct 2026), with AllClash (2016) agreeing on spells. The data gives only the 200.
- Skeleton Trap targeting: the data flags ground only, but its own i field and 1337wiki say it can be set to air. The text says either. The gr flag looks incomplete.
- Builder's Hut targeting: the data has no a or gr flag. 1337wiki says the turret hits air and ground. The text does not state it, so the flags need adding if the app shows them.
- X-Bow targeting: the data flags ground only, with air and ground as a listed mode. The text covers both modes.
- Spell Tower Earthquake: the data opens level 4 at TH17, Supercell's February 2026 notes list it under TH18, and 1337wiki gives both. No Town Hall appears in the text. Ground only comes from 1337wiki, and Supercell only confirms the spell damages ground troops.
- Mortar blind spot: 1337wiki says 4 tiles, the data lists no minimum range, and the game's own text hints at one. The text says 'right beside it', with no number.
- Firespitter: coc.guide says single target and the data says multiple. 1337wiki says each shot can pierce into a second unit. Went with the data.
- Spring Trap: Blueprint's FAQ says heroes and heavy troops get launched, but its article body and 1337wiki say over-capacity troops take damage and a knockback instead. Went with the article body, which also fits the data's capacity chart.
- Inferno Tower: Clash Ninja (old) says Freeze a few seconds in, Trophy Coach and the Freeze Spell entry say at lock-on. The text says 'as it heats up'. A search excerpt said multi-target stops healing, but Clash Ninja says healing has worked since December 2017, so I ignored it.
- Multi-Gear Tower: some search excerpts mention splash. The data and Blueprint say single target, so those were ignored.
- Tornado Trap count: Base Drop says two, the data and 1337wiki say one. The text gives no count.

Defence writer's weak lines:

- Single source: almost all attacker advice comes from 1337wiki's defence and trap pages (Oct 2026). Its stats matched the data and Supercell on the values I compared (Ricochet bounce, Multi-Archer speed, Revenge speeds, Spell Tower values, Firespitter range), apart from the Monolith unlock. Its counter advice mostly has no second source. guide-research.md already records errors on its army pages.
- All pages were read through WebFetch's small model, so wording and numbers were not checked line by line against the original pages.
- Multi-Gear Tower: no source says how to tell the mode by looking at it, so the entry gives no way to. Both beware lines are my own inference from the mode stats. 'Thin for its level' rests on 1337wiki and its hitpoint table only.
- Firespitter: the cone width (about 150 degrees), the pause on a new target and the piercing are 1337wiki only. Supercell and Blueprint only confirm the cone and stray shots.
- Revenge Tower: the Freeze, Totem, Ice Block and Overgrowth lines are 1337wiki only. No guide I read gives other counters.
- Super Wizard Tower: the counters and chain reach are 1337wiki only. 'A few tiles' stands for its figure of 4. Mobile Matters only names the troops that suffer.
- Spell Tower: triggers and counters are 1337wiki only, apart from Blueprint's Backpack Thrower page on Rage. The other sources predate Earthquake. The Log Launcher trick and baiting with a Skeleton Spell are not confirmed elsewhere.
- Giga Bomb: the list of what does not set it off, and Freeze and Overgrowth failing once armed, are 1337wiki only. Air and ground comes from the data.
- Seeking Air Mine: which flyers qualify is 1337wiki only, and the data does not state a housing limit.
- Monolith: 'Expect a few shots on the first hero' follows 1337wiki, but 'do not send a hurt one' is my own. 'Spreading out does not save a tank' is my own reasoning from single-target data.
- X-Bow beware line: my own reasoning from its long range and single target. 1337wiki gave no clear punish.
- Archer Tower: 'Give it a tank to shoot' is general game knowledge.
- Eagle Artillery: 'Slow attacks' is my inference from how it wakes and fires. AllClash's Freeze timing is from 2016, though 1337wiki agrees on the pause.
- Inferno Tower: both beware lines are general knowledge and the existing Yeti entry. Multi-target advice comes mainly from Clash Ninja's old guide (2018 or 2019).
- Mortar: the geared-up line is 1337wiki only, and the data only gives the 0.5s burst.
- Tornado Trap: 'Expect it near the Town Hall or core' rests on one promotional Blueprint base post. Balloons under Air Bombs and Scattershots is defender-side advice from search excerpts, not a page I read in full.
- Hidden Tesla and Giant Bomb: spotting by empty gaps is a rule of thumb from 1337wiki and Trophy Coach, not a certainty.
- Could not read: Playnews's August 2026 counter guide (403), Sportskeeda's TH17 page (405), Fandom and Reddit. Mobile Matters has no overview pages at the guessed URLs for Firespitter, Multi-Gear Tower or Giga Bomb (404).
- May go stale soon: TH19 is due mid-November. Base Drop (Aug 2026) reports an interview saying older merged defences will drift towards ordinary ones, with no patch yet, so TH16 merge advice could age first.

Glossary writer's weak lines:

- No source read, standard game knowledge: Tank, Draw fire, Lock on, Push, Carry, Spam, Mass, Dive, Clean-up, Lure, Snipe, Stall, Distraction, Mid-base, Time out, Clan Castle, Housing space, Splash, Single target, Multi-target, Chain, Aura, Melee and ranged, Tile, Minimum range, Spawned troops, Enrage, CWL, Siege Machine, Air mode and ground mode, Spell Tower effects, Inferno, Hogs, Loot, Farming, Trophy pushing, Friendly challenge, Meta.
- Hero walk, Hero charge, Queen Walk, Queen Charge: sources disagree. AllClash (2018) says a walk cleans the edge and a charge goes deep. Trophy Coach (2026) treats them as one technique with different scope. Our text mixes them, such as "Walk the Queen" in a Queen Charge plan (line 543) and "Queen Walk or Charge" (line 386). The pages say so.
- RC Walk, RC Charge: Blueprint uses near identical wording for both, and our RC Charge plans differ on timing. Defined by what our steps do.
- Warden Walk, Warden Charge: Blueprint calls the opening of its Warden Charge army a Warden walk, so the split is ours. Our text uses Warden Walks only once (line 724).
- Duke Charge: one Blueprint page (Mar 2026) plus our own steps. Base Drop does not use the name.
- Hydra: used loosely. Trophy Coach says two-flank air attack, Base Drop says Dragon Riders with Dragons or Electro Dragons, and our armies add Balloons, a Lava Hound or a Blimp. No source explains the name.
- GoHo: no source read spells out the letters. Go and Ho are inferred from AllClash's GoVaHo (2016), and Wizards come from our own plan.
- DragBat: only source is AllClash (2019), and its split between Dragons and Bats differs from ours. The page says both.
- Witch Slap: AllClash (2017) and Blueprint (Apr 2025) both differ from our steps. Defined by ours, with the difference stated.
- Sui: "suicide dive" comes from one Blueprint line (Jun 2025). The rest comes from its TH12 page, which never defines the word. No hits in our text today.
- Zap: no source defines it. Backed by one Blueprint example and how our own steps use it. Blueprint's page also labels some non-zap plans as Zap, so names vary.
- Hybrid: our line 293 ("air and hybrid attacks") means any air and ground mix, not the Queen Charge meaning. The page gives both.
- Snipe: our line 492 uses it for killing an Air Defense with Lightning, wider than the usual meaning. The one-star meaning is general knowledge.
- Core: "ground core" (line 410) means the main ground group, so that phrase goes to Push rather than Core.
- Compartment and Core: Trophy Coach mentions these words without defining them. The definitions are general knowledge.
- Smash: Trophy Coach uses the word for ground pushes but does not define it. Our "smash" is broader in places.
- Root Rider Smash: exact name has no hits. It is reached through "Root Rider Yeti" (line 692).
- Fireball: the level 24 threshold is Trophy Coach alone, and "aim is set by where the Warden stands" is inferred from the game text, which says closest defence.
- Guardian: the Poison, Earthquake and Lightning line is Mobile Matters alone, from launch in Nov 2025, and balance may have changed since. Base Drop's Town Hall 19 claims about Guardians are left out, as the brief asks.
- Supercharge: Mobile Matters says up to three stages, Clash Ninja says three or two for defences, and our data shows two for defences. The page follows the data.
- Super Troop: Base Drop says 15,000 to 25,000 Dark Elixir, and our data has 25,000 for every Super Troop. The page follows the data. "From about TH11" comes from the data and earlier research.
- Gear up: only source read directly is Clash Ninja (2019), though its build times match our data. The "needs level 10 and a Archer Tower" chip in guide_units.json reads wrongly and may be worth fixing.
- Minimum range: the Mortar line is general knowledge. Our data only lists the Eagle Artillery and Scattershot.
- Time out: three minutes is general knowledge, not in our data.

Left out of the glossary, and why:

- walk (bare): catches ordinary walking, as in "walks to the nearest defence". Only named forms such as Queen Walk, RC Walk and hero walk are aliases, plus "the walk" and "Walk the Royal Champion".
- charge (bare): same risk, and "Charge" in "Queen Walk or Charge" (line 386) stays plain as a result. Named forms only.
- side (bare): "the side of the Air Defense" (line 133) is not a flank. Multi-word forms such as "one side" and "opposite side" go to Flank.
- smash, smashes, smashing (bare): plain verbs in lines 66, 86, 251 and 273. Only "ground smash", "smash attacks", "for smash" and named armies are aliases.
- zap (bare): "Its Yetimites zap nearby buildings" (line 245) is an ordinary verb. Only Zap Dragons, Zap Witches, Zap Lalo and the two "Zap ... Air Defense" steps are aliases.
- merge (bare): "Cloned ones cannot merge" (line 103) is about Meteormites re-forming. Writers must use merged, merges or merging for defences, or the word will not highlight.
- boost (bare): also means Rage (line 304), a Rocket Balloon speed-up (line 202) and a Super Valkyrie effect (line 209). Three exact phrases go to Super Troop instead.
- push, pushes (bare): verbs in many lines, and Push is also a chip on the Air Sweeper and Spring Trap. Only "the push", "a push", "main push" and "ground push" are aliases.
- Rage, Freeze, Heal, Jump, Lightning, Poison, Haste, Clone, Totem, Earthquake, Invisibility, Overgrowth: shortened spell names. As aliases they would catch the full spell names, Rage Gem, the Dragon Duke's rage and the Ice Golem's freeze.
- Inferno (bare singular): catches Inferno Tower and Inferno Dragon. Only "Infernos", "an Inferno starts", "Inferno on a hero" and "reset Infernos" are aliases, so "A single-target Inferno" (line 384) highlights only "single-target".
- Blimp (bare): catches Battle Blimp. "Blimp drop" is the only alias.
- hero, King, Queen, Warden, Champion, Prince, Duke (bare): very frequent and they catch full names, including Apprentice Warden, Archer Queen and Dragon Duke. Heroes have their own pages.
- spell, trap, deploy, drop, cast, pet: everyday in the guide, and "spell" would light up inside every full spell name.
- clump, bunch, cluster, swarm, pack, lane, burst, anti-air, forgiving, niche, mid-table: plain English in context. Splash and Spam cover the ideas behind the first few.
- single, multiple, ricochet (damage type chips): "single" is everyday English and "ricochet" would catch Ricochet Cannon. Chain and Single target explain them.
- Wakes up after (Eagle Artillery chip): the chip already says it in plain words, and Housing space mentions it.
- Spell Tower (bare): full official name. The Spell Tower effects page is reached from the list or through units.
- GoWiPe, GoWiWi, Legend League: not used in our guide text, so pruned.
- Blacksmith and ore: covered as aliases of Hero equipment. Dead base: covered as an alias of Farming. RC: covered as an alias of RC Walk.

## Fixes and additions after the 2.10.1 review (6 November 2026)

Findings in `C:\Repos\CocWidget-review-2026-11-05`. The rule for this round: a game rule needs Supercell, two independent sources, or the data, and otherwise the text is worded to hold either way. COC Base Guide and Base Drop pages from before about July 2026 were dropped, as they invent mechanics.

Ben checked in game on 6 November 2026 that a Spell Tower does not cast when destroyed, so the text now says so.

Left unclaimed because only the game can settle them: Ice Block on underground Miners, a sleeping Revenge Tower being destroyed, and whether the Angry Spell adds speed (the data's chip says so, Supercell doesn't).

Rests on thin evidence and worth a player's eye: what to upgrade first at TH3 to TH8, hero order at TH13 to TH16, the tactics for each Guardian and Town Hall weapon, army counts in the eight plans worked out by hand, and TH18 army capacity of 352 (Supercell names Army Camp level 14 but not its size, so it rests on 1337wiki and Blueprint's army links).

The writers' own notes:

### Troops

- Targets: favourite targets come from Supercell's posts where they state one (Root Rider, Apprentice Warden, Thrower, Druid and Bear, Ruin Witch, Meteor Golem, Furnace), otherwise from coc.guide's game file tables plus 1337wiki. Goblins Farm is a third check for many.
- coc.guide has no page for Ruin Witch, Furnace or Meteor Golem, and shows no favourite target for Headhunter. Headhunter rests on guide_units.json (pt Heroes) and 1337wiki. Goblins Farm also says she hunts Clan Castle troops, which 1337wiki denies, so targets says heroes only.
- Healer targets is worded as who she heals, since she never attacks. Furnace targets describes its Firemites, since it does not move.
- Archer opens at TH2 in guide_units.json, while 1337wiki lists her at Barracks level 2 under TH1. Needs an in-game check. No line depends on it.
- Ruin Witch's DPS chart (25 to 28) shows damage Supercell says she does not deal. That needs fixing on the chart side, not in this text.
- The reader finding on Yeti funnel versus wide line was left alone. The spam plan and the smash line describe different armies.
- Ruin Witch's October figure moved out of role into a TH18 line, because role cannot carry a from.
- New source keys are prefixed TR to avoid clashes when merging with other drafts.
- U is kept only for Healer, Thrower and Meteor Golem. It is dated 29 Jul 2026, not Oct 2026 as guide_content.json says, so it predates the September Thrower nerf.

### Super Troops and Siege Machines

- Favourite targets come from game data for the machines, and from CoC Guide's copy of the game files for Super Troops, because guide_units.json has no pt for them. Super Yeti has no page there. Its target rests on the plain Yeti having none and guide_units.json listing none.
- DATA BUG: guide_units.json opens 13 of the 17 Super Troops a Town Hall too early. Its base.l is one below the real required level for all 17. Supercell's own post says Super Hog Rider needs Hog Rider level 10 and Town Hall 13, where the data has level 9 and h 12. Using base.l + 1 gives TH11 for Super Barbarian, Super Archer, Super Wall Breaker and Sneaky Goblin (Sauna floor), TH13 for Super Miner, Super Hog Rider and Super Yeti, and TH12 for the other ten, matching 1337wiki's table exactly. from values here follow h as briefed and stay correct after a fix, though some become redundant.
- Clan Castle donations reach below the build Town Hall, per 1337wiki and Theria: Wall Wrecker, Blimp and Slammer from TH10, Barracks and Log Launcher from TH11, Flame Flinger from TH12, Drill from TH13, Launcher from TH14, Wagon from TH15. from values here use the build Town Hall, as briefed. Supercell's May table lists Sky Wagon levels 1 and 2 under TH16, and I could not explain that column.
- Battle Drill has an empty with list because no source names a troop that pairs with it, only heroes.
- Source keys here all start with S to avoid clashes with other drafts. R and O are removed from every entry in this area. U is removed from Siege Barracks along with the Thrower in its with list.

### Spells, heroes, pets and equipment

- Schema: `picks` is keyed 'Hero | Pet' with hero, pet, list ('picks' or 'avoid') and why, because the same pet needs different text per hero. Only changed picks are listed: the old text names sites, repeats the Phoenix or Lizard error, or gives the wrong Spirit Fox range.
- Schema addition: `heroLists` holds a replacement `split` line for every hero (the old ones name sites) and replacement `gear` for the Minion Prince (Dark Crown in, Portal Pendant out) and Royal Champion (Haste Vial in). The Warden, Queen, King and Duke gear lists are unchanged.
- Dragon Duke is not in guide_units.json, so he cannot go in the Angry Spell's `with`. He is named in a use line instead.
- Equipment `from` comes from coc_data.json `h` level 1. Barbarian Puppet and Rage Vial list TH8 there, but the King starts with them at TH4, so they have no `from`. All his other pieces have one, as the Blacksmith opens at TH8.
- Equipment pieces with thin backing, given modest tips: Revenge Deck (read only on 1337wiki, and the tip stays with what the card does). Portal Pendant (Supercell describes it and gives no per-level stats, and 'niche' is guide opinion only). Noble Iron and Lavaloon Puppet ('low priority' and 'rarely best' are guide rankings). Frost Flake (guides disagree: one rates it essential, one situational, so the tip gives no rank). Metal Pants (the Seeking Air Mine advice is 1337wiki only).
- Trophy Coach (H, TD) could not be read directly as it returned 429. Those citations carry over from the review, read through the summarising fetch.
- Fire Heart: current guides still call it near-mandatory, but none postdates the October cut and Supercell says it wants his other pieces level with it. The text says test it rather than naming a replacement, since nothing dated after 17 October compares them.
- Angry Spell: the data still carries a chip 'Speed boost=35'. Supercell calls it a pure targeting change. The text makes no claim on speed, and 'even after leaving the circle' is left out because only a low quality site says it.
- Ice Block: wording avoids Miners entirely, as asked. The Skeleton and Bat Spell exception, and that it catches heroes, are left out as single source.
- Bat and Skeleton: reduced damage to the Town Hall is on 1337wiki, read raw. Second sources appeared only as search summaries and those pages refused direct reads, so the text says do not count on them to finish a Town Hall, which holds either way.
- Lightning: 1337wiki says it does not hit the Town Hall or Clan Castle. House of Clashers (older) says the Town Hall takes damage. Both agree on storages, so only storages are stated. The single-bolt Inferno retarget is on both.
- Rage: 'heroes half, Siege Machines none' is read raw on 1337wiki and appeared in a search summary from a second wiki. No stacking is backed raw by two. Worth a glance if you want it stricter.
- Overgrowth and Electro Dragons: read raw on 1337wiki, with a second page seen only as a search summary. Worded as 'gain least'.
- Left out as single source: Earthquake Boots counting toward the Earthquake penalty, Haste and Rage keeping the larger speed (worded to hold either way), Clone on pets and Siege Machines (only heroes stated, confirmed by two), Revive not returning the pet, Invisibility splash, trap and Eagle rules, Wall Wrecker and Log Launcher sticking on Overgrowth, Skeleton Spell wall tile rule, the Duke's 6-tile rage radius. The Clone 'deployed first is copied first' rule is single source, so the line just says send that troop in first.
- Clone 'Used in Hydra' line removed: the reviewer found no backing, though our TH15 plan uses it, so whoever owns the plans may want to check.
- Minion Prince: the '4 to 5 tiles' drop distance rested only on a distrusted page, and data and 1337wiki disagree on his range. Reworded without a number.
- Source removals: COC Base Guide (O) and Base Drop May 2026 (R) gone from every entry. Sportskeeda (Q) gone from Revive. U kept only on Freeze, Invisibility and Overgrowth, where the page is dated 29 July. H is not cited on the Duke, as it misses the October Fire Heart cut.
- Eagle Artillery: Rage dropped the Eagle mention and Freeze has from 11 to 16. Earthquake keeps it alongside Infernos with from 10, so that line still shows at TH17 and up, where the Eagle has merged but Infernos remain.

### Defences, traps and Town Hall threats

- Threats: Giga Inferno is one entry for TH13 to TH16, with its TH13 and TH14 up differences as lines using to and from. Town Hall 18 is an extra weapon-kind entry that carries the no-weapon rules and the star plan, since no Guardian entry is the right home for them.
- Revenge Tower: Supercell counts the sleeping state as Stage 1, so its Stage 3 at 25 buildings and Stage 4 at 50 are the Stage 2 and Stage 3 on the app chart. The text uses fallen buildings only (5, 25, 50). Those three numbers stay because no chart shows them. Consider relabelling the chart stages the same way.
- Revenge Tower: the text makes no claim on whether a sleeping tower can be destroyed. Aim to kill it before 25 holds either way.
- Spell Tower: Supercell lists level 4 under TH18 content in February 2026, but the game data has it at TH17. The Earthquake line follows the data, from 17. Worth one look in the game.
- Spell Tower: settled later by Ben in game. A destroyed tower does not cast.
- Giga Bomb: the data label says a troop of 18 housing, one wiki says 18 housing in total inside the radius, and Supercell gives neither. The lines hold either way: a few small troops do not set it off, and one Yeti does. Miner and Battle Drill are out of with, because burrowing past it rests on one wiki.
- Thin evidence, how to beat only. Logger: his log and push rules are Supercell, but coming from two sides and placing a Totem to one side are my own reasoning from those rules, and no guide states them. Smasher and Longshot Totem lines rest on one Blueprint guide and one wiki. The Town Hall 18 star plan rests on the targeting rule plus one Blueprint guide, and the three troops named come from long-standing targeting, not from a Town Hall 18 source. Stars count buildings comes from the existing glossary, not from a post about Guardians.
- Game rules with less than Supercell behind them. Weapon wake-up at damage or 51%: Clash Ninja, Theria Games and one wiki, as Supercell only says scratching the Town Hall sets it off. Inferno Artillery counting as a defence for defence-targeting troops: one wiki, plus Clash Ninja and Supercell saying the TH18 Town Hall stopped being one. Eagle weights: one wiki, a 2016 guide for spells only, and a search summary I could not open at source. Seeking Air Mine 5 housing and the Tornado pull by size: one wiki plus a Fandom search summary. Giga Tesla Freeze not stopping the blast: Clash Ninja, from before weapon levels were removed.
- Left out because only one source says it: Firespitter cone of about 150 degrees (now just broad), Super Wizard chain radius of 4 tiles (now well away), Spring Trap half damage on Siege Machines (Supercell says heroes and pets), Tornado holding troops on a wall, Longshot jumping down by herself if the Town Hall falls first, Inferno Artillery arming for a few seconds, and Longshot death blast size and Smasher rage lasting to the end.
- Dropped from existing lines: Builder decoy tip. X-Bow Tactical Overview, as nobody could confirm its current name. Log Launcher setting off Invisibility, and Log Launcher in with, because the cited Blueprint Backpack Throwers guide does not say it. Lit fuse on Giant Bomb, since Supercell removed the trap trigger delay in October 2025. Spell Tower Invisibility trigger rule, now worded so it holds either way.
- Sources removed: DF (Base Drop, April) from Monolith and Scattershot. DM (Base Drop, merged defences) from Multi-Archer Tower and Ricochet Cannon, as the review found it out of date. No entry here rested on COC Base Guide.
- Findings not taken, as the brief hands merged building counts to the data work: Cannon, Archer Tower, Wizard Tower, Multi-Archer Tower, Ricochet Cannon, Multi-Gear Tower and Super Wizard Tower counts, plus one geared Cannon per base.
- For the screens. Town Hall weapon data in static_data.json, per target per second: TH12 140, TH13 220, TH14 280, TH15 300, TH16 300, reach 10 tiles. Inferno Artillery: levels 40 to 60, reach 12 tiles, every 3.5 seconds. Guardians, levels 1 to 5: Smasher 12000 to 16000 health, 500 to 700 damage, reach 1.25. Longshot 7000 to 11000, 330 to 450, reach 11. Logger 8000 to 12000, 230 to 350, reach 7, and Supercell gives 8 at level 5, which the data does not show.
- Alert ranges, not in player text as they move: Smasher 14 and Longshot 19 tiles from one wiki, which fit the percentages in the 12 January 2026 post. Logger: only a pre-release 10 tile lock-on from Blueprint. Nothing later confirmed it, so no line ranks his wake-up against the other two.
- Could not read: Trophy Coach rate-limited this machine, so the Air Defense equipment line and Air Sweeper Freeze rest on the review's reading of the 22 October 2026 page. Supercell's May 2026 post does confirm Giant Arrow hits Air Defenses twice as hard. Fandom was not read, as told.
- The from values for geared modes come from the data: Cannon level 7 at TH6, Archer Tower level 10 at TH8, Mortar level 8 at TH10. They also need the matching Builder Base building, which the data does not tie to a Town Hall.

### Attack plans and army counts

- CAPACITIES. Army: 70, 80, 135, 150, 200, 200, 220, 240, 260, 280, 300, 300, 320, 320, 340, 352 for TH3 to TH18. Spells: 0, 0, 2, 4, 6, 7, 9, then 11 from TH10. Cross-checks: 1337wiki's Army Camp and Spell Factory tables. Camp and factory level gates match coc_data.json. Skycoach's armies add up to these totals, within 4 housing, from TH3 to TH17. Blueprint's in-game army links decode to these exact totals at TH9 to TH17.
- TH18 IS 352, NOT 340. Army Camp level 14 came with Supercell's April 2026 update, which names the level but not its capacity. 352 comes from 1337wiki (88 per camp), and Blueprint's TH18 Ruin Witch and Hydra army links both decode to exactly 352. Skycoach's TH18 army still totals 340, so it ignores level 14. Trophy Coach's TH18 Hydra is 350 and its TH17 version 340, which fits.
- HERO SLOTS. One hero to TH8, two at TH9 and TH10, three at TH11 and TH12, four from TH13, per 1337wiki's Town Hall pages and consistent with Skycoach's and Blueprint's armies. The checker enforces this. Hence the new TH8 summary line about one hero. It rests on one written source plus consistent armies.
- COUNTS COPIED. Skycoach: TH3 to TH6, TH8 GoHo, TH9 Witch Slap, TH11 Zap Witches, TH13 Yeti and Super Wizard, TH14 Mass Dragons with Blimp, TH15 Hydra, TH16 Root Riders and Valkyries. Decoded from Blueprint's in-game army links in the page HTML: TH9 Zap Dragons and Zap Lalo, TH10 all three, TH11 Electro Dragons and Queen Charge, TH12 Zap Dragons, TH13 Yeti Super Archer, TH14 Super Dragons and RC Walk Dragons, TH15 Root Rider and Warden Charge, TH16 Fireball, RC Walk Root Riders and RC Walk Dragons, TH17 Fireball, RC Walk Dragons and RC Walk Root Riders, TH18 Ruin Witch. 1337wiki's written armies, which also decode from its links: TH12 Queen Charge, TH18 Hydra, Rocket Backpack Throwers and Super Bowlers. Trophy Coach's named counts: TH17 Hydra Air, read through WebFetch only as curl gets a 429 there, and they add up to 340.
- COUNTS I WORKED OUT, not copied. TH7 Mass Dragons: 9 Dragons and 4 Balloons, from 1337wiki's 'a few Balloons in place of one Dragon', with Skycoach's 6 Lightning. TH7 Mass Hog Riders: no guide gives numbers, built from 1337wiki's Hogs, a few Wizards and Healing. TH8 Mass Dragons: same troops as TH7, with spells from my arithmetic below. TH8 Golem and Valkyrie: built from 1337wiki's description (one or two Golems, Wall Breakers, Valkyries, Wizards, Poison, Healing). TH12 Yeti Smash: no guide gives numbers. TH12 DragBat: Skycoach's army with its 1 Overgrowth swapped for 1 Rage, as in Blueprint's TH14 DragBat link, since no source gives Overgrowth a job there. TH13 Super Archer Clone Hydra: Blueprint's link has no Lava Hound or Rage, though its own steps use both, so I added them and trimmed Balloons, a Baby Dragon, Archers, a Headhunter and Invisibility to fit. TH18 Super Yeti with RC Charge and TH18 Root Rider Yeti: built on Blueprint's TH16 and TH17 Super Yeti shapes, scaled to 352.
- SPELL ARITHMETIC, from guide_units.json. TH7: top Lightning 240, top Air Defense 1,000 health. Five Lightning are needed, leaving 1 slot, and Rage takes 2. So Lightning and Rage don't fit together, and Rage is gone from TH7. TH8: Earthquake level 2 takes 17% of 1,050, then 4 Lightning at 270 finish it, for 5 slots, leaving 2 for Rage. So both fit at TH8 at top level. Hence the 'count before you cast' line, as lower levels can fall short. I assumed Earthquake then Lightning adds up simply. TH5: 2 Lightning can't kill even a level 1 Air Defense, which is what the TH5 line says.
- ONE GUIDE'S STEP ORDER, kept because nothing else exists. Skycoach: TH8 GoHo, TH12 DragBat, TH13 Yeti and Super Wizard, TH14 Mass Dragons with Blimp, TH15 Hydra, TH16 Root Riders and Valkyries. 1337wiki: TH7 Mass Hog Riders, TH8 Golem and Valkyrie, TH18 Rocket Backpack Throwers (Blueprint's Ruin Witch and Ninj guides agree on its opening and funnel). Trophy Coach: TH12 Yeti Smash, TH17 Hydra Air, TH18 Super Yeti with RC Charge, TH18 Root Rider Yeti, all thin. Blueprint: TH18 Ruin Witch. Most TH9 to TH17 plans rest on Blueprint and are echoed by 1337wiki, but the wiki's armies match Blueprint's links so often that I don't count them as independent.
- PLANS. Added TH18 Duke Charge Super Bowlers: 1337wiki gives a full army, and EZ Arena and Trophy Coach both name Super Bowler armies at TH18. No plan removed. TH4 Balloons and Archers stays, though 1337wiki prefers Giants and Balloons there, as Skycoach has the counts.
- LEVEL CHANGES. Fireball Super Yeti at TH16 and TH17 go easy to medium: it needs a Warden walk, a high-level Fireball and spell locking, and the 'most forgiving' quote belonged to another plan. TH18 Ruin Witch goes medium to hard: the real army is Rocket Backpack Throwers plus Witches. TH18 Super Yeti with RC Charge stays easy, but Trophy Coach's own rating is beginner to intermediate, so it could fairly be medium.
- TH18 FIXES. Ruin Witch now has Throwers, Healers and four heroes, from Blueprint's link. Super Yeti, Rocket Backpack Throwers and Duke Charge Hydra steps reordered. Every TH18 plan now gives a job on the Guardian. 'Freeze the Guardian' rests on Supercell's April notes (Guardians no longer activate while Frozen). 'Totem the Guardian' rests on 1337wiki, with Blueprint's Totem advice alongside. Poison on Guardians isn't advised, as Supercell cut it to 30%. Summary and abouts now say the armies were cut after the figures. The Thrower cut rests on Clash Ninja's log, 1337wiki and the game data's speed of 16, as Supercell hasn't posted it.
- CLAN CASTLE. Super Archers (TH13 Hydra), Earthquakes (TH18 Throwers, Ruin Witch), Revive (TH18 Hydra, Bowlers), Totems (TH16 RC Walk Root Riders) and Jump (TH17 Fireball) come from the Clan Castle, so they are named in steps but aren't in army or counts. The step says so each time.
- SOURCES REMOVED. Base Drop's May TH18 page (D) is gone from Root Rider Yeti, as it predates July and misassigns gear (Giant Gauntlet to the Queen). That also drops the Giants and Witch funnel and the ring base warning, which rested only on it. The Monolith advice now rests on Blueprint and 1337wiki (Skeleton on it) in place of 'remove it early'. Website names are out of all player text.
- SIEGE. One machine per plan, as briefed. Where guides list two, I kept the one the steps use: TH12 DragBat Battle Blimp, TH16 and TH18 Ruin Witch Log Launcher, TH18 Bowlers Sky Wagon. TH14 RC Walk Dragons keeps a Siege Barracks from its steps, though the link has it in the Clan Castle.
- SUPER TROOPS. No plan uses more than two types, and the checker enforces it. I did not find a Supercell statement of the limit. It is as briefed.
- PETS. Copied from army links where given, but I swapped a few that clash with the app's own advice. TH18 Bowlers: 1337wiki has Queen with Sneezy and Warden with Greedy Raven, changed to Unicorn and Sneezy as in Blueprint's TH17 version of that army. TH17 RC Walk Root Riders: Blueprint has Queen with Sneezy and Warden with Diggy, changed to Unicorn and Sneezy. Blueprint and 1337wiki put Sneezy on a ground Warden in Thrower armies (no air flag in the link), which conflicts with the Grand Warden entry's 'Clash Ninja says air mode only'. That line belongs to another area and is worth rechecking.
- QUEEN GEAR. Frozen Arrow stays, beside Giant Arrow as the second piece, per Trophy Coach's Queen Charge guide. That guide names no fallback. Healer Puppet replaces Giant Arrow at TH12, as it opens there.
- LOW TOWN HALL KING. Named in TH4 to TH6 steps from Supercell's May post. I left out his ability and equipment there, as 1337wiki alone describes them at those levels.
- NOT CHECKED. Trophy Coach pages came through WebFetch only. No army has been played in game. Blueprint's best-armies page (B) shows a March 2026 byline, not the October date in sources. Its TH9 and TH10 links are from April 2025, so those counts are the oldest here, though they still total correctly. Blueprint's own TH18 strategies page holds no decodable armies.

### Glossary

- Inferno became Inferno modes, in the game group, so one page settles single and multi-target. Its old aliases moved with it, and Multi-target mode moved here from Multi-target. No other page should still name Inferno in a see list.
- Whether Ranked gives loot: Supercell's April 2026 notes list "Ranked Battle Loot: Slightly decreased", so the text says Ranked pays some loot. It gives no amount.
- Decoy aliases: triple air mix on Hydra catches the Hydra (hard) line "A triple air mix", and Triple Strike on Hero ability catches the Super Bowler ability chip, so neither links to Three-star. If that plan line is reworded, drop the decoy.
- Tile no longer has bare tile or tiles, which linked about 75 stat chips and data lines. It now links prose forms such as tiles ahead and a few tiles. Other writers using "within 8 tiles" get no link, which is fine.
- Carry now links only on noun forms. The Barbarian line "carry an attack" and the Super Yeti line "carry the core" lose their link, as they are verbs.
- Drags is matched without case, so a plain verb such as "the Tornado Trap drags troops" would link to it. No such line exists in all-prose.md today. The merger should check the other drafts for "drags".
- Bare Eagle, Sweeper and Scatter are not aliases: Eagle and Sweeper would mark part of a unit name, and scatter is a plain verb. They are on the Short names page only.
- Bare mirror is not an alias, as it would mark Magic Mirror. Only your mirror, the mirror, a mirror and mirror attack link.
- Guardian and Town Hall weapon name the threat pages in words only. The units list is limited to guide_units.json for now. Once threats merge, add Smasher, Longshot and Logger to Guardian's units, and Giga Tesla, Giga Inferno and Inferno Artillery to Town Hall weapon's, as GlossaryTest allows threat names there.
- Smasher, Longshot, Logger, Giga Tesla, Giga Inferno and Inferno Artillery are aliases, and will also be threat page names. GlossaryTest only bars unit names, so this passes, but a threat page naming itself in its own text will link to the glossary.
- Bare zap and zaps are not aliases: they caught the Hidden Tesla line "zaps one target" and the Super Yeti line "Yetimites zap nearby buildings".
- Knights stays as an alias. All four hits in all-prose.md are on Ruin Witch and Angry Spell lines and mean Ruin Knights.
- Super Troop no longer cites Base Drop. Its facts come from guide_units.json: 25,000 Dark Elixir and 259,200 seconds on all 17, and every super takes more housing than its base troop.
- Root Rider Smash no longer cites the May Base Drop page, and Hydra no longer cites the July one. Their lines rest on Trophy Coach, Blueprint and our plans.
- Line left to other writers: the Eagle Artillery text says it merges at TH17, and TH18 has no Eagle. Town Hall weapon now says so. The P.E.K.K.A line "Multi-target Infernos split its focus" contradicts Inferno modes and needs its writer's fix.
- Pathing's example changed because the Overgrowth entry lists rooting the building you are hitting as a mistake.
- Crafted Defense is short and names this season's three. Re-check it when the phase ends, around the end of 2026.
- New source keys start with G, to avoid clashing with other drafts.

### What to upgrade first

- Thin guidance at TH3 to TH8. Only 1337wiki gives a per-Town Hall order there, and it looks partly machine generated. Trophy Coach gives broad stages, Clash Ninja gives a checklist, and I found no Base Drop page below TH9 and no Blueprint page below TH11. Every unlock and research line was checked against the game data, but the order at these levels is close to one source's opinion.
- Where guides agree: Laboratory first or nearly first at every Town Hall from TH9 up, offence before defence, walls as overflow, one hero at a time, and research only the army you use. 1337wiki puts new buildings ahead of the Lab up to TH12. I kept the Lab first, as Blueprint and Base Drop do, and placed new buildings as the early follow-up.
- Hero order is the main split. Trophy Coach says Queen and Warden first from TH11 to TH18, with the Royal Champion third. Blueprint says Royal Champion first from TH13 to TH16, because her timers grow quickly and suit Books of Heroes. Common ground used: take her cheap early levels at TH13, then Queen and Warden. At TH16 the Warden and Champion lead because both guides rank them highly and the easy plan uses them, which is my judgement call rather than a stated consensus.
- Some Blueprint pages offer skipping hero upgrades in favour of Hero Potions to rush. Left out, as it suits experienced players and conflicts with Trophy Coach and Base Drop.
- Army Camps have no new level at TH7, TH8, TH14 or TH16 in the game data, though TH7 adds a camp according to 1337wiki. Base Drop's TH14 page and Blueprint's TH16 page both recommend them at those levels. 1337wiki's TH14 and TH16 lists omit them, which supports the data. The TH14 and TH16 lines say so directly.
- Base Drop's TH13, TH15 and TH16 pages date from April and May 2026 and invent details, such as a Multi-Gear Tower at TH15 and the Giga Inferno still levelling. Not used. These three Town Halls rest on Blueprint, 1337wiki and Trophy Coach.
- Blueprint's TH14 page (June 2026) still lists a Town Hall weapon upgrade, which Supercell removed in October 2025. Clash Ninja's page stops at TH15 and still requires Giga levels. I used both only for points that hold today. Blueprint's TH17 (Nov 2024) and TH18 (Nov 2025) guides are too old to cite.
- Base Drop says a Guardian being upgraded leaves the base without it on war day. Supercell's TH18 notes say the lower level version still defends in Ranked, War and CWL. So no line advises timing Guardian upgrades around wars.
- Left out, as only 1337wiki states them firmly: the Hero Hall must be maxed before the next Town Hall, all merges are required before TH17 and TH18, and the Inferno Artillery must reach level 5 before TH18. Base Drop cites the wiki for the Hero Hall rule, and low-quality sites repeat the others. Supercell only confirms that merged buildings must be max level and that hero caps follow the Hero Hall. Worth checking in the game, as these would be strong reasons to prioritise if true.
- Super Troops taking their level from the base troop rests on the game data (matching level tables and a base field) plus general guide wording, not a Supercell line I could find. The 'Lab Assistant is free' line rests on Supercell's TH17 post from Nov 2024.
- Trophy Coach blocks direct fetches, so its pages were read only through the summarising fetch tool, not as raw text. Its hero order page was read once, and a search excerpt agrees with it. Its general priority page names the Queen and King as the main carries, which differs from the Queen and Warden on the hero page. I followed the hero page. Exact wording is unverified.
- Also left out: wall advice (same at every Town Hall, and Supercell's August 2026 update means Wall Rings no longer need a free builder, so it may suit a general line), magic item advice, saving hammers for TH19, Crafting Station modules (seasonal), and ore spending beyond a mention. Merges need maxed Cannons and Archer Towers, which a TH15 player would want to know before leaving, so a 'before you move up' section could carry that.
- TH16 to TH18 lines may go stale when Town Hall 19 lands, expected mid-November 2026. No line makes a claim about it.


## Sources

Official:
- [Clash of Titans season, 1 Nov 2026](https://supercell.com/en/games/clashofclans/blog/news/clash-of-titans-is-hot-off-the-press/)
- [Balance Changes, 17 Oct 2026](https://supercell.com/en/games/clashofclans/blog/news/balance-changes-incoming-october-2026/)
- [July Balance Update, 9 Jul 2026](https://supercell.com/en/games/clashofclans/blog/news/july-balance-update/)
- [Fan Content Policy](https://supercell.com/en/fan-content-policy/)

Game data:
- [ClashKing static data](https://assets.clashk.ing/static_data.json)
- [Clash Ninja max levels](https://www.clash.ninja/guides/max-levels-for-each-th)

Strategies:
- [Blueprint CoC, TH9 to TH17](https://blueprintcoc.com/blogs/coc-attack-strategies/best-clash-of-clans-army) (date unclear, 2026)
- [Blueprint CoC, TH18](https://blueprintcoc.com/blogs/town-hall-18/best-th18-attack-strategies) (Oct 2026)
- [Trophy Coach](https://trophycoach.com/clash-of-clans/guides/war-attack-strategies-2026) (Oct 2026)
- [Skycoach](https://skycoach.gg/blog/clash-of-clans/articles/best-attack-strategies-for-every-town-hall) (Jul 2026)
- [EZ Arena](https://www.ezarena.com/blog/best-attack-strategies-for-every-town-hall-2026/) (Mar 2026)
- [1337wiki overview](https://clash-of-clans.1337wiki.com/attack-strategies/) and [TH18](https://clash-of-clans.1337wiki.com/th18-attack-strategy/) (Oct 2026)
- [Base Drop TH18](https://cocbasedrop.com/blog/best-th18-attack-strategy-2026-war-cwl) (May 2026)

Pets:
- [AllClash](https://www.allclash.com/best-hero-pet-combinations-in-clash-of-clans/) (Mar 2026)
- [Base Drop](https://cocbasedrop.com/blog/best-pets-every-hero-coc-2026-tier-list) (Oct 2026)
- [GameRant](https://gamerant.com/clash-of-clans-best-hero-pet-combos-tier-list/) (Feb 2026)
- [Clash Ninja](https://www.clash.ninja/guides/which-pet-should-you-pair-with-your-heroes) (Apr 2026)

Town Hall 19:
- [Base Drop, what's confirmed](https://cocbasedrop.com/blog/town-hall-19-what-supercell-confirmed-2026) (Aug 2026)
- [Base Drop, second researcher](https://cocbasedrop.com/blog/laboratory-second-researcher-pets-coc-2026) (Sep 2026)
- [Gaming on Phone, November events](https://gamingonphone.com/news/clash-of-clans-november-2026-list-of-weekly-events-challenges-and-rewards)
