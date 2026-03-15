import { getDailyCalendar } from '../services/calendar.js';

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
    matchers: ['daily rambam (3 chapters)', 'daily rambam'],
    detailMode: 'rambam', // שינינו מצב ייעודי לרמב"ם שיתמוך במערך דו-ממדי
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

const HEBREW_ORDINALS = [
  "", "א", "ב", "ג", "ד", "ה", "ו", "ז", "ח", "ט", "י",
  "יא", "יב", "יג", "יד", "טו", "טז", "יז", "יח", "יט", "כ",
  "כא", "כב", "כג", "כד", "כה", "כו", "כז", "כח", "כט", "ל",
  "לא", "לב", "לג", "לד", "לה", "לו", "לז", "לח", "לט", "מ"
];

function getHebrewOrdinal(n) {
  if (n >= 1 && n < HEBREW_ORDINALS.length) return HEBREW_ORDINALS[n];
  return String(n);
}

function parseStartChapter(ref) {
  if (!ref) return 1;
  // Handle "Mishneh Torah, Sabbath.13-15" format (chapter after last dot)
  const dotMatch = ref.match(/\.(\d+)/);
  if (dotMatch) return parseInt(dotMatch[1], 10);
  // Fallback: standalone number in space-separated parts
  const parts = ref.split(' ');
  for (let i = parts.length - 1; i >= 0; i--) {
    const p = parts[i];
    if (/^\d+(-\d+)?$/.test(p)) return parseInt(p.split('-')[0], 10);
  }
  return 1;
}

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
    .replace(/&thinsp;/g, '')
    .replace(/\u2009/g, '')
    .replace(/\u05C0/g, '')
    .replace(/&amp;/g, '&')
    .replace(/&lt;/g, '<')
    .replace(/&gt;/g, '>')
    .replace(/&quot;/g, '"')
    .replace(/&#39;/g, "'")
    .trim();
}

async function fetchJson(pathname, searchParams = {}, { retries = 2, timeoutMs = 12000 } = {}) {
  const url = new URL(pathname, SEFARIA_BASE_URL);
  Object.entries(searchParams).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') url.searchParams.set(key, value);
  });

  for (let attempt = 0; attempt <= retries; attempt++) {
    const controller = new AbortController();
    const timer = setTimeout(() => controller.abort(), timeoutMs);
    try {
      const response = await fetch(url, { headers: { Accept: 'application/json' }, signal: controller.signal });
      clearTimeout(timer);
      if (!response.ok) throw new Error(`Sefaria request failed: ${response.status}`);
      const data = await response.json();
      if (data.error) throw new Error(`Sefaria API Error: ${data.error}`);
      return data;
    } catch (err) {
      clearTimeout(timer);
      if (attempt === retries) throw err;
      await new Promise(r => setTimeout(r, 800 * (attempt + 1)));
    }
  }
}

function flattenBlocks(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.flatMap((entry) => flattenBlocks(entry));
  const text = String(value).trim();
  return text ? [text] : [];
}

function findCalendarItem(calendarItems, matchers, configKey) {
  const matchValues = matchers.map(normalizeText);
  const matches = calendarItems.filter((item) => {
    const titleEn = normalizeText(item?.title?.en);
    const titleHe = normalizeText(item?.title?.he);
    const displayEn = normalizeText(item?.displayValue?.en || item?.displayValue);
    return matchValues.some((needle) => [titleEn, titleHe, displayEn].some((hay) => hay.includes(needle)));
  });

  if (configKey === 'rambam' && matches.length > 0) {
    const threeChaptersTrack = matches.find(m => (m.refs && m.refs.length >= 2) || (m.ref && m.ref.includes('-')));
    if (threeChaptersTrack) return threeChaptersTrack;
  }
  return matches[0] || null;
}

// הלוגיקה החדשה לרמב"ם שמתרגמת את Java ל-JS
function parseRambamAndroidStyle(textData) {
  const heRaw = textData?.he || textData?.text;
  if (!heRaw || !Array.isArray(heRaw)) return [];

  const startChapter = parseStartChapter(textData.ref);
  const result = [];
  let globalId = 1;

  if (Array.isArray(heRaw[0])) {
    // מערך דו ממדי - 3 פרקים
    heRaw.forEach((chapterArr, chIndex) => {
      const actualChapterNum = startChapter + chIndex;
      
      // הזרקת כותרת הפרק בדומה לאנדרואיד
      result.push({
        id: String(globalId++),
        isHeader: true,
        he: `פרק ${getHebrewOrdinal(actualChapterNum)}`,
        en: '',
        rashi: []
      });

      // הזרקת ההלכות
      chapterArr.forEach((halakha, hIndex) => {
        if (halakha && typeof halakha === 'string') {
          result.push({
            id: String(globalId++),
            isHeader: false,
            ordinal: getHebrewOrdinal(hIndex + 1),
            he: stripHtml(halakha),
            en: '',
            rashi: []
          });
        }
      });
    });
  } else {
    // פרק יחיד (גיבוי)
    heRaw.forEach((halakha, hIndex) => {
      if (halakha && typeof halakha === 'string') {
        result.push({
          id: String(globalId++),
          isHeader: false,
          ordinal: getHebrewOrdinal(hIndex + 1),
          he: stripHtml(halakha),
          en: '',
          rashi: []
        });
      }
    });
  }
  return result;
}

