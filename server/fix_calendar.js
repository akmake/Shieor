const fs = require('fs');

const path = '../server/services/calendar.js';
let text = fs.readFileSync(path, 'utf-8');

const oldBlock =       if (ref) {
        const he29 = heDay === 29 ? \\\\\char36{TEHILLIM_SCHEDULE[29].replace('Psalms ', 'תהלים ')} + \\char36{TEHILLIM_SCHEDULE[30].replace('Psalms ', '')}\\\ : null;
        const displayRef = ref.replace('Psalms ', 'תהלים ').replace(':', ':').replace('-', '-');;

// Let me just regex it properly.
let replaced = text.replace(/const he29 = heDay === 29 [\s\S]*?\.replace\('-', '-'\);/,
\unction getTehillimHebrewNum(nStr) {
          let n = parseInt(nStr, 10);
          if (isNaN(n) || n <= 0) return String(n);
          let h = '';
          if (n >= 100) { h += 'ק'; n -= 100; }
          if (n === 15) return h + 'טו';
          if (n === 16) return h + 'טז';
          if (n >= 90) { h += 'צ'; n -= 90; }
          else if (n >= 80) { h += 'פ'; n -= 80; }
          else if (n >= 70) { h += 'ע'; n -= 70; }
          else if (n >= 60) { h += 'ס'; n -= 60; }
          else if (n >= 50) { h += 'נ'; n -= 50; }
          else if (n >= 40) { h += 'מ'; n -= 40; }
          else if (n >= 30) { h += 'ל'; n -= 30; }
          else if (n >= 20) { h += 'כ'; n -= 20; }
          else if (n >= 10) { h += 'י'; n -= 10; }
          const ones = ['', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט'];
          if (n > 0) h += ones[n];
          return h;
        }

        const raw29 = heDay === 29 ? "תהלים " + TEHILLIM_SCHEDULE[29].replace('Psalms ', '') + "-" + TEHILLIM_SCHEDULE[30].replace('Psalms ', '') : null;
        let he29 = raw29 ? raw29.replace(/\\\\d+/g, match => getTehillimHebrewNum(match)) : null;
        const displayRef = ref.replace('Psalms ', 'תהלים ').replace(/\\\\d+/g, match => getTehillimHebrewNum(match));\);

fs.writeFileSync(path, replaced, 'utf-8');
console.log('Fixed calendar.js');
