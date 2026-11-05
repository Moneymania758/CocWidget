// Builds app/src/main/assets/guide_units.json: every troop, siege machine and spell level by level,
// with what the guide's charts need. It takes two sources, as neither has everything.
//
// ClashKing's static data has the base health and damage. Where the two disagree it has been the
// newer (it has July 2026's Dragon Rider nerf, for one), so its numbers win. clash-of-clans-data
// (MIT, values from the Clash of Clans Wiki) adds abilities, spell effects and Ruin Knights.
//
//   npm pack clash-of-clans-data, then extract it
//   node tools/build-guide-units.js path/to/static_data.json path/to/extracted/package
//
// It stops on a level field it has no label for, so a new ability can't silently go missing.

const fs = require('fs');
const path = require('path');

const OUTPUT = path.join(__dirname, '..', 'app', 'src', 'main', 'assets', 'guide_units.json');
// Super Troops open at this Town Hall, whatever their base troop's level allows.
const SUPER_TROOP_TOWN_HALL = 11;
const SECONDS = { days: 86400, hours: 3600, minutes: 60, seconds: 1 };

// Charts in the order they show. A chart's series share its one axis, so they must share a unit.
// A key is a field of the merged level: ClashKing's are dps, hp, death and heal.
const CHARTS = [
  { title: 'Damage per second', series: [
    ['dps', 'Normal'], ['stats.enraged.dps', 'Enraged'], ['stats.aura.dps', 'Aura'],
    ['stats.normal.dpsOnHeroes', 'Against heroes'], ['stats.stage2.dps', 'Stage 2'], ['stats.stage3.dps', 'Stage 3'],
    ['flameMaxDps', 'Flames, at most'], ['maxDamagePerSecond', 'Most per second'],
  ] },
  { title: 'Damage per hit', series: [
    ['stats.normal.damagePerShot', 'Normal'], ['damagePerHit', 'Normal'], ['stats.enraged.damagePerShot', 'Enraged'],
    ['stats.normal.chainDamagePerShot', 'Chain'], ['stats.aura.damagePerShot', 'Aura'],
  ] },
  { title: 'Spell damage', series: [['damage', 'Damage']] },
  { title: 'Damage to walls per second', series: [['stats.wall.dps', 'Against walls']] },
  { title: 'Damage to walls per hit', series: [['stats.wall.damagePerShot', 'Against walls'], ['damageVsWalls', 'Against walls']] },
  { title: 'Damage at point blank', series: [['pointBlankDamage', 'Point blank']] },
  { title: 'Damage when destroyed', series: [
    ['death', 'When destroyed'], ['damageWhenDestroyed', 'When destroyed'],
    ['damageWhenDestroyedHitbox1', 'Outer'], ['damageWhenDestroyedHitbox2', 'Inner'],
  ] },
  { title: 'Poison, most damage per second', series: [['poisonMaxDps', 'Poison']] },
  { title: 'Health', series: [['hp', 'Health']] },
  { title: 'Healing per second', series: [['heal', 'Troops'], ['stats.normal.healingPerSecondOnHeroes', 'Heroes']] },
  { title: 'Healing per pulse', series: [['healingPerPulse', 'Healing']] },
  { title: 'Total healing', series: [['totalHealing', 'Troops'], ['totalHealingOnHeroes', 'Heroes']] },
  { title: 'Damage boost', unit: '%', series: [['damageIncrease', 'Damage']] },
  { title: 'Speed boost', series: [['speedIncrease', 'Speed'], ['movementSpeedIncrease', 'Speed']] },
  { title: 'Damage to buildings', unit: '%', series: [['buildingDamagePercent', 'Buildings'], ['troopDamagePercent', 'Troops']] },
  { title: 'Slows movement', unit: '%', series: [['speedDecrease', 'Movement'], ['poisonSpeedDecrease', 'Movement']] },
  { title: 'Slows attacks', unit: '%', series: [['attackRateDecrease', 'Attacks'], ['poisonAttackRateDecrease', 'Attacks']] },
  { title: 'Lasts', unit: 's', series: [['spellDuration', 'Lasts'], ['angerDuration', 'Anger']] },
  { title: 'Radius', unit: ' tiles', series: [['radius', 'Radius']] },
  { title: 'Clones this much housing', series: [['clonedCapacity', 'Housing']] },
  { title: 'Recalls this much housing', series: [['recalledCapacity', 'Housing']] },
  { title: 'Damage taken reduced', unit: '%', series: [['incomingDamageReduction', 'Reduction']] },
  { title: 'Heroes revive with', unit: '%', series: [['heroHealPercent', 'Health']] },
  { title: 'Totem health', series: [['totemHitpoints', 'Health']] },
  { title: 'Bats summoned', series: [['batsGenerated', 'Bats']] },
  { title: 'Skeletons summoned', series: [['skeletonsGenerated', 'Skeletons']] },
  { title: 'Extra health from the aura', unit: '%', series: [['auraHpIncrease', 'Health']] },
  { title: 'Freeze when destroyed', unit: 's', series: [['deathFreezeTimeOnOffense', 'Attacking'], ['deathFreezeTimeOnDefense', 'Defending'], ['freezeTime', 'Attacking']] },
  { title: 'Barrels', series: [['barrelCount', 'Barrels']] },
  { title: 'Lifetime', unit: 's', series: [['lifetime', 'Lifetime']] },
];

