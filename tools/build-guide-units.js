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

// Defences and traps. Their fields differ from troops', so they have their own list of charts.
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
  { title: 'Health', series: [['hp', 'Health']] },
  { title: 'Launches this much housing', series: [['springCapacity', 'Housing']] },
  { title: 'Skeletons spawned', series: [['spawnedUnits', 'Skeletons']] },
  { title: "Skeletons' level", series: [['skeletonLevel', 'Level']] },
  { title: 'Damage radius', unit: ' tiles', series: [['damageRadius', 'Radius']] },
  { title: 'Lasts', unit: 's', series: [['duration', 'Lasts']] },
];
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
  for (const npm of [...read('defenses'), ...read('traps')]) {
    const isTrap = npm.category === 'trap';
    const entry = (isTrap ? data.traps : data.buildings).find(b => b.name === npm.name && (b.village || 'home') === 'home');
    // No levels to chart: it's a home for seasonal defences, whose modules change every season.
    if (!entry || !entry.levels?.length) continue;
    const npmLevels = Object.fromEntries((npm.levels || []).filter(level => !level.supercharge).map(level => [level.level, flatten(level)]));
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
      stats: [...defenceFacts(npm, entry), ...stats],
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
    const charge = entry.levels[entry.levels.length - 1].supercharge;
    if (charge?.levels?.length) {
      unit.subs = [{
        n: 'Supercharges',
        plain: 1,
        note: 'For a defence at its final level, at the top Town Hall.',
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

const [staticPath, npmPath] = process.argv.slice(2);
if (!staticPath || !npmPath) {
  console.error('Usage: node tools/build-guide-units.js path/to/static_data.json path/to/clash-of-clans-data/package');
  process.exit(1);
}
const staticData = JSON.parse(fs.readFileSync(staticPath, 'utf8'));
const units = [...build(staticData, readNpm(npmPath)), ...buildDefences(staticData, npmPath)];
fs.writeFileSync(OUTPUT, JSON.stringify(units));
console.log(`Wrote ${units.length} units to ${OUTPUT}`);
