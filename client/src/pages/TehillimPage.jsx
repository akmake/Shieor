import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowRight, Play, Pause } from 'lucide-react';
import { getDailyStudy, normalizeDate, shiftDate } from '../utils/study';

const TEAL = '#0d9488';

function getSettings() {
  try { return { fontSize: 20, scrollSpeed: 40, ...JSON.parse(localStorage.getItem('shieor-settings') || '{}') }; }
  catch (_) { return { fontSize: 20, scrollSpeed: 40 }; }
}

function TehillimChapter({ header, body, fontSize }) {
  return (
    <div style={{ marginBottom: '2.5rem' }}>
      {/* כותרת פרק */}
      <div style={{
        textAlign: 'center',
        padding: '1rem 0 0.75rem',
        borderBottom: `2px solid ${TEAL}22`,
        marginBottom: '1rem',
      }}>
        <span style={{
          color: TEAL,
          fontSize: Math.max(fontSize + 8, 26),
          fontWeight: '700',
          letterSpacing: '0.02em',
        }} className="font-sbl">
          {header}
        </span>
      </div>

      {/* גוף הפרק — טקסט זורם */}
      {body && (
        <p style={{
          fontSize,
          lineHeight: 2.1,
          textAlign: 'justify',
          margin: 0,
          color: 'var(--ink)',
          wordSpacing: '0.05em',
        }} className="font-sbl">
          {body}
        </p>
      )}
    </div>
  );
}

export default function TehillimPage() {
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({ loading: true, error: '', data: null });
  const [autoScroll, setAutoScroll] = useState(false);
  const scrollIntervalRef = useRef(null);

  const date = normalizeDate(searchParams.get('date'));
  const scrollKey = `shieor-pos-tehillim-${date}`;

  useEffect(() => {
    let alive = true;
    async function load() {
      setState({ loading: true, error: '', data: null });
      try {
        const data = await getDailyStudy(date);
        if (!alive) return;
        setState({ loading: false, error: '', data });
      } catch (_) {
        if (!alive) return;
        setState({ loading: false, error: 'לא הצלחנו לטעון את התהילים כרגע.', data: null });
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

  // שחזור מיקום
  useEffect(() => {
    if (state.loading) return;
    const saved = localStorage.getItem(scrollKey);
    if (saved) setTimeout(() => window.scrollTo({ top: parseInt(saved, 10), behavior: 'instant' }), 100);
  }, [state.loading, scrollKey]);

  // גלילה אוטומטית
  useEffect(() => {
    if (autoScroll) {
      const speed = getSettings().scrollSpeed;
      const msPerPx = Math.round(1000 / speed);
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

  const study = state.data?.studies?.tehillim;
  const sections = study?.sections || [];
  const { fontSize } = getSettings();

  // Group sections into chapters: each chapter = { header, body }
  const chapters = [];
  for (const row of sections) {
    if (row.isHeader) {
      chapters.push({ header: row.he, body: '' });
    } else if (chapters.length > 0) {
      chapters[chapters.length - 1].body += (chapters[chapters.length - 1].body ? ' ' : '') + (row.he || '');
    } else {
      chapters.push({ header: '', body: row.he || '' });
    }
  }

  return (
    <div className="mx-auto w-full max-w-3xl px-4 pb-16 pt-6 sm:px-6">
      {/* ── בר ניווט עליון ── */}
      <div className="flex items-center gap-3 flex-wrap mb-5">
        <Link
          to={`/?date=${date}`}
          className="inline-flex items-center gap-2 rounded-full border border-[var(--line)] bg-white/80 px-4 py-2 text-sm font-medium text-[var(--ink)] shadow-sm"
        >
          <ArrowRight size={16} />
          חזרה
        </Link>

        <button
          onClick={() => setAutoScroll(v => !v)}
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

      {/* ── כותרת ── */}
      {study && (
        <div style={{ textAlign: 'center', marginBottom: '1.5rem' }}>
          <h1 style={{ fontSize: '1.75rem', fontWeight: '700', color: TEAL, margin: '0 0 0.25rem' }}>
            {study.title || 'תהלים יומי'}
          </h1>
          {study.label && (
            <p style={{ fontSize: '1rem', color: 'var(--muted)', margin: 0 }}>
              {study.label}
            </p>
          )}
        </div>
      )}

      {/* ── תוכן ── */}
      {state.loading && (
        <div className="glass-panel p-6 text-center text-[var(--muted)]">טוען תהלים...</div>
      )}

      {!state.loading && state.error && (
        <div className="glass-panel p-6 text-center text-[var(--ink)]">{state.error}</div>
      )}

      {!state.loading && !state.error && chapters.length > 0 && (
        <section className="glass-panel p-5 sm:p-8">
          {chapters.map((ch, i) => (
            <TehillimChapter key={i} header={ch.header} body={ch.body} fontSize={fontSize} />
          ))}
        </section>
      )}

      {!state.loading && !state.error && chapters.length === 0 && (
        <div className="glass-panel p-6 text-center text-[var(--muted)]">לא נמצא תהלים עבור היום הזה.</div>
      )}
    </div>
  );
}
