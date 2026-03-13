import { Outlet } from 'react-router-dom';
import Navbar from './Navbar';

export default function Layout() {
  return (
    <div className="app-shell min-h-screen text-slate-900">
      <Navbar />
      <main className="relative z-10 pt-24">
        <Outlet />
      </main>
      <footer className="relative z-10 px-4 pb-10 pt-3 text-center text-sm text-[var(--muted)]">
        Shieor · Daily Learning
      </footer>
    </div>
  );
}
