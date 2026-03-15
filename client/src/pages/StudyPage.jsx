import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowRight, Play, Pause } from 'lucide-react';
import StudyReader from '../components/study/StudyReader';
import { getDailyStudy, normalizeDate, shiftDate } from '../utils/study';

function getScrollSpeed() {
  try {
    return JSON.parse(localStorage.getItem('shieor-settings') || '{}').scrollSpeed ?? 40;
  } catch (_) {
    return 40;
  }
}

export default function StudyPage({ studyKey }) {
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({ loading: true, error: '', data: null });
  const [autoScroll, setAutoScroll] = useState(false);
  const scrollIntervalRef = useRef(null);

  const date = normalizeDate(searchParams.get('date'));
  const isShnayimMikra = studyKey === 'shnayimMikra';
  // שניים מקרא: scroll key לפי ראשון של השבוע כדי לשמור מיקום לאורך כל הפרשה
  const weekSunday = (() => {
    const [y, m, d] = date.split('-').map(Number);
    const dow = new Date(Date.UTC(y, m - 1, d)).getUTCDay();
    return shiftDate(date, -dow);
  })();
  const scrollKey = `shieor-pos-${studyKey}-${isShnayimMikra ? weekSunday : date}`;

  useEffect(() => {
    let alive = true;
    async function load() {
      setState((prev) => ({ ...prev, loading: true, error: '' }));
      try {
        const data = await getDailyStudy(date);
        if (!alive) return;
        setState({ loading: false, error: '', data });
      } catch (_) {
        if (!alive) return;
        setState({ loading: false, error: 'לא הצלחנו לטעון את הלימוד היומי כרגע.', data: null });
      }
    }
    load();
    return () => { alive = false; };
  }, [date]);

  // שמירת מיקום גלילה
  useEffect(() => {
    const handleScroll = () => localStorage.setItem(scrollKey, String(window.scrollY));
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [scrollKey]);

  // שחזור מיקום גלילה לאחר טעינת תוכן
  useEffect(() => {
    if (state.loading) return;
    const saved = localStorage.getItem(scrollKey);
    if (saved) setTimeout(() => window.scrollTo({ top: parseInt(saved, 10), behavior: 'instant' }), 400);
  }, [state.loading, scrollKey]);

  // גלילה אוטומטית
  useEffect(() => {
    if (autoScroll) {
      const msPerPx = Math.round(1000 / getScrollSpeed());
      scrollIntervalRef.current = setInterval(() => {
        window.scrollBy({ top: 1, behavior: 'instant' });
        if (window.scrollY + window.innerHeight >= document.body.scrollHeight - 10) {
          setAutoScroll(false);
        }
      }, msPerPx);
    } else {
      clearInterval(scrollIntervalRef.current);
    }
    return () => clearInterval(scrollIntervalRef.current);
  }, [autoScroll]);

  const study = state.data?.studies?.[studyKey];

  return (
    <div className="mx-auto w-full max-w-5xl px-4 pb-16 pt-6 sm:px-6 lg:px-8">
      <div className="flex items-center gap-3 flex-wrap">
        <Link
          to={`/?date=${date}`}
          className="inline-flex items-center gap-2 rounded-full border border-[var(--line)] bg-white/80 px-4 py-2 text-sm font-medium text-[var(--ink)] shadow-sm"
        >
          <ArrowRight size={16} />
          חזרה
        </Link>

        <button
          onClick={() => setAutoScroll((v) => !v)}
          className={`inline-flex items-center gap-2 rounded-full border px-4 py-2 text-sm font-medium shadow-sm transition ${
            autoScroll
              ? 'border-[var(--brand)] bg-[var(--brand)] text-white'
              : 'border-[var(--line)] bg-white/80 text-[var(--ink)]'
          }`}
        >
          {autoScroll ? <Pause size={14} /> : <Play size={14} />}
          {autoScroll ? 'עצור' : 'גלילה אוטומטית'}
        </button>
      </div>

{state.loading ? <div className="glass-panel mt-4 p-6 text-center text-[var(--muted)]">טוען תוכן...</div> : null}
      {!state.loading && state.error ? <div className="glass-panel mt-4 p-6 text-center text-[var(--ink)]">{state.error}</div> : null}
      {!state.loading && !state.error && study ? <div className="mt-4"><StudyReader study={study} /></div> : null}
    </div>
  );
}
