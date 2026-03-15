import React from 'react';

const TEAL = '#0d9488';

const HE_NUMS = [
  '', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט', 'י',
  'יא', 'יב', 'יג', 'יד', 'טו', 'טז', 'יז', 'יח', 'יט', 'כ',
  'כא', 'כב', 'כג', 'כד', 'כה', 'כו', 'כז', 'כח', 'כט', 'ל',
  'לא', 'לב', 'לג', 'לד', 'לה', 'לו', 'לז', 'לח', 'לט', 'מ',
  'מא', 'מב', 'מג', 'מד', 'מה', 'מו', 'מז', 'מח', 'מט', 'נ',
  'נא', 'נב', 'נג', 'נד', 'נה', 'נו', 'נז', 'נח', 'נט', 'ס',
];

function heNum(n) {
  return n >= 1 && n < HE_NUMS.length ? HE_NUMS[n] : String(n);
}

function getSettings() {
  try { return { fontSize: 20, scrollSpeed: 40, shnayimMikraConnected: true, ...JSON.parse(localStorage.getItem('shieor-settings') || '{}') }; }
  catch (_) { return { fontSize: 20, scrollSpeed: 40, shnayimMikraConnected: true }; }
}

// ─── sub-components ──────────────────────────────────────────────────────────

function AliyahHeader({ text }) {
  return (
    <div style={{ textAlign: 'center', padding: '1.75rem 0 0.5rem' }}>
      <span style={{ color: TEAL, fontSize: '1.3rem', fontWeight: '600', letterSpacing: '0.04em' }} className="font-sbl">
        {text}
      </span>
    </div>
  );
}

function ChapterHeader({ text }) {
  return (
    <div style={{ textAlign: 'center', padding: '0.75rem 0 0.4rem' }}>
      <span style={{ color: TEAL, fontSize: '1rem', fontWeight: '400', opacity: 0.8 }} className="font-sbl">
        {text}
      </span>
    </div>
  );
}

function RashiBlock({ rashi, rowId, rashiFontSize }) {
  return (
    <div style={{
      marginTop: '0.35rem',
      marginBottom: '0.2rem',
      paddingRight: '0.75rem',
      borderRight: `2.5px solid ${TEAL}`,
      opacity: 0.92,
    }}>
      <span style={{ fontSize: 11, fontWeight: '700', color: '#b58900', letterSpacing: '0.08em', display: 'block', marginBottom: '0.2rem' }}>
        רש״י
      </span>
      {rashi.map((r, i) => (
        <p
          key={`${rowId}-r${i}`}
          style={{ fontSize: rashiFontSize, lineHeight: 1.75, margin: i < rashi.length - 1 ? '0 0 0.35rem 0' : 0, textAlign: 'justify' }}
          className="font-sbl text-[var(--ink)]"
        >
          {r.he}
        </p>
      ))}
    </div>
  );
}

// הלכה ברמב"ם
function HalachaRow({ row, fontSize, rashiFontSize }) {
  const hasRashi = row.rashi?.length > 0;
  return (
    <div style={{ marginBottom: '0.85rem' }}>
      <p
        style={{ fontSize, lineHeight: 1.75, textAlign: 'justify', margin: 0 }}
        className="font-sbl text-[var(--ink)]"
      >
        <span style={{ fontWeight: '700', color: TEAL, fontSize: fontSize + 1 }}>
          {row.ordinal}{'\u00A0'}
        </span>
        {row.he}
      </p>
      {hasRashi && <RashiBlock rashi={row.rashi} rowId={row.id} rashiFontSize={rashiFontSize} />}
    </div>
  );
}

// פסוק בחומש / שניים מקרא / תניא
function VerseRow({ row, fontSize, rashiFontSize, isShnayimMikra, shnayimMikraConnected }) {
  const hasRashi = row.rashi?.length > 0;
  const prefix = row.verseNum != null ? `${heNum(row.verseNum)}.\u00A0` : '';

  if (isShnayimMikra) {
    if (shnayimMikraConnected) {
      // רצוף: שני פסוקים + תרגום בשורה אחת
      return (
        <div style={{ marginBottom: '0.9rem' }}>
          <p style={{ fontSize, lineHeight: 1.75, textAlign: 'justify', margin: 0 }} className="font-sbl text-[var(--ink)]">
            {prefix}{row.he}{' '}
            {prefix}{row.he}
            {row.en ? <span style={{ color: 'var(--muted)' }}>{' '}{row.en}</span> : null}
          </p>
        </div>
      );
    }
    // מופרד: כל שורה בנפרד
    return (
      <div style={{ marginBottom: '0.9rem' }}>
        <p style={{ fontSize, lineHeight: 1.75, textAlign: 'justify', margin: '0 0 0.1rem 0' }} className="font-sbl text-[var(--ink)]">{prefix}{row.he}</p>
        <p style={{ fontSize, lineHeight: 1.75, textAlign: 'justify', margin: '0 0 0.1rem 0' }} className="font-sbl text-[var(--ink)]">{prefix}{row.he}</p>
        {row.en ? <p style={{ fontSize, lineHeight: 1.75, textAlign: 'justify', margin: 0, color: 'var(--muted)' }} className="font-sbl">{row.en}</p> : null}
      </div>
    );
  }

  return (
    <div style={{ marginBottom: hasRashi ? '0.75rem' : '0.15rem' }}>
      <p style={{ fontSize, lineHeight: 1.75, textAlign: 'justify', margin: 0 }} className="font-sbl text-[var(--ink)]">
        {prefix}{row.he}
      </p>
      {hasRashi && <RashiBlock rashi={row.rashi} rowId={row.id} rashiFontSize={rashiFontSize} />}
    </div>
  );
}

// ─── main export ─────────────────────────────────────────────────────────────

export default function StudyReader({ study }) {
  const { fontSize = 20, shnayimMikraConnected = true } = getSettings();
  const rashiFontSize = Math.max(14, fontSize - 3);
  const isShnayimMikra = study.key === 'shnayimMikra';

  return (
    <div>

      {/* ── כותרת קטנה ── */}
      {(study.label || study.title) ? (
        <p style={{ fontSize: 13, fontWeight: 600, color: 'var(--muted)', marginBottom: '1.25rem', letterSpacing: '0.04em' }}>
          {study.label || study.title}
        </p>
      ) : null}

      {/* ── תוכן הלימוד ── */}
      {Array.isArray(study.sections) && study.sections.length > 0 ? (
        <div>
          {study.sections.map((row, index) => {
            if (row.isHeader) {
              return row.isAliyahHeader
                ? <AliyahHeader key={row.id ?? index} text={row.he} />
                : <ChapterHeader key={row.id ?? index} text={row.he} />;
            }
            if (row.ordinal) {
              return <HalachaRow key={row.id ?? index} row={row} fontSize={fontSize} rashiFontSize={rashiFontSize} />;
            }
            return <VerseRow key={row.id ?? index} row={row} fontSize={fontSize} rashiFontSize={rashiFontSize} isShnayimMikra={isShnayimMikra} shnayimMikraConnected={shnayimMikraConnected} />;
          })}
        </div>
      ) : (
        <p style={{ fontSize: 14, color: 'var(--muted)', textAlign: 'center', padding: '2rem 0' }}>
          לא חזר תוכן עבור היום הזה.
        </p>
      )}

    </div>
  );
}
