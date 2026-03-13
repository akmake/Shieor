import { Link, NavLink, useLocation } from 'react-router-dom';
import { BookOpenText, House, Layers3, Sparkles, ScrollText } from 'lucide-react';

const ITEMS = [
  { to: '/', label: 'בית', icon: House, end: true },
  { to: '/chumash', label: 'חומש', icon: BookOpenText },
  { to: '/rambam', label: 'רמב"ם', icon: Layers3 },
  { to: '/tanya', label: 'תניא', icon: Sparkles },
  { to: '/shnayim-mikra', label: 'שניים מקרא', icon: ScrollText },
];

export default function Navbar() {
  const location = useLocation();
  const currentDate = new URLSearchParams(location.search).get('date');

  return (
    <nav className="fixed inset-x-0 top-0 z-50 px-3 pt-3 sm:px-6">
      <div className="mx-auto flex w-full max-w-6xl items-center justify-between gap-3 rounded-[28px] border border-[var(--line)] bg-white/80 px-4 py-3 shadow-[0_16px_70px_rgba(15,23,42,0.1)] backdrop-blur-xl">
        <Link to={currentDate ? `/?date=${currentDate}` : '/'} className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[var(--ink)] text-white">
            <ScrollText size={17} />
          </div>
          <div>
            <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--brand)]">Shieor</p>
            <p className="text-sm text-[var(--ink)]">לימוד יומי</p>
          </div>
        </Link>

        <div className="hidden items-center gap-2 lg:flex">
          {ITEMS.map(({ to, label, icon: Icon, end }) => (
            <NavLink
              key={to}
              to={currentDate ? `${to}?date=${currentDate}` : to}
              end={Boolean(end)}
              className={({ isActive }) =>
                `inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${
                  isActive ? 'bg-[var(--ink)] text-white' : 'text-[var(--muted)] hover:bg-white hover:text-[var(--ink)]'
                }`
              }
            >
              <Icon size={16} />
              {label}
            </NavLink>
          ))}
        </div>
      </div>
    </nav>
  );
}
