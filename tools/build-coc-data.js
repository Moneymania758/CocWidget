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
const KINDS = {
  buildings: 'building',
  traps: 'trap',
  troops: 'troop',
  spells: 'spell',
  heroes: 'hero',
  pets: 'pet',
  guardians: 'guardian',
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
const CRAFTING_STATION = 1000097;
const TOWN_HALL = 1000001;
const OTHER_GROUPS = { traps: 'traps', heroes: 'heroes', pets: 'pets', guardians: 'guardians' };
// Super, seasonal and event troops never appear in an export, so they need no group to be skipped.
const LAB_CATEGORIES = new Set(['troops', 'spells']);

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
  let merge;
  for (const level of entry.levels || []) {
    if ('build_time' in level) {
      times[level.level - 1] = level.build_time;
      costs[level.level - 1] = level.build_cost;
    } else if ('upgrade_time' in level) {
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
  const station = (data.buildings || []).find(entry => entry._id === CRAFTING_STATION);
  for (const defence of station?.seasonal_defenses || []) {
    for (const module of defence.modules || []) {
      out[module._id] = { ...itemFor(module, 'module'), d: defence.name };
    }
  }

  fs.writeFileSync(OUTPUT, JSON.stringify(out));
  console.log(`Wrote ${Object.keys(out).length} entries to ${OUTPUT}`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
