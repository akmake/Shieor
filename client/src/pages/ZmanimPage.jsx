import { useState, useEffect } from 'react';
import { Clock, MapPin } from 'lucide-react';

const CITIES = [
  { label: 'תל אביב', locationId: 531  },
  { label: 'ירושלים', locationId: 247  },
  { label: 'חיפה',    locationId: 689  },
  { label: 'באר שבע', locationId: 688  },
];

// מיפוי סוגי זמנים מחבד לתצוגה
const ZMANIM_ORDER = [
  { type: 'AlosHashachar',    label: 'עלות השחר'     },
  { type: 'EarliestTefillin', label: 'משיכיר'         },
  { type: 'NetzHachamah',     label: 'הנץ החמה'       },
  { type: 'LatestShema',      label: 'סוף זמן ק"ש'    },
  { type: 'LatestTefillah',   label: 'סוף זמן תפילה'  },
  { type: 'Chatzos',          label: 'חצות היום'      },
  { type: 'MinchahGedolah',   label: 'מנחה גדולה'     },
  { type: 'MinchahKetanah',   label: 'מנחה קטנה'      },
  { type: 'PlagHaminchah',    label: 'פלג המנחה'      },
  { type: 'CandleLighting',   label: 'הדלקת נרות'     },
  { type: 'Shkiah',           label: 'שקיעת החמה'     },
  { type: 'Tzeis',            label: 'צאת הכוכבים'    },
  { type: 'ChatzosNight',     label: 'חצות הלילה'     },
];

const CACHE_TTL_MS  = 30 * 24 * 60 * 60 * 1000; // 30 יום
const CACHE_VERSION = 'v1';

function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function addDays(dateStr, n) {
  const d = new Date(dateStr);
  d.setDate(d.getDate() + n);
  return d.toISOString().slice(0, 10);
}

function formatDateHebrew(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  return new Date(y, m - 1, d).toLocaleDateString('he-IL', {
    weekday: 'long', year: 'numeric', month: 'long', day: 'numeric',
  });
}

// ── Cache helpers ─────────────────────────────────────────────────────────────
function cacheKey(locationId) {
  return `chabad_zmanim_${CACHE_VERSION}_${locationId}`;
}

function readCache(locationId) {
  try {
    const raw = localStorage.getItem(cacheKey(locationId));
    if (!raw) return null;
    const { savedAt, days } = JSON.parse(raw);
    if (Date.now() - savedAt > CACHE_TTL_MS) return null;
    return days; // { 'YYYY-MM-DD': [ {type, time}, ... ] }
  } catch {
    return null;
  }
}

function writeCache(locationId, days) {
  try {
    localStorage.setItem(cacheKey(locationId), JSON.stringify({ savedAt: Date.now(), days }));
  } catch { /* localStorage מלא */ }
}

// ── Fetch מהשרת (30 ימים) ────────────────────────────────────────────────────
async function fetchFromServer(locationId, from, to) {
  const res = await fetch(`/api/zmanim?locationId=${locationId}&from=${from}&to=${to}`);
  if (!res.ok) throw new Error(`שגיאת שרת ${res.status}`);
  const docs = await res.json();
  // הופך מ-array ל-map לפי תאריך
  const map = {};
  docs.forEach(doc => { map[doc.date] = doc.zmanim; });
  return map;
}

