function StudyTextRow({ row, index, showSecondary }) {
  return (
    <article className="reader-row">
      <div className="reader-index">{index + 1}</div>
      <div className="min-w-0 flex-1">
        {row.he ? <p className="reader-he">{row.he}</p> : null}
        {showSecondary && row.en ? <p className="reader-en">{row.en}</p> : null}
      </div>
    </article>
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
          {showSecondary ? 'מקרא וטקסט תרגום מוצגים יחד לקריאה רציפה.' : 'הקטע היומי מוצג כאן ישירות בתוך האפליקציה.'}
        </p>

        {Array.isArray(study.sections) && study.sections.length > 0 ? (
          <div className="mt-5 space-y-4">
            {study.sections.map((row, index) => (
              <StudyTextRow key={row.id || `${index + 1}`} row={row} index={index} showSecondary={showSecondary} />
            ))}
          </div>
        ) : (
          <div className="mt-5 rounded-2xl border border-dashed border-[var(--line)] bg-[var(--soft)] p-5 text-sm text-[var(--muted)]">
            לא חזר תוכן טקסטואלי עבור היום הזה. המיפוי כבר מחזיר את הייחוס היומי, ונוכל לדייק עוד את גרסת הטקסט אם תרצה.
          </div>
        )}
      </section>

      {Array.isArray(study.commentary) && study.commentary.length > 0 ? (
        <section className="glass-panel p-5 sm:p-6">
          <h2 className="text-2xl font-semibold text-[var(--ink)]">רש"י</h2>
          <div className="mt-4 space-y-4">
            {study.commentary.map((entry) => (
              <article key={entry.id} className="rounded-2xl border border-[var(--line)] bg-white/80 p-4 shadow-sm">
                <p className="text-sm font-semibold text-[var(--ink)]">
                  {entry.author || 'רש"י'} {entry.ref ? `· ${entry.ref}` : ''}
                </p>
                {entry.he ? <p className="mt-3 text-lg leading-9 text-[var(--ink)]">{entry.he}</p> : null}
                {entry.en ? <p className="mt-3 text-sm leading-7 text-[var(--muted)]">{entry.en}</p> : null}
              </article>
            ))}
          </div>
        </section>
      ) : null}
    </div>
  );
}
