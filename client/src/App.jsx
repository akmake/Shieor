import { Suspense } from 'react';
import { Route, Routes } from 'react-router-dom';
import Layout from './components/layout/Layout';
import HomePage from './pages/HomePage';
import ChumashPage from './pages/ChumashPage';
import RambamPage from './pages/RambamPage';
import RambamOnePage from './pages/RambamOnePage';
import TanyaPage from './pages/TanyaPage';
import SeferHamitzvotPage from './pages/SeferHamitzvotPage';
import ShnayimMikraPage from './pages/ShnayimMikraPage';
import SettingsPage from './pages/SettingsPage';
import ZmanimPage from './pages/ZmanimPage';
import AdminPage from './pages/AdminPage';

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
          <Route path="rambam-one" element={<RambamOnePage />} />
          <Route path="tanya" element={<TanyaPage />} />
          <Route path="sefer-hamitzvot" element={<SeferHamitzvotPage />} />
          <Route path="shnayim-mikra" element={<ShnayimMikraPage />} />
          <Route path="settings" element={<SettingsPage />} />
          <Route path="zmanim" element={<ZmanimPage />} />
          <Route path="admin" element={<AdminPage />} />
        </Route>
      </Routes>
    </Suspense>
  );
}
