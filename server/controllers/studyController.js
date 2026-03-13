const SEFARIA_BASE_URL = 'https://www.sefaria.org';
const DEFAULT_TIMEZONE = 'Asia/Jerusalem';

const STUDY_CONFIG = {
  chumash: {
    key: 'chumash',
    title: 'חומש עם רש"י',
    subtitle: 'העלייה היומית מתוך פרשת השבוע',
    accent: 'blue',
    kind: 'aliyah',
    matchers: ['daily chumash', 'chumash', 'parashat hashavua'],
    detailMode: 'rashi',
    rules: [
      'הלימוד הוא לפי עליית היום בפרשת השבוע.',
      'שישי כולל עלייה ו׳+ז׳ לפי מנהג חב"ד.',
      'בשבת מקובל לעשות חזרה כללית על הפרשה.',
    ],
    schedule: { sunday: "א'", monday: "ב'", tuesday: "ג'", wednesday: "ד'", thursday: "ה'", friday: "ו' + ז'", shabbat: 'חזרה' },
  },
  rambam: {
    key: 'rambam',
    title: "רמב\"ם יומי",
    subtitle: 'שלושה פרקים במשנה תורה',
    accent: 'emerald',
    kind: 'chapters',
    // התיקון הקריטי: הורדנו את "mishneh torah" כדי לא להיתפס על מסלול פרק אחד
    matchers: ['daily rambam (3 chapters)', 'daily rambam'],
    detailMode: 'plain',
    rules: ['מסלול של 3 פרקים ביום.'],
  },
  tanya: {
    key: 'tanya',
    title: 'תניא יומי',
    subtitle: 'קטע יומי',
    accent: 'violet',
    kind: 'segment',
    matchers: ['tanya yomi', 'daily tanya', 'tanya'],
    detailMode: 'plain',
    rules: ['חלוקה יומית רציפה לאורך שנה.'],
  },
  shnayimMikra: {
    key: 'shnayimMikra',
    title: 'שניים מקרא',
    subtitle: 'פרשת השבוע עם אונקלוס',
    accent: 'amber',
    kind: 'parasha',
    matchers: ['parashat hashavua', 'weekly torah portion'],
    detailMode: 'onkelos',
    rules: ['מטרת המסלול להשלים את כל הפרשה לפני שבת.'],
  },
};

function normalizeDateParam(value) {
  if (!value) return new Date().toISOString().slice(0, 10);
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return null;
  return parsed.toISOString().slice(0, 10);
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase();
}

function stripHtml(html) {
  if (!html) return '';
  return String(html)
    .replace(/<br\s*\/?>/gi, ' ')
    .replace(/<[^>]+>/g, '')
    .replace(/&nbsp;/g, ' ')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .trim();
}

async function fetchJson(pathname, searchParams = {}) {
  const url = new URL(pathname, SEFARIA_BASE_URL);
  Object.entries(searchParams).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
  });

  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) throw new Error(`Sefaria request failed: ${response.status}`);
  
  const data = await response.json();
  if (data.error) throw new Error(`Sefaria API Error: ${data.error}`);
  return data;
}

function flattenBlocks(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.flatMap((entry) => flattenBlocks(entry));
  const text = String(value).trim();
  return text ? [text] : [];
}

function findCalendarItem(calendarItems, matchers) {
  const matchValues = matchers.map(normalizeText);
  return calendarItems.find((item) => {
    const titleEn = normalizeText(item?.title?.en);
    const titleHe = normalizeText(item?.title?.he);
    const displayEn = normalizeText(item?.displayValue?.en || item?.displayValue);
    // וידוא שהטקסט מכיל את מילות המפתח (תופס את מסלול 3 הפרקים באופן בלעדי)
    return matchValues.some((needle) => [titleEn, titleHe, displayEn].some((hay) => hay.includes(needle)));
  }) || null;
}

function mapSectionsHebrew(textData, startVerse) {
  const hebrew = flattenBlocks(textData?.he || textData?.text);
  const result = [];

  for (let i = 0; i < hebrew.length; i += 1) {
    result.push({
      id: String(i + 1),
      he: stripHtml(hebrew[i]),
      en: '',
      rashi: [],
      verseNum: startVerse + i,
    });
  }
  return result.filter((row) => row.he);
}

function mapRashiOnly(textData) {
  const list = Array.isArray(textData?.commentary) ? textData.commentary : [];
  return list
    .filter(entry => entry?.collectiveTitle?.he === 'רש"י' || entry?.collectiveTitle?.en === 'Rashi')
    .map((entry, index) => ({
      id: entry?.ref || `rashi-${index + 1}`,
      anchorRef: entry?.anchorRef || '',
      he: stripHtml(flattenBlocks(entry?.he || entry?.text).join(' ')),
    }))
    .filter((row) => row.he);
}

