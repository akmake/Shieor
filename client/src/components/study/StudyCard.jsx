import { Link } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { STUDY_ROUTES } from '../../utils/study';

const ACCENTS = {
  blue: 'accent-blue',
  emerald: 'accent-emerald',
  violet: 'accent-violet',
  amber: 'accent-amber',
};

export default function StudyCard({ studyKey, study, date }) {
  return (
    <Link to={`${STUDY_ROUTES[studyKey]}?date=${date}`} className={`study-card ${ACCENTS[study.accent] || ''}`}>
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-[var(--muted)]">{study.kind}</p>
          <h3 className="mt-2 text-xl font-semibold text-[var(--ink)]">{study.title}</h3>
        </div>
        <span className="inline-flex h-10 w-10 items-center justify-center rounded-full bg-white/90 text-[var(--ink)] shadow-sm">
          <ArrowLeft size={18} />
        </span>
      </div>

      <p className="mt-4 text-sm font-medium text-[var(--ink)]">{study.label || study.subtitle}</p>
      {study.ref ? <p className="mt-1 text-sm text-[var(--muted)]">{study.ref}</p> : null}
      <p className="mt-4 line-clamp-4 text-[15px] leading-7 text-[var(--muted)]">{study.preview || study.subtitle}</p>
    </Link>
  );
}
