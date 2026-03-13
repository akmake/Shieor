import { ChevronLeft, ChevronRight } from 'lucide-react';
import { formatDateLabel } from '../../utils/study';

export default function DateNavigator({ date, hebrewDate, onPrev, onNext }) {
  return (
    <section className="glass-panel flex items-center justify-between gap-3 p-3 sm:p-4">
      <button type="button" className="nav-btn" onClick={onNext} aria-label="היום הבא">
        <ChevronRight size={18} />
      </button>

      <div className="text-center">
        <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-[var(--muted)]">Daily Date</p>
        <h2 className="mt-1 text-lg font-semibold text-[var(--ink)] sm:text-xl">{formatDateLabel(date)}</h2>
        {hebrewDate ? <p className="mt-1 text-sm text-[var(--muted)]">{hebrewDate}</p> : null}
      </div>

      <button type="button" className="nav-btn" onClick={onPrev} aria-label="היום הקודם">
        <ChevronLeft size={18} />
      </button>
    </section>
  );
}
