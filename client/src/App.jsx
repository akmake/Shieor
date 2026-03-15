import { Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import ChumashPage from './pages/ChumashPage';
import RambamPage from './pages/RambamPage';
import TanyaPage from './pages/TanyaPage';
import ShnayimMikraPage from './pages/ShnayimMikraPage';
import SettingsPage from './pages/SettingsPage';
import ZmanimPage from './pages/ZmanimPage';

function PageLoader() {
  return <div className="mx-auto mt-20 w-fit rounded-full bg-white/80 px-5 py-2 text-sm text-slate-600">טוען עמוד...</div>;
}

export default function App() {
  return (
    <Suspense fallback={<PageLoader />}>
      <Routes>
        <Route path="/" element={<Layout />}>
          <Route index element={<HomePage />} />
          <Route path="chumash" element={<ChumashPage />} />
          <Route path="rambam" element={<RambamPage />} />
          <Route path="tanya" element={<TanyaPage />} />
          <Route path="shnayim-mikra" element={<ShnayimMikraPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="zmanim" element={<ZmanimPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
