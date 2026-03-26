import { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router-dom';
import { ArrowRight } from 'lucide-react';
import * as pdfjsLib from 'pdfjs-dist';

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
// פענוח וניקוי טקסט PDF איכותי ללא Tesseract
// ═══════════════════════════════════════════════
const hebrewMap = {
  0xe0: 'א', 0x2021: 'א', 0xe1: 'ב', 0x00B7: 'ב', 0xe2: 'ג', 0x201A: 'ג', 0xe3: 'ד', 0x201E: 'ד',
  0xe4: 'ה', 0x2030: 'ה', 0xe5: 'ו', 0x00C2: 'ו', 0xe6: 'ז', 0x00CA: 'ז', 0xe7: 'ח', 0x00C1: 'ח',
  0xe8: 'ט', 0x00CB: 'ט', 0xe9: 'י', 0x00C8: 'י', 0xC8: 'י', 0xea: 'ך', 0x00CD: 'ך', 0xCD: 'ך',
  0xeb: 'כ', 0x00CE: 'כ', 0xec: 'ל', 0x00CF: 'ל', 0xCF: 'ל', 0xed: 'ם', 0x00CC: 'ם',
  0xee: 'מ', 0x00D3: 'מ', 0xD3: 'מ', 0xef: 'ן', 0x00D4: 'ן', 0xf0: 'נ', 0xF8FF: 'נ',
  0xf1: 'ס', 0x00D2: 'ס', 0xf2: 'ע', 0x00DA: 'ע', 0xf3: 'ף', 0x00DB: 'ף', 0xf4: 'פ', 0x00D9: 'פ', 0xD9: 'פ',
  0xf5: 'ץ', 0x0131: 'ץ', 0xf6: 'צ', 0x02C6: 'צ', 0xf7: 'ק', 0x02DC: 'ק', 0xf8: 'ר', 0x00AF: 'ר',
  0xf9: 'ש', 0x02D8: 'ש', 0xfa: 'ת', 0x02D9: 'ת'
};

function fixHebrew(text) {
  let out = '';
  for (let i = 0; i < text.length; i++) {
    const code = text.charCodeAt(i);
    if (hebrewMap[code]) out += hebrewMap[code];
    else out += text[i];
  }
  return out;
}

async function extractOCR(file, onProgress) {
  const buf = await file.arrayBuffer();
  const pdf = await pdfjsLib.getDocument({ data: buf }).promise;
  const numPages = pdf.numPages;

  let allParagraphs = [];

  for (let i = 1; i <= numPages; i++) {
    onProgress?.(1, i, numPages);
    const page = await pdf.getPage(i);
    const viewport = page.getViewport({ scale: 1 });
    const content = await page.getTextContent();

    let items = content.items.map(item => {
       return {
         y: Math.round(item.transform[5]),
         x: item.transform[4],
         w: item.width,
         fontSize: Math.abs(item.transform[3]),
         fixedText: fixHebrew(item.str).split('').map(c => {
             // החלפת סוגריים וסימנים דומים כי הכיוון הופך אותם
             if(c === '(') return ')';
             if(c === ')') return '(';
             if(c === '[') return ']';
             if(c === ']') return '[';
             if(c === '{') return '}';
             if(c === '}') return '{';
             if(c === '<') return '>';
             if(c === '>') return '<';
             return c;
         }).reverse().join('')
       };
    });

    // בודקים היכן לרוב נמצא הכתב. כותרות רצות (מודפסות בראש העמוד, זוגיים או אי-זוגיים) לרוב נמצאות מעל נקודה מסוימת.
    // מכיוון שיש כותרות גם בעמודים אי זוגיים וגם בעמודים זוגיים בספר, עדיף להוריד כל טקסט שנמצא ממש בקופסה העליונה.
    const topCropY = viewport.height * 0.90; // נוריד כל דבר שנמצא ב-10% העליונים

    // מסנן הערות שוליים, מספרי הערות ורווחים לא רלוונטיים
    // גודל פונט 10 ומטה הם לרוב הערות בתחתית הדף או מספרים קטנים בגוף הטקסט
    items = items.filter(item => {
       if (item.fontSize < 11) return false;
       if (item.fixedText.trim().length === 0) return false;
       
       // הסרת שורות כותרת ומספרי עמוד בהדר הפיזי (90% ומעלה מגובה העמוד המקורי)
       // העמוד הראשון לרוב לא מקבל צנזור כזה כדי לא לאבד את כותרת המאמר עצמו
       if (i !== 1 && item.y > topCropY) {
           return false;
       }
       
       return true;
    });

    // מיון לפי מיקום בציר ה-Y (שורות) ובציר ה-X (מימין לשמאל בתוך השורה)
    items.sort((a,b) => {
       if (Math.abs(b.y - a.y) > 5) return b.y - a.y;
       return b.x - a.x;
    });

    let lines = [];
    let currentLine = '';
    let lastY = -1000;
    let lastLeft = -1000;

    for (const item of items) {
       // שורה חדשה
       if (Math.abs(item.y - lastY) > 5) {
           if (currentLine.length > 0) {
               lines.push(currentLine);
           }
           currentLine = item.fixedText;
       } else {
           // אם המרחק בין המילה הנוכחית לקודמת גדול מ-4, אז מדובר במילה חדשה ויש להוסיף רווח
           let gap = lastLeft - (item.x + item.w);
           if (gap > 4) {
               currentLine += ' ';
           }
           currentLine += item.fixedText;
       }
       lastY = item.y;
       lastLeft = item.x;
    }
    if (currentLine.length > 0) lines.push(currentLine);

    for (let line of lines) {
       if (!line || line.trim() === '') continue;

       // הפיכת הטקסט לעברית רגילה כבר קרתה ברמת המילה, אין צורך להפוך את כל השורה
       let reversed = line.trim();

       // מסננים מתקדמים לפסולות הנותרות
       if (/^\d{1,4}$/.test(reversed)) continue; // שורת מספור עמוד (כמו 250) בלבד
       if (reversed.includes('ספר המאמרים') || reversed.includes('תשכ"ד') || reversed.includes('ש"פ צו') || reversed.includes('שבת הגדול') || reversed.includes("ניסן ה'תשכ\"ד")) continue; // כותרות עמוד שנשארו

       // מנקים רווחים כפולים (שקורים בעקבות תיקונים)
       reversed = reversed.replace(/\s{2,}/g, ' ').trim();

       // מסירים קווים ויזואלים (---------) שנשארו מההערות שוליים, אם איכשהו קיבלו גודל פונט 13
       if (reversed.includes('__') || reversed.includes('--')) continue;

       if (reversed.length < 2) continue;

       allParagraphs.push(reversed);
    }
  }

  let paragraphs = [];
  let currentPara = [];
  
  for (let l of allParagraphs) {
     currentPara.push(l);
     // חיבור שורות לכדי פסקה מחוברת ברגע שיש סימן סיום (כמו נקודה או נקודתיים)
     if (l.endsWith('.') || l.endsWith(':') || l.endsWith(';')) {
        paragraphs.push(currentPara.join(' '));
        currentPara = [];
     }
  }
  // הוספת הפסקה האחרונה
  if (currentPara.length > 0) paragraphs.push(currentPara.join(' '));

  return { text: paragraphs.join('\n\n'), numPages };
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
