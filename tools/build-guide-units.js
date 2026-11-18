// Builds app/src/main/assets/guide_units.json: every troop, siege machine, spell, defence, trap, hero
// and piece of equipment level by level, with what the guide's charts need. It takes two sources, as
// neither has everything.
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
  { title: 'Speed boost', series: [['speedIncrease', 'Speed'], ['movementSpeedIncrease', 'Speed'], ['heroSpeedIncrease', 'Heroes, pets and building-targeting troops']] },
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

// Where the plain air and ground flags would mislead. The first three are flagged for one setting
// only, or not at all, though the clash-of-clans-data entry for each says it can hit both.
const HITS_NOTES = {
  'X-Bow': () => 'Hits ground, or air and ground',
  'Skeleton Trap': () => 'Set to chase ground or air',
  "Builder's Hut": entry => `Hits air and ground from TH${entry.levels.find(level => level.dps > 0).required_townhall}`,
  'Air Sweeper': () => 'Pushes air troops back',
  'Spiky Tower': () => 'Spikes whatever hits it, air or ground',
};

// Defences and traps. Their fields differ from troops', so they have their own list of charts.
const CRAFTING_STATION = 1000097;
// Figures neither source has yet, from Supercell's TH19 release notes (16 Nov 2026), one per level.
const FROM_NOTES = {
  'Heroic Spell': {
    incomingDamageReduction: [34, 36, 38, 40],
    speedIncrease: [5, 6, 7, 8],
    heroSpeedIncrease: [2, 3, 3, 4],
    spellDuration: [9.75, 10.75, 11.75, 12.75],
    radius: [5, 5, 5, 5],
  },
  // ClashKing has no housing for these two, which drops at level 3.
  'P.E.K.K.A Titan Squad': { housing: [46, 46, 44] },
  'Baby Dragon Squad': { housing: [26, 26, 24] },
};
// What each squad ability is called in the notes, and the field its number comes from.
const SQUAD_ABILITIES = {
  SquadSplashCapAbility: { name: 'Splash Resistance', key: 'splash_damage_cap', row: 'splashCap' },
  SquadInvisibilityAbility: { name: 'Stealthy Start', key: 'duration_ms', row: 'invisibleFor', ms: true },
  SquadTrapShieldAbility: { name: 'Trap Sense', key: 'trap_damage_reduction_percent', row: 'trapCut' },
  SquadDeployShieldAbility: { name: 'Shielded Start', key: 'duration_ms', row: 'shieldFor', ms: true },
  SquadSharedTargetAbility: { name: 'Focus Attack' },
  SquadBabyDragonRageAbility: { name: 'Tantrum' },
};
const SQUAD_CHARTS = [
  { title: 'Housing', series: [['housing', 'Housing']] },
  { title: 'One splash hits at most', series: [['splashCap', 'Members']] },
  { title: 'Invisible after landing', unit: 's', series: [['invisibleFor', 'Invisible']] },
  { title: 'Trap damage cut', unit: '%', series: [['trapCut', 'Trap damage']] },
  { title: "Can't be hurt after landing", unit: 's', series: [['shieldFor', 'Shield']] },
];
const DEFENCE_CHARTS = [
  { title: 'Damage per second', series: [
    ['stats.normal.dps', 'Normal'], ['stats.gearedUpBurst.dps', 'Burst'], ['stats.gearedUpFastAttack.dps', 'Fast attack'],
    ['stats.fastAttack.dps', 'Fast attack'], ['stats.multiTarget.dps', 'Multi-target'], ['stats.normal.dpsAfter1p5s', 'After 1.5s'],
    ['stats.stage1.dps', 'Stage 1'], ['stats.stage2.dps', 'Stage 2'], ['stats.stage3.dps', 'Stage 3'],
  ] },
  { title: 'Damage per hit', series: [
    ['stats.normal.damagePerShot', 'Normal'], ['stats.normal.damagePerHit', 'Normal'], ['stats.normal.damagePerShotMin', 'At least'],
    ['stats.gearedUpBurst.damagePerShot', 'Burst'], ['stats.gearedUpFastAttack.damagePerShot', 'Fast attack'],
    ['stats.fastAttack.damagePerShot', 'Fast attack'], ['stats.multiTarget.damagePerShot', 'Multi-target'],
    ['stats.stage1.damagePerShot', 'Stage 1'], ['stats.stage2.damagePerShot', 'Stage 2'], ['stats.stage3.damagePerShot', 'Stage 3'],
  ] },
  { title: 'Chain damage per hit', series: [
    ['stats.normal.secondaryChainDamagePerShot', 'Next target'], ['stats.stage2.secondaryChainDamagePerShot', 'Stage 2'],
    ['stats.stage3.secondaryChainDamagePerShot', 'Stage 3'], ['stats.stage3.tertiaryChainDamagePerShot', 'Stage 3, third target'],
  ] },
  { title: 'Splash damage', series: [['stats.normal.splashDamageMax', 'At most'], ['stats.normal.splashDamageMin', 'At least']] },
  { title: 'Shockwave damage per hit', series: [['stats.normal.shockwaveDamagePerHit', 'Shockwave']] },
  { title: 'Trap damage', series: [['damage', 'Damage']] },
  { title: 'Damage when destroyed', series: [['deathDamage', 'When destroyed']] },
  { title: 'Bonus damage', unit: '%', series: [['stats.normal.bonusDamagePercent', 'Bonus']] },
  { title: 'Targets at once', series: [['stats.multiTarget.numberOfTargets', 'Targets']] },
  { title: 'Push strength', series: [['stats.normal.pushStrength', 'Push']] },
  { title: 'Repairs per second', series: [['stats.builder.repairPerSecond', 'Repairs']] },
  { title: 'Repairs per hit', series: [['stats.builder.repairPerHit', 'Repairs']] },
  { title: 'Spark damage', series: [['sparkDamage', 'At most']] },
  { title: 'Spike damage', unit: '%', series: [['spikeShare', "Of the attacker's health"]] },
  { title: 'Health', series: [['hp', 'Health']] },
  { title: 'Launches this much housing', series: [['springCapacity', 'Housing']] },
  { title: 'Skeletons spawned', series: [['spawnedUnits', 'Skeletons']] },
  { title: "Skeletons' level", series: [['skeletonLevel', 'Level']] },
  { title: 'Damage radius', unit: ' tiles', series: [['damageRadius', 'Radius']] },
  { title: 'Lasts', unit: 's', series: [['duration', 'Lasts']] },
];
// TH19's defences, which the other source doesn't have yet. ClashKing has their health, damage and
// costs. The rest is from Supercell's release notes (16 Nov 2026), one figure per level.
const NEW_DEFENCES = {
  'Spiky Tower': {
    special: "Never attacks. Each hit it takes sends a spike back at the attacker. Spells and hero equipment abilities don't set it off, and Freeze doesn't stop it.",
    stats: [{ n: 'Wakes up within', v: '11 tiles, after a 1s spin-up' }],
    levels: { spikeShare: [6, 9] },
  },
  'Firecracker Tower': {
    special: 'Its rockets splash where they land and spray sparks behind their target.',
    damageType: 'splash',
    levels: { 'stats.normal.damagePerShot': [483, 501], sparkDamage: [365, 385] },
  },
};
const DEFENCE_NOT_STATS = new Set([
  'level', 'hitpoints', 'buildCost', 'townHallRequired', 'xpGained', 'stats.normal.dpsInitial',
]);
const MODE_NAMES = {
  gearedUpBurst: 'Burst mode', gearedUpFastAttack: 'Fast attack mode', fastAttack: 'Fast attack mode', multiTarget: 'Multi-target mode',
  airAndGround: 'Air and ground mode', stage1: 'Stage 1', stage2: 'Stage 2', stage3: 'Stage 3',
};

