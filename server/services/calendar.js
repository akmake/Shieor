import { readFileSync, writeFileSync, existsSync } from 'fs';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const TORAHCALC_BASE = 'https://www.torahcalc.com';
const HEBCAL_BASE = 'https://www.hebcal.com';
const SEFARIA_BASE = 'https://www.sefaria.org';

// ── Tanya daily cache (persisted to disk) ───────────────────────────────────
const __dirname = dirname(fileURLToPath(import.meta.url));
const TANYA_CACHE_PATH = join(__dirname, '../data/tanya-cache.json');

function loadTanyaCache() {
  try {
    if (existsSync(TANYA_CACHE_PATH)) {
      return new Map(Object.entries(JSON.parse(readFileSync(TANYA_CACHE_PATH, 'utf8'))));
    }
  } catch (_) {}
  return new Map();
}

function saveTanyaCache(map) {
  try {
    writeFileSync(TANYA_CACHE_PATH, JSON.stringify(Object.fromEntries(map), null, 2));
  } catch (_) {}
}

const tanyaCache = loadTanyaCache();

async function fetchJson(url, timeoutMs = 10000) {
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), timeoutMs);
  try {
    const res = await fetch(url, { headers: { Accept: 'application/json' }, signal: controller.signal });
    clearTimeout(timer);
    if (!res.ok) throw new Error(`HTTP ${res.status}`);
    const data = await res.json();
    if (data?.error) throw new Error(`API error: ${data.error}`);
    return data;
  } catch (err) {
    clearTimeout(timer);
    throw err;
  }
}

function toShabbatDate(dateString) {
  const [y, m, d] = dateString.split('-').map(Number);
  const dow = new Date(Date.UTC(y, m - 1, d)).getUTCDay(); // 0=Sun … 6=Sat
  const daysToShabbat = (6 - dow + 7) % 7;
  const dt = new Date(Date.UTC(y, m - 1, d + daysToShabbat));
  return dt.toISOString().slice(0, 10);
}

function rambamUrlToRef(url) {
  // "https://www.sefaria.org/Mishneh_Torah%2C_Sabbath.13-15?lang=bi"
  // → "Mishneh Torah, Sabbath.13-15"
  try {
    const path = new URL(url).pathname.slice(1); // "Mishneh_Torah%2C_Sabbath.13-15"
    return decodeURIComponent(path).replace(/_/g, ' ');
  } catch (_) {
    return null;
  }
}

// Parse a TorahCalc "name" field into individual Sefaria chapter refs.
// "Eruvin 4-6"  → ["Mishneh Torah, Eruvin.4", …, "Mishneh Torah, Eruvin.6"]
// "Eruvin 7, Eruvin 8, Rest on the Tenth of Tishrei 1"
//             → ["Mishneh Torah, Eruvin.7", "Mishneh Torah, Eruvin.8",
//                "Mishneh Torah, Rest on the Tenth of Tishrei.1"]
function nameToChapterRefs(name) {
  if (!name) return [];
  const rangeMatch = name.match(/^(.+)\s+(\d+)-(\d+)$/);
  if (rangeMatch) {
    const book = rangeMatch[1];
    const start = parseInt(rangeMatch[2], 10);
    const end   = parseInt(rangeMatch[3], 10);
    return Array.from({ length: end - start + 1 }, (_, i) => `Mishneh Torah, ${book}.${start + i}`);
  }
  return name.split(', ').map(entry => {
    const m = entry.match(/^(.+)\s+(\d+)$/);
    return m ? `Mishneh Torah, ${m[1]}.${m[2]}` : null;
  }).filter(Boolean);
}

function seferHamitzvotNameToRef(name) {
  const match = String(name || '').match(/(Positive|Negative)\s+Commandments?\s+(\d+)(?:-(\d+))?/i);
  if (!match) return null;
  const type = match[1].toLowerCase();
  const start = match[2];
  const end = match[3];
  const base = type === 'positive'
    ? 'Sefer HaMitzvot, Positive Commandments'
    : 'Sefer HaMitzvot, Negative Commandments';
  return end ? `${base} ${start}-${end}` : `${base} ${start}`;
}

function buildSefariaCalendarUrl(dateString) {
  const [year, month, day] = dateString.split('-').map(Number);
  const params = new URLSearchParams({
    timezone: 'Asia/Jerusalem',
    year: String(year),
    month: String(month),
    day: String(day),
  });
  return `${SEFARIA_BASE}/api/calendars?${params.toString()}`;
}

