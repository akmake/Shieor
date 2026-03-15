import { useState } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { BookOpenText, House, Layers3, Settings, Sparkles, ScrollText, Menu, X } from 'lucide-react';

const ITEMS = [
  { to: '/', label: 'בית', icon: House, end: true },
  { to: '/chumash', label: 'חומש', icon: BookOpenText },
  { to: '/rambam', label: 'רמב"ם', icon: Layers3 },
  { to: '/tanya', label: 'תניא', icon: Sparkles },
  { to: '/shnayim-mikra', label: 'שניים מקרא', icon: ScrollText },
  { to: '/settings', label: 'הגדרות', icon: Settings },
];

export default function Navbar() {
  const location = useLocation();
  const currentDate = new URLSearchParams(location.search).get('date');
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  const closeMenu = () => setIsMobileMenuOpen(false);

  return (
    <nav className="fixed inset-x-0 top-0 z-50 px-3 pt-3 sm:px-6">
      <div className="mx-auto w-full max-w-6xl rounded-[28px] border border-[var(--line)] bg-white/90 shadow-[0_16px_70px_rgba(15,23,42,0.1)] backdrop-blur-xl transition-all duration-300">
        <div className="flex items-center justify-between gap-3 px-4 py-3">
          <Link to={currentDate ? `/?date=${currentDate}` : '/'} className="flex items-center gap-3" onClick={closeMenu}>
            <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-[var(--ink)] text-white">
              <ScrollText size={17} />
            </div>
            <div>
              <p className="text-[11px] font-semibold uppercase tracking-[0.24em] text-[var(--brand)]">Shieor</p>
              <p className="text-sm text-[var(--ink)]">לימוד יומי</p>
            </div>
          </Link>

          {/* Desktop Menu */}
          <div className="hidden items-center gap-2 lg:flex">
            {ITEMS.map(({ to, label, icon: Icon, end }) => (
              <NavLink
                key={to}
                to={currentDate ? `${to}?date=${currentDate}` : to}
                end={Boolean(end)}
                className={({ isActive }) =>
                  `inline-flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${
                    isActive ? 'bg-[var(--ink)] text-white' : 'text-[var(--muted)] hover:bg-gray-100 hover:text-[var(--ink)]'
                  }`
                }
              >
                <Icon size={16} />
                {label}
              </NavLink>
            ))}
          </div>

          {/* Mobile Menu Toggle */}
          <button 
            className="flex items-center justify-center rounded-full p-2 text-[var(--ink)] transition-colors hover:bg-gray-100 lg:hidden"
            onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
          >
            {isMobileMenuOpen ? <X size={24} /> : <Menu size={24} />}
          </button>
        </div>

        {/* Mobile Dropdown Menu */}
        {isMobileMenuOpen && (
          <div className="border-t border-[var(--line)] px-4 pb-4 pt-2 lg:hidden">
            <div className="flex flex-col gap-1">
              {ITEMS.map(({ to, label, icon: Icon, end }) => (
                <NavLink
                  key={to}
                  to={currentDate ? `${to}?date=${currentDate}` : to}
                  end={Boolean(end)}
                  onClick={closeMenu}
                  className={({ isActive }) =>
                    `flex items-center gap-3 rounded-2xl px-4 py-3 text-base font-medium transition ${
                      isActive ? 'bg-[var(--ink)] text-white' : 'text-[var(--muted)] hover:bg-gray-100 hover:text-[var(--ink)]'
                    }`
                  }
                >
                  <Icon size={20} />
                  {label}
                </NavLink>
              ))}
            </div>
          </div>
        )}
      </div>
    </nav>
  );
}