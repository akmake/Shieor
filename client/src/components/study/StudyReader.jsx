import React from 'react';

function getSettings() {
  try {
    return JSON.parse(localStorage.getItem('shieor-settings') || '{}');
  } catch (_) {
    return {};
  }
}

function StudyTextRow({ row, isShnayimMikra }) {
  if (row.isHeader) {
    return (
      <div className="mt-8 mb-3 pb-2 border-b-2 border-[var(--line)]">
        <h3 className="text-lg font-bold text-[var(--ink)]">{row.he}</h3>
      </div>
    );
  }

  const { fontSize = 20 } = getSettings();
  const rashiFontSize = Math.max(12, fontSize - 4);

  return (
    <div className="mb-6">
      {isShnayimMikra ? (
        <>
          <p style={{ fontSize }} className="leading-[2.6rem] text-[var(--ink)] font-sbl">{row.he}</p>
          <p style={{ fontSize }} className="mt-1 leading-[2.6rem] text-[var(--ink)] font-sbl">{row.he}</p>
          {row.en ? (
            <p style={{ fontSize: rashiFontSize }} className="mt-2 leading-8 text-[var(--muted)] font-sbl">{row.en}</p>
          ) : null}
        </>
      ) : (
        <>
          {row.he ? (
            <p style={{ fontSize }} className="leading-[2.6rem] text-[var(--ink)] font-sbl">{row.he}</p>
          ) : null}

          {row.rashi && row.rashi.length > 0 && (
            <div className="mt-3 rounded-xl border border-[var(--line)] bg-[var(--soft)] px-4 py-3">
              <span style={{ fontSize: 11 }} className="font-bold text-[#b58900] mb-2 block tracking-wide">רש״י</span>
              <div className="space-y-2">
                {row.rashi.map((r, rIdx) => (
                  <p key={`${row.id}-rashi-${rIdx}`} style={{ fontSize: rashiFontSize }} className="leading-7 text-[var(--ink)] font-rashi">
                    {r.he}
                  </p>
                ))}
              </div>
            </div>
          )}
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