// Fields that belong to something the unit makes, not to the unit itself.
const SUB_CHARTS = [
  { title: 'Ruin Knights summoned, at most', series: [['maximumRuinKnightsSummoned', 'Ruin Knights']] },
  { title: 'Golemites when destroyed', series: [['golemitesSpawned', 'Golemites']] },
  { title: 'Lava Pups when destroyed', series: [['pupsOnOffense', 'Attacking'], ['pupsOnDefense', 'Defending']] },
  { title: 'Firemites spawned', series: [['firemitesSpawned', 'Firemites']] },
  { title: 'Skeletons per summon', series: [['skeletonsPerSummon', 'Skeletons']] },
  { title: 'Skeletons at once, at most', series: [['maxSkeletons', 'Skeletons']] },
  { title: "Skeletons' level", series: [['skeletonLevel', 'Level']] },
  { title: 'P.E.K.K.As released', series: [['pekkasSpawned', 'P.E.K.K.As']] },
  { title: 'Wizards released', series: [['wizardsSpawned', 'Wizards']] },
  { title: 'Troops per barrel', series: [
    ['troopsSpawnedPerBarrel.minions', 'Minions'], ['troopsSpawnedPerBarrel.balloons', 'Balloons'], ['troopsSpawnedPerBarrel.babyDragons', 'Baby Dragons'],
    ['troopsSpawnedPerBarrel.barbarians', 'Barbarians'], ['troopsSpawnedPerBarrel.archers', 'Archers'], ['troopsSpawnedPerBarrel.giants', 'Giants'], ['troopsSpawnedPerBarrel.wallBreakers', 'Wall Breakers'],
  ] },
  { title: "Those troops' level", series: [
    ['troopLevel', 'Level'], ['troopLevels.minions', 'Minions'], ['troopLevels.balloons', 'Balloons'], ['troopLevels.babyDragons', 'Baby Dragons'],
  ] },
];

// Level fields that aren't stats about the unit.
const NOT_STATS = new Set([
  'level', 'hitpoints', 'stats.normal.dps', 'townHallRequired', 'laboratoryRequired', 'researchCost',
  'researchCostResource', 'ruinKnightLevel', 'stats.normal.healingPerSecond', 'stats.normal.deathDamage', 'damagePerSecond',
  'stats.normal.healingPerPulse', 'stats.normal.healingPerPulseOnHeroes',
]);

// What a unit makes, in the order they show. Without stats of its own, a sub-unit gets the note.
const NO_STATS = 'The game data has no level by level stats for these.';
const SUBS = {
  'Ruin Witch': [{ name: 'Ruin Knight', from: 'ruinKnight', icon: 'troops/ruin_knight/icon.webp', fields: SUB_CHARTS[0] }],
  Golem: [{
    name: 'Golemite', fifth: true, fields: SUB_CHARTS[1],
    note: 'Worked out from the game text, which gives a Golemite one-fifth of the Golem\'s strength and health.',
  }],
  'Lava Hound': [{ name: 'Lava Pup', fields: SUB_CHARTS[2], note: NO_STATS }],
  'Ice Hound': [{ name: 'Ice Pup', fields: SUB_CHARTS[2], note: NO_STATS }],
  Furnace: [{ name: 'Firemite', fields: SUB_CHARTS[3], note: NO_STATS }],
  Witch: [{ name: 'Skeleton', fields: [SUB_CHARTS[4], SUB_CHARTS[5], SUB_CHARTS[6]], note: NO_STATS }],
  'Siege Barracks': [{ name: 'P.E.K.K.As and Wizards', fields: [SUB_CHARTS[7], SUB_CHARTS[8]] }],
  'Troop Launcher': [{ name: 'Barrel troops', fields: [SUB_CHARTS[9], SUB_CHARTS[10]] }],
  'Sky Wagon': [{ name: 'Barrel troops', fields: [SUB_CHARTS[9], SUB_CHARTS[10]] }],
  Yeti: [{ name: 'Yetimite', note: NO_STATS }],
  'Super Yeti': [{ name: 'Yetimite', note: NO_STATS }],
  'Meteor Golem': [{ name: 'Meteormite', note: NO_STATS }],
  Druid: [{ name: 'Bear', note: NO_STATS }],
  'Super Witch': [{ name: 'Big Boy', note: NO_STATS }],
};

