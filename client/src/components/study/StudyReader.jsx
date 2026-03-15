import React from 'react';

const HE_NUMS = ['','א','ב','ג','ד','ה','ו','ז','ח','ט','י','יא','יב','יג','יד','טו','טז','יז','יח','יט','כ','כא','כב','כג','כד','כה','כו','כז','כח','כט','ל','לא','לב','לג','לד','לה','לו','לז','לח','לט','מ','מא','מב','מג','מד','מה','מו','מז','מח','מט','נ','נא','נב','נג','נד','נה','נו','נז','נח','נט','ס'];
function heNum(n) { return (n >= 1 && n < HE_NUMS.length) ? HE_NUMS[n] : String(n); }

function getSettings() {
  try {
    return JSON.parse(localStorage.getItem('shieor-settings') || '{}');
  } catch (_) {
    return {};
  }
}

const TEAL = '#0d9488';

function RashiBox({ rashi, rowId, rashiFontSize }) {
  return (
    <div className="mt-3 rounded-xl border border-[var(--line)] bg-[var(--soft)] px-4 py-3">
      <span style={{ fontSize: 11 }} className="font-bold text-[#b58900] mb-2 block tracking-wide">רש״י</span>
      <div className="space-y-2">
        {rashi.map((r, rIdx) => (
          <p key={`${rowId}-rashi-${rIdx}`} style={{ fontSize: rashiFontSize, textAlign: 'justify' }} className="leading-7 text-[var(--ink)] font-rashi m-0">
            {r.he}
          </p>
        ))}
      </div>
    </div>
  );
}

function StudyTextRow({ row, isShnayimMikra }) {
  if (row.isHeader) {
    return (
      <div className="mt-10 mb-5 text-center">
        <h3 style={{ fontFamily: "'BA HaYetzira', sans-serif", color: TEAL, fontSize: '2rem', fontWeight: 'normal' }}>
          {row.he}
        </h3>
      </div>
    );
  }

  const { fontSize = 20 } = getSettings();
  const rashiFontSize = Math.max(12, fontSize - 4);
  const hasRashi = row.rashi && row.rashi.length > 0;

  // רמב"ם – drop cap עם float כמו בתמונה
  if (row.ordinal) {
    return (
      <div className="mb-7" style={{ overflow: 'hidden' }}>
        <p style={{ fontSize, textAlign: 'justify', lineHeight: '2.6rem', margin: 0 }} className="text-[var(--ink)] font-sbl">
          <span style={{ fontSize: fontSize + 1, fontWeight: '700', color: TEAL, fontFamily: "'BA HaYetzira', sans-serif" }}>
            {row.ordinal}.{'  '}
          </span>
          {row.he}
        </p>
        {hasRashi && <RashiBox rashi={row.rashi} rowId={row.id} rashiFontSize={rashiFontSize} />}
      </div>
    );
  }

  const prefix = row.verseNum != null ? `${heNum(row.verseNum)}.  ` : '';

  return (
    <div className={hasRashi ? 'mb-5' : 'mb-1'}>
      {isShnayimMikra ? (
        <>
          <p style={{ fontSize, textAlign: 'justify' }} className="leading-[2.6rem] text-[var(--ink)] font-sbl">{prefix}{row.he}</p>
          <p style={{ fontSize, textAlign: 'justify' }} className="leading-[2.6rem] text-[var(--ink)] font-sbl">{prefix}{row.he}</p>
          {row.en ? (
            <p style={{ fontSize: rashiFontSize, textAlign: 'justify' }} className="mt-1 leading-8 text-[var(--muted)] font-sbl">{row.en}</p>
          ) : null}
        </>
      ) : (
        <>
          {row.he ? (
            <p style={{ fontSize, textAlign: 'justify' }} className="leading-[2.6rem] text-[var(--ink)] font-sbl">{prefix}{row.he}</p>
          ) : null}
          {hasRashi && <RashiBox rashi={row.rashi} rowId={row.id} rashiFontSize={rashiFontSize} />}
        </>
      )}
    </div>
  );
}

export default function StudyReader({ study }) {
  const isShnayimMikra = study.key === 'shnayimMikra';

  return (
    <div className="space-y-5">
      <section className="glass-panel p-5 sm:p-6">
        <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-[var(--muted)]">{study.kind}</p>
        <h1 className="mt-2 text-3xl font-semibold text-[var(--ink)] sm:text-4xl">{study.title}</h1>
        <p className="mt-3 text-[15px] leading-7 text-[var(--muted)]">{study.subtitle}</p>

        <div className="mt-4 rounded-2xl border border-[var(--line)] bg-white/70 p-4">
          <p className="text-sm font-semibold text-[var(--ink)]">{study.label}</p>
          {study.ref ? <p className="mt-1 text-sm text-[var(--muted)]">{study.ref}</p> : null}
        </div>

        {Array.isArray(study.rules) && study.rules.length > 0 ? (
          <div className="mt-4 rounded-2xl border border-[var(--line)] bg-[var(--soft)] p-4">
            <h2 className="text-sm font-semibold text-[var(--ink)]">כללי מסלול</h2>
            <ul className="mt-2 space-y-2 text-sm leading-7 text-[var(--muted)]">
              {study.rules.map((rule) => (
                <li key={rule}>• {rule}</li>
              ))}
            </ul>
          </div>
        ) : null}
      </section>

      <section className="glass-panel p-5 sm:p-6">
        <h2 className="text-2xl font-semibold text-[var(--ink)]">תוכן הלימוד של היום</h2>
        {isShnayimMikra && (
          <p className="mt-1 text-sm text-[var(--muted)]">כל פסוק מוצג פעמיים בעברית ופעם אחת תרגום אונקלוס.</p>
        )}

        {Array.isArray(study.sections) && study.sections.length > 0 ? (
          <div className="mt-5">
            {study.sections.map((row, index) => (
              <StudyTextRow
                key={row.id || `${index + 1}`}
                row={row}
                isShnayimMikra={isShnayimMikra}
              />
            ))}
          </div>
        ) : (
          <div className="mt-5 rounded-2xl border border-dashed border-[var(--line)] bg-[var(--soft)] p-5 text-sm text-[var(--muted)]">
            לא חזר תוכן טקסטואלי עבור היום הזה.
          </div>
        )}
      </section>
    </div>
  );
}
