import { ChevronLeft, ChevronRight } from 'lucide-react';

function toHebrewLetters(num) {
  const ones = ['', 'א', 'ב', 'ג', 'ד', 'ה', 'ו', 'ז', 'ח', 'ט'];
  const tens = ['', 'י', 'כ', 'ל', 'מ', 'נ', 'ס', 'ע', 'פ', 'צ'];
  const hundreds = ['', 'ק', 'ר', 'ש', 'ת'];

  if (!Number.isInteger(num) || num <= 0) return String(num || '');
  if (num === 15) return 'טו';
  if (num === 16) return 'טז';

  let n = num;
  let out = '';

  while (n >= 400) {
    out += 'ת';
    n -= 400;
  }
  if (n >= 100) {
    out += hundreds[Math.floor(n / 100)];
    n %= 100;
  }
  if (n >= 10) {
    out += tens[Math.floor(n / 10)];
    n %= 10;
  }
  out += ones[n];
  return out;
}

function withHebrewPunctuation(letters) {
  if (!letters) return '';
  if (letters.length === 1) return `${letters}'`;
  return `${letters.slice(0, -1)}"${letters.slice(-1)}`;
}

function formatHebrewCalendarDate(dateString) {
  try {
    const parts = new Intl.DateTimeFormat('he-IL-u-ca-hebrew', {
      weekday: 'long',
      day: 'numeric',
      month: 'long',
      year: 'numeric',
    }).formatToParts(new Date(dateString));

    const weekday = parts.find((p) => p.type === 'weekday')?.value || '';
    const day = Number(parts.find((p) => p.type === 'day')?.value || 0);
    const month = parts.find((p) => p.type === 'month')?.value || '';
    const year = Number(parts.find((p) => p.type === 'year')?.value || 0);

    const hebDay = withHebrewPunctuation(toHebrewLetters(day));
    const hebYear = withHebrewPunctuation(toHebrewLetters(year % 1000));
    return `${weekday}, ${hebDay} ${month} ${hebYear}`.trim();
  } catch (_) {
    return '';
  }
}

export default function DateNavigator({ date, onPrev, onNext }) {
  const displayedDate = formatHebrewCalendarDate(date);

  return (
    <section className="flex items-center justify-between gap-3 rounded-3xl border border-[var(--line)] bg-white px-4 py-3 shadow-sm">
      <button type="button" className="nav-btn" onClick={onNext} aria-label="היום הבא">
        <ChevronRight size={18} />
      </button>

      <div className="text-center">
        <p className="text-[11px] font-semibold uppercase tracking-[0.3em] text-[var(--brand)]">תאריך עברי</p>
        <h2 className="mt-1 text-lg font-semibold text-[var(--ink)] sm:text-xl" style={{ color: 'var(--ink)' }}>{displayedDate}</h2>
      </div>

      <button type="button" className="nav-btn" onClick={onPrev} aria-label="היום הקודם">
        <ChevronLeft size={18} />
      </button>
    </section>
  );
}
