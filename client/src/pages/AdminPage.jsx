import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import * as pdfjsLib from 'pdfjs-dist';
import { createWorker } from 'tesseract.js';

pdfjsLib.GlobalWorkerOptions.workerSrc = new URL(
  'pdfjs-dist/build/pdf.worker.min.mjs',
  import.meta.url
).toString();

const API = '/api/articles';

// ═══════════════════════════════════════════════
// רינדור עמוד — חותך header + הערות שוליים
// ═══════════════════════════════════════════════
async function renderPage(page, scale = 2.5) {
  const viewport = page.getViewport({ scale });

  // עמוד 1: header קטן יותר (הכותרת הרצינית נמצאת נמוך)
  // שאר העמודים: header גדול יותר (כותרת ראשה + מספר עמוד)
  const topCrop   = Math.floor(viewport.height * 0.055); // ~5.5% header
  const botCrop   = Math.floor(viewport.height * 0.305); // ~30.5% הערות שוליים

  const contentH = viewport.height - topCrop - botCrop;

  const canvas = document.createElement('canvas');
  canvas.width  = viewport.width;
  canvas.height = contentH;

  const ctx = canvas.getContext('2d');
  ctx.fillStyle = '#ffffff';
  ctx.fillRect(0, 0, canvas.width, canvas.height);
  ctx.save();
  ctx.translate(0, -topCrop);
  await page.render({ canvasContext: ctx, viewport }).promise;
  ctx.restore();

  return canvas;
}

// ═══════════════════════════════════════════════
// ניקוי טקסט OCR
// ═══════════════════════════════════════════════
const HEADER_PATTERNS = [
  /ש"פ\s*צו/,
  /שבת.{0,4}הגדול/,
  /מזמור.{0,6}צמאה/,
  /ניסן.{0,10}מאמר/,
  /ה'תשכ"ד/,
  /ô"ù|ä'úùë/,   // garbled header fragments that slipped through
];

function isHeaderLine(line) {
  if (line.length > 80) return false; // headers are short
  return HEADER_PATTERNS.some(p => p.test(line));
}

// שורות הערות שוליים שדלפו מתחתית הדף
function isFootnoteLine(line) {
  // שורת הערה: מתחילה במספר ערבי + סוגריים  e.g. "9) ראה..."
  if (/^\d{1,3}\)\s/.test(line)) return true;
  // מספר הערה אמצעי בשורה: "...יט. 6) תהלים..."  or  "...ע' 7) יל"ש..."
  if (/\d{1,2}\)\s[\u05D0-\u05EA]/.test(line)) return true;
  // קיצור ביבליוגרפי אופייני להערות: ובכ"מ
  if (/ובכ"מ/.test(line)) return true;
  return false;
}

// שורות OCR מקולקלות — קווי עיצוב, מפרידים גרפיים
function isGarbledLine(line) {
  if (/[>|]/.test(line)) return true;         // תווי ASCII שאין להם מקום בטקסט עברי
  if (/[=+]{3,}/.test(line)) return true;     // רצף של = או +
  if (/^--/.test(line)) return true;           // שורה שמתחילה ב--
  return false;
}

