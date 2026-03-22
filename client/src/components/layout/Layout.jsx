import { Outlet, useLocation } from 'react-router-dom';
import Navbar from './Navbar';

export default function Layout() {
  const { pathname } = useLocation();
  const isStudyPage = [
    '/chumash',
    '/rambam',
    '/rambam-one',
    '/tanya',
    '/sefer-hamitzvot',
    '/shnayim-mikra',
  ].includes(pathname);

  return (
    <div className="app-shell min-h-screen text-slate-900">
      <Navbar floating={!isStudyPage} />
      <main className={`relative z-10 ${isStudyPage ? 'pt-6' : 'pt-24'}`}>
        <Outlet />
      </main>
    </div>
  );
}
