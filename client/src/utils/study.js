import api from './api';
import { saveStudyData, getStudyData } from './db';

export const STUDY_ROUTES = { chumash: '/chumash', rambam: '/rambam', tanya: '/tanya', shnayimMikra: '/shnayim-mikra' };
const SEFARIA_BASE_URL = 'https://www.sefaria.org';
const TORAHCALC_BASE_URL = 'https://www.torahcalc.com';
const HEBCAL_BASE_URL = 'https://www.hebcal.com';
const TIMEZONE = 'Asia/Jerusalem';

const FALLBACK_STUDIES = {
  chumash: { key: 'chumash', title: 'חומש עם רש"י', subtitle: 'העלייה היומית מתוך פרשת השבוע', accent: 'blue', kind: 'aliyah', matchers: ['daily chumash', 'chumash', 'parashat hashavua'], detailMode: 'rashi' },
  rambam: { key: 'rambam', title: 'רמב"ם יומי', subtitle: 'שלושה פרקים במשנה תורה', accent: 'emerald', kind: 'chapters', matchers: ['daily rambam (3 chapters)', 'daily rambam'], detailMode: 'plain' },
  tanya: { key: 'tanya', title: 'תניא יומי', subtitle: 'קטע יומי', accent: 'violet', kind: 'segment', matchers: ['tanya yomi', 'daily tanya', 'tanya'], detailMode: 'plain' },
  shnayimMikra: { key: 'shnayimMikra', title: 'שניים מקרא', subtitle: 'פרשת השבוע עם תרגום', accent: 'amber', kind: 'parasha', matchers: ['parashat hashavua', 'weekly torah portion'], detailMode: 'onkelos' },
};

export function normalizeDate(value) {
  if (!value) return new Date().toISOString().slice(0, 10);
  const parsed = new Date(value);
  return Number.isNaN(parsed.getTime()) ? new Date().toISOString().slice(0, 10) : parsed.toISOString().slice(0, 10);
}

export function shiftDate(dateString, offsetDays) {
  const [y, m, d] = dateString.split('-').map(Number);
  const dt = new Date(Date.UTC(y, m - 1, d + offsetDays));
  return dt.toISOString().slice(0, 10);
}

