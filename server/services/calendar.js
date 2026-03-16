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

    const r3Today = tcToday?.data?.dailyRambam3;
    const r3Yest  = tcYest?.data?.dailyRambam3;

    if (r3Today?.url && r3Yest?.url) {
      const refToday = rambamUrlToRef(r3Today.url); // e.g. "Mishneh Torah, Sabbath.13-15"
      const refYest  = rambamUrlToRef(r3Yest.url);  // e.g. "Mishneh Torah, Sabbath.10-12"

      if (refToday && refYest) {
        console.log(`[calendar] Rambam today=${refToday} | yesterday=${refYest}`);
        items.push({
          title: { en: 'Daily Rambam (3 chapters)', he: r3Today.hebrewName || 'רמב"ם יומי' },
          ref: refToday,
          refYesterday: refYest,          // last chapter of this → chapter 1 of our 3
          displayValue: { he: r3Today.hebrewName || refToday },
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
