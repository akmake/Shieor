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
    schedule: {
      sunday: "עלייה א'",
      monday: "עלייה ב'",
      tuesday: "עלייה ג'",
      wednesday: "עלייה ד'",
      thursday: "עלייה ה'",
      friday: "עלייה ו' + ז'",
      shabbat: 'חזרה כללית',
    },
  },
  rambam: {
    key: 'rambam',
    title: "רמב\"ם יומי",
    subtitle: 'שלושה פרקים במשנה תורה',
    accent: 'emerald',
    kind: 'chapters',
    matchers: ['daily rambam (3 chapters)', 'daily rambam', 'mishneh torah'],
    detailMode: 'plain',
    rules: [
      'מסלול של 3 פרקים ביום.',
      'המחזור משלים את משנה תורה בכ-11 חודשים.',
      'קיימים מסלולים נוספים (פרק אחד וספר המצוות), אך כאן נשארים על 3 פרקים.',
    ],
    schedule: {
      track: '3 פרקים ליום',
      cycle: 'מחזור שנתי מקוצר',
    },
  },
  tanya: {
    key: 'tanya',
    title: 'תניא יומי',
    subtitle: 'קטע יומי במחזור י״ט כסלו עד י״ט כסלו',
    accent: 'violet',
    kind: 'segment',
    matchers: ['tanya yomi', 'daily tanya', 'tanya'],
    detailMode: 'plain',
    rules: [
      'חלוקה יומית רציפה לאורך שנה.',
      'בשנה מעוברת הקטעים לרוב קצרים יותר.',
      'בשנה פשוטה הקטעים ארוכים יותר כדי לשמור על אותו מחזור.',
    ],
    schedule: {
      cycle: 'י״ט כסלו עד י״ט כסלו',
      leapYear: 'שנה מעוברת משנה את חלוקת הקטעים',
    },
  },
  shnayimMikra: {
    key: 'shnayimMikra',
    title: 'שניים מקרא ואחד תרגום',
    subtitle: 'פרשת השבוע עם תרגום אונקלוס',
    accent: 'amber',
    kind: 'parasha',
    matchers: ['parashat hashavua', 'weekly torah portion'],
    detailMode: 'onkelos',
    rules: [
      'קוראים כל פסוק פעמיים מקרא ופעם אחת תרגום.',
      'החלוקה מקבילה לעליות השבוע.',
      'מטרת המסלול להשלים את כל הפרשה לפני שבת.',
    ],
    schedule: {
      cadence: 'לפי עליות השבוע',
      completion: 'השלמת כל הפרשה לפני שבת',
    },
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

async function fetchJson(pathname, searchParams = {}) {
  const url = new URL(pathname, SEFARIA_BASE_URL);
  Object.entries(searchParams).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      url.searchParams.set(key, value);
    }
  });

  const response = await fetch(url, { headers: { Accept: 'application/json' } });
  if (!response.ok) {
    throw new Error(`Sefaria request failed: ${response.status} ${response.statusText}`);
  }
  return response.json();
}

function flattenBlocks(value) {
  if (!value) return [];
  if (Array.isArray(value)) return value.flatMap((entry) => flattenBlocks(entry));
  const text = String(value).trim();
  return text ? [text] : [];
}

function findCalendarItem(calendarItems, matchers) {
  const matchValues = matchers.map(normalizeText);
  return (
    calendarItems.find((item) => {
      const titleEn = normalizeText(item?.title?.en);
      const titleHe = normalizeText(item?.title?.he);
      const displayEn = normalizeText(item?.displayValue?.en || item?.displayValue);
      return matchValues.some((needle) => [titleEn, titleHe, displayEn].some((hay) => hay.includes(needle)));
    }) || null
  );
}

function mapSections(textData) {
  const hebrew = flattenBlocks(textData?.he || textData?.text);
  const english = flattenBlocks(textData?.text);
  const len = Math.max(hebrew.length, english.length);
  const result = [];

  for (let i = 0; i < len; i += 1) {
    result.push({
      id: String(i + 1),
      he: hebrew[i] || '',
      en: english[i] || '',
    });
  }

  return result.filter((row) => row.he || row.en).slice(0, 40);
}

function mapCommentary(textData) {
  const list = Array.isArray(textData?.commentary) ? textData.commentary : [];
  return list
    .map((entry, index) => ({
      id: entry?.ref || `commentary-${index + 1}`,
      ref: entry?.ref || '',
      author: entry?.collectiveTitle?.he || entry?.collectiveTitle?.en || '',
      he: flattenBlocks(entry?.he).join(' '),
      en: flattenBlocks(entry?.text).join(' '),
    }))
    .filter((row) => row.he || row.en)
    .slice(0, 14);
}

async function fetchTextByMode(ref, mode) {
  if (!ref) return { sections: [], commentary: [] };

  if (mode === 'onkelos') {
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
      // Fallback below to bilingual text if the Onkelos translation key is unavailable.
    }
  }

  const textData = await fetchJson(`/api/texts/${encodeURIComponent(ref)}`, {
    context: 0,
    commentary: mode === 'rashi' ? 1 : 0,
    pad: 0,
    lang: 'bi',
  });

  return {
    sections: mapSections(textData),
    commentary: mode === 'rashi' ? mapCommentary(textData) : [],
  };
}

function sectionPreview(sections) {
  if (!sections.length) return '';
  const first = sections[0];
  const value = first.he || first.en || '';
  return value.slice(0, 180);
}

async function resolveStudy(calendarItems, config) {
  const item = findCalendarItem(calendarItems, config.matchers);
  if (!item) {
    return {
      ...config,
      available: false,
      label: '',
      ref: '',
      heRef: '',
      url: '',
      sections: [],
      commentary: [],
      preview: '',
    };
  }

  const textPayload = await fetchTextByMode(item.ref, config.detailMode);

  return {
    ...config,
    available: true,
    label: item?.displayValue?.he || item?.displayValue?.en || item?.displayValue || '',
    ref: item?.ref || '',
    heRef: item?.heRef || '',
    url: item?.url || '',
    preview: sectionPreview(textPayload.sections),
    sections: textPayload.sections,
    commentary: textPayload.commentary,
    source: {
      title: item?.title || null,
      category: item?.category || '',
      order: item?.order || 0,
    },
  };
}

export const getDailyStudy = async (req, res, next) => {
  try {
    const date = normalizeDateParam(req.query.date);
    if (!date) {
      res.status(400).json({ message: 'Invalid date. Expected YYYY-MM-DD.' });
      return;
    }

    const calendar = await fetchJson('/api/calendars', {
      date,
      timezone: DEFAULT_TIMEZONE,
    });

    const calendarItems = Array.isArray(calendar?.calendar_items) ? calendar.calendar_items : [];
    const studies = {};

    for (const config of Object.values(STUDY_CONFIG)) {
      studies[config.key] = await resolveStudy(calendarItems, config);
    }

    res.json({
      date,
      timezone: DEFAULT_TIMEZONE,
      hebrewDate: calendar?.date?.hebrew || '',
      studies,
    });
  } catch (error) {
    next(error);
  }
};