function mapSectionsHebrew(textData, startVerse) {
  const hebrew = flattenBlocks(textData?.he || textData?.text);
  const result = [];
  for (let i = 0; i < hebrew.length; i += 1) {
    result.push({
      id: String(i + 1),
      isHeader: false,
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
      const baseVerseMatch = torahData.ref ? torahData.ref.match(/:(\d+)/) : null;
      const startVerse = baseVerseMatch ? parseInt(baseVerseMatch[1], 10) : 1;

      for (let i = 0; i < len; i += 1) {
        sections.push({
          id: String(i + 1),
          isHeader: false,
          he: stripHtml(torahHe[i] || ''),
          en: stripHtml(onkelosHe[i] || ''),
          rashi: [],
          verseNum: startVerse + i,
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

  if (mode === 'rambam') {
    return { sections: parseRambamAndroidStyle(textData) };
  }

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
  const item = findCalendarItem(calendarItems, config.matchers, config.key);
  if (!item) {
    return { ...config, available: false, label: '', ref: '', sections: [], preview: '' };
  }

  let refsToFetch = [item.ref];

  // שניים מקרא – כל 7 העליות עם כותרות עליות + כותרות פרקים
  if (config.key === 'shnayimMikra' && item.extraDetails && Array.isArray(item.extraDetails.aliyot)) {
    const ALIYOT_NAMES = ['ראשונה', 'שנייה', 'שלישית', 'רביעית', 'חמישית', 'שישית', 'שביעית'];
    let allSections = [];
    let globalId = 1;
    let displayedChapter = null; // הפרק האחרון שהוצג

    for (let i = 0; i < 7; i++) {
      const r = item.extraDetails.aliyot[i];
      if (!r) continue;

      // כותרת עלייה
      allSections.push({ id: String(globalId++), isHeader: true, isAliyahHeader: true, he: `עלייה ${ALIYOT_NAMES[i]}`, en: '', rashi: [] });

      // פרק התחלה של עלייה זו (למשל "Leviticus 2:7-2:16" → 2)
      const chMatch = r.match(/(\d+):/);
      const aliyahStartChapter = chMatch ? parseInt(chMatch[1], 10) : null;

      // אם עלייה זו מתחילה בפרק חדש שטרם הוצג – הצג כותרת פרק
      if (aliyahStartChapter !== null && aliyahStartChapter !== displayedChapter) {
        displayedChapter = aliyahStartChapter;
        allSections.push({ id: String(globalId++), isHeader: true, isChapterHeader: true, he: `פרק ${getHebrewOrdinal(displayedChapter)}`, en: '', rashi: [] });
      }

      try {
        const payload = await fetchTextByMode(r, config.detailMode);
        let prevVerse = null;
        for (const s of payload.sections) {
          // מעבר פרק בתוך העלייה: verseNum חוזר ל-1
          if (s.verseNum === 1 && prevVerse !== null && prevVerse > 1) {
            if (displayedChapter !== null) displayedChapter++;
            allSections.push({ id: String(globalId++), isHeader: true, isChapterHeader: true, he: `פרק ${getHebrewOrdinal(displayedChapter)}`, en: '', rashi: [] });
          }
          allSections.push({ ...s, id: String(globalId++) });
          if (s.verseNum != null) prevVerse = s.verseNum;
        }
      } catch (err) {
        console.error(`[shnayimMikra] aliyah ${i + 1} failed:`, err.message);
      }
    }
    return {
      ...config, available: true,
      label: item?.displayValue?.he || item.ref,
      ref: item?.ref || '',
      preview: allSections.find(s => !s.isHeader)?.he?.slice(0, 180) || '',
      sections: allSections,
    };
  }

  // חומש – עלייה לפי יום השבוע
  if (config.kind === 'aliyah' && item.extraDetails && Array.isArray(item.extraDetails.aliyot)) {
    const dayOfWeek = new Date(dateString + 'T00:00:00Z').getUTCDay();
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

  const displayLabel = item?.displayValue?.he || item.ref;

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

    console.log(`[study] requested date: "${req.query.date}" → normalized: "${date}"`);

    const { items: calendarItems, hebrewDate } = await getDailyCalendar(date);
    const studies = {};

    for (const config of Object.values(STUDY_CONFIG)) {
      studies[config.key] = await resolveStudy(calendarItems, config, date);
    }

    const rambamLabel = studies.rambam?.label || '–';
    console.log(`[study] response for ${date} | rambam: ${rambamLabel}`);
    res.json({ date, timezone: DEFAULT_TIMEZONE, hebrewDate, studies });
  } catch (error) {
    next(error);
  }
};