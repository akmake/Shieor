import api from './api';

export const STUDY_ROUTES = { chumash: '/chumash', rambam: '/rambam', tanya: '/tanya', shnayimMikra: '/shnayim-mikra' };
const SEFARIA_BASE_URL = 'https://www.sefaria.org';
const TIMEZONE = 'Asia/Jerusalem';

const FALLBACK_STUDIES = {
  chumash: { key: 'chumash', title: 'חומש עם רש"י', subtitle: 'העלייה היומית מתוך פרשת השבוע', accent: 'blue', kind: 'aliyah', matchers: ['daily chumash', 'chumash', 'parashat hashavua'], detailMode: 'rashi' },
  // התיקון גם בלקוח - הסרת mishneh torah מהרמב"ם
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
  const parsed = new Date(dateString);
  parsed.setDate(parsed.getDate() + offsetDays);
  return normalizeDate(parsed);
}

export function formatDateLabel(dateString) {
  return new Intl.DateTimeFormat('he-IL', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric' }).format(new Date(dateString));
}

function normalizeText(value) { return String(value || '').trim().toLowerCase(); }

function stripHtml(html) {
  if (!html) return '';
  return String(html).replace(/<br\s*\/?>/gi, ' ').replace(/<[^>]+>/g, '').replace(/&nbsp;/g, ' ').replace(/&amp;/g, '&').replace(/&lt;/g, '<').replace(/&gt;/g, '>').replace(/&quot;/g, '"').replace(/&#39;/g, "'").trim();
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
      for (let i = 0; i < total; i += 1) sections.push({ id: String(i + 1), he: stripHtml(torahHe[i] || ''), en: stripHtml(onkelosHe[i] || ''), rashi: [] });
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

async function buildDailyStudyFallback(dateString) {
  const date = normalizeDate(dateString);
  const calendar = await fetchJson('/api/calendars', { date, timezone: TIMEZONE });
  const calendarItems = Array.isArray(calendar?.calendar_items) ? calendar.calendar_items : [];
  const studies = {};

  for (const config of Object.values(FALLBACK_STUDIES)) {
    const item = findCalendarItem(calendarItems, config.matchers);
    if (!item) {
      studies[config.key] = { ...config, available: false, label: '', ref: '', sections: [] };
      continue;
    }

    let refsToFetch = [item.ref];
    
    if (config.key === 'rambam' && Array.isArray(item.refs) && item.refs.length > 0) {
      refsToFetch = item.refs;
    } else if ((config.kind === 'aliyah' || config.kind === 'parasha') && item.extraDetails && Array.isArray(item.extraDetails.aliyot)) {
      const dayOfWeek = new Date(dateString).getDay();
      if (dayOfWeek === 5) refsToFetch = [item.extraDetails.aliyot[5], item.extraDetails.aliyot[6]].filter(Boolean);
      else refsToFetch = [item.extraDetails.aliyot[dayOfWeek === 6 ? 6 : dayOfWeek]].filter(Boolean);
    }

    let allSections = [];
    for (const r of refsToFetch) {
      if (!r) continue;
      try {
        const payload = await fetchStudyText(r, config.detailMode);
        const startId = allSections.length;
        const adjustedSections = payload.sections.map((s) => ({ ...s, id: String(startId + parseInt(s.id, 10)) }));
        allSections = allSections.concat(adjustedSections);
      } catch (err) {}
    }

    studies[config.key] = {
      ...config, available: true, label: refsToFetch.length === 1 && refsToFetch[0] !== item.ref ? refsToFetch[0] : (item?.displayValue?.he || item.ref),
      ref: item?.ref || '', sections: allSections,
    };
  }

  return { date, timezone: TIMEZONE, studies };
}

export async function getDailyStudy(dateString) {
  const date = normalizeDate(dateString);
  try {
    const { data } = await api.get('/api/study/day', { params: { date } });
    return data;
  } catch (apiError) {
    return buildDailyStudyFallback(date);
  }
}