// Single values on a whole unit, shown as facts rather than charts.
const TOP_STATS = [
  ['summonCooldown', 'Summons every', 's'], ['auraRange', 'Aura range', ' tiles'], ['numberOfTargets', 'Heals at once', ''],
  ['evolveTime', 'Becomes a Bear after', 's'], ['freezeRadiusOnOffense', 'Freeze radius', ' tiles'], ['clonedLifespan', 'Clones last', 's'],
  ['stunTime', 'Stuns for', 's'], ['boostTime', 'Boost stays', 's'], ['hitpointDecayPerSecond', 'Loses health per second', ''],
  ['hpDecayPerSecond', 'Loses health per second', ''], ['wallAttackSpeed', 'Hits walls every', 's'], ['lifetime', 'Lifetime', 's'],
  ['radius', 'Radius', ' tiles'], ['spellDuration', 'Lasts', 's'], ['shotsPerBurst', 'Shots per burst', ''], ['timeBetweenBursts', 'Between bursts', 's'],
];

const cleanInfo = text => (text || '').replace(/\\n/g, ' ').replace(/\s+/g, ' ').trim();

function flatten(object, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(object || {})) {
    if (key === 'images' || key === 'researchTime') continue;
    if (value && typeof value === 'object' && !Array.isArray(value)) flatten(value, `${prefix}${key}.`, out);
    else if (typeof value === 'number') out[prefix + key] = value;
  }
  return out;
}

const round = value => Math.round(value * 100) / 100;

// The charts can tell three lines apart. A chart with more is split into one chart per line.
const MAX_SERIES = 3;

const number = value => value.toLocaleString('en-GB');

// A value that's the same at every level is a fact, not a chart: it has no progression to show.
function chartsAndStats(defs, rows) {
  const charts = [];
  const stats = [];
  for (const def of defs) {
    const series = [];
    for (const [key, label] of def.series) {
      const values = rows.map(row => (row[key] === undefined ? null : round(row[key])));
      if (values.every(v => v === null) || series.some(s => s.key === key)) continue;
      // Both sources can have the same figure under one label, and that's one line, not two.
      if (series.some(s => s.n === label && values.every((v, i) => v === null || s.v[i] === null || s.v[i] === v))) continue;
      series.push({ key, n: label, v: values });
    }
    if (!series.length) continue;
    const varies = s => rows.length > 1 && new Set(s.v.filter(v => v !== null)).size > 1;
    const unit = def.unit || '';
    const chart = (title, lines) => ({ t: title, ...(unit ? { u: unit } : {}), s: lines.map(s => ({ n: s.n, v: s.v })) });
    if (series.length > MAX_SERIES) {
      for (const s of series) {
        if (varies(s)) charts.push(chart(`${def.title}: ${s.n}`, [s]));
        else stats.push({ n: `${def.title}: ${s.n}`, v: `${number(s.v.find(v => v !== null))}${unit}` });
      }
    } else if (series.some(varies)) {
      // A flat line beside moving ones says nothing, and can sit exactly on another flat line.
      const moving = series.filter(varies);
      for (const s of series.filter(s => !varies(s))) {
        stats.push({ n: `${def.title} (${s.n.toLowerCase()})`, v: `${number(s.v.find(v => v !== null))}${unit}` });
      }
      const stages = series.some(s => s.key === 'stats.stage2.dps');
      const title = moving.length === 1 && series.length > 1 ? `${def.title}: ${moving[0].n}` : def.title;
      charts.push(chart(title, moving.map(s => (stages && s.key === 'dps' ? { ...s, n: 'Stage 1' } : s))));
    } else {
      for (const s of series) {
        const name = series.length > 1 ? `${def.title} (${s.n.toLowerCase()})` : def.title;
        stats.push({ n: name, v: `${number(s.v.find(v => v !== null))}${unit}` });
      }
    }
  }
  return { charts, stats };
}

function usedKeys(defs) {
  return defs.flatMap(def => def.series.map(([key]) => key));
}