function cleanText(rawText) {
  const lines = rawText.split('\n');
  const out   = [];

  for (let line of lines) {
    line = line.trim();

    // מסיר שורות ריקות — ינוהלו בנפרד
    if (!line) { out.push(''); continue; }

    // מספרי עמודים: שורה שהיא רק ספרות (249, 250...)
    if (/^\d{1,4}$/.test(line)) continue;

    // כותרות ראשה
    if (isHeaderLine(line)) continue;

    // הערות שוליים שדלפו
    if (isFootnoteLine(line)) continue;

    // שורות OCR מקולקלות
    if (isGarbledLine(line)) continue;

    // מסיר סמני הערות שוליים inline:
    // מספר קטן (1–66) הצמוד לאחר תו עברי או לפני תו עברי
    line = line.replace(/(?<=[\u05D0-\u05EA'"\u05F3\u05F4,.])\s*\d{1,2}(?=[\s,.\u05D0-\u05EA'"\u05F3\u05F4(]|$)/g, '');
    // ניקוי גם מספר שנשאר לבדו בין רווחים
    line = line.replace(/(?<!\S)\d{1,2}(?!\S)/g, '');

    line = line.replace(/\s{2,}/g, ' ').trim();
    if (line.length < 2) continue;

    out.push(line);
  }

  // מקפל שורות ריקות מרובות לאחת
  return out
    .join('\n')
    .replace(/\n{3,}/g, '\n\n')
    .trim();
}

// ═══════════════════════════════════════════════
// חילוץ OCR מלא
// ═══════════════════════════════════════════════
async function extractOCR(file, onProgress) {
  const buf = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: buf }).promise;
  const numPages = pdf.numPages;

  const worker = await createWorker('heb', 1, {
    workerPath: 'https://cdn.jsdelivr.net/npm/tesseract.js@5/dist/worker.min.js',
    langPath:   'https://tessdata.projectnaptha.com/4.0.0',
    corePath:   'https://cdn.jsdelivr.net/npm/tesseract.js-core@5/tesseract-core-simd-lstm.wasm.js',
    logger: (m) => {
      if (m.status === 'recognizing text') onProgress?.(m.progress, null, null);
    },
  });

  const pageTexts = [];
  for (let i = 1; i <= numPages; i++) {
    onProgress?.(0, i, numPages);
    const page   = await pdf.getPage(i);
    const canvas = await renderPage(page);
    const { data: { text } } = await worker.recognize(canvas);
    pageTexts.push(cleanText(text));
  }

  await worker.terminate();

  // מחבר עמודים — שומר על מעברי פסקאות
  const full = pageTexts
    .filter(t => t.length > 10)
    .join('\n\n');

  return { text: full, numPages };
}

// ═══════════════════════════════════════════════
// קומפוננטה
// ═══════════════════════════════════════════════
export default function AdminPage() {
  const [articles, setArticles] = useState([]);
  const [selected, setSelected]  = useState(null);
  const [status,   setStatus]    = useState(''); // 'extracting' | 'uploading' | ''
  const [progress, setProgress]  = useState({ page: 0, total: 0, ocr: 0 });
  const [title,    setTitle]     = useState('');
  const [file,     setFile]      = useState(null);
  const [preview,  setPreview]   = useState('');
  const [pageCount,setPageCount] = useState(0);
  const [error,    setError]     = useState('');
  const fileRef = useRef();

  const loadArticles = async () => {
    const res  = await fetch(API);
    const data = await res.json();
    setArticles(data);
  };
  useEffect(() => { loadArticles(); }, []);

  const handleFileChange = async (e) => {
    const f = e.target.files[0];
    if (!f) return;
    setFile(f); setError(''); setPreview(''); setStatus('extracting');
    setProgress({ page: 0, total: 0, ocr: 0 });
    try {
      const { text, numPages } = await extractOCR(f, (ocr, page, total) => {
        setProgress({ ocr: ocr ?? 0, page: page ?? 0, total: total ?? 0 });
      });
      setPreview(text);
      setPageCount(numPages);
    } catch (err) {
      setError('שגיאה: ' + err.message);
    } finally {
      setStatus('');
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    if (!file || !preview) return;
    setError(''); setStatus('uploading');
    const form = new FormData();
    form.append('pdf', file);
    form.append('rawText', preview);
    form.append('pageCount', pageCount);
    if (title) form.append('title', title);
    try {
      const res  = await fetch(`${API}/upload`, { method: 'POST', body: form });
      const data = await res.json();
      if (!res.ok) throw new Error(data.message);
      setTitle(''); setFile(null); setPreview(''); setPageCount(0);
      fileRef.current.value = '';
      await loadArticles();
      setSelected(data);
    } catch (err) {
      setError(err.message);
    } finally {
      setStatus('');
    }
  };

  const handleDelete = async (id) => {
    if (!confirm('למחוק?')) return;
    await fetch(`${API}/${id}`, { method: 'DELETE' });
    if (selected?._id === id) setSelected(null);
    loadArticles();
  };

  const handleSelect = async (id) => {
    const res  = await fetch(`${API}/${id}`);
    const data = await res.json();
    setSelected(data);
  };

  return (
    <div className="mx-auto w-full max-w-5xl px-4 pb-16 pt-6 sm:px-6 lg:px-8">
      <div className="mb-6 flex items-center gap-3">
        <Link to="/" className="inline-flex items-center gap-2 rounded-full border border-[var(--line)] bg-white/80 px-4 py-2 text-sm font-medium text-[var(--ink)] shadow-sm">
          <ArrowRight size={16} />חזרה
        </Link>
        <h1 className="text-xl font-bold text-[var(--ink)]">מאמרים</h1>
      </div>

      {/* טופס העלאה */}
      <form onSubmit={handleUpload} className="glass-panel mb-6 p-5">
        <h2 className="mb-4 text-base font-semibold text-[var(--ink)]">העלאת מאמר</h2>
        <div className="flex flex-col gap-3">
          <input
            type="text"
            placeholder="כותרת (אופציונלי)"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            className="rounded-2xl border border-[var(--line)] bg-white/60 px-4 py-2 text-sm text-[var(--ink)] outline-none focus:border-[var(--brand)]"
          />
          <input
            ref={fileRef} type="file" accept=".pdf"
            onChange={handleFileChange}
            disabled={status === 'extracting'}
            className="text-sm text-[var(--muted)]"
          />

          {status === 'extracting' && (
            <div className="rounded-2xl border border-[var(--line)] bg-slate-50 p-3 text-sm text-[var(--brand)]">
              {progress.total > 0
                ? `עמוד ${progress.page} מתוך ${progress.total} — ${Math.round(progress.ocr * 100)}%`
                : 'טוען... (בפעם הראשונה עשוי לקחת כדקה)'}
            </div>
          )}

          {preview && status !== 'extracting' && (
            <div className="max-h-48 overflow-y-auto rounded-2xl border border-[var(--line)] bg-slate-50 p-4 text-sm leading-relaxed text-[var(--ink)] whitespace-pre-wrap">
              {preview.slice(0, 800)}…
            </div>
          )}

          {error && <p className="text-sm text-red-500">{error}</p>}

          <button
            type="submit"
            disabled={status !== '' || !preview}
            className="w-fit rounded-full bg-[var(--brand)] px-6 py-2 text-sm font-medium text-white hover:opacity-90 disabled:opacity-50"
          >
            {status === 'uploading' ? 'שומר...' : 'שמור מאמר'}
          </button>
        </div>
      </form>

      <div className="flex gap-4">
        {/* רשימה */}
        <div className="w-64 shrink-0">
          <p className="mb-3 text-sm font-medium text-[var(--muted)]">{articles.length} מאמרים</p>
          {articles.length === 0 && (
            <div className="glass-panel p-4 text-center text-sm text-[var(--muted)]">אין מאמרים עדיין</div>
          )}
          <ul className="flex flex-col gap-2">
            {articles.map((a) => (
              <li key={a._id} onClick={() => handleSelect(a._id)}
                className={`glass-panel cursor-pointer px-4 py-3 transition-all ${selected?._id === a._id ? 'border-[var(--brand)]' : ''}`}>
                <div className="flex items-start justify-between gap-2">
                  <div className="min-w-0">
                    <p className="truncate text-sm font-medium text-[var(--ink)]">{a.title}</p>
                    <p className="mt-0.5 text-xs text-[var(--muted)]">
                      {new Date(a.createdAt).toLocaleDateString('he-IL')} · {a.pageCount} עמ׳
                    </p>
                  </div>
                  <button
                    onClick={(e) => { e.stopPropagation(); handleDelete(a._id); }}
                    className="shrink-0 text-xs text-[var(--muted)] hover:text-red-500"
                  >✕</button>
                </div>
              </li>
            ))}
          </ul>
        </div>

        {/* תצוגה */}
        <div className="flex-1">
          {selected ? (
            <div className="glass-panel p-5">
              <h2 className="mb-1 text-lg font-bold text-[var(--ink)]">{selected.title}</h2>
              <p className="mb-4 text-xs text-[var(--muted)]">{selected.originalFilename} · {selected.pageCount} עמ׳</p>
              <pre
                className="max-h-[60vh] overflow-y-auto whitespace-pre-wrap text-sm leading-loose text-[var(--ink)]"
                style={{ fontFamily: 'inherit' }}
              >
                {selected.rawText}
              </pre>
            </div>
          ) : (
            <div className="glass-panel flex h-40 items-center justify-center text-sm text-[var(--muted)]">
              בחר מאמר לצפייה
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
