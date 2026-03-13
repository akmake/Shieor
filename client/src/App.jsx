import { lazy, Suspense } from 'react';
import { Routes, Route } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import ChumashPage from './pages/ChumashPage';
import RambamPage from './pages/RambamPage';
import TanyaPage from './pages/TanyaPage';
import ShnayimMikraPage from './pages/ShnayimMikraPage';

function PageLoader() {
  return (
    <div className="flex min-h-[50vh] items-center justify-center text-slate-500">
      טוען עמוד...
    </div>
  );
}

export default function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
        </Route>
        <Route path="/chumash" element={<ChumashPage />} />
        <Route path="/rambam" element={<RambamPage />} />
        <Route path="/tanya" element={<TanyaPage />} />
        <Route path="/shnayim-mikra" element={<ShnayimMikraPage />} />
      </Routes>
    </Suspense>
  );
}
