// Rebuilds app/src/main/assets/coc_data.json (id -> name, kind, village, per-level upgrade seconds)
// from ClashKing's static game data. Run it after a game update adds new buildings, troops or levels.
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

async function main() {
  const source = process.argv[2];
  const data = source
    ? JSON.parse(fs.readFileSync(source, 'utf8'))
    : await (await fetch(SOURCE_URL)).json();

  const out = {};
  for (const [category, kind] of Object.entries(KINDS)) {
    for (const entry of data[category] || []) {
      // t[n - 1] is the time to upgrade to level n. Buildings and traps store that as build_time
      // on level n. Everything else stores upgrade_time on level n - 1 (time to leave that level).
      const times = [];
      for (const level of entry.levels || []) {
        if ('build_time' in level) times[level.level - 1] = level.build_time;
        else if ('upgrade_time' in level) times[level.level] = level.upgrade_time;
      }
      out[entry._id] = {
        n: entry.name,
        k: kind,
        v: entry.village || '',
        t: Array.from(times, t => t ?? 0),
      };
    }
  }

  fs.writeFileSync(OUTPUT, JSON.stringify(out));
  console.log(`Wrote ${Object.keys(out).length} entries to ${OUTPUT}`);
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
