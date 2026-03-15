import { useState, useEffect } from 'react';
import { Clock, MapPin } from 'lucide-react';

const CITIES = [
  { label: 'תל אביב', geonameid: 293397 },
  { label: 'ירושלים', geonameid: 281184 },
  { label: 'חיפה',    geonameid: 294801 },
  { label: 'באר שבע', geonameid: 1024898 },
];

const ZMANIM = [
  { key: 'alotHaShachar',     label: 'עלות השחר' },
  { key: 'misheyakir',        label: 'משיכיר' },
  { key: 'sunrise',           label: 'הנץ החמה' },
  { key: 'sofZmanShmaMGA',    label: 'סוף זמן ק"ש (מג"א)' },
  { key: 'sofZmanShma',       label: 'סוף זמן ק"ש (גר"א)' },
  { key: 'sofZmanTfillaMGA',  label: 'סוף זמן תפילה (מג"א)' },
  { key: 'sofZmanTfilla',     label: 'סוף זמן תפילה (גר"א)' },
  { key: 'chatzot',           label: 'חצות היום' },
  { key: 'minchaGedola',      label: 'מנחה גדולה' },
  { key: 'minchaKetana',      label: 'מנחה קטנה' },
  { key: 'plagHaMincha',      label: 'פלג המנחה' },
  { key: 'sunset',            label: 'שקיעת החמה' },
  { key: 'beinHaShmashos',    label: 'בין השמשות' },
  { key: 'tzeit',             label: 'צאת הכוכבים' },
  { key: 'chatzotNight',      label: 'חצות הלילה' },
];

function todayISO() {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function formatTime(isoString) {
  if (!isoString) return null;
  const d = new Date(isoString);
  return d.toLocaleTimeString('he-IL', { hour: '2-digit', minute: '2-digit', hour12: false });
}

function formatDateHebrew(dateStr) {
  const [y, m, d] = dateStr.split('-').map(Number);
  const date = new Date(y, m - 1, d);
  return date.toLocaleDateString('he-IL', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
}

export default function ZmanimPage() {
  const [cityIdx, setCityIdx] = useState(0);
  const [date, setDate] = useState(todayISO());
  const [times, setTimes] = useState(null);
  const [hebrewDate, setHebrewDate] = useState('');
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');

  useEffect(() => {
    const city = CITIES[cityIdx];
    setLoading(true);
    setError('');
    setTimes(null);
    fetch(`https://www.hebcal.com/zmanim?cfg=json&geonameid=${city.geonameid}&date=${date}`)
      .then(r => r.json())
      .then(data => {
        setTimes(data.times || {});
        setHebrewDate(data.date?.hebrew || '');
      })
      .catch(() => setError('שגיאה בטעינת הזמנים, נסה שוב'))
      .finally(() => setLoading(false));
  }, [cityIdx, date]);

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
          {hebrewDate && <p className="mt-1 text-sm text-[var(--muted)]">{hebrewDate}</p>}
        </div>

        {/* Controls */}
        <div className="mb-6 flex flex-col gap-3 sm:flex-row">
          {/* City selector */}
          <div className="flex flex-1 gap-2">
            {CITIES.map((city, i) => (
              <button
                key={city.geonameid}
                onClick={() => setCityIdx(i)}
                className={`flex-1 rounded-full py-2 text-sm font-medium transition ${
                  cityIdx === i
                    ? 'bg-[var(--ink)] text-white'
                    : 'border border-[var(--line)] text-[var(--muted)] hover:bg-gray-50 hover:text-[var(--ink)]'
                }`}
              >
                {city.label}
              </button>
            ))}
          </div>
          {/* Date picker */}
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
          <span>{CITIES[cityIdx].label} · {formatDateHebrew(date)}</span>
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

        {!loading && !error && times && (
          <div className="overflow-hidden rounded-3xl border border-[var(--line)] bg-white shadow-sm">
            {ZMANIM.map(({ key, label }, i) => {
              const val = formatTime(times[key]);
              if (!val) return null;
              return (
                <div
                  key={key}
                  className={`flex items-center justify-between px-5 py-3.5 ${
                    i !== 0 ? 'border-t border-[var(--line)]' : ''
                  }`}
                >
                  <span className="text-base text-[var(--ink)]">{label}</span>
                  <span className="font-mono text-base font-semibold tabular-nums text-[var(--ink)]">{val}</span>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
