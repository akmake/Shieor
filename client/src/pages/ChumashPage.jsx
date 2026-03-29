import { useEffect, useRef, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { ArrowRight, Play, Pause } from 'lucide-react';
import { getDailyStudy, normalizeDate } from '../utils/study';

const TEAL = '#0d9488';
const GOLD = '#b58900';

const HE_NUMS = [
  '', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י',
  'יא', 'יב', 'יג', 'יד', 'טו', 'טז', 'יז', 'יח', 'יט', 'כ',
  'כא', 'כב', 'כג', 'כד', 'כה', 'כו', 'כז', 'כח', 'כט', 'ל',
  'לא', 'לב', 'לג', 'לד', 'לה', 'לו', 'לז', 'לח', 'לט', 'מ',
  'מא', 'מב', 'מג', 'מד', 'מה', 'מו', 'מז', 'מח', 'מט', 'נ',
  'נא', 'נב', 'נג', 'נד', 'נה', 'נו', 'נז', 'נח', 'נט', 'ס',
  'סא', 'סב', 'סג', 'סד', 'סה', 'סו', 'סז', 'סח', 'סט', 'ע',
  'עא', 'עב', 'עג', 'עד', 'עה', 'עו',
];

function heNum(n) {
  return n >= 1 && n < HE_NUMS.length ? HE_NUMS[n] : String(n);
}

function getSettings() {
  try { return { fontSize: 20, scrollSpeed: 40, ...JSON.parse(localStorage.getItem('shieor-settings') || '{}') }; }
  catch (_) { return { fontSize: 20, scrollSpeed: 40 }; }
}

/* ═══════════════════════ כותרת עלייה ═══════════════════════ */
function AliyahHeader({ text }) {
  return (
    <div style={{
      textAlign: 'center',
      padding: '2rem 0 1rem',
    }}>
      <div style={{
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        gap: '1rem',
      }}>
        <span style={{
          flex: '0 1 80px',
          height: '1px',
          background: `linear-gradient(to left, ${TEAL}44, transparent)`,
        }} />
        <span style={{
          color: TEAL,
          fontSize: '1.2rem',
          fontWeight: '700',
          letterSpacing: '0.06em',
        }} className="font-sbl">
          {text}
        </span>
        <span style={{
          flex: '0 1 80px',
          height: '1px',
          background: `linear-gradient(to right, ${TEAL}44, transparent)`,
        }} />
      </div>
    </div>
  );
}

/* ═══════════════════════ כותרת פרק ═══════════════════════ */
function ChapterHeader({ text }) {
  return (
    <div style={{
      textAlign: 'center',
      padding: '0.75rem 0 0.5rem',
    }}>
      <span style={{
        color: TEAL,
        fontSize: '1.85rem',
        fontWeight: '700',
      }} className="font-sbl">
        {text}
      </span>
    </div>
  );
}

/* ═══════════════════════ בלוק רש"י ═══════════════════════ */
function RashiBlock({ rashi, rashiFontSize }) {
  return (
    <div style={{
      marginTop: '0.5rem',
      marginBottom: '0.25rem',
      paddingRight: '0.85rem',
      paddingLeft: '0.5rem',
      paddingTop: '0.5rem',
      paddingBottom: '0.4rem',
      borderRight: `3px solid ${GOLD}99`,
      background: `linear-gradient(to left, ${GOLD}08, transparent 70%)`,
      borderRadius: '0 6px 6px 0',
    }}>
      <span style={{
        fontSize: 11,
        fontWeight: '700',
        color: GOLD,
        letterSpacing: '0.1em',
        display: 'block',
        marginBottom: '0.3rem',
        userSelect: 'none',
      }}>
        רש״י
      </span>
      {rashi.map((r, i) => (
        <p
          key={`r-${i}`}
          style={{
            fontSize: rashiFontSize,
            lineHeight: 1.75,
            margin: i < rashi.length - 1 ? '0 0 0.4rem 0' : 0,
            textAlign: 'justify',
          }}
          className="font-sbl text-[var(--ink)]"
        >
          {r.he}
        </p>
      ))}
    </div>
  );
}

/* ═══════════════════════ שורת פסוק ═══════════════════════ */
function VerseRow({ row, fontSize, rashiFontSize }) {
  const hasRashi = row.rashi?.length > 0;
  return (
    <div style={{
      marginBottom: hasRashi ? '1.25rem' : '0.3rem',
      paddingBottom: hasRashi ? '0.75rem' : 0,
      borderBottom: hasRashi ? `1px solid ${TEAL}10` : 'none',
    }}>
      {/* פסוק */}
      <p style={{
        fontSize,
        lineHeight: 2,
        textAlign: 'justify',
        margin: 0,
      }} className="font-sbl text-[var(--ink)]">
        {row.verseNum != null && (
          <span style={{
            color: TEAL,
            fontSize: fontSize - 2,
            fontWeight: '700',
            marginLeft: '0.35rem',
          }}>
            {heNum(row.verseNum)}
          </span>
        )}
        {row.he}
      </p>

      {/* רש"י */}
      {hasRashi && (
        <RashiBlock rashi={row.rashi} rashiFontSize={rashiFontSize} />
      )}
    </div>
  );
}

/* ═══════════════════════ עמוד חומש ═══════════════════════ */
export default function ChumashPage() {
  const [searchParams] = useSearchParams();
  const [state, setState] = useState({ loading: true, error: '', data: null });
  const [autoScroll, setAutoScroll] = useState(false);
  const scrollIntervalRef = useRef(null);

  const date = normalizeDate(searchParams.get('date'));
  const scrollKey = `shieor-pos-chumash-${date}`;

  /* טעינת נתונים */
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
        setState({ loading: false, error: 'לא הצלחנו לטעון את לימוד החומש כרגע.', data: null });
      }
    }
    load();
    return () => { alive = false; };
  }, [date]);

  /* שמירת מיקום גלילה */
  useEffect(() => {
    const handleScroll = () => localStorage.setItem(scrollKey, String(window.scrollY));
    window.addEventListener('scroll', handleScroll, { passive: true });
    return () => window.removeEventListener('scroll', handleScroll);
  }, [scrollKey]);

  /* שחזור מיקום גלילה */
  useEffect(() => {
    if (state.loading) return;
    const saved = localStorage.getItem(scrollKey);
    if (saved) setTimeout(() => window.scrollTo({ top: parseInt(saved, 10), behavior: 'instant' }), 100);
  }, [state.loading, scrollKey]);

  /* גלילה אוטומטית */
  useEffect(() => {
    if (autoScroll) {
      const speed = getSettings().scrollSpeed;
      const msPerPx = Math.round(1000 / speed);
      scrollIntervalRef.current = setInterval(() => {
        window.scrollBy({ top: 1, behavior: 'instant' });
        if (window.scrollY + window.innerHeight >= document.body.scrollHeight - 10)
          setAutoScroll(false);
      }, msPerPx);
    } else {
      clearInterval(scrollIntervalRef.current);
    }
    return () => clearInterval(scrollIntervalRef.current);
  }, [autoScroll]);

  const study = state.data?.studies?.chumash;
  const sections = study?.sections || [];
  const { fontSize } = getSettings();
  const rashiFontSize = Math.max(14, fontSize - 3);

  return (
    <div className="mx-auto w-full max-w-3xl px-4 pb-16 pt-6 sm:px-6">

      {/* ── ניווט עליון ── */}
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

      {/* ── כותרת פרשה ── */}
      {study && (
        <div style={{ textAlign: 'center', marginBottom: '1.25rem' }}>
          <h1 style={{
            fontSize: '1.75rem',
            fontWeight: '700',
            color: TEAL,
            margin: '0 0 0.2rem',
          }} className="font-sbl">
            {study.title || 'חומש עם רש"י'}
          </h1>
          {study.label && (
            <p style={{ fontSize: '1.05rem', color: 'var(--muted)', margin: 0 }} className="font-sbl">
              {study.label}
            </p>
          )}
          {study.ref && (
            <p style={{ fontSize: '0.85rem', color: 'var(--muted)', margin: '0.15rem 0 0', opacity: 0.7 }}>
              {study.ref}
            </p>
          )}
        </div>
      )}

      {/* ── תוכן ── */}
      {state.loading && (
        <div className="glass-panel p-6 text-center text-[var(--muted)]">טוען חומש...</div>
      )}

      {!state.loading && state.error && (
        <div className="glass-panel p-6 text-center text-[var(--ink)]">{state.error}</div>
      )}

      {!state.loading && !state.error && sections.length > 0 && (
        <section className="glass-panel p-5 sm:p-8">
          {sections.map((row, idx) => {
            if (row.isHeader) {
              return row.isAliyahHeader
                ? <AliyahHeader key={row.id ?? idx} text={row.he} />
                : <ChapterHeader key={row.id ?? idx} text={row.he} />;
            }
            return (
              <VerseRow
                key={row.id ?? idx}
                row={row}
                fontSize={fontSize}
                rashiFontSize={rashiFontSize}
              />
            );
          })}
        </section>
      )}

      {!state.loading && !state.error && sections.length === 0 && (
        <div className="glass-panel p-6 text-center text-[var(--muted)]">לא נמצא תוכן חומש עבור היום הזה.</div>
      )}
    </div>
  );
}
