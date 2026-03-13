import { useState } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';

const studyCards = [
  {
    title: 'חומש יומי',
    desc: 'העלייה היומית עם רש"י',
    to: '/chumash',
    color: 'bg-blue-600',
  },
  {
    title: 'רמב"ם יומי',
    desc: '3 פרקים במשנה תורה',
    to: '/rambam',
    color: 'bg-emerald-600',
  },
  {
    title: 'תניא יומי',
    desc: 'קטע יומי בספר התניא',
    to: '/tanya',
    color: 'bg-purple-600',
  },
  {
    title: 'שניים מקרא',
    desc: 'הפרשה עם תרגום אונקלוס',
    to: '/shnayim-mikra',
    color: 'bg-orange-600',
  },
];

function formatDate(date) {
  return date.toISOString().slice(0, 10);
}

function getHebrewDayName(date) {
  const days = ['ראשון', 'שני', 'שלישי', 'רביעי', 'חמישי', 'שישי', 'שבת'];
  return days[date.getDay()];
}

export default function HomePage() {
  const [searchParams, setSearchParams] = useSearchParams();
  const navigate = useNavigate();
  const today = new Date();
  const paramDate = searchParams.get('date');
  const [date, setDate] = useState(paramDate ? new Date(paramDate) : today);

  // עדכון ה-URL כשמשנים יום
  function updateDate(newDate) {
    setDate(newDate);
    setSearchParams(newDate.toISOString().slice(0, 10) === formatDate(today) ? {} : { date: formatDate(newDate) });
  }

  function goPrevDay() {
    const d = new Date(date);
    d.setDate(d.getDate() - 1);
    updateDate(d);
  }
  function goNextDay() {
    const d = new Date(date);
    d.setDate(d.getDate() + 1);
    updateDate(d);
  }

  return (
    <main className="min-h-screen flex flex-col items-center justify-start bg-gradient-to-b from-blue-50 to-slate-100 px-2 pt-8" dir="rtl" lang="he">
      <div className="w-full max-w-md mx-auto flex flex-col gap-4">
        <div className="flex items-center justify-between mb-2">
          <button onClick={goPrevDay} className="rounded-full px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold">יום קודם</button>
          <div className="text-lg font-bold text-blue-900">
            {date.toLocaleDateString('he-IL')}<br />
            <span className="text-xs text-slate-500">יום {getHebrewDayName(date)}</span>
          </div>
          <button onClick={goNextDay} className="rounded-full px-4 py-2 bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold">יום הבא</button>
        </div>

        <div className="grid grid-cols-1 gap-4">
          {studyCards.map(card => (
            <Link
              key={card.to}
              to={{ pathname: card.to, search: `?date=${formatDate(date)}` }}
              className={`block rounded-2xl shadow-md p-6 text-white ${card.color} hover:scale-[1.03] transition-all`}
            >
              <div className="text-2xl font-black mb-1">{card.title}</div>
              <div className="text-md font-medium opacity-80">{card.desc}</div>
            </Link>
          ))}
        </div>
      </div>
    </main>
  );
}