export function formatDateLabel(dateString) {
  return new Intl.DateTimeFormat('he-IL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(dateString));
}

function normalizeText(value) { return String(value || '').trim().toLowerCase(); }

function stripHtml(html) {
  if (!html) return '';
  return String(html).replace(/<br\s*\/?>/gi, ' ').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').replace(/&thinsp;/g, '').replace(/\u2009/g, '').replace(/\u05C0/g, '').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").trim();
}

function flatten(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.flatMap((entry) => flatten(entry));
  return String(value).trim() ? [String(value).trim()] : [];
}

function mapSectionsHebrew(textData, startVerse) {
  const he = flatten(textData?.he || textData?.text);
  const rows = [];
  for (let i = 0; i < he.length; i += 1) {
    rows.push({ id: String(i + 1), he: stripHtml(he[i]), en: '', rashi: [], verseNum: startVerse + i });
  }
  return rows.filter((row) => row.he);
}

function mapRashiOnly(textData) {
  const items = Array.isArray(textData?.commentary) ? textData.commentary : [];
  return items.filter(entry => entry?.collectiveTitle?.he === 'רש"י' || entry?.collectiveTitle?.en === 'Rashi').map((entry, index) => ({ id: entry?.ref || `rashi-${index + 1}`, anchorRef: entry?.anchorRef || '', he: stripHtml(flatten(entry?.he || entry?.text).join(' ')) })).filter((row) => row.he);
}

function findCalendarItem(calendarItems, matchers) {
  const needles = matchers.map(normalizeText);
  return calendarItems.find((item) => {
    const titleEn = normalizeText(item?.title?.en);
    const titleHe = normalizeText(item?.title?.he);
    const displayEn = normalizeText(item?.displayValue?.en || item?.displayValue);
    return needles.some((needle) => [titleEn, titleHe, displayEn].some((hay) => hay.includes(needle)));
  }) || null;
}

async function fetchJson(url, params = {}) {
  const target = new URL(url, SEFARIA_BASE_URL);
  Object.entries(params).forEach(([key, value]) => { if (value !== undefined && value !== null && value !== '') target.searchParams.set(key, value); });
  const response = await fetch(target.toString());
  if (!response.ok) throw new Error(`Request failed: ${response.status}`);
  const data = await response.json();
  if (data.error) throw new Error(`Sefaria API Error: ${data.error}`);
  return data;
}

async function fetchStudyText(ref, detailMode) {
  if (!ref) return { sections: [] };
  const safeRef = encodeURI(ref.replace(/ /g, '_'));

  if (detailMode === 'onkelos') {
    try {
      const onkelosRef = ref.startsWith('Onkelos') ? ref : `Onkelos ${ref}`;
      const safeOnkelosRef = encodeURI(onkelosRef.replace(/ /g, '_'));
      const torahData = await fetchJson(`/api/texts/${safeRef}`, { context: 0, commentary: 0, pad: 0, lang: 'he' });
      const onkelosData = await fetchJson(`/api/texts/${safeOnkelosRef}`, { context: 0, commentary: 0, pad: 0, lang: 'he' });
      const torahHe = flatten(torahData?.he || torahData?.text);
      const onkelosHe = flatten(onkelosData?.he || onkelosData?.text);
      const total = Math.max(torahHe.length, onkelosHe.length);
      const sections = [];
      for (let i = 0; i < total; i += 1) sections.push({ id: String(i + 1), he: stripHtml(torahHe[i] || ''), en: stripHtml(onkelosHe[i] || ''), rashi: [], verseNum: i + 1 });
      return { sections: sections.filter((row) => row.he || row.en) };
    } catch (_) {}
  }

  const textData = await fetchJson(`/api/texts/${safeRef}`, { context: 0, commentary: detailMode === 'rashi' ? 1 : 0, pad: 0, lang: 'he' });
  const baseVerseMatch = textData.ref ? textData.ref.match(/:(\d+)/) : null;
  const startVerse = baseVerseMatch ? parseInt(baseVerseMatch[1], 10) : 1;
  const sections = mapSectionsHebrew(textData, startVerse);

  if (detailMode === 'rashi') {
    const rashis = mapRashiOnly(textData);
    rashis.forEach(r => {
      const vNumMatch = r.anchorRef ? r.anchorRef.match(/:(\d+)/) : null;
      const vNum = vNumMatch ? parseInt(vNumMatch[1], 10) : startVerse;
      const targetSec = sections.find(s => s.verseNum === vNum);
      if (targetSec) targetSec.rashi.push(r);
      else if (sections.length > 0) sections[sections.length - 1].rashi.push(r);
    });
  }

  return { sections };
}

function toShabbatDate(dateString) {
  const [y, m, d] = dateString.split('-').map(Number);
  const dow = new Date(Date.UTC(y, m - 1, d)).getUTCDay();
  const daysToShabbat = (6 - dow + 7) % 7;
  return new Date(Date.UTC(y, m - 1, d + daysToShabbat)).toISOString().slice(0, 10);
}

function rambamUrlToRef(url) {
  try {
    const path = new URL(url).pathname.slice(1);
    return decodeURIComponent(path).replace(/_/g, ' ');
  } catch (_) { return null; }
}

async function buildCalendarItems(dateString) {
  const items = [];

  // Rambam from TorahCalc
  try {
    const tc = await fetchJson(`${TORAHCALC_BASE_URL}/api/dailylearning?date=${dateString}`, {});
    const r3 = tc?.data?.dailyRambam3;
    if (r3?.url) {
      const ref = rambamUrlToRef(r3.url);
      if (ref) items.push({
        title: { en: 'Daily Rambam (3 chapters)', he: r3.hebrewName || 'רמב"ם יומי' },
        ref, displayValue: { he: r3.hebrewName || ref },
      });
    }
  } catch (_) {}

  // Parasha from Hebcal
  try {
    const shabbat = toShabbatDate(dateString);
    const hc = await fetchJson(`${HEBCAL_BASE_URL}/shabbat`, { cfg: 'json', geonameid: '281184', M: 'on', lg: 'he', leyning: 'on', dt: shabbat });
    const parashat = (hc?.items || []).find(i => i.category === 'parashat');
    if (parashat?.leyning) {
      const { leyning } = parashat;
      const aliyot = ['1', '2', '3', '4', '5', '6', '7'].map(k => leyning[k] || null);
      const heParasha = parashat.hebrew || parashat.title_orig || '';
      items.push({
        title: { en: 'Parashat HaShavua', he: heParasha },
        ref: leyning.torah || aliyot.find(Boolean) || '',
        displayValue: { he: heParasha },
        extraDetails: { aliyot },
      });
    }
  } catch (_) {}

  // Tanya from Sefaria (best-effort)
  try {
    const sefaria = await fetchJson(`${SEFARIA_BASE_URL}/api/calendars`, { date: dateString, timezone: TIMEZONE });
    const tanya = (sefaria?.calendar_items || []).find(i => String(i?.title?.en || '').toLowerCase().includes('tanya'));
    if (tanya) items.push(tanya);
  } catch (_) {}

  return items;
}

async function buildDailyStudyFallback(dateString) {
  const date = normalizeDate(dateString);
  const calendarItems = await buildCalendarItems(date);
  const studies = {};

  for (const config of Object.values(FALLBACK_STUDIES)) {
    const item = findCalendarItem(calendarItems, config.matchers);
    if (!item) {
      studies[config.key] = { ...config, available: false, label: '', ref: '', sections: [] };
      continue;
    }

    // שניים מקרא: כל 7 העליות עם כותרות
    if (config.key === 'shnayimMikra' && item.extraDetails?.aliyot) {
      const ALIYOT_NAMES = ['ראשונה', 'שנייה', 'שלישית', 'רביעית', 'חמישית', 'שישית', 'שביעית'];
      let allSections = [];
      let globalId = 1;
      let displayedChapter = null;
      const HE_NUMS_FB = ['','א','ב','ג','ד','ה','ו','ז','ח','ט','י','יא','יב','יג','יד','טו','טז','יז','יח','יט','כ','כא','כב','כג','כד','כה','כו','כז','כח','כט','ל','לא','לב','לג','לד','לה','לו','לז','לח','לט','מ'];
      const heChap = n => (n >= 1 && n < HE_NUMS_FB.length) ? HE_NUMS_FB[n] : String(n);

      for (let i = 0; i < 7; i++) {
        const r = item.extraDetails.aliyot[i];
        if (!r) continue;
        allSections.push({ id: String(globalId++), isHeader: true, isAliyahHeader: true, he: `עלייה ${ALIYOT_NAMES[i]}`, en: '', rashi: [] });
        const chMatch = r.match(/(\d+):/);
        const aliyahStartChapter = chMatch ? parseInt(chMatch[1], 10) : null;
        if (aliyahStartChapter !== null && aliyahStartChapter !== displayedChapter) {
          displayedChapter = aliyahStartChapter;
          allSections.push({ id: String(globalId++), isHeader: true, isChapterHeader: true, he: `פרק ${heChap(displayedChapter)}`, en: '', rashi: [] });
        }
        try {
          const payload = await fetchStudyText(r, config.detailMode);
          let prevVerse = null;
          for (const s of payload.sections) {
            if (s.verseNum === 1 && prevVerse !== null && prevVerse > 1) {
              if (displayedChapter !== null) displayedChapter++;
              allSections.push({ id: String(globalId++), isHeader: true, isChapterHeader: true, he: `פרק ${heChap(displayedChapter)}`, en: '', rashi: [] });
            }
            allSections.push({ ...s, id: String(globalId++) });
            if (s.verseNum != null) prevVerse = s.verseNum;
          }
        } catch (_) {}
      }
      studies[config.key] = { ...config, available: true, label: item?.displayValue?.he || item.ref, ref: item?.ref || '', sections: allSections };
      continue;
    }

    let refsToFetch = [item.ref];
    if (config.kind === 'aliyah' && item.extraDetails?.aliyot) {
      const dayOfWeek = new Date(date + 'T00:00:00Z').getUTCDay();
      if (dayOfWeek === 5) refsToFetch = [item.extraDetails.aliyot[5], item.extraDetails.aliyot[6]].filter(Boolean);
      else refsToFetch = [item.extraDetails.aliyot[dayOfWeek === 6 ? 6 : dayOfWeek]].filter(Boolean);
    }

    let allSections = [];
    for (const r of refsToFetch) {
      if (!r) continue;
      try {
        const payload = await fetchStudyText(r, config.detailMode);
        const startId = allSections.length;
        allSections = allSections.concat(payload.sections.map(s => ({ ...s, id: String(startId + parseInt(s.id, 10)) })));
      } catch (_) {}
    }

    studies[config.key] = {
      ...config, available: true,
      label: item?.displayValue?.he || item.ref,
      ref: item?.ref || '', sections: allSections,
    };
  }

  return { date, timezone: TIMEZONE, studies };
}

export async function getDailyStudy(dateString) {
  const date = normalizeDate(dateString);

  try {
    const cached = await getStudyData(date);
    if (cached) return cached;
  } catch (_) {}

  let result;
  try {
    const { data } = await api.get('/api/study/day', { params: { date } });
    result = data;
  } catch (_) {
    result = await buildDailyStudyFallback(date);
  }

  try {
    await saveStudyData(date, result);
  } catch (_) {}

  return result;
}

export async function downloadMonth(startDate, onProgress) {
  const date = normalizeDate(startDate);
  for (let i = 0; i < 30; i++) {
    const d = shiftDate(date, i);
    if (onProgress) onProgress(i + 1, 30);
    await getDailyStudy(d);
  }
}