function readNpm(root) {
  const home = path.join(root, 'data', 'home');
  const read = dir => fs.readdirSync(path.join(home, dir)).map(file => JSON.parse(fs.readFileSync(path.join(home, dir, file), 'utf8')));
  const byName = {};
  for (const troop of read('troops')) {
    byName[troop.name] = troop;
    if (troop.superTroop) byName[troop.superTroop.name] = troop.superTroop;
  }
  for (const entry of [...read('siege-machines'), ...read('spells')]) byName[entry.name] = entry;
  return byName;
}

const seconds = time => Object.entries(SECONDS).reduce((sum, [key, size]) => sum + (time?.[key] || 0) * size, 0);

function build(data, npm) {
  const buildingLevelTownHall = (name, level) => {
    const building = data.buildings.find(b => b.name === name && (b.village || 'home') === 'home');
    return building?.levels.find(l => l.level === level)?.required_townhall;
  };
  const opens = entry => buildingLevelTownHall(entry.production_building, entry.production_building_level);
  const troops = data.troops.filter(t => t.village === 'home' && !t.is_seasonal);
  const spells = data.spells.filter(s => !s.is_seasonal);
  const knownKeys = new Set([...NOT_STATS, ...usedKeys(CHARTS), ...usedKeys(SUB_CHARTS)]);
  const out = [];

  for (const entry of [...troops, ...spells]) {
    const isSpell = !troops.includes(entry);
    const extra = npm[entry.name] || {};
    if (!npm[entry.name]) console.warn(`No clash-of-clans-data entry for ${entry.name}`);
    const superInfo = entry.super_troop;
    const base = superInfo && data.troops.find(t => t._id === superInfo.original_id);
    let unlock = opens(entry);
    let kind = isSpell ? 'spell' : (entry.production_building === 'Workshop' ? 'siege' : 'troop');
    if (superInfo) {
      const level = base.levels.find(l => l.level === superInfo.original_min_level);
      unlock = Math.max(SUPER_TROOP_TOWN_HALL, opens(base), level.required_townhall);
      kind = 'super';
    }
    // A Super Troop's lower levels exist in the data, but its base troop must be this high first.
    const levels = entry.levels.filter(level => !superInfo || level.level >= superInfo.original_min_level);
    const npmLevels = Object.fromEntries((extra.levels || []).map(level => [level.level, flatten(level)]));
    // Damage per hit is damage per second times a unit's fixed attack time, and the other source
    // only has it for its own levels and figures. So take that ratio from a level where both sources
    // agree on the damage, and work out every level from ClashKing's numbers.
    const hitKey = ['stats.normal.damagePerShot', 'damagePerHit'].find(key => Object.values(npmLevels).some(row => row[key] !== undefined));
    const ratios = levels.flatMap(level => {
      const row = npmLevels[level.level];
      if (!row || !hitKey || row[hitKey] === undefined) return [];
      const dps = row['stats.normal.dps'] ?? row.damagePerSecond;
      return dps > 0 && dps === level.dps ? [row[hitKey] / dps] : [];
    });
    if (new Set(ratios.map(r => r.toFixed(2))).size > 1) console.warn(`${entry.name}: damage per hit ratios differ: ${[...new Set(ratios.map(r => r.toFixed(2)))].join(', ')}`);
    const ratio = ratios[0];
    const rows = levels.map(level => {
      const row = { ...(npmLevels[level.level] || {}) };
      for (const key of Object.keys(row)) {
        if (!knownKeys.has(key)) throw new Error(`No label for ${entry.name}'s level field ${key}`);
      }
      const npmDps = row['stats.normal.dps'] ?? row.damagePerSecond;
      if (hitKey) {
        delete row[hitKey];
        if (ratio && level.dps > 0) row[hitKey] = level.dps * ratio;
      }
      if (!isSpell) {
        row.hp = level.hitpoints;
        // The Furnace has a token figure here, and the other source rightly says it does no damage.
        const npmHasDamage = npmDps !== undefined || !npmLevels[level.level];
        if (level.dps > 0 && npmHasDamage) row.dps = level.dps;
        if (level.dps < 0) row.heal = -level.dps;
        // The Stone Slammer's figure is flat here, while the other source splits it by distance.
        if (level.death_damage && row.damageWhenDestroyedHitbox1 === undefined) row.death = level.death_damage;
      } else {
        if (level.damage > 0 && row.damage === undefined && row.maxDamagePerSecond === undefined) row.damage = level.damage;
      }
      return row;
    });

    const { charts, stats } = chartsAndStats(CHARTS, rows);
    for (const [key, label, unit] of TOP_STATS) {
      if (typeof extra[key] === 'number' && !stats.some(s => s.n === label) && !charts.some(c => c.t === label)) {
        stats.push({ n: label, v: `${extra[key]}${unit}` });
      }
    }

    const subs = (SUBS[entry.name] || []).map(sub => {
      const made = { n: sub.name };
      if (sub.note) made.note = sub.note;
      if (sub.icon) made.icon = sub.icon;
      const fields = sub.fields ? [].concat(sub.fields) : [];
      let result = chartsAndStats(fields, rows);
      if (sub.from) {
        const source = extra[sub.from];
        made.info = cleanInfo(source.description);
        const subRows = levels.map(level => {
          const own = (source.levels || []).find(l => l.level === level.level);
          if (!own) return {};
          const row = flatten(own);
          return { ...row, hp: own.hitpoints, dps: own.stats?.normal?.dps };
        });
        const own = chartsAndStats(CHARTS, subRows);
        result = { charts: [...own.charts, ...result.charts], stats: [...own.stats, ...result.stats] };
        made.facts = { hs: source.housingSpace, sp: source.movementSpeed, as: source.attackSpeed, rg: source.range, dt: source.damageType, tt: source.targetType };
      }
      if (sub.fifth) {
        const fifth = key => levels.map(level => (level[key] ? round(level[key] / 5) : null));
        const own = [
          { t: 'Damage per second', s: [{ n: 'Normal', v: fifth('dps') }] },
          { t: 'Damage when destroyed', s: [{ n: 'When destroyed', v: fifth('death_damage') }] },
          { t: 'Health', s: [{ n: 'Health', v: fifth('hitpoints') }] },
        ];
        result = { charts: [...own, ...result.charts], stats: result.stats };
      }
      if (result.charts.length) made.charts = result.charts;
      if (result.stats.length) made.stats = result.stats;
      return made;
    });

    // Stats a sub-unit took are dropped from the unit's own lists by being outside CHARTS.
    const unit = {
      id: entry._id,
      n: entry.name,
      k: kind,
      g: kind === 'troop' ? (entry.production_building === 'Dark Barracks' ? 'dark' : 'elixir') : kind,
      h: unlock,
      i: cleanInfo(entry.info),
      hs: entry.housing_space,
      lv: levels.map(level => level.level),
      // A unit's first level comes with it, whatever the data's level 1 says (Barbarian's says 3).
      th: levels.map((level, index) => (index === 0 ? unlock : Math.max(unlock, level.required_townhall))),
      charts,
    };
    if (stats.length) unit.stats = stats;
    if (subs.length) unit.subs = subs;
    if (!isSpell) {
      unit.f = entry.is_flying ? 1 : 0;
      unit.a = entry.is_air_targeting ? 1 : 0;
      unit.gr = entry.is_ground_targeting ? 1 : 0;
      unit.rg = extra.range ?? entry.attack_range / 100;
      if (entry.movement_speed) unit.sp = extra.movementSpeed ?? entry.movement_speed;
      if (entry.attack_speed) unit.as = extra.attackSpeed ?? entry.attack_speed / 1000;
      if (extra.damageType && extra.damageType !== 'none') unit.dt = extra.damageType;
      if (extra.attackType) unit.dt = extra.attackType;
      if (extra.preferredTarget && extra.preferredTarget !== 'None') unit.pt = extra.preferredTarget;
      if (extra.specialAbility) unit.ab = extra.specialAbility;
    }
    if (superInfo) {
      unit.base = { id: superInfo.original_id, l: superInfo.original_min_level };
      if (extra.boostCost) unit.boost = { c: extra.boostCost, r: extra.boostCostResource, t: seconds(extra.boostDuration) };
    } else {
      // Upgrade costs are stored on the level you leave, so they shift up by one.
      unit.r = entry.upgrade_resource;
      unit.c = levels.map((_, index) => (index === 0 ? 0 : levels[index - 1].upgrade_cost));
      unit.tm = levels.map((_, index) => (index === 0 ? 0 : levels[index - 1].upgrade_time));
    }
    out.push(unit);
  }
  return out;
}

const [staticPath, npmPath] = process.argv.slice(2);
if (!staticPath || !npmPath) {
  console.error('Usage: node tools/build-guide-units.js path/to/static_data.json path/to/clash-of-clans-data/package');
  process.exit(1);
}
const units = build(JSON.parse(fs.readFileSync(staticPath, 'utf8')), readNpm(npmPath));
fs.writeFileSync(OUTPUT, JSON.stringify(units));
console.log(`Wrote ${units.length} units to ${OUTPUT}`);
