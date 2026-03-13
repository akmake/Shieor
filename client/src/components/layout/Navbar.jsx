import { Link, NavLink } from 'react-router-dom';
import { House, Book, Scroll, Layers } from 'lucide-react';

export default function Navbar() {
  return (
    <nav className="fixed inset-x-0 top-0 z-50 border-b border-slate-200 bg-white/95 backdrop-blur">
      <div className="flex items-center justify-between px-4 py-4 sm:px-6">
        <Link to="/" className="flex items-center gap-3">
          <div className="rounded-xl bg-slate-900 p-2 text-white">
            <Scroll size={18} />
          </div>
          <span className="text-sm font-black uppercase tracking-[0.2em] text-slate-900">MyStudy</span>
        </Link>

        <div className="flex items-center gap-2">
          <NavLink
            to="/"
            end
            className={({ isActive }) =>
              `flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${
                isActive ? 'bg-slate-900 text-white' : 'text-slate-600 hover:bg-slate-100'
              }`
            }
          >
            <House size={16} />
            בית
          </NavLink>
          <NavLink
            to="/chumash"
            className={({ isActive }) =>
              `flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${
                isActive ? 'bg-blue-600 text-white' : 'text-blue-600 hover:bg-blue-100'
              }`
            }
          >
            <Book size={16} />
            חומש
          </NavLink>
          <NavLink
            to="/rambam"
            className={({ isActive }) =>
              `flex items-center gap-2 rounded-full px-4 py-2 text-sm font-medium transition ${
                isActive ? 'bg-emerald-600 text-white' : 'text-emerald-600 hover:bg-emerald-100'
              }`
            }
          >
            <Layers size={16} />
            רמב"ם
          </NavLink>
        </div>
      </div>
    </nav>
  );
}
