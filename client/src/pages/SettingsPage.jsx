import { useState } from 'react';
import { clearAllDatabases } from '../utils/db';

const DEFAULTS = { fontSize: 20, scrollSpeed: 40, shnayimMikraConnected: true };

function loadSettings() {
  try {
    return { ...DEFAULTS, ...JSON.parse(localStorage.getItem('shieor-settings') || '{}') };
  } catch (_) {
    return DEFAULTS;
  }
}

function saveSettings(s) {
  localStorage.setItem('shieor-settings', JSON.stringify(s));
}


export default function SettingsPage() {
  const [settings, setSettings] = useState(loadSettings);
  const [cleared, setCleared] = useState(false);

  function update(key, value) {
    const next = { ...settings, [key]: value };
    setSettings(next);
    saveSettings(next);
  }

  async function handleClear() {
    await clearAllDatabases();
    setCleared(true);
    setTimeout(() => setCleared(false), 3000);
  }

  const previewStyle = { fontSize: settings.fontSize, lineHeight: '2.4rem', fontFamily: '"SBL Hebrew", serif' };

  return (
    <div className="mx-auto w-full max-w-2xl px-4 pb-16 pt-6 sm:px-6">
      <h1 className="text-3xl font-semibold text-[var(--ink)]">הגדרות</h1>
      <p className="mt-2 text-sm text-[var(--muted)]">ההגדרות נשמרות בדפדפן ומיושמות מיד.</p>

      <div className="mt-6 space-y-5">

        {/* גודל גופן */}
        <div className="glass-panel p-5">
          <label className="block text-sm font-semibold text-[var(--ink)]">גודל גופן</label>
          <p className="mt-1 text-xs text-[var(--muted)]">גודל הטקסט בדפי הלימוד</p>
          <div className="mt-3 flex items-center gap-4">
            <input
              type="range"
              min={14}
              max={30}
              step={1}
              value={settings.fontSize}
              onChange={(e) => update('fontSize', Number(e.target.value))}
              className="flex-1 accent-[var(--brand)]"
            />
            <span className="w-10 text-center text-sm font-bold text-[var(--ink)]">{settings.fontSize}</span>
          </div>
          <div className="mt-4 rounded-xl border border-[var(--line)] bg-[var(--soft)] p-4">
            <p className="text-xs text-[var(--muted)] mb-2">תצוגה מקדימה:</p>
            <p style={previewStyle} className="text-[var(--ink)]">וְאֵ֣לֶּה שְׁמ֗וֹת בְּנֵי יִשְׂרָאֵ֛ל</p>
          </div>
        </div>

        {/* מהירות גלילה */}
        <div className="glass-panel p-5">
          <label className="block text-sm font-semibold text-[var(--ink)]">מהירות גלילה אוטומטית</label>
          <p className="mt-1 text-xs text-[var(--muted)]">מספר פיקסלים לשנייה</p>
          <div className="mt-3 flex items-center gap-4">
            <span className="text-xs text-[var(--muted)]">איטי</span>
            <input
              type="range"
              min={10}
              max={120}
              step={5}
              value={settings.scrollSpeed}
              onChange={(e) => update('scrollSpeed', Number(e.target.value))}
              className="flex-1 accent-[var(--brand)]"
            />
            <span className="text-xs text-[var(--muted)]">מהיר</span>
            <span className="w-10 text-center text-sm font-bold text-[var(--ink)]">{settings.scrollSpeed}</span>
          </div>
        </div>

        {/* שניים מקרא */}
        <div className="glass-panel p-5">
          <label className="block text-sm font-semibold text-[var(--ink)]">תצוגת שניים מקרא</label>
          <p className="mt-1 text-xs text-[var(--muted)]">כיצד להציג כל פסוק — רצוף בשורה אחת או מופרד לשורות</p>
          <div className="mt-3 flex gap-3">
            <button
              onClick={() => update('shnayimMikraConnected', true)}
              className={`flex-1 rounded-xl border py-2 text-sm font-medium transition ${settings.shnayimMikraConnected ? 'border-[var(--brand)] bg-[var(--brand)] text-white' : 'border-[var(--line)] text-[var(--ink)]'}`}
            >
              רצוף
            </button>
            <button
              onClick={() => update('shnayimMikraConnected', false)}
              className={`flex-1 rounded-xl border py-2 text-sm font-medium transition ${!settings.shnayimMikraConnected ? 'border-[var(--brand)] bg-[var(--brand)] text-white' : 'border-[var(--line)] text-[var(--ink)]'}`}
            >
              מופרד
            </button>
          </div>
        </div>

        {/* ניקוי מטמון */}
        <div className="glass-panel p-5">
          <h2 className="text-sm font-semibold text-[var(--ink)]">ניקוי נתונים שמורים</h2>
          <p className="mt-1 text-xs text-[var(--muted)]">
            מוחק את כל הלימודים שהורדו לשימוש אופליין. הנתונים יורדו מחדש בגישה הבאה לכל יום.
          </p>
          <button
            onClick={handleClear}
            className="mt-4 inline-flex items-center gap-2 rounded-full border border-red-200 bg-red-50 px-4 py-2 text-sm font-medium text-red-600 transition hover:bg-red-100"
          >
            {cleared ? '✓ נוקה בהצלחה' : 'נקה נתונים שמורים'}
          </button>
        </div>

      </div>
    </div>
  );
}
