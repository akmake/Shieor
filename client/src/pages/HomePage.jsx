import { useEffect, useState } from 'react';
import { useSearchParams } from 'react-router-dom';
import DateNavigator from '../components/study/DateNavigator';
import StudyCard from '../components/study/StudyCard';
import { getDailyStudy, normalizeDate, shiftDate } from '../utils/study';

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [state, setState] = useState({
    loading: true,
    error: '',
    data: null,
  });

  const date = normalizeDate(searchParams.get('date'));

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
        setState({ loading: false, error: 'לא הצלחנו לטעון את הדף היומי כרגע.', data: null });
      }
    }

    load();
    return () => {
      alive = false;
    };
  }, [date]);

  const studies = state.data?.studies ? Object.entries(state.data.studies) : [];

  return (
    <div className="mx-auto w-full max-w-6xl px-4 pb-16 pt-6 sm:px-6 lg:px-8">
      <section className="hero-panel p-6 sm:p-8">
        <div className="hero-bubble hero-bubble-a" />
        <div className="hero-bubble hero-bubble-b" />
        <div className="relative z-10 grid gap-6 lg:grid-cols-[1.3fr_0.7fr]">
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.32em] text-[var(--brand)]">Shieor</p>
            <h1 className="mt-4 max-w-3xl text-4xl font-semibold leading-[1.05] text-[var(--ink)] sm:text-5xl lg:text-6xl">
              לומדים יומי, בצורה חכמה וברורה.
            </h1>
            <p className="mt-5 max-w-2xl text-base leading-8 text-[var(--muted)] sm:text-lg">
              תאריך אחד שולט בהכל, וכל מסלול מחזיר את התוכן שלו לפי הכללים שלו: חומש עם רש&quot;י,
              רמב&quot;ם 3 פרקים, תניא יומי ושניים מקרא ואחד תרגום.
            </p>
          </div>
          <div className="glass-panel p-4 sm:p-5">
            <p className="text-sm font-medium text-[var(--muted)]">מה כבר מחובר</p>
            <div className="mt-3 space-y-2 text-sm leading-7 text-[var(--ink)]">
              <p>ניווט קדימה ואחורה בין ימים.</p>
              <p>תוכן לימוד אמיתי בתוך האתר.</p>
              <p>תצוגה Mobile-first עם טיפוגרפיה נקייה.</p>
            </div>
          </div>
        </div>
      </section>

      <div className="mt-5">
        <DateNavigator
          date={date}
          hebrewDate={state.data?.hebrewDate}
          onPrev={() => setSearchParams({ date: shiftDate(date, -1) })}
          onNext={() => setSearchParams({ date: shiftDate(date, 1) })}
        />
      </div>

      {state.loading ? <div className="glass-panel mt-5 p-6 text-center text-[var(--muted)]">טוען לימודי היום...</div> : null}
      {!state.loading && state.error ? <div className="glass-panel mt-5 p-6 text-center text-[var(--ink)]">{state.error}</div> : null}

      {!state.loading && !state.error ? (
        <section className="mt-5 grid gap-4 md:grid-cols-2">
          {studies.map(([studyKey, study]) => (
            <StudyCard key={studyKey} studyKey={studyKey} study={study} date={date} />
          ))}
        </section>
      ) : null}
    </div>
  );
}