// ── קומפוננטה ─────────────────────────────────────────────────────────────────
export default function ZmanimPage() {
  const [cityIdx, setCityIdx]     = useState(0);
  const [date, setDate]           = useState(todayISO());
  const [zmanimMap, setZmanimMap] = useState(null); // cache ב-state
  const [loading, setLoading]     = useState(false);
  const [error, setError]         = useState('');

  const city = CITIES[cityIdx];

  // כשמשתנה עיר — טוען מ-cache או מהשרת
  useEffect(() => {
    let cancelled = false;
    async function load() {
      setLoading(true);
      setError('');
      setZmanimMap(null);

      // בדוק cache
      let cached = readCache(city.locationId);
      if (!cached) {
        try {
          const from = todayISO();
          const to   = addDays(from, 30);
          cached = await fetchFromServer(city.locationId, from, to);
          writeCache(city.locationId, cached);
        } catch (err) {
          if (!cancelled) setError('שגיאה בטעינת הזמנים');
          setLoading(false);
          return;
        }
      }

      if (!cancelled) {
        setZmanimMap(cached);
        setLoading(false);
      }
    }
    load();
    return () => { cancelled = true; };
  }, [city.locationId]);

  // הזמנים ליום הנבחר
  const todayZmanim = zmanimMap?.[date] ?? null;

  return (
    <div className="min-h-screen bg-white" dir="rtl">
      <div className="mx-auto max-w-lg px-4 py-8">

        {/* Header */}
        <div className="mb-8 text-center">
          <div className="mb-3 flex justify-center">
            <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-[var(--ink)] text-white">
              <Clock size={26} />
            </div>
          </div>
          <h1 className="text-2xl font-bold text-[var(--ink)]">זמנים הלכתיים</h1>
          <p className="mt-1 text-xs text-[var(--muted)]">לפי שיטת אדמו"ר הזקן · Chabad.org</p>
        </div>

        {/* Controls */}
        <div className="mb-6 flex flex-col gap-3 sm:flex-row">
          <div className="flex flex-1 gap-2">
            {CITIES.map((c, i) => (
              <button
                key={c.locationId}
                onClick={() => setCityIdx(i)}
                className={`flex-1 rounded-full py-2 text-sm font-medium transition ${
                  cityIdx === i
                    ? 'bg-[var(--ink)] text-white'
                    : 'border border-[var(--line)] text-[var(--muted)] hover:bg-gray-50 hover:text-[var(--ink)]'
                }`}
              >
                {c.label}
              </button>
            ))}
          </div>
          <input
            type="date"
            value={date}
            onChange={e => setDate(e.target.value)}
            className="rounded-full border border-[var(--line)] bg-white px-4 py-2 text-sm text-[var(--ink)] focus:outline-none focus:ring-2 focus:ring-[var(--ink)]"
          />
        </div>

        {/* Location badge */}
        <div className="mb-4 flex items-center gap-1.5 text-sm text-[var(--muted)]">
          <MapPin size={14} />
          <span>{city.label} · {formatDateHebrew(date)}</span>
        </div>

        {/* Content */}
        {loading && (
          <div className="flex justify-center py-16">
            <div className="h-8 w-8 animate-spin rounded-full border-2 border-[var(--ink)] border-t-transparent" />
          </div>
        )}

        {error && (
          <div className="rounded-2xl bg-red-50 p-4 text-center text-sm text-red-600">{error}</div>
        )}

        {!loading && !error && todayZmanim === null && zmanimMap && (
          <div className="rounded-2xl bg-yellow-50 p-4 text-center text-sm text-yellow-700">
            אין נתונים לתאריך זה — בחר תאריך בטווח 30 הימים הקרובים
          </div>
        )}

        {!loading && !error && todayZmanim && (
          <div className="overflow-hidden rounded-3xl border border-[var(--line)] bg-white shadow-sm">
            {ZMANIM_ORDER.map(({ type, label }, i) => {
              const zman = todayZmanim.find(z => z.type === type);
              if (!zman) return null;
              return (
                <div
                  key={type}
                  className={`flex items-center justify-between px-5 py-3.5 ${
                    i !== 0 ? 'border-t border-[var(--line)]' : ''
                  }`}
                >
                  <span className="text-base text-[var(--ink)]">{label}</span>
                  <span className="font-mono text-base font-semibold tabular-nums text-[var(--ink)]">
                    {zman.time}
                  </span>
                </div>
              );
            })}
          </div>
        )}

      </div>
    </div>
  );
}
