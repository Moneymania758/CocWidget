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