function flatten(object, prefix = '', out = {}) {
  for (const [key, value] of Object.entries(object || {})) {
    if (key === 'images' || key === 'researchTime' || key === 'buildTime') continue;
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
    for (let i = series.length - 1; i > 0; i--) {
      const same = series.findIndex(s => s !== series[i] && JSON.stringify(s.v) === JSON.stringify(series[i].v));
      if (same >= 0 && same < i) {
        series[same].n = `${series[same].n} and ${series[i].n}`;
        series.splice(i, 1);
      }
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

// How far the other source's level numbers sit above ClashKing's, from the levels where health
// and damage per second both agree. 0 when none do.
function levelShift(name, levels, npmLevels) {
  const counts = {};
  for (const level of levels) {
    for (const row of npmLevels) {
      if (row.hitpoints === level.hitpoints && (row.stats?.normal?.dps ?? row.damagePerSecond) === level.dps) {
        counts[row.level - level.level] = (counts[row.level - level.level] || 0) + 1;
      }
    }
  }
  const shifts = Object.keys(counts).map(Number);
  if (shifts.length > 1) console.warn(`${name}: the other source's levels match at more than one shift: ${shifts.join(', ')}`);
  return shifts.sort((a, b) => counts[b] - counts[a])[0] || 0;
}

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
    // The data's original_min_level is one under the level the game asks for. Supercell has Super Hog
    // Rider needing Hog Rider level 10, where the data says 9, and all 17 are out by the same one.
    const superInfo = entry.super_troop && { ...entry.super_troop, original_min_level: entry.super_troop.original_min_level + 1 };
    const base = superInfo && data.troops.find(t => t._id === superInfo.original_id);
    let unlock = opens(entry);
    let kind = isSpell ? 'spell' : entry.squad ? 'squad' : (entry.production_building === 'Workshop' ? 'siege' : 'troop');
    if (superInfo) {
      const level = base.levels.find(l => l.level === superInfo.original_min_level);
      unlock = Math.max(SUPER_TROOP_TOWN_HALL, opens(base), level.required_townhall);
      kind = 'super';
    }
    // A Super Troop's lower levels exist in the data, but its base troop must reach
    // original_min_level first, which opens at unlock. TH19 numbered most of them from 1 (Super
    // Barbarian 4 is the old 8), so start at the highest level unlock allows. Before TH19 this gave
    // the same levels as starting at the base troop's level, for all 17.
    const first = superInfo ? Math.max(0, entry.levels.findLastIndex(level => level.required_townhall <= unlock)) : 0;
    const levels = entry.levels.slice(first);
    // The other source still numbers Super Troops the old way (its Super Wizard starts at 9, ours at
    // 5 since TH19), so its rows are matched to ours by health and damage.
    const shift = superInfo ? levelShift(entry.name, levels, extra.levels || []) : 0;
    const npmLevels = Object.fromEntries((extra.levels || []).map(level => [level.level - shift, flatten(level)]));
    // Damage per hit is damage per second times a unit's fixed attack time, and the other source
    // only has it for its own levels and figures. So take that ratio from a level where both sources
    // agree on the damage, and work out every level from ClashKing's numbers. A chain's hit is a
    // fixed share of the first, so it's worked out the same way.
    const ratioFor = (key, label) => {
      const ratios = levels.flatMap(level => {
        const row = npmLevels[level.level];
        if (!row || !key || row[key] === undefined) return [];
        const dps = row['stats.normal.dps'] ?? row.damagePerSecond;
        return dps > 0 && dps === level.dps ? [row[key] / dps] : [];
      });
      if (new Set(ratios.map(r => r.toFixed(2))).size > 1) console.warn(`${entry.name}: ${label} ratios differ: ${[...new Set(ratios.map(r => r.toFixed(2)))].join(', ')}`);
      return ratios[0];
    };
    const hitKey = ['stats.normal.damagePerShot', 'damagePerHit'].find(key => Object.values(npmLevels).some(row => row[key] !== undefined));
    const ratio = ratioFor(hitKey, 'damage per hit');
    const chainKey = 'stats.normal.chainDamagePerShot';
    const chainRatio = ratioFor(chainKey, 'chain damage per hit');
    const rows = levels.map((level, index) => {
      const row = { ...(npmLevels[level.level] || {}) };
      for (const key of Object.keys(row)) {
        if (!knownKeys.has(key)) throw new Error(`No label for ${entry.name}'s level field ${key}`);
      }
      for (const [key, values] of Object.entries(FROM_NOTES[entry.name] || {})) if (row[key] === undefined) row[key] = values[index];
      // A squad has no health or damage of its own: its members fight at the player's levels.
      if (entry.squad) {
        const own = entry.squad.levels.find(l => l.level === level.level) || {};
        const ability = SQUAD_ABILITIES[own.ability];
        if (own.ability && !ability) throw new Error(`No name for ${entry.name}'s ability ${own.ability}`);
        if (ability?.key) row[ability.row] = own[ability.key] / (ability.ms ? 1000 : 1);
        return row;
      }
      const npmDps = row['stats.normal.dps'] ?? row.damagePerSecond;
      if (hitKey) {
        delete row[hitKey];
        if (ratio && level.dps > 0) row[hitKey] = level.dps * ratio;
      }
      if (chainRatio) {
        delete row[chainKey];
        if (level.dps > 0) row[chainKey] = level.dps * chainRatio;
      }
      if (entry.name === 'Ruin Witch') {
        for (const key of ['stats.normal.damagePerShot', 'damagePerHit', 'stats.normal.dps']) delete row[key];
      }
      if (!isSpell) {
        row.hp = level.hitpoints;
        // The Furnace has a token figure here, and the other source rightly says it does no damage.
        const npmHasDamage = npmDps !== undefined || !npmLevels[level.level];
        if (level.dps > 0 && npmHasDamage && entry.name !== 'Ruin Witch') row.dps = level.dps;
        if (level.dps < 0) row.heal = -level.dps;
        // The Stone Slammer's figure is flat here, while the other source splits it by distance.
        if (level.death_damage && row.damageWhenDestroyedHitbox1 === undefined) row.death = level.death_damage;
      } else {
        if (level.damage > 0 && row.damage === undefined && row.maxDamagePerSecond === undefined) row.damage = level.damage;
      }
      return row;
    });

    const { charts, stats } = chartsAndStats(entry.squad ? SQUAD_CHARTS : CHARTS, rows);
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
      hs: entry.housing_space ?? FROM_NOTES[entry.name]?.housing?.[0],
      lv: levels.map(level => level.level),
      // A unit's first level comes with it, whatever the data's level 1 says (Barbarian's says 3).
      th: levels.map((level, index) => (index === 0 ? unlock : Math.max(unlock, level.required_townhall))),
      charts,
    };
    if (stats.length) unit.stats = stats;
    if (subs.length) unit.subs = subs;
    if (entry.squad) {
      const first = entry.squad.levels.find(l => l.ability);
      if (first) unit.ab = `${SQUAD_ABILITIES[first.ability].name}, from level ${first.level}`;
      // Members as [id, count], and the Squad Barracks level that brings the squad.
      unit.sq = { m: entry.squad.members.map(m => [m.troop_id, m.count]), b: entry.production_building_level };
    } else if (!isSpell) {
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

const moneyText = (cost, resource) => {
  const short = cost >= 1e6 ? `${round(cost / 1e6)}M` : cost >= 1e3 ? `${round(cost / 1e3)}K` : `${cost}`;
  return `${short} ${resource}`;
};

const timeText = total => {
  const days = Math.floor(total / 86400), hours = Math.floor((total % 86400) / 3600), minutes = Math.floor((total % 3600) / 60);
  if (days) return hours ? `${days}d ${hours}h` : `${days}d`;
  if (hours) return minutes ? `${hours}h ${minutes}m` : `${hours}h`;
  return minutes ? `${minutes}m` : `${total}s`;
};

const tiles = value => (value === 1 ? '1 tile' : `${value} tiles`);

// One mode a defence can switch to, in a few words.
function modeText(mode) {
  return [
    typeof mode.range === 'number' ? `${mode.range} tile range` : '',
    typeof mode.attackSpeed === 'number' ? `hits every ${mode.attackSpeed}s` : '',
    mode.numberOfTargets ? `${mode.numberOfTargets} targets` : '',
  ].filter(Boolean).join(', ');
}

// The Spell Tower's modes are its spells, each opened by a level.
function spellText(mode) {
  return [
    mode.damageIncrease ? `+${mode.damageIncrease}% damage` : '',
    mode.maxDps ? `up to ${mode.maxDps} damage per second` : '',
    mode.troopDamagePercent ? `${mode.troopDamagePercent}% to troops` : '',
    mode.spellDuration ? `lasts ${mode.spellDuration}s` : '',
    mode.rechargeTime ? `recharges in ${mode.rechargeTime}s` : '',
  ].filter(Boolean).join(', ');
}

function defenceFacts(npm, entry) {
  const stats = [];
  const add = (name, value) => { if (value !== undefined && value !== null && value !== '') stats.push({ n: name, v: `${value}` }); };
  add('Size', npm.size);
  const normal = npm.modes?.normal || {};
  if (normal.minRange) add('Can\'t hit closer than', tiles(normal.minRange));
  if (normal.triggerRange) add('Pops up at', tiles(normal.triggerRange));
  if (normal.activationHousingSpace) add('Wakes up after', `${normal.activationHousingSpace} housing deployed`);
  if (normal.shotsPerBurst) add('Shots per burst', normal.shotsPerBurst);
  if (normal.timeBetweenBursts) add('Between bursts', `${normal.timeBetweenBursts}s`);
  if (normal.numberOfTargets) add('Targets at once', normal.numberOfTargets);
  if (normal.maxChainTargets) add('Chains to', `${normal.maxChainTargets} targets`);
  if (normal.chainRange) add('Chain range', tiles(normal.chainRange));
  if (normal.splashRadius) add('Splash radius', tiles(normal.splashRadius));
  add('Special', npm.specialAbility);
  if (npm.triggerRadius) add('Triggers within', tiles(npm.triggerRadius));
  if (npm.damageRadius) add('Damage radius', tiles(npm.damageRadius));
  if (npm.triggerHousingSpace) add('Needs a troop of', `${npm.triggerHousingSpace} housing`);
  add('Prefers', npm.favoriteTarget);
  for (const [key, mode] of Object.entries(npm.modes || {})) {
    if (key === 'normal') continue;
    if (MODE_NAMES[key]) add(MODE_NAMES[key], modeText(mode));
    else {
      const name = key[0].toUpperCase() + key.slice(1);
      const opens = (npm.levels || []).find(level => level.unlocksSpell?.startsWith(name))?.level;
      add(name, `${opens ? `From level ${opens}: ` : ''}${spellText(mode)}`);
    }
  }
  if (npm.gearUp) {
    const gear = npm.gearUp;
    const needs = gear.requiresBuilderBuilding ? `, needs level ${gear.requiresLevel} and a Builder Base ${gear.requiresBuilderBuilding}` : '';
    add('Gear up', `${moneyText(gear.cost, gear.costResource)}, ${timeText(seconds(gear.time))}${needs}`);
  }
  const merge = entry.levels.find(level => level.merge_requirement)?.merge_requirement;
  if (merge) {
    const groups = {};
    for (const part of merge) {
      const key = `${part.name} level ${part.level}${part.geared_up ? ', geared up' : ''}`;
      groups[key] = (groups[key] || 0) + 1;
    }
    add('Merged from', Object.entries(groups).map(([key, count]) => `${count} × ${key}`).join(', '));
  }
  return stats;
}

// Every home village defence and trap, level by level. Both sources agreed on health and damage when
// this was written, except that ClashKing gives the Inferno Tower's starting damage and the other
// source its peak, which is the figure worth charting.
function buildDefences(data, root) {
  const read = dir => fs.readdirSync(path.join(root, 'data', 'home', dir)).map(file => JSON.parse(fs.readFileSync(path.join(root, 'data', 'home', dir, file), 'utf8')));
  const known = new Set([...DEFENCE_NOT_STATS, ...usedKeys(DEFENCE_CHARTS)]);
  const out = [];
  const fromWiki = [...read('defenses'), ...read('traps')];
  const wikiNames = new Set(fromWiki.map(npm => npm.name));
  const newOnes = data.buildings.filter(b => b.type === 'Defense' && (b.village || 'home') === 'home' && !wikiNames.has(b.name) && b._id !== CRAFTING_STATION);
  for (const entry of newOnes) {
    if (!NEW_DEFENCES[entry.name]) throw new Error(`${entry.name} is in neither NEW_DEFENCES nor the other source`);
  }
  for (const npm of [...fromWiki, ...newOnes.map(entry => newDefence(entry, data))]) {
    const isTrap = npm.category === 'trap';
    const entry = (isTrap ? data.traps : data.buildings).find(b => b.name === npm.name && (b.village || 'home') === 'home');
    // The Crafting Station is a home for seasonal defences, whose modules change every season.
    // ClashKing gave it one level in assets 2.5.0, but there is still nothing to chart.
    if (!entry || !entry.levels?.length || entry._id === CRAFTING_STATION) continue;
    const npmLevels = npm.extraStats
      ? Object.fromEntries(npm.levels.map((level, index) => [entry.levels[index].level, level]))
      : Object.fromEntries((npm.levels || []).filter(level => !level.supercharge).map(level => [level.level, flatten(level)]));
    const rows = entry.levels.map(level => {
      const row = { ...(npmLevels[level.level] || {}) };
      for (const key of Object.keys(row)) {
        if (!known.has(key)) throw new Error(`No label for ${npm.name}'s level field ${key}`);
      }
      if (isTrap) {
        if (!row.damage) delete row.damage;
      } else {
        row.hp = level.hitpoints;
        if (row['stats.normal.dps'] === undefined && level.dps > 0) row['stats.normal.dps'] = level.dps;
      }
      return row;
    });
    const { charts, stats } = chartsAndStats(DEFENCE_CHARTS.map(def => (npm.name === 'Inferno Tower' && def.title === 'Damage per second'
      ? { ...def, series: def.series.map(([key, label]) => [key, { 'stats.normal.dps': 'Single, at its peak', 'stats.normal.dpsAfter1p5s': 'Single, after 1.5s' }[key] || label]) }
      : def)), rows);
    // One source lists a Monolith at TH14, before the first level's own Town Hall in the other.
    const counts = (npm.availablePerTownHall || []).filter(t => t.count > 0 && t.townHallLevel >= (entry.levels[0].required_townhall || 0));
    const unlock = counts[0]?.townHallLevel ?? entry.levels[0].required_townhall;
    const townHalls = entry.levels.map((level, index) => (index === 0 ? unlock : Math.max(unlock, level.required_townhall ?? unlock)));
    const perTownHall = [];
    for (const { townHallLevel: th, count } of counts) {
      const cap = entry.levels.filter((_, index) => townHalls[index] <= th).length;
      const last = perTownHall[perTownHall.length - 1];
      if (!last || last.n !== count || last.l !== cap) perTownHall.push({ h: th, n: count, l: cap });
    }
    const unit = {
      id: entry._id,
      n: npm.name,
      k: isTrap ? 'trap' : 'defence',
      g: isTrap ? 'trap' : 'defence',
      h: unlock,
      lv: entry.levels.map(level => level.level),
      th: townHalls,
      charts,
      stats: [...defenceFacts(npm, entry), ...(npm.extraStats || []), ...stats],
      byTh: perTownHall,
      r: entry.upgrade_resource || npm.levels?.[0]?.buildCostResource || '',
      c: entry.levels.map(level => level.build_cost || 0),
      tm: entry.levels.map(level => level.build_time || 0),
    };
    const normal = npm.modes?.normal;
    if (normal?.range) unit.rg = normal.range;
    if (normal?.attackSpeed) unit.as = normal.attackSpeed;
    const damageType = normal?.damageType || npm.damageType;
    if (damageType && damageType !== 'none') unit.dt = damageType;
    const air = isTrap ? entry.air_trigger : entry.is_air_targeting;
    const ground = isTrap ? entry.ground_trigger : entry.is_ground_targeting;
    if (air) unit.a = 1;
    if (ground) unit.gr = 1;
    const hits = HITS_NOTES[npm.name]?.(entry);
    if (hits) unit.hn = hits;
    const charge = entry.levels[entry.levels.length - 1].supercharge;
    if (charge?.levels?.length) {
      unit.subs = [{
        n: 'Supercharges',
        plain: 1,
        note: `For a defence at its final level, from Town Hall ${Math.max(entry.levels[entry.levels.length - 1].required_townhall, charge.required_townhall)}.`,
        stats: charge.levels.map(stage => ({
          n: `Stage ${stage.level}`,
          v: [
            stage.dps_buff ? `+${stage.dps_buff} damage per second` : '',
            stage.hitpoints_buff ? `+${stage.hitpoints_buff} health` : '',
            moneyText(stage.build_cost, charge.upgrade_resource),
            timeText(stage.build_time),
          ].filter(Boolean).join(', '),
        })),
      }];
    }
    out.push(unit);
  }
  return out;
}

// A defence the other source doesn't have yet, shaped as if it did. Its count per Town Hall comes from
// what each Town Hall level unlocks.
function newDefence(entry, data) {
  const notes = NEW_DEFENCES[entry.name];
  const townHall = data.buildings.find(b => b.type === 'Town Hall');
  const availablePerTownHall = [];
  let count = 0;
  for (const level of townHall.levels) {
    count += (level.unlocks || []).filter(unlock => unlock._id === entry._id).reduce((sum, unlock) => sum + unlock.quantity, 0);
    if (count > 0) availablePerTownHall.push({ townHallLevel: level.level, count });
  }
  const first = entry.levels[0];
  return {
    name: entry.name,
    size: `${entry.width}x${entry.width}`,
    specialAbility: notes.special,
    damageType: notes.damageType,
    modes: { normal: {
      ...(first.attack_range ? { range: first.attack_range / 100 } : {}),
      ...(first.attack_speed ? { attackSpeed: first.attack_speed / 1000 } : {}),
    } },
    availablePerTownHall,
    extraStats: notes.stats || [],
    levels: entry.levels.map((level, index) => Object.fromEntries(Object.entries(notes.levels).map(([key, values]) => [key, values[index]]))),
  };
}

// Mistakes in the other source. It has the Flame Blower at 2,700 at level 15, but ClashKing puts levels 15
// to 17 on one ability level and the other two have 2,250.
const EQUIPMENT_CORRECTIONS = { 'Flame Blower': { 15: { abilityDamage: 2250 } } };

// Equipment. Health, damage and healing are ClashKing's. The rest are the other source's labels for
// what each piece's ability does.
const EQUIPMENT_CHARTS = [
  { title: 'Extra hero health', series: [['hp', 'Health']] },
  { title: 'Extra hero damage per second', series: [['dps', 'Damage']] },
  { title: 'Heals the hero when used', series: [['heal', 'Healing'], ['stats.healthRecovery', 'Healing']] },
  { title: 'Hero heals per second', series: [['stats.selfHealingPerSecond', 'Healing']] },
  { title: 'Hero heals per hit', series: [['stats.healPerHit', 'Healing']] },
  { title: 'Hero heals per counter', series: [['stats.healPerCounter', 'Healing']] },
  { title: 'Aura heals per second', series: [['stats.healingPerSecond', 'Healing']] },
  { title: 'Hero attack speed', unit: '%', series: [['stats.attackSpeedIncrease', 'Attack speed']] },
  { title: 'Attack speed while active', unit: '%', series: [['stats.abilityAttackSpeedIncrease', 'Attack speed']] },
  { title: 'Extra damage per hit', series: [['stats.damagePerShotIncrease', 'Damage']] },
  { title: 'Damage per hit', series: [['stats.damagePerHit', 'Damage']] },
  { title: 'Ability damage', series: [['stats.projectileDamage', 'Damage'], ['stats.abilityDamage', 'Damage']] },
  { title: 'Ability damage, in total', series: [['stats.abilityTotalDamage', 'Damage']] },
  { title: 'Damage per target', series: [['stats.projectileDamagePerTarget', 'Damage']] },
  { title: 'Counter damage', series: [['stats.counterDamage', 'Damage']] },
  { title: 'Damage when the hero falls', series: [['stats.damageOnDefeat', 'Damage']] },
  { title: 'Aura damage per second', series: [['stats.auraDps', 'Damage']] },
  { title: 'Aura damage per hit', series: [['stats.auraDamagePerHit', 'Damage']] },
  { title: 'Extra damage, share of target health', unit: '%', series: [
    ['stats.extraDamageBelow180', 'Up to 180 housing'], ['stats.extraDamage181To250', '181 to 250 housing'], ['stats.extraDamage251Plus', 'Over 250 housing'],
  ] },
  { title: 'Earthquake damage', unit: '%', series: [['stats.buildingDamagePercent', 'Buildings'], ['stats.troopDamagePercent', 'Troops']] },
  { title: 'Damage boost', unit: '%', series: [['stats.damageIncrease', 'Damage'], ['stats.damageIncreasePercent', 'Damage']] },
  { title: 'Damage boost, at most', unit: '%', series: [['stats.maxDamageIncrease', 'Damage']] },
  { title: 'Health boost', unit: '%', series: [['stats.hpIncreasePercent', 'Health']] },
  { title: 'Health boost, at most', series: [['stats.maxHpIncrease', 'Health']] },
  { title: 'Health boost, at most', unit: '%', series: [['stats.maxHealthIncrease', 'Health']] },
  { title: 'Damage taken reduced', unit: '%', series: [['stats.incomingDamageReduction', 'Reduction']] },
  { title: 'Speed boost', series: [['stats.speedIncrease', 'Speed'], ['stats.movementSpeedIncrease', 'Speed']] },
  { title: 'Lasts', unit: 's', series: [['stats.abilityDuration', 'Lasts']] },
  { title: 'Attacks it lasts', series: [['stats.numberOfAttacks', 'Attacks']] },
  { title: 'Time between strikes', unit: 's', series: [['stats.cooldownTime', 'Time']] },
  { title: 'Attack range', unit: ' tiles', series: [['stats.attackRange', 'Range']] },
  { title: 'Targets', series: [['stats.numberOfTargets', 'Targets']] },
  { title: 'Damage radius', unit: ' tiles', series: [['stats.damageRadius', 'Radius']] },
  { title: 'Slows', unit: '%', series: [['stats.slowDown', 'Slows'], ['stats.slowDownPercent', 'Slows']] },
  { title: 'Slow lasts', unit: 's', series: [['stats.slowDownDuration', 'Lasts']] },
  { title: 'Stun lasts', unit: 's', series: [['stats.stunDuration', 'Lasts']] },
  { title: 'Barbarians summoned', series: [['stats.summonedBarbarians', 'Barbarians']] },
  { title: 'Barbarian damage boost', unit: '%', series: [['stats.barbarianDamageIncrease', 'Damage']] },
  { title: 'Barbarian speed boost', series: [['stats.barbarianSpeedIncrease', 'Speed']] },
  { title: 'Archers summoned', series: [['stats.summonedArchers', 'Archers']] },
  { title: 'Archers stay invisible', unit: 's', series: [['stats.archerInvisibilityDuration', 'Lasts']] },
  { title: 'Hog Riders summoned', series: [['stats.summonedHogRiders', 'Hog Riders']] },
  { title: "Hog Riders' level", series: [['stats.hogRiderLevel', 'Level']] },
  { title: 'Healers summoned', series: [['stats.summonedHealers', 'Healers']] },
  { title: "Healers' level", series: [['stats.healerLevel', 'Level']] },
  { title: 'Lavaloons summoned', series: [['stats.summonedLavaloons', 'Lavaloons']] },
  { title: "Lavaloons' level", series: [['stats.lavaloonLevel', 'Level']] },
  { title: "Henchmen's level", series: [['stats.henchmenLevel', 'Level']] },
  { title: "Giant Giant's level", series: [['stats.giantGiantLevel', 'Level']] },
  { title: 'Snakes, at most', series: [['stats.maxSummonedSnakes', 'Snakes']] },
  { title: "Snakes' level", series: [['stats.snakeLevel', 'Level']] },
  { title: 'Clones', series: [['stats.clones', 'Clones']] },
  { title: 'Clones last', unit: 's', series: [['stats.cloneDuration', 'Lasts']] },
  { title: 'Clone damage per second', series: [['stats.cloneDps', 'Damage']] },
  { title: 'Clone health', series: [['stats.cloneHealth', 'Health']] },
  { title: 'Portal radius', unit: ' tiles', series: [['stats.portalTileRadius', 'Radius']] },
  { title: 'Recalls this much housing', series: [['stats.recallCapacity', 'Housing']] },
  { title: 'Recalled troops heal', unit: '%', series: [['stats.healingOnRedeploy', 'Healing']] },
  { title: 'Recalled troops speed up for', unit: 's', series: [['stats.hasteOnRedeploy', 'Lasts']] },
];
// Already charted from ClashKing, under its own field names.
const EQUIPMENT_NOT_STATS = new Set(['dpsIncrease', 'damageReductionIncrease']);
// The same stats in ClashKing's ability fields, with what turns them into the other source's unit.
// ClashKing wins where both have a stat. Supercell's 22 May 2026 balance table matches it, not the
// other source, on the Giant Arrow (1,500 at level 18) and Rocket Backpack (2,150 at level 27).
const EQUIPMENT_ABILITY_FIELDS = {
  summonedBarbarians: ['TroopCount', 1], summonedArchers: ['TroopCount', 1], summonedHogRiders: ['TroopCount', 1],
  summonedHealers: ['TroopCount', 1], summonedLavaloons: ['TroopCount', 1], clones: ['TroopCount', 1],
  hogRiderLevel: ['TroopLevel', 1], healerLevel: ['TroopLevel', 1], lavaloonLevel: ['TroopLevel', 1],
  henchmenLevel: ['TroopLevel', 1], giantGiantLevel: ['TroopLevel', 1],
  abilityDuration: ['DeactivateAfterTime', 0.001], numberOfAttacks: ['DeactivateAfterNumberOfHits', 1],
  damagePerShotIncrease: ['ExtraDamageFlat', 1], projectileDamage: ['Damage', 1], projectileDamagePerTarget: ['Damage', 1],
  damagePerHit: ['Damage', 1], abilityDamage: ['ActAsPenetratingDamage', 1], counterDamage: ['ReflectedDamage', 1],
  incomingDamageReduction: ['ShieldProtectionPercent', 1], slowDown: ['FrostOnHitPercent', 1], slowDownPercent: ['FrostOnHitPercent', 1],
  slowDownDuration: ['FrostOnHitTime', 0.001], damageIncrease: ['BoostDamagePercentage', 1], maxDamageIncrease: ['BoostDamagePercentage', 1],
  maxHealthIncrease: ['PercentHealthIncrease', 1], healthRecovery: ['HealOnActivation', 1], selfHealingPerSecond: ['Regeneration', 1],
  healPerCounter: ['HealOnReflect', 1], healPerHit: ['SelfDamagePerHit', -1], attackRange: ['AttackRange', 0.01], cooldownTime: ['ActivationCooldown', 0.001],
  healingOnRedeploy: ['RecallHealPercent', 1], recallCapacity: ['RecallHousing', 1], hasteOnRedeploy: ['RecallSpeedBoostTimeMS', 0.001],
  portalTileRadius: ['RecallTroopsRadius', 0.01], damageRadius: ['DamageRadius', 0.01], maxSummonedSnakes: ['MaxActivations', 1],
};
const HERO_CHARTS = [
  { title: 'Damage per second', series: [['dps', 'Damage']] },
  { title: 'Damage per hit', series: [['perHit', 'Damage']] },
  { title: 'Health', series: [['hp', 'Health']] },
];

// A stat from the other source, which can be a number or text like "+5%" or "4 tiles".
const statNumber = value => (typeof value === 'number' ? value : parseFloat(String(value).replace('+', '')));

const readAll = dir => fs.readdirSync(dir).map(file => JSON.parse(fs.readFileSync(path.join(dir, file), 'utf8')));

function buildHeroes(data, root) {
  const speeds = Object.fromEntries(readAll(path.join(root, 'data', 'home', 'heroes')).map(hero => [hero.name, hero.movementSpeed]));
  return data.heroes.filter(hero => hero.village === 'home').map(hero => {
    const levels = hero.levels;
    const attackTime = hero.attack_speed / 1000;
    const rows = levels.map(level => ({ dps: level.dps, perHit: level.dps * attackTime, hp: level.hitpoints }));
    const { charts, stats } = chartsAndStats(HERO_CHARTS, rows);
    const unit = {
      id: hero._id,
      n: hero.name,
      k: 'hero',
      g: 'hero',
      h: levels[0].required_townhall,
      lv: levels.map(level => level.level),
      th: levels.map(level => level.required_townhall),
      charts,
    };
    if (stats.length) unit.stats = stats;
    unit.f = hero.is_flying ? 1 : 0;
    unit.a = hero.is_air_targeting ? 1 : 0;
    unit.gr = hero.is_ground_targeting ? 1 : 0;
    unit.rg = hero.attack_range / 100;
    unit.sp = speeds[hero.name] ?? hero.movement_speed;
    unit.as = attackTime;
    // Upgrade costs are stored on the level you leave, so they shift up by one.
    unit.r = hero.upgrade_resource;
    unit.c = levels.map((_, index) => (index === 0 ? 0 : levels[index - 1].upgrade_cost));
    unit.tm = levels.map((_, index) => (index === 0 ? 0 : levels[index - 1].upgrade_time));
    return unit;
  });
}

function buildEquipment(data, root) {
  // Matched by name, as the other source has the ids of Monolith Arrow and Electro Fangs swapped.
  const byName = Object.fromEntries(readAll(path.join(root, 'data', 'home', 'hero-equipment')).map(piece => [piece.name, piece]));
  const known = new Set(usedKeys(EQUIPMENT_CHARTS));
  return data.equipment.map(entry => {
    const extra = byName[entry.name];
    if (!extra) throw new Error(`No clash-of-clans-data entry for ${entry.name}`);
    const levels = entry.levels;
    const extras = Object.fromEntries(extra.levels.map(level => [level.level, level]));
    const rows = levels.map(level => {
      const row = {};
      // A piece with none of one of these has no such line, rather than a line of zeros.
      if (level.hitpoints) row.hp = level.hitpoints;
      if (level.dps) row.dps = level.dps;
      if (level.heal_on_activation) row.heal = level.heal_on_activation;
      const fixes = EQUIPMENT_CORRECTIONS[entry.name]?.[level.level];
      for (const [name, value] of Object.entries({ ...extras[level.level]?.stats, ...fixes })) {
        if (EQUIPMENT_NOT_STATS.has(name)) continue;
        const key = `stats.${name}`;
        if (!known.has(key)) throw new Error(`No label for ${entry.name}'s level field ${name}`);
        const field = EQUIPMENT_ABILITY_FIELDS[name];
        const own = field && level.abilities.map(ability => ability[field[0]]).find(found => typeof found === 'number');
        const other = statNumber(value);
        if (typeof own === 'number') {
          const converted = round(own * field[1]);
          if (Math.abs(converted - other) > 0.001) console.warn(`${entry.name} level ${level.level} ${name}: ${converted} from ClashKing, ${other} from the other source`);
          row[key] = converted;
        } else {
          row[key] = other;
        }
      }
      return row;
    });
    const { charts, stats } = chartsAndStats(EQUIPMENT_CHARTS, rows);
    // The data lets most epics in at TH8, but a piece is no use before its hero opens.
    const hero = data.heroes.find(found => found.name === entry.hero);
    const heroOpens = hero.levels[0].required_townhall;
    const unit = {
      id: entry._id,
      n: entry.name,
      k: 'equipment',
      g: 'equipment',
      h: Math.max(heroOpens, levels[0].required_townhall),
      lv: levels.map(level => level.level),
      th: levels.map(level => Math.max(heroOpens, level.required_townhall)),
      charts,
    };
    if (stats.length) unit.stats = stats;
    unit.ab = `${extra.abilityType} ability`;
    return unit;
  });
}

// ClashKing's TH19 data (assets 2.5.0) moved equipment abilities out of each level
// into a separate file under new field names. Until that format is settled, an older static_data.json
// can lend its abilities, matched by piece and level. The update changed no ability values.
function borrowAbilities(data, olderPath) {
  const older = JSON.parse(fs.readFileSync(olderPath, 'utf8'));
  const byName = Object.fromEntries(older.equipment.map(piece => [piece.name, piece]));
  for (const piece of data.equipment) {
    for (const level of piece.levels) {
      if (level.abilities) continue;
      level.abilities = byName[piece.name]?.levels.find(old => old.level === level.level)?.abilities || [];
    }
  }
}

const [staticPath, npmPath, abilitiesPath] = process.argv.slice(2);
if (!staticPath || !npmPath) {
  console.error('Usage: node tools/build-guide-units.js path/to/static_data.json path/to/clash-of-clans-data/package [older static_data.json for equipment abilities]');
  process.exit(1);
}
const staticData = JSON.parse(fs.readFileSync(staticPath, 'utf8'));
if (abilitiesPath) borrowAbilities(staticData, abilitiesPath);
if (staticData.equipment.some(piece => piece.levels.some(level => !level.abilities))) {
  console.error('This static_data.json has no equipment abilities. Pass an older copy that does as the third argument.');
  process.exit(1);
}
const units = [
  ...build(staticData, readNpm(npmPath)),
  ...buildDefences(staticData, npmPath),
  ...buildHeroes(staticData, npmPath),
  ...buildEquipment(staticData, npmPath),
];
fs.writeFileSync(OUTPUT, JSON.stringify(units));
console.log(`Wrote ${units.length} units to ${OUTPUT}`);