export async function getDailyCalendar(dateString) {
  const items = [];
  let hebrewDate = '';

  // ── 1. Rambam 3-chapters: yesterday's last chapter + today's first two ─────
  // TorahCalc is offset by +1 chapter vs Chabad. Fix: take chapter 3 of D-1
  // and chapters 1-2 of D, giving exactly the 3 chapters Chabad uses.
  try {
    const [y, m, d] = dateString.split('-').map(Number);
    const yesterday = new Date(Date.UTC(y, m - 1, d - 1)).toISOString().slice(0, 10);

    const [tcToday, tcYest] = await Promise.all([
      fetchJson(`${TORAHCALC_BASE}/api/dailylearning?date=${dateString}`),
      fetchJson(`${TORAHCALC_BASE}/api/dailylearning?date=${yesterday}`),
    ]);

    const r1Today = tcToday?.data?.dailyRambam;
    const r3Today = tcToday?.data?.dailyRambam3;
    const r3Yest  = tcYest?.data?.dailyRambam3;
    const shmToday = tcToday?.data?.dailySeferHamitzvos;

    if (r1Today?.url) {
      const ref1 = rambamUrlToRef(r1Today.url);
      if (ref1) {
        items.push({
          title: { en: 'Daily Rambam (1 chapter)', he: 'רמב"ם יומי (פרק 1)' },
          ref: ref1,
          displayValue: { he: r1Today.hebrewName || ref1 },
        });
      }
    }

    // TorahCalc is offset +1 vs Chabad. Chabad[D] = [TorahCalc[D-1].ch3, TorahCalc[D].ch1, TorahCalc[D].ch2].
    // When chapters cross a book boundary TorahCalc omits the URL, so always use the `name` field.
    const yesterdayChapters = nameToChapterRefs(r3Yest?.name);
    const todayChapters     = nameToChapterRefs(r3Today?.name);

    if (yesterdayChapters.length > 0 && todayChapters.length >= 2) {
      // Last chapter of yesterday's block → chapter 1 of our 3
      const refYesterday = yesterdayChapters[yesterdayChapters.length - 1];

      // First two chapters of today's block → chapters 2-3 of our 3.
      // They are always in the same book (cross-book split always falls on ch3).
      const ch1 = todayChapters[0];
      const ch2 = todayChapters[1];
      const mCh1 = ch1.match(/^(.*?)\.(\d+)$/);
      const mCh2 = ch2.match(/^(.*?)\.(\d+)$/);
      const refToday = (mCh1 && mCh2 && mCh1[1] === mCh2[1])
        ? `${mCh1[1]}.${mCh1[2]}-${mCh2[2]}`
        : ch1;

      console.log(`[calendar] Rambam today=${refToday} | yesterday=${refYesterday}`);
      items.push({
        title: { en: 'Daily Rambam (3 chapters)', he: r3Today?.hebrewName || 'רמב"ם יומי' },
        ref: refToday,
        refYesterday,
        displayValue: { he: r3Today?.hebrewName || refToday },
      });
    }

    if (shmToday?.name) {
      const shmRef = seferHamitzvotNameToRef(shmToday.name);
      if (shmRef) {
        items.push({
          title: { en: 'Daily Sefer HaMitzvot', he: 'ספר המצוות היומי' },
          ref: shmRef,
          displayValue: { he: shmToday.hebrewName || shmToday.name },
        });
      }
    }
  } catch (err) {
    console.error('[calendar] TorahCalc Rambam failed:', err.message);
  }

  // ── 2. Parasha (Chumash + Shnayim Mikra) from Hebcal ──────────────────────
  try {
    const shabbat = toShabbatDate(dateString);
    console.log(`[calendar] Hebcal shabbat date: ${shabbat} (for ${dateString})`);
    const hc = await fetchJson(
      `${HEBCAL_BASE}/shabbat?cfg=json&geonameid=281184&M=on&lg=he&leyning=on&dt=${shabbat}`
    );
    const parashat = (hc?.items || []).find(i => i.category === 'parashat');
    if (parashat?.leyning) {
      const { leyning } = parashat;
      // 0-indexed aliyot array: index 0 = Sunday = leyning["1"], … index 6 = Shabbat = leyning["7"]
      const aliyot = ['1', '2', '3', '4', '5', '6', '7'].map(k => leyning[k] || null);
      const heParasha = parashat.hebrew || parashat.title_orig || '';
      hebrewDate = parashat.hdate || '';
      console.log(`[calendar] Parasha: ${heParasha} | shabbat aliyah: ${aliyot[6]}`);
      items.push({
        title: { en: 'Parashat HaShavua', he: heParasha },
        ref: leyning.torah || aliyot.find(Boolean) || '',
        displayValue: { he: heParasha },
        extraDetails: { aliyot },
      });
    }
  } catch (err) {
    console.error('[calendar] Hebcal failed:', err.message);
  }

  // ── 3. Tanya from Sefaria + daily cache ─────────────────────────────────────
  try {
    const cachedRef = tanyaCache.get(dateString);
    if (cachedRef) {
      items.push({
        title: { en: 'Tanya', he: 'תניא יומי' },
        ref: cachedRef,
        displayValue: { he: cachedRef },
      });
      console.log(`[calendar] Tanya from cache: ${cachedRef}`);
    } else {
      const sefaria = await fetchJson(buildSefariaCalendarUrl(dateString));
      const calItems = Array.isArray(sefaria?.calendar_items) ? sefaria.calendar_items : [];
      for (const item of calItems) {
        const en = String(item?.title?.en || '').toLowerCase();
        if (en.includes('tanya')) {
          if (item.ref) {
            tanyaCache.set(dateString, item.ref);
            saveTanyaCache(tanyaCache);
          }
          items.push(item);
          console.log(`[calendar] Tanya from Sefaria (${dateString}): ${item.ref}`);
          break;
        }
      }
      if (!hebrewDate && sefaria?.date?.hebrew) hebrewDate = sefaria.date.hebrew;
    }
  } catch (err) {
    console.error('[calendar] Sefaria Tanya fetch failed:', err.message);
  }

  return { items, hebrewDate };
}
