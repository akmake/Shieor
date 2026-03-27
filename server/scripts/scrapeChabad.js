/**
 * scrapeChabad.js
 * ---------------
 * שולף זמנים הלכתיים מ-Chabad.org לשנה קדימה ושומר ב-MongoDB.
 *
 * הרצה:
 *   node server/scripts/scrapeChabad.js
 *
 * להוספת עיר חדשה — הוסף ל-CITIES בלבד.
 */

import dotenv from 'dotenv';
import mongoose from 'mongoose';
import Zman from '../models/Zman.js';

dotenv.config();

// ── ערים ──────────────────────────────────────────────────────────────────────
const CITIES = [
  { locationId: 531,  name: 'תל אביב' },
  { locationId: 247,  name: 'ירושלים' },
  { locationId: 689,  name: 'חיפה'    },
  { locationId: 688,  name: 'באר שבע' },
];

// ── קבועים ────────────────────────────────────────────────────────────────────
const AID      = 143790;
const HEADERS  = {
  'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36',
  'Accept':     'application/json',
};
const DELAY_MS = 800; // המתנה בין בקשות — כדי לא להעמיס על השרת

// ── עזרים ─────────────────────────────────────────────────────────────────────
const sleep = ms => new Promise(r => setTimeout(r, ms));

function pad(n) { return String(n).padStart(2, '0'); }

/** מחזיר את כל החודשים מהיום עד שנה קדימה */
function getMonthRanges() {
  const ranges = [];
  const now = new Date();
  for (let i = 0; i < 12; i++) {
    const d = new Date(now.getFullYear(), now.getMonth() + i, 1);
    const year  = d.getFullYear();
    const month = d.getMonth() + 1;
    const lastDay = new Date(year, month, 0).getDate();
    ranges.push({
      year, month,
      startdate: `${month}%2F1%2F${year}`,
      enddate:   `${month}%2F${lastDay}%2F${year}`,
      tdate:     `${month}-1-${year}`,
    });
  }
  return ranges;
}

/** שולף חודש אחד לעיר אחת, מחזיר מערך { date, zmanim } */
async function fetchMonth(locationId, range) {
  const url =
    `https://he.chabad.org/webservices/zmanim/zmanim/Get_Zmanim` +
    `?locationid=${locationId}&locationtype=1&tdate=${range.tdate}` +
    `&aid=${AID}&startdate=${range.startdate}&enddate=${range.enddate}`;

  const res = await fetch(url, { headers: HEADERS });
  if (!res.ok) throw new Error(`HTTP ${res.status} for ${url}`);
  const data = await res.json();

  return (data.Days || []).map(day => {
    // DisplayDate = "M/D/YYYY"
    const [m, d, y] = day.DisplayDate.split('/');
    const date = `${y}-${pad(m)}-${pad(d)}`;
    const zmanim = (day.Zmanim || []).map(z => ({
      type: z.EssentialZmanType,
      time: z.Zman,
    }));
    return { date, zmanim };
  });
}

// ── ראשי ──────────────────────────────────────────────────────────────────────
async function main() {
  await mongoose.connect(process.env.MONGO_URI);
  console.log('MongoDB connected');

  const ranges = getMonthRanges();
  console.log(`Fetching ${ranges.length} months × ${CITIES.length} cities\n`);

  for (const city of CITIES) {
    console.log(`── ${city.name} (locationId=${city.locationId}) ──`);

    for (const range of ranges) {
      const label = `${range.year}/${pad(range.month)}`;
      try {
        const days = await fetchMonth(city.locationId, range);

        const ops = days.map(({ date, zmanim }) => ({
          updateOne: {
            filter: { locationId: city.locationId, date },
            update: { $set: { cityName: city.name, zmanim } },
            upsert: true,
          },
        }));

        const result = await Zman.bulkWrite(ops);
        console.log(`  ${label} → ${result.upsertedCount} חדשים, ${result.modifiedCount} עודכנו`);
      } catch (err) {
        console.error(`  ${label} → שגיאה: ${err.message}`);
      }

      await sleep(DELAY_MS);
    }
  }

  console.log('\nסיום.');
  await mongoose.disconnect();
}

main().catch(err => {
  console.error(err);
  process.exit(1);
});
