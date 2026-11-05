// Rebuilds app/src/main/assets/coc_data.json from ClashKing's static game data: per id, the name,
// kind and village, per-level upgrade seconds and costs and the Town Hall each level needs, plus
// what the time-to-max planner uses (its group, merges, and what each Town Hall level unlocks).
// Run it after a game update adds new buildings, troops or levels.
//
//   node tools/build-coc-data.js                     downloads https://assets.clashk.ing/static_data.json
//   node tools/build-coc-data.js path/to/static.json uses a local copy

const fs = require('fs');
const path = require('path');

const SOURCE_URL = 'https://assets.clashk.ing/static_data.json';
const OUTPUT = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'coc_data.json');
// The guide's game facts live apart, so the widget doesn't parse text it never shows.
const GUIDE_OUTPUT = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'guide_facts.json');
const KINDS = {
  buildings: 'building',
  traps: 'trap',
  troops: 'troop',
  spells: 'spell',
  heroes: 'hero',
  pets: 'pet',
  guardians: 'guardian',
  equipment: 'equipment',
};

// Planner groups for home village items. Anything without one (the Town Hall itself, the Crafting
// Station, decorations) isn't planned.
const BUILDING_GROUPS = {
  Defense: 'defences',
  Worker: 'defences',
  Army: 'army',
  Resource: 'resources',
  Worker2: 'resources',
  Helper: 'resources',
  Wall: 'walls',
};
// Levels ClashKing lists none for, from the Clash of Clans wiki. Reinforcement Camps can't be
// upgraded: level 1 is building the second camp (the first comes with Builder Hall 6).
const MISSING_LEVELS = {
  1000049: { t: [864000], c: [4000000], h: [9] },
};
// Supercharges ClashKing lists that the game doesn't appear to offer. Plain Wizard Towers aren't on
// Clash Ninja's list, and TH18 exports with everything else charged show none on them.
const NO_SUPERCHARGE = new Set([1000011]);
const CRAFTING_STATION = 1000097;
const TOWN_HALL = 1000001;
const OTHER_GROUPS = { traps: 'traps', heroes: 'heroes', pets: 'pets', guardians: 'guardians', equipment: 'equipment' };
// Hero equipment is paid for in ore, several kinds per level.
const ORES = { shiny_ore: 'Shiny Ore', glowy_ore: 'Glowy Ore', starry_ore: 'Starry Ore' };
// Super, seasonal and event troops never appear in an export, so they need no group to be skipped.
const LAB_CATEGORIES = new Set(['troops', 'spells']);
// Levels that also need a building upgraded far enough: research needs the Laboratory, pets the
// Pet House and heroes the Hero Hall.
const REQUIRED_BUILDINGS = {
  required_lab_level: 1000007,
  required_pet_house_level: 1000068,
  required_hero_tavern_level: 1000071,
  required_blacksmith_level: 1000070,
};

// Super Troops open at this Town Hall, whatever their base troop's level allows.
const SUPER_TROOP_TOWN_HALL = 11;

// Some game text holds a literal backslash and n where a line break was meant.
const cleanInfo = text => (text || '').replace(/\\n/g, ' ').replace(/\s+/g, ' ').trim();

// A hero or pet: how it moves and hits, and its stats at its top level.
function fighterFacts(entry) {
  const top = entry.levels[entry.levels.length - 1];
  return {
    id: entry._id,
    n: entry.name,
    f: entry.is_flying ? 1 : 0,
    a: entry.is_air_targeting ? 1 : 0,
    g: entry.is_ground_targeting ? 1 : 0,
    r: entry.attack_range,
    hp: top.hitpoints,
    d: top.dps,
  };
}

// The Town Hall each home village troop, siege machine and spell opens at. It comes from the level
// of the building that makes it, as level 1's required_townhall doesn't say (Super Yeti has 3).
// Seasonal and event units are left out, as nobody can plan around them.
function armyFacts(data) {
  const townHallOf = (buildingName, level) => {
    const building = data.buildings.find(b => b.name === buildingName && (b.village || 'home') === 'home');
    return building?.levels.find(l => l.level === level)?.required_townhall;
  };
  const opens = troop => townHallOf(troop.production_building, troop.production_building_level);
  const out = [];
  for (const troop of data.troops) {
    if (troop.village !== 'home' || troop.is_seasonal) continue;
    let th = opens(troop);
    let kind = troop.production_building === 'Workshop' ? 'siege' : 'troop';
    if (troop.super_troop) {
      // It also needs its base troop researched far enough.
      const base = data.troops.find(t => t._id === troop.super_troop.original_id);
      const level = base.levels.find(l => l.level === troop.super_troop.original_min_level);
      th = Math.max(SUPER_TROOP_TOWN_HALL, opens(base), level.required_townhall);
      kind = 'super';
    }
    out.push({ id: troop._id, n: troop.name, k: kind, h: th });
  }
  for (const spell of data.spells) {
    if (spell.is_seasonal) continue;
    out.push({ id: spell._id, n: spell.name, k: 'spell', h: opens(spell) });
  }
  return out;
}

function guideFacts(data) {
  return {
    heroes: data.heroes.filter(hero => hero.village === 'home').map(fighterFacts),
    pets: data.pets.map(fighterFacts),
    equipment: data.equipment.map(entry => ({ id: entry._id, n: entry.name, i: cleanInfo(entry.info) })),
    army: armyFacts(data),
  };
}

