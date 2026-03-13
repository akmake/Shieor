import api from './api';

export const STUDY_ROUTES = {
  chumash: '/chumash',
  rambam: '/rambam',
  tanya: '/tanya',
  shnayimMikra: '/shnayim-mikra',
};

const SEFARIA_BASE_URL = 'https://www.sefaria.org';
const TIMEZONE = 'Asia/Jerusalem';

const FALLBACK_STUDIES = {
  chumash: {
    key: 'chumash',
    title: 'חומש עם רש"י',
    subtitle: 'העלייה היומית מתוך פרשת השבוע',
    accent: 'blue',
    kind: 'aliyah',
    matchers: ['daily chumash', 'chumash', 'parashat hashavua'],
    detailMode: 'rashi',
    rules: [
      'העלייה נקבעת לפי יום השבוע.',
      'ביום שישי בדרך כלל ו׳ + ז׳.',
      'בשבת מקובל לעשות חזרה.',
    ],
  },
  rambam: {
    key: 'rambam',
    title: 'רמב"ם יומי',
    subtitle: 'שלושה פרקים במשנה תורה',
    accent: 'emerald',
    kind: 'chapters',
    matchers: ['daily rambam (3 chapters)', 'daily rambam', 'mishneh torah'],
    detailMode: 'plain',
    rules: ['מסלול של 3 פרקים ליום.'],
  },
  tanya: {
    key: 'tanya',
    title: 'תניא יומי',
    subtitle: 'קטע יומי במחזור שנתי',
    accent: 'violet',
    kind: 'segment',
    matchers: ['tanya yomi', 'daily tanya', 'tanya'],
    detailMode: 'plain',
    rules: ['קטע יומי מותאם לתאריך במחזור השנתי.'],
  },
  shnayimMikra: {
    key: 'shnayimMikra',
    title: 'שניים מקרא ואחד תרגום',
    subtitle: 'פרשת השבוע עם תרגום',
    accent: 'amber',
    kind: 'parasha',
    matchers: ['parashat hashavua', 'weekly torah portion'],
    detailMode: 'onkelos',
    rules: ['קריאה של כל פסוק פעמיים מקרא ופעם תרגום.'],
  },
};

export function normalizeDate(value) {
  if (!value) return new Date().toISOString().slice(0, 10);
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) return new Date().toISOString().slice(0, 10);
  return parsed.toISOString().slice(0, 10);
}

export function shiftDate(dateString, offsetDays) {
  const parsed = new Date(dateString);
  parsed.setDate(parsed.getDate() + offsetDays);
  return normalizeDate(parsed);
}

export function formatDateLabel(dateString) {
  const parsed = new Date(dateString);
  return new Intl.DateTimeFormat('he-IL', {
    weekday: 'long',
    day: 'numeric',
    month: 'long',
    year: 'numeric',
  }).format(parsed);
}

function normalizeText(value) {
  return String(value || '').trim().toLowerCase();
}

function flatten(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.flatMap((entry) => flatten(entry));
  const text = String(value).trim();
  return text ? [text] : [];
}

function mapSections(textData) {
  const he = flatten(textData?.he || textData?.text);
  const en = flatten(textData?.text);
  const total = Math.max(he.length, en.length);
  const rows = [];

  for (let i = 0; i < total; i += 1) {
    rows.push({
      id: String(i + 1),
      he: he[i] || '',
      en: en[i] || '',
    });
  }

  return rows.filter((row) => row.he || row.en).slice(0, 40);
}

function mapCommentary(textData) {
  const items = Array.isArray(textData?.commentary) ? textData.commentary : [];
  return items
    .map((entry, index) => ({
      id: entry?.ref || `commentary-${index + 1}`,
      ref: entry?.ref || '',
      author: entry?.collectiveTitle?.he || entry?.collectiveTitle?.en || '',
      he: flatten(entry?.he).join(' '),
      en: flatten(entry?.text).join(' '),
    }))
    .filter((row) => row.he || row.en)
    .slice(0, 14);
}

function findCalendarItem(calendarItems, matchers) {
  const needles = matchers.map(normalizeText);
  return (
    calendarItems.find((item) => {
      const titleEn = normalizeText(item?.title?.en);
      const titleHe = normalizeText(item?.title?.he);
      const displayEn = normalizeText(item?.displayValue?.en || item?.displayValue);
      return needles.some((needle) => [titleEn, titleHe, displayEn].some((hay) => hay.includes(needle)));
    }) || null
  );
}

async function fetchJson(url, params = {}) {
  const target = new URL(url, SEFARIA_BASE_URL);
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') target.searchParams.set(key, value);
  });

  const response = await fetch(target.toString());
  if (!response.ok) {
    throw new Error(`Request failed: ${response.status}`);
  }
  return response.json();
}

async function fetchStudyText(ref, detailMode) {
  if (!ref) return { sections: [], commentary: [] };

  if (detailMode === 'onkelos') {
    try {
      const onkelosData = await fetchJson(`/api/texts/${encodeURIComponent(ref)}`, {
        context: 0,
        commentary: 0,
        pad: 0,
        vhe: 'Tanach with Text Only',
        ven: 'Onkelos English',
      });
      return { sections: mapSections(onkelosData), commentary: [] };
    } catch (_) {
      // Continue to bi-lingual fallback.
    }
  }

  const textData = await fetchJson(`/api/texts/${encodeURIComponent(ref)}`, {
    context: 0,
    commentary: detailMode === 'rashi' ? 1 : 0,
    pad: 0,
    lang: 'bi',
  });

  return {
    sections: mapSections(textData),
    commentary: detailMode === 'rashi' ? mapCommentary(textData) : [],
  };
}

function buildPreview(sections) {
  if (!sections.length) return '';
  return String(sections[0].he || sections[0].en || '').slice(0, 180);
}

async function buildDailyStudyFallback(dateString) {
  const date = normalizeDate(dateString);
  const calendar = await fetchJson('/api/calendars', { date, timezone: TIMEZONE });
  const calendarItems = Array.isArray(calendar?.calendar_items) ? calendar.calendar_items : [];
  const studies = {};

  for (const config of Object.values(FALLBACK_STUDIES)) {
    const item = findCalendarItem(calendarItems, config.matchers);
    if (!item) {
      studies[config.key] = {
        ...config,
        available: false,
        label: '',
        ref: '',
        heRef: '',
        url: '',
        preview: '',
        sections: [],
        commentary: [],
      };
      continue;
    }

    const textPayload = await fetchStudyText(item.ref, config.detailMode);
    studies[config.key] = {
      ...config,
      available: true,
      label: item?.displayValue?.he || item?.displayValue?.en || item?.displayValue || '',
      ref: item?.ref || '',
      heRef: item?.heRef || '',
      url: item?.url || '',
      preview: buildPreview(textPayload.sections),
      sections: textPayload.sections,
      commentary: textPayload.commentary,
    };
  }

  return {
    date,
    timezone: TIMEZONE,
    hebrewDate: calendar?.date?.hebrew || '',
    studies,
  };
}

export async function getDailyStudy(dateString) {
  const date = normalizeDate(dateString);

  try {
    const { data } = await api.get('/api/study/day', {
      params: { date },
    });
    return data;
  } catch (apiError) {
    console.warn('Primary study API failed, using direct Sefaria fallback.', apiError);
    return buildDailyStudyFallback(date);
  }
}
