import { useEffect, useState } from 'react';
import { useSearchParams, Link } from 'react-router-dom';
import { BookOpen } from 'lucide-react';
import DateNavigator from '../components/study/DateNavigator';
import StudyCard from '../components/study/StudyCard';
import { getDailyStudy, normalizeDate, shiftDate } from '../utils/study';

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const [state, setState] = useState({ loading: true, error: '', data: null });
  const [articles, setArticles] = useState([]);

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
    return () => { alive = false; };
  }, [date]);

  useEffect(() => {
    async function loadArticles() {
      try {
        const res = await fetch('/api/articles');
        const data = await res.json();
        setArticles(data);
      } catch (err) {
        console.error('Failed to load articles', err);
      }
    }
    loadArticles();
  }, []);

  const studies = state.data?.studies ? Object.entries(state.data.studies) : [];

  return (
    <div className="mx-auto w-full max-w-6xl px-4 pb-16 pt-6 sm:px-6 lg:px-8">
      <div className="mt-5">
        <DateNavigator
          date={date}
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

      {articles.length > 0 && (
        <section className="mt-10">
          <h2 className="mb-4 text-2xl font-bold flex items-center gap-2">
            <BookOpen className="h-6 w-6 text-blue-600" />
            מאמרים ללימוד
          </h2>
          <div className="grid gap-4 md:grid-cols-2">
            {articles.map((article) => (
              <Link key={article._id} to={`/article/${article._id}`} className="glass-panel block p-4 hover:bg-slate-50 transition-colors">
                <h3 className="font-bold text-lg text-[var(--ink)]">{article.title || 'מאמר ללא כותרת'}</h3>
                <p className="text-sm text-[var(--muted)] mt-2">
                  צורף בתאריך: {new Date(article.createdAt).toLocaleDateString('he-IL')} • {article.pages} עמודים
                </p>
              </Link>
            ))}
          </div>
        </section>
      )}
    </div>
  );
}