function groupFor(category, entry) {
  if ((entry.village || 'home') !== 'home') return '';
  if (category === 'buildings') return entry._id === CRAFTING_STATION ? '' : BUILDING_GROUPS[entry.type] || '';
  if (LAB_CATEGORIES.has(category)) return 'lab';
  return OTHER_GROUPS[category] || '';
}

function itemFor(entry, kind, group = '') {
  // t[n - 1] and c[n - 1] are the time and cost to upgrade to level n. Buildings and traps
  // store those as build_time and build_cost on level n. Everything else stores upgrade_time
  // and upgrade_cost on level n - 1 (to leave that level). h[n - 1] is the Town Hall that
  // level n needs, stored on level n for both. 99 marks a level no Town Hall unlocks.
  const times = [];
  const costs = [];
  const townHalls = [];
  // b is the building a level also needs, and q[n - 1] its level for level n, stored on level n.
  const needs = [];
  // Equipment only: ores[n - 1] is the ore to reach level n, stored on level n - 1 like troops.
  const ores = [];
  let building;
  let merge;
  let supercharge;
  for (const level of entry.levels || []) {
    if (typeof level.upgrade_cost === 'object') ores[level.level] = level.upgrade_cost;
    // Extra stages on a building's final level, open only at the highest Town Hall.
    if (level.supercharge) {
      const stages = level.supercharge.levels;
      supercharge = {
        l: level.level,
        r: level.supercharge.upgrade_resource,
        t: stages.map(stage => stage.build_time),
        c: stages.map(stage => stage.build_cost),
      };
    }
    for (const [key, id] of Object.entries(REQUIRED_BUILDINGS)) {
      if (key in level) {
        building = id;
        needs[level.level - 1] = level[key];
      }
    }
    if ('build_time' in level) {
      times[level.level - 1] = level.build_time;
      costs[level.level - 1] = level.build_cost;
    } else if ('upgrade_time' in level && typeof level.upgrade_cost !== 'object') {
      times[level.level] = level.upgrade_time;
      costs[level.level] = level.upgrade_cost;
    }
    townHalls[level.level - 1] = level.required_townhall ?? 99;
    if (level.merge_requirement) {
      merge = { l: level.level, s: level.merge_requirement.map(r => [r._id, r.level]) };
    }
  }
  const item = {
    n: entry.name,
    k: kind,
    v: entry.village || '',
    t: Array.from(times, t => t ?? 0),
    c: Array.from(costs, c => c ?? 0),
    h: Array.from(townHalls, h => h ?? 99),
    r: entry.upgrade_resource || '',
  };
  const alt = (entry.levels || []).find(level => level.alt_upgrade_resource);
  if (alt) item.a = alt.alt_upgrade_resource;
  if (group) item.g = group;
  if (merge) item.m = merge;
  if (building) {
    item.b = building;
    item.q = Array.from(needs, q => q ?? 0);
  }
  if (supercharge && !NO_SUPERCHARGE.has(entry._id)) item.s = supercharge;
  // c holds the first ore, x the others: { "Glowy Ore": [...], "Starry Ore": [...] }.
  if (ores.length) {
    const [first, ...rest] = Object.keys(ORES);
    item.r = ORES[first];
    item.c = Array.from(ores, cost => cost?.[first] ?? 0);
    for (const key of rest) {
      const costs = Array.from(ores, cost => cost?.[key] ?? 0);
      if (costs.some(c => c > 0)) (item.x ??= {})[ORES[key]] = costs;
    }
  }
  return item;
}

async function main() {
  const source = process.argv[2];
  const data = source
    ? JSON.parse(fs.readFileSync(source, 'utf8'))
    : await (await fetch(SOURCE_URL)).json();

  const out = {};
  for (const [category, kind] of Object.entries(KINDS)) {
    for (const entry of data[category] || []) {
      const item = itemFor(entry, kind, groupFor(category, entry));
      if (item.t.length === 0 && MISSING_LEVELS[entry._id]) Object.assign(item, MISSING_LEVELS[entry._id]);
      // The Town Hall lists what each of its levels adds: u[n - 1] is [[id, quantity], ...].
      if (category === 'buildings' && entry._id === TOWN_HALL) {
        item.u = entry.levels.map(level => (level.unlocks || []).map(u => [u._id, u.quantity]));
      }
      out[entry._id] = item;
    }
  }
  // Crafted defences are upgraded one module at a time, and the export names the module. d is the
  // defence it belongs to, which is what the icon shows.
  // Exports list equipment the player doesn't own at level 1. The Blacksmith unlocks common
  // pieces, but epics come from events, so o is the hero a piece belongs to and e marks an epic.
  for (const entry of data.equipment || []) {
    const hero = (data.heroes || []).find(h => h.name === entry.hero);
    if (hero) out[entry._id].o = hero._id;
    if (entry.rarity === 'Epic') out[entry._id].e = 1;
  }
  const station = (data.buildings || []).find(entry => entry._id === CRAFTING_STATION);
  for (const defence of station?.seasonal_defenses || []) {
    for (const module of defence.modules || []) {
      out[module._id] = { ...itemFor(module, 'module'), d: defence.name };
    }
  }

  fs.writeFileSync(OUTPUT, JSON.stringify(out));
  console.log(`Wrote ${Object.keys(out).length} entries to ${OUTPUT}`);
  fs.writeFileSync(GUIDE_OUTPUT, JSON.stringify(guideFacts(data)));
  console.log(`Wrote ${GUIDE_OUTPUT}`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