async function fetchTextByMode(ref, mode) {
  if (!ref) return { sections: [] };

  const safeRef = encodeURI(ref.replace(/ /g, '_'));

  if (mode === 'onkelos') {
    try {
      const onkelosRef = ref.startsWith('Onkelos') ? ref : `Onkelos ${ref}`;
      const safeOnkelosRef = encodeURI(onkelosRef.replace(/ /g, '_'));
      
      const torahData = await fetchJson(`/api/texts/${safeRef}`, { context: 0, commentary: 0, pad: 0, lang: 'he' });
      const onkelosData = await fetchJson(`/api/texts/${safeOnkelosRef}`, { context: 0, commentary: 0, pad: 0, lang: 'he' });
      
      const torahHe = flattenBlocks(torahData?.he || torahData?.text);
      const onkelosHe = flattenBlocks(onkelosData?.he || onkelosData?.text);
      const len = Math.max(torahHe.length, onkelosHe.length);
      const sections = [];
      
      for (let i = 0; i < len; i += 1) {
        sections.push({
          id: String(i + 1),
          he: stripHtml(torahHe[i] || ''),
          en: stripHtml(onkelosHe[i] || ''),
          rashi: []
        });
      }
      return { sections: sections.filter((row) => row.he || row.en) };
    } catch (_) {}
  }

  const textData = await fetchJson(`/api/texts/${safeRef}`, {
    context: 0,
    commentary: mode === 'rashi' ? 1 : 0,
    pad: 0,
    lang: 'he',
  });

  const baseVerseMatch = textData.ref ? textData.ref.match(/:(\d+)/) : null;
  const startVerse = baseVerseMatch ? parseInt(baseVerseMatch[1], 10) : 1;
  const sections = mapSectionsHebrew(textData, startVerse);

  if (mode === 'rashi') {
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

async function resolveStudy(calendarItems, config, dateString) {
  const item = findCalendarItem(calendarItems, config.matchers);
  if (!item) {
    return { ...config, available: false, label: '', ref: '', sections: [], preview: '' };
  }

  let refsToFetch = [item.ref];

  // רמב"ם: משיכת מערך ה-refs המלא (ה-3 פרקים שמוחזרים מהמסלול של ה-3)
  if (config.key === 'rambam' && Array.isArray(item.refs) && item.refs.length > 0) {
    refsToFetch = item.refs;
  } 
  else if ((config.kind === 'aliyah' || config.kind === 'parasha') && item.extraDetails && Array.isArray(item.extraDetails.aliyot)) {
    const dayOfWeek = new Date(dateString).getDay();
    if (dayOfWeek === 5) refsToFetch = [item.extraDetails.aliyot[5], item.extraDetails.aliyot[6]].filter(Boolean);
    else refsToFetch = [item.extraDetails.aliyot[dayOfWeek === 6 ? 6 : dayOfWeek]].filter(Boolean);
  }

  let allSections = [];

  for (const r of refsToFetch) {
    if (!r) continue;
    try {
      const payload = await fetchTextByMode(r, config.detailMode);
      const startId = allSections.length;
      const adjustedSections = payload.sections.map((s) => ({ ...s, id: String(startId + parseInt(s.id, 10)) }));
      allSections = allSections.concat(adjustedSections);
    } catch (err) {
      console.error(`Failed fetching chunk ${r}:`, err.message);
    }
  }

  const displayLabel = refsToFetch.length === 1 && refsToFetch[0] !== item.ref ? refsToFetch[0] : (item?.displayValue?.he || item.ref);

  return {
    ...config,
    available: true,
    label: displayLabel,
    ref: item?.ref || '',
    preview: allSections.length ? (allSections[0].he || '').slice(0, 180) : '',
    sections: allSections,
  };
}

export const getDailyStudy = async (req, res, next) => {
  try {
    const date = normalizeDateParam(req.query.date);
    if (!date) return res.status(400).json({ message: 'Invalid date.' });

    const calendar = await fetchJson('/api/calendars', { date, timezone: DEFAULT_TIMEZONE });
    const calendarItems = Array.isArray(calendar?.calendar_items) ? calendar.calendar_items : [];
    const studies = {};

    for (const config of Object.values(STUDY_CONFIG)) {
      studies[config.key] = await resolveStudy(calendarItems, config, date);
    }

    res.json({ date, timezone: DEFAULT_TIMEZONE, hebrewDate: calendar?.date?.hebrew || '', studies });
  } catch (error) {
    next(error);
  }
};