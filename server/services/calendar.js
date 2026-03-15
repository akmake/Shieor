const TORAHCALC_BASE = 'https://www.torahcalc.com';
const HEBCAL_BASE = 'https://www.hebcal.com';
const SEFARIA_BASE = 'https://www.sefaria.org';

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

export async function getDailyCalendar(dateString) {
  const items = [];
  let hebrewDate = '';

  // ── 1. Rambam 3-chapters from TorahCalc ────────────────────────────────────
  try {
    const tc = await fetchJson(`${TORAHCALC_BASE}/api/dailylearning?date=${dateString}`);
    const r3 = tc?.data?.dailyRambam3;
    if (r3?.url) {
      const ref = rambamUrlToRef(r3.url);
      if (ref) {
        console.log(`[calendar] Rambam ref from TorahCalc: ${ref}`);
        items.push({
          title: { en: 'Daily Rambam (3 chapters)', he: r3.hebrewName || 'רמב"ם יומי' },
          ref,
          displayValue: { he: r3.hebrewName || ref },
        });
      }
    }
  } catch (err) {
    console.error('[calendar] TorahCalc failed:', err.message);
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

  // ── 3. Tanya from Sefaria (best-effort; Sefaria always returns today's data) ─
  try {
    const sefaria = await fetchJson(
      `${SEFARIA_BASE}/api/calendars?date=${dateString}&timezone=Asia/Jerusalem`
    );
    const calItems = Array.isArray(sefaria?.calendar_items) ? sefaria.calendar_items : [];
    for (const item of calItems) {
      const en = String(item?.title?.en || '').toLowerCase();
      if (en.includes('tanya')) {
        items.push(item);
        console.log(`[calendar] Tanya from Sefaria: ${item.ref || item.displayValue?.he}`);
        break;
      }
    }
    // Use Sefaria's Hebrew date if we don't have one yet
    if (!hebrewDate && sefaria?.date?.hebrew) hebrewDate = sefaria.date.hebrew;
  } catch (err) {
    console.error('[calendar] Sefaria Tanya fetch failed:', err.message);
  }

  return { items, hebrewDate };
}
