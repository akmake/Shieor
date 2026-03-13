import React from 'react';

function StudyTextRow({ row, index, showSecondary }) {
  return (
    <div className="mb-8">
      {/* הפסוק (והאונקלוס אם זה שניים מקרא) */}
      <article className="reader-row">
        <div className="reader-index">{index + 1}</div>
        <div className="min-w-0 flex-1">
          {/* הזרקת הפונט SBL Hebrew בגודל מוגדל */}
          {row.he ? <p className="reader-he text-2xl leading-[2.5rem] text-[var(--ink)] font-sbl">{row.he}</p> : null}
          
          {/* אונקלוס (מקבל גם את אותו פונט) */}
          {showSecondary && row.en ? (
            <p className="reader-en mt-2 text-xl font-medium leading-9 text-[var(--muted)] font-sbl">
              {row.en}
            </p>
          ) : null}
        </div>
      </article>

      {/* הרש"י צמוד מיד מתחת לפסוק */}
      {row.rashi && row.rashi.length > 0 && (
        <div className="mt-4 ml-2 mr-10 space-y-3 rounded-xl border border-[var(--line)] bg-[var(--soft)] p-4 shadow-sm sm:mr-12">
          {row.rashi.map((r, rIdx) => (
            <div key={`${row.id}-rashi-${rIdx}`}>
              <span className="text-xs font-bold text-[#b58900] mb-1 block">רש"י:</span>
              {/* הזרקת הפונט BA HaYetzira לרש"י */}
              <p className="text-xl leading-8 text-[var(--ink)] font-rashi">{r.he}</p>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}

export default function StudyReader({ study }) {
  const showSecondary = study.key === 'shnayimMikra';

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
        <p className="mt-1 text-sm text-[var(--muted)]">
          {showSecondary ? 'מקרא ותרגום אונקלוס מוצגים יחד לקריאה רציפה.' : 'הקטע היומי מוצג כאן עם רש"י לכל פסוק (במידה וקיים).'}
        </p>

        {Array.isArray(study.sections) && study.sections.length > 0 ? (
          <div className="mt-5 space-y-2">
            {study.sections.map((row, index) => (
              <StudyTextRow key={row.id || `${index + 1}`} row={row} index={index} showSecondary={showSecondary} />
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