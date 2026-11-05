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

Data flags that look incomplete and are not fixed: Skeleton Trap is flagged ground only, though it can be set to air. X-Bow is flagged ground only, with air as a listed mode. Builder's Hut has no air or ground flag. Those pages' "Hits" chips will read accordingly.

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
